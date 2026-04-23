/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.ai.ragent.rag.core.mcp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.infra.ai.domain.service.LLMService;
import com.nageoffer.ai.ragent.infra.ai.infra.util.LLMResponseCleaner;
import com.nageoffer.ai.ragent.rag.core.prompt.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nageoffer.ai.ragent.rag.constant.RAGConstant.MCP_PARAMETER_EXTRACT_PROMPT_PATH;

/**
 * 基于 LLM 的 MCP 参数提取器实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMMCPParameterExtractor implements MCPParameterExtractor {

    private final LLMService llmService;
    private final PromptTemplateLoader promptTemplateLoader;

    @Override
    public Map<String, Object> extractParameters(String userQuestion, MCPTool tool) {
        return extractParameters(userQuestion, tool, null);
    }

    @Override
    public Map<String, Object> extractParameters(String userQuestion, MCPTool tool, String customPromptTemplate) {
        if (tool == null || CollUtil.isEmpty(tool.getParameters())) {
            return Collections.emptyMap();
        }

        // 构建 Prompt：优先使用自定义提示词
        List<ChatMessage> messages = new ArrayList<>(3);
        String systemPrompt = StrUtil.isNotBlank(customPromptTemplate)
                ? customPromptTemplate
                : promptTemplateLoader.load(MCP_PARAMETER_EXTRACT_PROMPT_PATH);

        messages.add(ChatMessage.system(systemPrompt));
        messages.add(ChatMessage.user("工具定义如下：\n" + buildToolDefinition(tool)));
        messages.add(ChatMessage.user("请根据以上工具定义，从下面的问题中提取参数：\n" + userQuestion));

        String raw = null;
        try {
            // 调用 LLM 提取参数
            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .temperature(0.1D)
                    .topP(0.3D)
                    .thinking(false)
                    .build();
            raw = llmService.chat(request);
            log.info("MCP 参数提取 LLM 响应: {}", raw);

            // 解析 JSON 响应
            Map<String, Object> extracted = parseJsonResponse(raw, tool);

            // 填充默认值
            fillDefaults(extracted, tool);

            log.info("MCP 参数提取完成, toolId: {}, 使用自定义提示词: {}, 参数: {}",
                    tool.getToolId(), StrUtil.isNotBlank(customPromptTemplate), extracted);

            return extracted;
        } catch (JSONException e) {
            log.warn("MCP 参数提取-JSON解析失败, toolId: {}, 响应: {}", tool.getToolId(), raw, e);
            return buildDefaultParameters(tool);
        } catch (Exception e) {
            log.error("MCP 参数提取异常, toolId: {}", tool.getToolId(), e);
            return buildDefaultParameters(tool);
        }
    }

    private Map<String, Object> buildDefaultParameters(MCPTool tool) {
        Map<String, Object> defaultParams = new HashMap<>();
        fillDefaults(defaultParams, tool);
        return defaultParams;
    }

    /**
     * 构建工具定义描述（供 LLM 理解）
     */
    private String buildToolDefinition(MCPTool tool) {
        StringBuilder sb = new StringBuilder();
        sb.append("工具ID: ").append(tool.getToolId()).append("\n");
        sb.append("功能描述: ").append(tool.getDescription()).append("\n");
        sb.append("参数列表:\n");

        for (Map.Entry<String, MCPTool.ParameterDef> entry : tool.getParameters().entrySet()) {
            String paramName = entry.getKey();
            MCPTool.ParameterDef def = entry.getValue();

            sb.append("  - ").append(paramName);
            sb.append(" (类型: ").append(def.getType());
            sb.append(def.isRequired() ? ", 必填" : ", 可选");
            sb.append("): ").append(def.getDescription());

            if (def.getDefaultValue() != null) {
                sb.append(" [默认值: ").append(def.getDefaultValue()).append("]");
            }
            if (CollUtil.isNotEmpty(def.getEnumValues())) {
                sb.append(" [可选值: ").append(String.join(", ", def.getEnumValues())).append("]");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     */
    private Map<String, Object> parseJsonResponse(String raw, MCPTool tool) {
        if (StrUtil.isBlank(raw)) {
            return new HashMap<>();
        }
        // 清理可能的 markdown 代码块
        String cleaned = LLMResponseCleaner.stripMarkdownCodeFence(raw);
        JSONObject obj = JSON.parseObject(cleaned);
        if (obj == null) {
            log.warn("LLM 返回的不是 JSON 对象: {}", raw);
            return new HashMap<>();
        }
        Map<String, Object> result = new HashMap<>();
        // 只提取工具定义中声明的参数
        for (String paramName : tool.getParameters().keySet()) {
            if (obj.containsKey(paramName) && obj.get(paramName) != null) {
                Object value = obj.get(paramName);
                result.put(paramName, convertValue(value));
            }
        }
        return result;
    }

    /**
     * 转换值为普通 Java 对象
     */
    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            if (value instanceof Double d) {
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return null;
                }
                if (d == Math.floor(d)) {
                    if (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                        return d.intValue();
                    } else if (d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                        return d.longValue();
                    }
                }
            }
            return value;
        }
        if (value instanceof JSONArray arr) {
            return arr.toJavaList(Object.class);
        }
        if (value instanceof JSONObject jsonObj) {
            return new LinkedHashMap<>(jsonObj);
        }
        return value;
    }


    /**
     * 填充默认值
     */
    private void fillDefaults(Map<String, Object> params, MCPTool tool) {
        if (tool.getParameters() == null) {
            return;
        }

        for (Map.Entry<String, MCPTool.ParameterDef> entry : tool.getParameters().entrySet()) {
            String paramName = entry.getKey();
            MCPTool.ParameterDef def = entry.getValue();

            if (!params.containsKey(paramName) && def.getDefaultValue() != null) {
                params.put(paramName, def.getDefaultValue());
            }
        }
    }
}

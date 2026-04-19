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

package com.nageoffer.ai.ragent.chatbot.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nageoffer.ai.ragent.chatbot.common.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.common.MessageContext;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.infra.chat.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * LLM 问题检测器
 * <p>
 * 使用大模型语义分析判断消息是否为问题
 */
@Slf4j
@RequiredArgsConstructor
public class LlmQuestionDetector implements QuestionDetector {

    private final LLMService llmService;
    private final ChatbotProperties properties;

    private static final Gson GSON = new Gson();

    /**
     * LLM 检测的系统提示词
     */
    private static final String DETECTION_SYSTEM_PROMPT = """
            你是一个消息分析助手。请分析用户发送的消息，判断它是否是一个需要回答的问题。

            分析规则：
            1. 如果消息包含明确的疑问词（如"什么"、"怎么"、"如何"、"为什么"、"哪"等），认为是问题
            2. 如果消息以问号结尾，认为是问题
            3. 如果消息是在请求帮助或信息，认为是问题
            4. 如果消息只是陈述、通知或闲聊，认为不是问题
            5. 如果消息是命令式语句（如"打开xxx"、"关闭xxx"），认为不是问题

            请以 JSON 格式返回分析结果：
            {
              "is_question": true/false,
              "confidence": 0.0-1.0,
              "reason": "判断理由"
            }

            只返回 JSON，不要有其他内容。
            """;

    @Override
    public DetectionResult detect(String message, MessageContext context) {
        if (message == null || message.isBlank()) {
            return DetectionResult.notQuestion();
        }

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.system(DETECTION_SYSTEM_PROMPT),
                            ChatMessage.user(message)
                    ))
                    .temperature(0.1)
                    .maxTokens(200)
                    .build();

            String response = llmService.chat(request);
            return parseDetectionResult(response, message);
        } catch (Exception e) {
            log.warn("LLM 问题检测失败: {}", e.getMessage());
            // 降级：默认认为不是问题
            return DetectionResult.notQuestion();
        }
    }

    /**
     * 解析 LLM 返回的检测结果
     */
    private DetectionResult parseDetectionResult(String response, String originalMessage) {
        try {
            // 尝试提取 JSON
            String jsonStr = extractJson(response);
            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

            boolean isQuestion = json.has("is_question") && json.get("is_question").getAsBoolean();
            double confidence = json.has("confidence") ? json.get("confidence").getAsDouble() : 0.5;

            // 检查置信度阈值
            double threshold = properties.getDetection().getLlmThreshold();
            if (isQuestion && confidence >= threshold) {
                log.debug("LLM 检测为问题，置信度: {}", confidence);
                return DetectionResult.questionByLlm(originalMessage, confidence);
            }

            return DetectionResult.notQuestion();
        } catch (Exception e) {
            log.warn("解析 LLM 检测结果失败: {}", e.getMessage());
            return DetectionResult.notQuestion();
        }
    }

    /**
     * 从响应中提取 JSON 字符串
     */
    private String extractJson(String response) {
        // 如果响应本身就是 JSON
        if (response.trim().startsWith("{")) {
            int end = response.lastIndexOf('}');
            if (end > 0) {
                return response.trim().substring(0, end + 1);
            }
        }

        // 尝试查找 JSON 块
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        throw new IllegalArgumentException("无法从响应中提取 JSON");
    }
}

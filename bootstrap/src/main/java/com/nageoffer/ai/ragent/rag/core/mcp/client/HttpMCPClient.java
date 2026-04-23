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

package com.nageoffer.ai.ragent.rag.core.mcp.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nageoffer.ai.ragent.rag.core.mcp.MCPTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 OkHttp 的 MCP 客户端实现
 * 使用 Streamable HTTP 传输协议（JSON-RPC 2.0）与远程 MCP Server 通信
 */
@Slf4j
@RequiredArgsConstructor
public class HttpMCPClient implements MCPClient {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String INITIALIZED_NOTIFICATION_METHOD = "notifications/initialized";

    private final OkHttpClient httpClient;
    private final String serverUrl;
    private final AtomicLong requestId = new AtomicLong(1);

    @Override
    public boolean initialize() {
        JSONObject params = new JSONObject();
        params.put("protocolVersion", "2026-02-28");
        JSONObject clientInfo = new JSONObject();
        clientInfo.put("name", "ragent-bootstrap");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);

        JSONObject result = sendRequest("initialize", params);
        if (result == null) {
            log.error("MCP 初始化失败，跳过 initialized 通知发送");
            return false;
        }
        // MCP 协议要求：收到 initialize 响应后，发送 notifications/initialized 通知
        sendInitializedNotification();
        return true;
    }

    @Override
    public List<MCPTool> listTools() {
        JSONObject result = sendRequest("tools/list", new JSONObject());
        List<MCPTool> tools = new ArrayList<>();
        if (result == null || !result.containsKey("tools")) {
            return tools;
        }

        JSONArray toolsArray = result.getJSONArray("tools");
        if (toolsArray == null) {
            return tools;
        }
        for (int i = 0; i < toolsArray.size(); i++) {
            JSONObject toolObj = toolsArray.getJSONObject(i);
            MCPTool tool = convertToMcpTool(toolObj);
            tools.add(tool);
        }
        return tools;
    }

    @Override
    public String callTool(String toolName, Map<String, Object> arguments) {
        if (toolName == null || toolName.isEmpty()) {
            log.warn("MCP 工具调用失败，toolName 为空");
            return null;
        }

        JSONObject params = new JSONObject();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : new HashMap<>());

        JSONObject result = sendRequest("tools/call", params);
        if (result == null) {
            return null;
        }

        String textResult = extractTextContent(result);
        boolean isError = result.getBooleanValue("isError");
        if (isError) {
            log.warn("MCP 工具调用返回错误，toolName={}, errorText={}", toolName, textResult);
            return null;
        }
        return textResult;
    }

    /**
     * 发送 JSON-RPC 2.0 请求
     */
    private JSONObject sendRequest(String method, JSONObject params) {
        JSONObject rpcRequest = new JSONObject();
        rpcRequest.put("jsonrpc", "2.0");
        rpcRequest.put("id", requestId.getAndIncrement());
        rpcRequest.put("method", method);
        rpcRequest.put("params", params);

        String url = resolveMcpEndpointUrl();
        String requestBody = rpcRequest.toJSONString();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("MCP 请求失败，method={}, url={}, 原因=HTTP 状态码 {}", method, url, response.code());
                return null;
            }

            String body = response.body() != null ? response.body().string() : null;
            if (body == null) {
                log.warn("MCP 请求失败，method={}, url={}, 原因=响应体为空", method, url);
                return null;
            }

            JSONObject rpcResponse = JSON.parseObject(body);
            if (rpcResponse.containsKey("error") && rpcResponse.get("error") != null) {
                JSONObject error = rpcResponse.getJSONObject("error");
                log.error("MCP JSON-RPC 错误，method={}, code={}, message={}",
                        method, error.getInteger("code"), error.getString("message"));
                return null;
            }

            return rpcResponse.getJSONObject("result");
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("MCP 请求异常，method={}, url={}, 原因={}", method, url, reason);
            return null;
        }
    }

    /**
     * 发送 JSON-RPC 2.0 通知（无 id，不期望响应）
     */
    private void sendInitializedNotification() {
        String method = INITIALIZED_NOTIFICATION_METHOD;
        JSONObject notification = new JSONObject();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);

        String url = resolveMcpEndpointUrl();
        String body = notification.toJSONString();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("MCP 通知发送失败，method={}, HTTP 状态码={}", method, response.code());
            }
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("MCP 通知发送异常，method={}, 原因={}", method, reason);
        }
    }

    private String resolveMcpEndpointUrl() {
        return serverUrl.endsWith("/mcp") ? serverUrl : serverUrl + "/mcp";
    }

    private String extractTextContent(JSONObject result) {
        if (result == null || !result.containsKey("content")) {
            return null;
        }
        JSONArray content = result.getJSONArray("content");
        if (content == null) {
            return null;
        }
        List<String> textSegments = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            JSONObject contentObj = content.getJSONObject(i);
            if (contentObj != null && contentObj.containsKey("text")) {
                String text = contentObj.getString("text");
                if (text != null) {
                    textSegments.add(text);
                }
            }
        }
        if (textSegments.isEmpty()) {
            return null;
        }
        return String.join("\n", textSegments);
    }

    /**
     * 将 MCP 标准 Tool Schema 转换为 bootstrap 的 MCPTool
     */
    private MCPTool convertToMcpTool(JSONObject toolObj) {
        String name = toolObj.getString("name");
        if (name == null) {
            name = "";
        }
        String description = toolObj.getString("description");
        if (description == null) {
            description = "";
        }

        Map<String, MCPTool.ParameterDef> parameters = new HashMap<>();
        List<String> requiredList = new ArrayList<>();

        JSONObject inputSchema = toolObj.getJSONObject("inputSchema");
        if (inputSchema != null) {
            // 解析 required 列表
            JSONArray requiredArr = inputSchema.getJSONArray("required");
            if (requiredArr != null) {
                for (int i = 0; i < requiredArr.size(); i++) {
                    requiredList.add(requiredArr.getString(i));
                }
            }

            // 解析 properties
            JSONObject properties = inputSchema.getJSONObject("properties");
            if (properties != null) {
                for (String key : properties.keySet()) {
                    JSONObject propObj = properties.getJSONObject(key);
                    MCPTool.ParameterDef paramDef = MCPTool.ParameterDef.builder()
                            .type(propObj != null && propObj.containsKey("type") ? propObj.getString("type") : "string")
                            .description(propObj != null && propObj.containsKey("description") ? propObj.getString("description") : "")
                            .required(requiredList.contains(key))
                            .build();

                    // 解析枚举值
                    if (propObj != null) {
                        JSONArray enumArr = propObj.getJSONArray("enum");
                        if (enumArr != null) {
                            List<String> enumValues = new ArrayList<>();
                            for (int i = 0; i < enumArr.size(); i++) {
                                enumValues.add(enumArr.getString(i));
                            }
                            paramDef.setEnumValues(enumValues);
                        }
                    }

                    parameters.put(key, paramDef);
                }
            }
        }

        return MCPTool.builder()
                .toolId(name)
                .description(description)
                .parameters(parameters)
                .mcpServerUrl(serverUrl)
                .build();
    }
}

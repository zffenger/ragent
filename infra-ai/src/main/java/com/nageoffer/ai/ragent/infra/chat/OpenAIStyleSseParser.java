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

package com.nageoffer.ai.ragent.infra.chat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.NoArgsConstructor;

/**
 * OpenAI 协议风格 SSE 解析器
 * 支持从 delta/message 中提取 content，以及可选的 reasoning_content
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class OpenAIStyleSseParser {

    private static final String DATA_PREFIX = "data:";
    private static final String DONE_MARKER = "[DONE]";

    static ParsedEvent parseLine(String line, boolean reasoningEnabled) {
        if (line == null || line.isBlank()) {
            return ParsedEvent.empty();
        }

        String payload = line.trim();
        if (payload.startsWith(DATA_PREFIX)) {
            payload = payload.substring(DATA_PREFIX.length()).trim();
        }
        if (DONE_MARKER.equalsIgnoreCase(payload)) {
            return ParsedEvent.done();
        }

        JSONObject obj = JSON.parseObject(payload);
        JSONArray choices = obj.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return ParsedEvent.empty();
        }

        JSONObject choice0 = choices.getJSONObject(0);
        String content = extractText(choice0, "content");
        String reasoning = reasoningEnabled ? extractText(choice0, "reasoning_content") : null;
        boolean completed = hasFinishReason(choice0);

        return new ParsedEvent(content, reasoning, completed);
    }

    private static boolean hasFinishReason(JSONObject choice) {
        if (choice == null || !choice.containsKey("finish_reason")) {
            return false;
        }
        return choice.get("finish_reason") != null;
    }

    private static String extractText(JSONObject choice, String fieldName) {
        if (choice == null) {
            return null;
        }
        JSONObject delta = choice.getJSONObject("delta");
        if (delta != null && delta.containsKey(fieldName)) {
            String value = delta.getString(fieldName);
            if (value != null) {
                return value;
            }
        }
        JSONObject message = choice.getJSONObject("message");
        if (message != null && message.containsKey(fieldName)) {
            String value = message.getString(fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    record ParsedEvent(String content, String reasoning, boolean completed) {

        static ParsedEvent empty() {
            return new ParsedEvent(null, null, false);
        }

        static ParsedEvent done() {
            return new ParsedEvent(null, null, true);
        }

        boolean hasContent() {
            return content != null && !content.isEmpty();
        }

        boolean hasReasoning() {
            return reasoning != null && !reasoning.isEmpty();
        }
    }
}

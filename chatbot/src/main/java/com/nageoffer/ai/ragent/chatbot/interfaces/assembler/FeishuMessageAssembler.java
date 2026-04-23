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

package com.nageoffer.ai.ragent.chatbot.interfaces.assembler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import com.nageoffer.ai.ragent.chatbot.domain.vo.BotPlatform;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.feishu.FeishuMessageDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书消息组装器
 */
@Slf4j
public final class FeishuMessageAssembler {

    private FeishuMessageAssembler() {
    }

    public static MessageContext toMessageContext(FeishuMessageDTO message, String content, BotConfig botConfig) {
        String botName = botConfig.getName() != null ? botConfig.getName() : "智能助手";

        return MessageContext.builder()
                .platform(BotPlatform.FEISHU)
                .chatType(message.getChatType())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .atBot(content.contains("@" + botName))
                .botName(botName)
                .rawContent(content)
                .messageId(message.getMessageId())
                .timestamp(message.getCreateTime())
                .botConfig(botConfig)
                .build();
    }

    public static String extractTextContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            if (content.startsWith("{")) {
                JSONObject json = JSON.parseObject(content);
                if (json != null && json.containsKey("text")) {
                    return json.getString("text");
                }
            }
            return content;
        } catch (Exception e) {
            log.debug("解析消息内容失败，直接返回原始内容: {}", e.getMessage());
            return content;
        }
    }
}

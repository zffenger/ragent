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

package com.nageoffer.ai.ragent.chatbot.domain.entity;

import com.nageoffer.ai.ragent.chatbot.domain.vo.BotPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息上下文
 * <p>
 * 封装消息处理的上下文信息，包括来源平台、聊天类型、发送者信息等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageContext {

    /**
     * 消息来源平台
     */
    private BotPlatform platform;

    /**
     * 聊天类型：p2p（单聊）、group（群聊）
     */
    private String chatType;

    /**
     * 聊天 ID（会话 ID）
     */
    private String chatId;

    /**
     * 消息发送者 ID
     */
    private String senderId;

    /**
     * 消息发送者名称
     */
    private String senderName;

    /**
     * 发送者类型
     */
    private String senderType;

    /**
     * 是否 @了机器人
     */
    private boolean atBot;

    /**
     * 机器人名称
     */
    private String botName;

    /**
     * 原始消息内容
     */
    private String rawContent;

    /**
     * 消息 ID
     */
    private String messageId;

    /**
     * 消息时间戳
     */
    private Long timestamp;

    /**
     * 机器人配置
     */
    private BotConfig botConfig;

    /**
     * 是否为单聊
     */
    public boolean isPrivateChat() {
        return "p2p".equals(chatType) || "private".equals(chatType);
    }

    /**
     * 是否为群聊
     */
    public boolean isGroupChat() {
        return "group".equals(chatType);
    }

    /**
     * 获取会话唯一标识（用于 RAG 会话管理）
     */
    public String getConversationId() {
        return platform.getCode() + "_" + chatId;
    }
}

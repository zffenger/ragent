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

package com.nageoffer.ai.ragent.chatbot.wework.dto;

import lombok.Data;

/**
 * 企业微信事件 DTO
 * <p>
 * 企业微信回调推送的 XML 事件结构
 */
@Data
public class WeWorkEvent {

    /**
     * 企业微信 CorpId
     */
    private String toUserName;

    /**
     * 发送方 ID（成员 UserID 或群聊 ID）
     */
    private String fromUserName;

    /**
     * 消息创建时间戳
     */
    private Long createTime;

    /**
     * 消息类型
     * 如：text, event 等
     */
    private String msgType;

    /**
     * 事件类型（仅事件消息有）
     * 如：enter_chat（进入会话）, location_report（位置上报）等
     */
    private String event;

    /**
     * 消息 ID
     */
    private String msgId;

    /**
     * 应用 AgentId
     */
    private String agentId;

    /**
     * 是否群聊消息
     */
    private String chatType;

    /**
     * 群聊 ID（群聊消息有）
     */
    private String chatId;

    // ========== 文本消息字段 ==========

    /**
     * 文本消息内容
     */
    private String content;

    // ========== 事件消息字段 ==========

    /**
     * 事件 Key 值
     */
    private String eventKey;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 外部联系人 ID
     */
    private String externalUserId;
}

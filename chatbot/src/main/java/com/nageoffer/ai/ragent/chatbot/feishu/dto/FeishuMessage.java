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

package com.nageoffer.ai.ragent.chatbot.feishu.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * 飞书消息 DTO
 * <p>
 * 飞书消息事件的详细内容
 */
@Data
public class FeishuMessage {

    /**
     * 消息类型
     * 如：text, post, image, file 等
     */
    @SerializedName("message_type")
    private String msgType;

    /**
     * 消息内容（JSON 字符串）
     * 文本消息格式：{"text":"消息内容"}
     */
    private String content;

    /**
     * 消息 ID
     */
    @SerializedName("message_id")
    private String messageId;

    /**
     * 根消息 ID（回复消息时）
     */
    @SerializedName("root_id")
    private String rootId;

    /**
     * 父消息 ID（回复消息时）
     */
    @SerializedName("parent_id")
    private String parentId;

    /**
     * 聊天类型
     * p2p: 单聊
     * group: 群聊
     */
    @SerializedName("chat_type")
    private String chatType;

    /**
     * 聊天 ID
     */
    @SerializedName("chat_id")
    private String chatId;

    /**
     * 消息创建时间戳
     */
    @SerializedName("create_time")
    private Long createTime;

    /**
     * 消息更新时间戳
     */
    @SerializedName("update_time")
    private Long updateTime;

    /**
     * 是否已删除
     */
    private Boolean deleted;

    /**
     * 是否已撤回
     */
    private Boolean recalled;

    /**
     * 发送者信息
     */
    private Sender sender;

    /**
     * 发送者 ID（便捷方法）
     */
    public String getSenderId() {
        return sender != null ? sender.getSenderId() : null;
    }

    /**
     * 发送者类型
     */
    public String getSenderType() {
        return sender != null ? sender.getSenderType() : null;
    }

    /**
     * 发送者租户 Key
     */
    public String getTenantKey() {
        return sender != null ? sender.getTenantKey() : null;
    }

    /**
     * 发送者信息
     */
    @Data
    public static class Sender {
        /**
         * 发送者 ID
         */
        @SerializedName("sender_id")
        private String senderId;

        /**
         * 发送者类型
         * open_id, user_id, union_id, app_id 等
         */
        @SerializedName("sender_type")
        private String senderType;

        /**
         * 租户 Key
         */
        @SerializedName("tenant_key")
        private String tenantKey;
    }
}

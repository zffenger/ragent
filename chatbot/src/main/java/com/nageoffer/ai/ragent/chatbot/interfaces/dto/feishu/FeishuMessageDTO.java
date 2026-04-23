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

package com.nageoffer.ai.ragent.chatbot.interfaces.dto.feishu;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * 飞书消息事件 DTO
 * <p>
 * 飞书 Webhook 推送的消息事件结构（schema 2.0）
 */
@Data
public class FeishuMessageDTO {

    /**
     * 消息详情
     */
    private Message message;

    /**
     * 发送者信息
     */
    private Sender sender;

    // ==================== 便捷方法 ====================

    /**
     * 消息类型
     */
    public String getMsgType() {
        return message != null ? message.getMsgType() : null;
    }

    /**
     * 消息内容
     */
    public String getContent() {
        return message != null ? message.getContent() : null;
    }

    /**
     * 消息 ID
     */
    public String getMessageId() {
        return message != null ? message.getMessageId() : null;
    }

    /**
     * 聊天类型
     */
    public String getChatType() {
        return message != null ? message.getChatType() : null;
    }

    /**
     * 聊天 ID
     */
    public String getChatId() {
        return message != null ? message.getChatId() : null;
    }

    /**
     * 消息创建时间戳
     */
    public Long getCreateTime() {
        return message != null ? message.getCreateTime() : null;
    }

    /**
     * 是否已删除
     */
    public Boolean getDeleted() {
        return message != null ? message.getDeleted() : null;
    }

    /**
     * 是否已撤回
     */
    public Boolean getRecalled() {
        return message != null ? message.getRecalled() : null;
    }

    /**
     * 发送者 ID（优先返回 open_id）
     */
    public String getSenderId() {
        if (sender == null || sender.getSenderId() == null) {
            return null;
        }
        SenderId senderId = sender.getSenderId();
        // 优先返回 open_id
        if (senderId.getOpenId() != null) {
            return senderId.getOpenId();
        }
        if (senderId.getUserId() != null) {
            return senderId.getUserId();
        }
        return senderId.getUnionId();
    }

    /**
     * 发送者类型
     */
    public String getSenderType() {
        return sender != null ? sender.getSenderType() : null;
    }

    /**
     * 租户 Key
     */
    public String getTenantKey() {
        return sender != null ? sender.getTenantKey() : null;
    }

    // ==================== 内部类 ====================

    /**
     * 消息详情
     */
    @Data
    public static class Message {
        /**
         * 聊天 ID
         */
        @JSONField(name = "chat_id")
        private String chatId;

        /**
         * 聊天类型
         * p2p: 单聊
         * group: 群聊
         */
        @JSONField(name = "chat_type")
        private String chatType;

        /**
         * 消息内容（JSON 字符串）
         * 文本消息格式：{"text":"消息内容"}
         */
        private String content;

        /**
         * 消息创建时间戳
         */
        @JSONField(name = "create_time")
        private Long createTime;

        /**
         * 消息 ID
         */
        @JSONField(name = "message_id")
        private String messageId;

        /**
         * 消息类型
         * text, post, image, file 等
         */
        @JSONField(name = "message_type")
        private String msgType;

        /**
         * 消息更新时间戳
         */
        @JSONField(name = "update_time")
        private Long updateTime;

        /**
         * 是否已删除
         */
        private Boolean deleted;

        /**
         * 是否已撤回
         */
        private Boolean recalled;
    }

    /**
     * 发送者信息
     */
    @Data
    public static class Sender {
        /**
         * 发送者 ID（包含多种 ID 格式）
         */
        @JSONField(name = "sender_id")
        private SenderId senderId;

        /**
         * 发送者类型
         * user, app 等
         */
        @JSONField(name = "sender_type")
        private String senderType;

        /**
         * 租户 Key
         */
        @JSONField(name = "tenant_key")
        private String tenantKey;
    }

    /**
     * 发送者 ID（多种格式）
     */
    @Data
    public static class SenderId {
        /**
         * Open ID
         */
        @JSONField(name = "open_id")
        private String openId;

        /**
         * Union ID
         */
        @JSONField(name = "union_id")
        private String unionId;

        /**
         * User ID
         */
        @JSONField(name = "user_id")
        private String userId;
    }
}

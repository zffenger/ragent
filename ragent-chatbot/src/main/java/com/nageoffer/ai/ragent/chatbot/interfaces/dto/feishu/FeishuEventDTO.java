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
 * 飞书事件 DTO
 * <p>
 * 飞书 Webhook 推送的事件结构（schema 2.0）
 */
@Data
public class FeishuEventDTO {

    /**
     * 事件 Schema 版本
     */
    private String schema;

    /**
     * 事件头部信息
     */
    private Header header;

    /**
     * 事件详细内容
     */
    private FeishuMessageDTO event;

    // ==================== 便捷方法 ====================

    /**
     * 事件类型
     */
    public String getType() {
        return header != null ? header.getEventType() : null;
    }

    /**
     * URL 验证时返回的 challenge 字符串
     */
    public String getChallenge() {
        return null;
    }

    /**
     * 事件 ID
     */
    public String getEventId() {
        return header != null ? header.getEventId() : null;
    }

    /**
     * 事件时间戳
     */
    public Long getTs() {
        return header != null ? header.getCreateTime() : null;
    }

    /**
     * 应用 ID
     */
    public String getAppId() {
        return header != null ? header.getAppId() : null;
    }

    /**
     * 租户 Key
     */
    public String getTenantKey() {
        return header != null ? header.getTenantKey() : null;
    }

    // ==================== 内部类 ====================

    /**
     * 事件头部信息
     */
    @Data
    public static class Header {
        /**
         * 事件 ID
         */
        @JSONField(name = "event_id")
        private String eventId;

        /**
         * 验证令牌
         */
        private String token;

        /**
         * 事件创建时间戳
         */
        @JSONField(name = "create_time")
        private Long createTime;

        /**
         * 事件类型
         * 如：im.message.receive_v1
         */
        @JSONField(name = "event_type")
        private String eventType;

        /**
         * 租户 Key
         */
        @JSONField(name = "tenant_key")
        private String tenantKey;

        /**
         * 应用 ID
         */
        @JSONField(name = "app_id")
        private String appId;
    }
}

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
 * 飞书事件 DTO
 * <p>
 * 飞书 Webhook 推送的事件结构
 */
@Data
public class FeishuEvent {

    /**
     * 事件类型
     * 如：url_verification, im.message.receive_v1
     */
    private String type;

    /**
     * 事件模式
     * webhook 模式下为 "webhook"
     */
    private String mode;

    /**
     * URL 验证时返回的 challenge 字符串
     */
    private String challenge;

    /**
     * 事件 ID
     */
    @SerializedName("event_id")
    private String eventId;

    /**
     * 事件时间戳
     */
    private Long ts;

    /**
     * 事件详细内容
     */
    private FeishuMessage event;

    /**
     * 应用 ID
     */
    @SerializedName("app_id")
    private String appId;

    /**
     * 租户 Key
     */
    @SerializedName("tenant_key")
    private String tenantKey;
}

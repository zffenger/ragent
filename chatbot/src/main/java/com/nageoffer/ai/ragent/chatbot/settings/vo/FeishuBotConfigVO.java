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

package com.nageoffer.ai.ragent.chatbot.settings.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书机器人配置 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuBotConfigVO {

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 飞书应用 ID
     */
    private String appId;

    /**
     * 飞书应用密钥
     */
    private String appSecret;

    /**
     * 加密密钥
     */
    private String encryptKey;

    /**
     * 验证令牌
     */
    private String verificationToken;

    /**
     * 机器人名称
     */
    private String botName;
}

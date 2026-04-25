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

package com.nageoffer.ai.ragent.settings.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 聊天机器人 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatBotVO {

    /**
     * 主键 ID
     */
    private String id;

    /**
     * 机器人名称
     */
    private String name;

    /**
     * 平台：FEISHU/WEWORK
     */
    private String platform;

    /**
     * 机器人描述
     */
    private String description;

    // ==================== 平台配置 ====================

    /**
     * 应用 ID（飞书）
     */
    private String appId;

    /**
     * 应用密钥
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
     * 企业 ID（企微）
     */
    private String corpId;

    /**
     * 应用 AgentId（企微）
     */
    private String agentId;

    /**
     * 回调 Token（企微）
     */
    private String token;

    /**
     * EncodingAESKey（企微）
     */
    private String encodingAesKey;

    /**
     * 机器人显示名称
     */
    private String botName;

    // ==================== 检索配置 ====================

    /**
     * 绑定的检索域 ID
     */
    private String domainId;

    /**
     * 绑定的检索域名称
     */
    private String domainName;

    /**
     * 问题检测模式：KEYWORD/LLM/COMPOSITE
     */
    private String detectionMode;

    /**
     * 检测关键词列表
     */
    private List<String> detectionKeywords;

    /**
     * 是否启用 @触发
     */
    private Boolean atTriggerEnabled;

    /**
     * LLM 检测置信度阈值
     */
    private BigDecimal llmThreshold;

    /**
     * 回答模式：LLM/RAG
     */
    private String answerMode;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 最大 Token 数
     */
    private Integer maxTokens;

    // ==================== 状态 ====================

    /**
     * 是否启用
     */
    private Boolean enabled;
}

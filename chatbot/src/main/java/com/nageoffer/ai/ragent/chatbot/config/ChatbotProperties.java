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

package com.nageoffer.ai.ragent.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 机器人模块配置属性
 * <p>
 * 用于配置飞书和企微机器人的连接参数、问题检测策略和回答生成方式
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chatbot")
public class ChatbotProperties {

    /**
     * 是否启用机器人模块
     */
    private boolean enabled = false;

    /**
     * 飞书机器人配置
     */
    private FeishuConfig feishu = new FeishuConfig();

    /**
     * 企微机器人配置
     */
    private WeWorkConfig wework = new WeWorkConfig();

    /**
     * 问题检测配置
     */
    private DetectionConfig detection = new DetectionConfig();

    /**
     * 回答生成配置
     */
    private AnswerConfig answer = new AnswerConfig();

    /**
     * 飞书机器人配置
     */
    @Data
    public static class FeishuConfig {
        /**
         * 是否启用飞书机器人
         */
        private boolean enabled = false;

        /**
         * 飞书应用 ID
         */
        private String appId;

        /**
         * 飞书应用密钥
         */
        private String appSecret;

        /**
         * 加密密钥（可选，用于消息加密）
         */
        private String encryptKey;

        /**
         * 验证令牌（可选，用于签名验证）
         */
        private String verificationToken;

        /**
         * 机器人名称（用于识别 @ 事件）
         */
        private String botName = "智能助手";
    }

    /**
     * 企微机器人配置
     */
    @Data
    public static class WeWorkConfig {
        /**
         * 是否启用企微机器人
         */
        private boolean enabled = false;

        /**
         * 企业 ID
         */
        private String corpId;

        /**
         * 应用 AgentId
         */
        private String agentId;

        /**
         * 应用 Secret
         */
        private String secret;

        /**
         * 回调配置 Token
         */
        private String token;

        /**
         * 回调配置 EncodingAESKey
         */
        private String encodingAesKey;

        /**
         * 机器人名称（用于识别 @ 事件）
         */
        private String botName = "智能助手";
    }

    /**
     * 问题检测配置
     */
    @Data
    public static class DetectionConfig {
        /**
         * 检测模式：KEYWORD（关键词）、LLM（大模型）、COMPOSITE（组合）
         */
        private DetectionMode mode = DetectionMode.COMPOSITE;

        /**
         * 触发关键词列表
         */
        private List<String> keywords = List.of(
                "?", "？", "请", "怎么", "如何", "什么", "为什么", "能不能", "可以吗", "吗"
        );

        /**
         * 是否启用 @机器人 触发
         */
        private boolean atTriggerEnabled = true;

        /**
         * LLM 检测的置信度阈值（0-1）
         */
        private double llmThreshold = 0.7;
    }

    /**
     * 回答生成配置
     */
    @Data
    public static class AnswerConfig {
        /**
         * 回答生成模式：LLM（直接调用大模型）、RAG（检索增强生成）
         */
        private AnswerMode mode = AnswerMode.RAG;

        /**
         * 默认系统提示词（LLM 模式使用）
         */
        private String defaultSystemPrompt = "你是一个智能客服助手，请简洁准确地回答用户问题。";

        /**
         * 最大 Token 数
         */
        private int maxTokens = 2000;
    }

    /**
     * 问题检测模式
     */
    public enum DetectionMode {
        /**
         * 仅关键词检测
         */
        KEYWORD,
        /**
         * 仅 LLM 语义检测
         */
        LLM,
        /**
         * 组合模式：先关键词，不确定时用 LLM
         */
        COMPOSITE
    }

    /**
     * 回答生成模式
     */
    public enum AnswerMode {
        /**
         * 直接调用 LLM 生成回答
         */
        LLM,
        /**
         * 使用 RAG 检索增强生成
         */
        RAG
    }
}

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

import com.nageoffer.ai.ragent.chatbot.core.CompositeQuestionDetector;
import com.nageoffer.ai.ragent.chatbot.core.KeywordQuestionDetector;
import com.nageoffer.ai.ragent.chatbot.core.LlmQuestionDetector;
import com.nageoffer.ai.ragent.chatbot.core.QuestionDetector;
import com.nageoffer.ai.ragent.chatbot.feishu.FeishuApiClient;
import com.nageoffer.ai.ragent.chatbot.feishu.FeishuMessageHandler;
import com.nageoffer.ai.ragent.chatbot.feishu.FeishuSignatureValidator;
import com.nageoffer.ai.ragent.chatbot.service.AnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.service.LlmAnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.wework.WeWorkApiClient;
import com.nageoffer.ai.ragent.chatbot.wework.WeWorkMessageHandler;
import com.nageoffer.ai.ragent.chatbot.wework.WeWorkSignatureValidator;
import com.nageoffer.ai.ragent.infra.chat.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 机器人模块自动配置类
 * <p>
 * 根据配置自动装配飞书/企微机器人相关组件
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChatbotProperties.class)
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ChatbotAutoConfiguration {

    // ==================== 问题检测器 ====================

    /**
     * 关键词问题检测器
     */
    @Bean
    @ConditionalOnMissingBean
    public KeywordQuestionDetector keywordQuestionDetector(ChatbotProperties properties) {
        log.info("初始化关键词问题检测器");
        return new KeywordQuestionDetector(properties);
    }

    /**
     * LLM 问题检测器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "chatbot.detection", name = "mode", havingValue = "LLM")
    public LlmQuestionDetector llmQuestionDetector(LLMService llmService, ChatbotProperties properties) {
        log.info("初始化 LLM 问题检测器");
        return new LlmQuestionDetector(llmService, properties);
    }

    /**
     * 组合问题检测器（默认）
     */
    @Bean
    @ConditionalOnMissingBean(QuestionDetector.class)
    @ConditionalOnProperty(prefix = "chatbot.detection", name = "mode", havingValue = "COMPOSITE", matchIfMissing = true)
    public CompositeQuestionDetector compositeQuestionDetector(
            KeywordQuestionDetector keywordDetector,
            LLMService llmService,
            ChatbotProperties properties) {
        log.info("初始化组合问题检测器");
        LlmQuestionDetector llmDetector = new LlmQuestionDetector(llmService, properties);
        return new CompositeQuestionDetector(keywordDetector, llmDetector, properties);
    }

    // ==================== 回答生成器 ====================

    /**
     * LLM 回答生成器
     */
    @Bean
    @ConditionalOnMissingBean(AnswerGenerator.class)
    @ConditionalOnProperty(prefix = "chatbot.answer", name = "mode", havingValue = "LLM")
    public LlmAnswerGenerator llmAnswerGenerator(LLMService llmService, ChatbotProperties properties) {
        log.info("初始化 LLM 回答生成器");
        return new LlmAnswerGenerator(llmService, properties);
    }

    // RAG 回答生成器在 RagAnswerGenerator.java 中通过 @Component 注解

    // ==================== 飞书机器人组件 ====================

    /**
     * 飞书签名验证器
     */
    @Bean
    @ConditionalOnProperty(prefix = "chatbot.feishu", name = "enabled", havingValue = "true")
    public FeishuSignatureValidator feishuSignatureValidator(ChatbotProperties properties) {
        log.info("初始化飞书签名验证器");
        return new FeishuSignatureValidator(properties);
    }

    /**
     * 飞书 API 客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "chatbot.feishu", name = "enabled", havingValue = "true")
    public FeishuApiClient feishuApiClient(
            okhttp3.OkHttpClient okHttpClient,
            StringRedisTemplate stringRedisTemplate,
            ChatbotProperties properties) {
        log.info("初始化飞书 API 客户端");
        return new FeishuApiClient(okHttpClient, stringRedisTemplate, properties);
    }

    /**
     * 飞书消息处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "chatbot.feishu", name = "enabled", havingValue = "true")
    public FeishuMessageHandler feishuMessageHandler(
            QuestionDetector questionDetector,
            AnswerGenerator answerGenerator,
            FeishuApiClient feishuApiClient,
            ChatbotProperties properties) {
        log.info("初始化飞书消息处理器");
        return new FeishuMessageHandler(questionDetector, answerGenerator, feishuApiClient, properties);
    }

    // ==================== 企微机器人组件 ====================

    /**
     * 企微签名验证器
     */
    @Bean
    @ConditionalOnProperty(prefix = "chatbot.wework", name = "enabled", havingValue = "true")
    public WeWorkSignatureValidator weWorkSignatureValidator(ChatbotProperties properties) {
        log.info("初始化企微签名验证器");
        return new WeWorkSignatureValidator(properties);
    }

    /**
     * 企微 API 客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "chatbot.wework", name = "enabled", havingValue = "true")
    public WeWorkApiClient weWorkApiClient(
            okhttp3.OkHttpClient okHttpClient,
            StringRedisTemplate stringRedisTemplate,
            ChatbotProperties properties) {
        log.info("初始化企微 API 客户端");
        return new WeWorkApiClient(okHttpClient, stringRedisTemplate, properties);
    }

    /**
     * 企微消息处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "chatbot.wework", name = "enabled", havingValue = "true")
    public WeWorkMessageHandler weWorkMessageHandler(
            QuestionDetector questionDetector,
            AnswerGenerator answerGenerator,
            WeWorkApiClient weWorkApiClient,
            ChatbotProperties properties) {
        log.info("初始化企微消息处理器");
        return new WeWorkMessageHandler(questionDetector, answerGenerator, weWorkApiClient, properties);
    }
}

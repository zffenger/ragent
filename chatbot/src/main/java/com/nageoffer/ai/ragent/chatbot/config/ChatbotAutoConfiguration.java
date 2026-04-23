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
import com.nageoffer.ai.ragent.chatbot.feishu.FeishuApiClientFactory;
import com.nageoffer.ai.ragent.chatbot.feishu.FeishuMessageHandler;
import com.nageoffer.ai.ragent.chatbot.service.AnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.service.LlmAnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.wework.WeWorkApiClientFactory;
import com.nageoffer.ai.ragent.chatbot.wework.WeWorkMessageHandler;
import com.nageoffer.ai.ragent.infra.chat.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 机器人模块自动配置类
 * <p>
 * 机器人配置存储在数据库 t_chat_bot 表中，支持多机器人动态配置
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotAutoConfiguration {

    // ==================== 问题检测器 ====================

    /**
     * 关键词问题检测器
     */
    @Bean
    @ConditionalOnMissingBean
    public KeywordQuestionDetector keywordQuestionDetector() {
        log.info("初始化关键词问题检测器");
        return new KeywordQuestionDetector();
    }

    /**
     * 组合问题检测器（默认）
     */
    @Bean
    @ConditionalOnMissingBean(QuestionDetector.class)
    public CompositeQuestionDetector compositeQuestionDetector(
            KeywordQuestionDetector keywordDetector,
            LLMService llmService) {
        log.info("初始化组合问题检测器");
        LlmQuestionDetector llmDetector = new LlmQuestionDetector(llmService);
        return new CompositeQuestionDetector(keywordDetector, llmDetector);
    }

    // ==================== 回答生成器 ====================

    /**
     * LLM 回答生成器
     */
    @Bean
    @ConditionalOnMissingBean(AnswerGenerator.class)
    public LlmAnswerGenerator llmAnswerGenerator(LLMService llmService) {
        log.info("初始化 LLM 回答生成器");
        return new LlmAnswerGenerator(llmService);
    }

    // RAG 回答生成器在 RagAnswerGenerator.java 中通过 @Component 注解

    // ==================== 飞书机器人组件 ====================

    /**
     * 飞书 API 客户端工厂
     */
    @Bean
    public FeishuApiClientFactory feishuApiClientFactory(
            okhttp3.OkHttpClient okHttpClient,
            StringRedisTemplate stringRedisTemplate) {
        log.info("初始化飞书 API 客户端工厂");
        return new FeishuApiClientFactory(okHttpClient, stringRedisTemplate);
    }

    /**
     * 飞书消息处理器
     */
    @Bean
    public FeishuMessageHandler feishuMessageHandler(
            QuestionDetector questionDetector,
            AnswerGenerator answerGenerator,
            FeishuApiClientFactory apiClientFactory) {
        log.info("初始化飞书消息处理器");
        return new FeishuMessageHandler(questionDetector, answerGenerator, apiClientFactory);
    }

    // ==================== 企微机器人组件 ====================

    /**
     * 企微 API 客户端工厂
     */
    @Bean
    public WeWorkApiClientFactory weWorkApiClientFactory(
            okhttp3.OkHttpClient okHttpClient,
            StringRedisTemplate stringRedisTemplate) {
        log.info("初始化企微 API 客户端工厂");
        return new WeWorkApiClientFactory(okHttpClient, stringRedisTemplate);
    }

    /**
     * 企微消息处理器
     */
    @Bean
    public WeWorkMessageHandler weWorkMessageHandler(
            QuestionDetector questionDetector,
            AnswerGenerator answerGenerator) {
        log.info("初始化企微消息处理器");
        return new WeWorkMessageHandler(questionDetector, answerGenerator);
    }
}

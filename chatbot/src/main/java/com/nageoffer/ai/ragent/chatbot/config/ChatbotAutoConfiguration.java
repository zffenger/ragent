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

import com.nageoffer.ai.ragent.chatbot.application.MessageProcessApplication;
import com.nageoffer.ai.ragent.chatbot.domain.service.AnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.domain.service.QuestionDetector;
import com.nageoffer.ai.ragent.chatbot.infra.adapter.feishu.FeishuApiClientFactory;
import com.nageoffer.ai.ragent.chatbot.infra.adapter.feishu.FeishuMessageSender;
import com.nageoffer.ai.ragent.chatbot.infra.adapter.wework.WeWorkApiClientFactory;
import com.nageoffer.ai.ragent.chatbot.infra.llm.LlmAnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.infra.llm.LlmQuestionDetector;
import com.nageoffer.ai.ragent.infra.chat.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 机器人模块自动配置类
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(QuestionDetector.class)
    public LlmQuestionDetector llmQuestionDetector(LLMService llmService) {
        log.info("初始化 LLM 问题检测器");
        return new LlmQuestionDetector(llmService);
    }

    @Bean
    @ConditionalOnMissingBean(AnswerGenerator.class)
    public LlmAnswerGenerator llmAnswerGenerator(LLMService llmService) {
        log.info("初始化 LLM 回答生成器");
        return new LlmAnswerGenerator(llmService);
    }

    @Bean
    public MessageProcessApplication messageProcessApplication(
            QuestionDetector questionDetector,
            AnswerGenerator answerGenerator,
            FeishuMessageSender feishuMessageSender) {
        log.info("初始化消息处理应用服务");
        return new MessageProcessApplication(questionDetector, answerGenerator, feishuMessageSender);
    }

    @Bean
    public FeishuApiClientFactory feishuApiClientFactory(
            okhttp3.OkHttpClient okHttpClient,
            StringRedisTemplate stringRedisTemplate) {
        log.info("初始化飞书 API 客户端工厂");
        return new FeishuApiClientFactory(okHttpClient, stringRedisTemplate);
    }

    @Bean
    public WeWorkApiClientFactory weWorkApiClientFactory(
            okhttp3.OkHttpClient okHttpClient,
            StringRedisTemplate stringRedisTemplate) {
        log.info("初始化企微 API 客户端工厂");
        return new WeWorkApiClientFactory(okHttpClient, stringRedisTemplate);
    }
}

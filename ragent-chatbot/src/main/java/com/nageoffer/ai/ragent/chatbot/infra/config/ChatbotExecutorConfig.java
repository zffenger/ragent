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

package com.nageoffer.ai.ragent.chatbot.infra.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 机器人模块线程池配置
 * <p>
 * 为消息处理和回答生成提供独立的线程池，使用 TTL 包装确保上下文透传
 */
@Configuration
public class ChatbotExecutorConfig {

    /**
     * 消息处理线程池
     * <p>
     * 用于异步处理飞书/企微 Webhook 消息
     */
    @Bean("chatbotMessageExecutor")
    public Executor chatbotMessageExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4,
                16,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new CustomizableThreadFactory("chatbot-msg-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 使用 TTL 包装，确保上下文透传
        return TtlExecutors.getTtlExecutor(executor);
    }

    /**
     * 回答生成线程池
     * <p>
     * 用于异步生成回答（特别是 RAG 模式下可能耗时较长）
     */
    @Bean("chatbotAnswerExecutor")
    public Executor chatbotAnswerExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                8,
                120L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new CustomizableThreadFactory("chatbot-answer-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 使用 TTL 包装，确保上下文透传
        return TtlExecutors.getTtlExecutor(executor);
    }
}

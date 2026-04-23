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

package com.nageoffer.ai.ragent.chatbot.service;

import com.nageoffer.ai.ragent.chatbot.common.BotConfig;
import com.nageoffer.ai.ragent.chatbot.common.MessageContext;
import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.infra.chat.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * LLM 回答生成器
 * <p>
 * 直接调用 LLM 生成回答，不进行知识库检索
 */
@Slf4j
@RequiredArgsConstructor
public class LlmAnswerGenerator implements AnswerGenerator {

    private final LLMService llmService;

    /**
     * 默认系统提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个智能客服助手，请简洁准确地回答用户问题。";

    /**
     * 默认最大 Token 数
     */
    private static final int DEFAULT_MAX_TOKENS = 2000;

    @Override
    public String generate(String question, MessageContext context) {
        log.info("使用 LLM 生成回答，问题: {}", question);

        BotConfig botConfig = context.getBotConfig();
        String systemPrompt = botConfig != null && botConfig.getSystemPrompt() != null
                ? botConfig.getSystemPrompt()
                : DEFAULT_SYSTEM_PROMPT;
        int maxTokens = botConfig != null && botConfig.getMaxTokens() != null
                ? botConfig.getMaxTokens()
                : DEFAULT_MAX_TOKENS;

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system(systemPrompt),
                        ChatMessage.user(question)
                ))
                .maxTokens(maxTokens)
                .temperature(0.3)
                .topP(0.8)
                .build();

        try {
            String answer = llmService.chat(request);
            log.debug("LLM 回答生成完成，长度: {}", answer != null ? answer.length() : 0);
            return answer;
        } catch (Exception e) {
            log.error("LLM 回答生成失败: {}", e.getMessage(), e);
            return "抱歉，我暂时无法回答这个问题，请稍后再试。";
        }
    }
}

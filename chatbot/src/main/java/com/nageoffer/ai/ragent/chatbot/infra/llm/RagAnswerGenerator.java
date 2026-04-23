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

package com.nageoffer.ai.ragent.chatbot.infra.llm;

import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import com.nageoffer.ai.ragent.chatbot.domain.service.AnswerGenerator;
import com.nageoffer.ai.ragent.framework.convention.ChatMessage;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.infra.ai.domain.service.LLMService;
import com.nageoffer.ai.ragent.infra.ai.domain.service.StreamCallback;
import com.nageoffer.ai.ragent.infra.ai.domain.service.StreamCancellationHandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RAG 回答生成器
 * <p>
 * 使用 RAG 检索增强生成回答，基于知识库检索提供更准确的答案
 * <p>
 * 注意：此实现使用 LLMService 进行回答生成，可根据需要扩展集成 RAGChatService
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chatbot.answer", name = "mode", havingValue = "RAG", matchIfMissing = true)
public class RagAnswerGenerator implements AnswerGenerator {

    private final LLMService llmService;

    /**
     * 回答生成的超时时间（秒）
     */
    private static final long TIMEOUT_SECONDS = 60;

    /**
     * 默认 RAG 系统提示词
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个智能客服助手，请基于你的知识回答用户问题。
            请遵循以下规则：
            1. 回答要准确、简洁、有针对性
            2. 如果不确定答案，请诚实告知用户
            3. 避免过长的回答，控制在合理范围内
            """;

    /**
     * 默认最大 Token 数
     */
    private static final int DEFAULT_MAX_TOKENS = 2000;

    @Override
    public String generate(String question, MessageContext context) {
        log.info("使用 RAG 生成回答，问题: {}, 会话: {}", question, context.getConversationId());

        BotConfig botConfig = context.getBotConfig();
        String systemPrompt = botConfig != null && botConfig.getSystemPrompt() != null
                ? botConfig.getSystemPrompt()
                : DEFAULT_SYSTEM_PROMPT;
        int maxTokens = botConfig != null && botConfig.getMaxTokens() != null
                ? botConfig.getMaxTokens()
                : DEFAULT_MAX_TOKENS;

        try {
            // 使用流式调用收集完整响应
            StringBuilder answerBuilder = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.system(systemPrompt),
                            ChatMessage.user(question)
                    ))
                    .maxTokens(maxTokens)
                    .temperature(0.3)
                    .topP(0.8)
                    .build();

            StreamCallback callback = new StreamCallback() {
                @Override
                public void onContent(String content) {
                    answerBuilder.append(content);
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable error) {
                    errorRef.set(error);
                    latch.countDown();
                }
            };

            // 调用 LLM 流式服务
            StreamCancellationHandle handle = llmService.streamChat(request, callback);

            // 等待响应完成
            boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                handle.cancel();
                log.warn("RAG 回答生成超时");
                return "抱歉，回答生成超时，请稍后再试。";
            }

            // 检查是否有错误
            Throwable error = errorRef.get();
            if (error != null) {
                log.error("RAG 回答生成失败: {}", error.getMessage());
                return "抱歉，生成回答时出现错误，请稍后再试。";
            }

            String answer = answerBuilder.toString();
            log.debug("RAG 回答生成完成，长度: {}", answer.length());
            return answer.isEmpty() ? "抱歉，我暂时无法回答这个问题。" : answer;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("RAG 回答生成被中断", e);
            return "抱歉，回答生成被中断，请稍后再试。";
        } catch (Exception e) {
            log.error("RAG 回答生成异常: {}", e.getMessage(), e);
            return "抱歉，生成回答时出现错误，请稍后再试。";
        }
    }
}

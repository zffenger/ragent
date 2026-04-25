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

package com.nageoffer.ai.ragent.rag.application.impl;

import java.util.List;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.framework.context.UserContext;
import com.nageoffer.ai.ragent.framework.trace.RagTraceContext;
import com.nageoffer.ai.ragent.llm.domain.service.StreamCallback;
import com.nageoffer.ai.ragent.rag.infra.aop.ChatRateLimit;
import com.nageoffer.ai.ragent.rag.application.RAGChatService;
import com.nageoffer.ai.ragent.rag.application.handler.StreamCallbackFactory;
import com.nageoffer.ai.ragent.rag.application.handler.StreamTaskManager;
import com.nageoffer.ai.ragent.rag.application.pipeline.StreamChatContext;
import com.nageoffer.ai.ragent.rag.application.pipeline.StreamChatPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 对话服务默认实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGChatServiceImpl implements RAGChatService {

    private final StreamChatPipeline chatPipeline;
    private final StreamCallbackFactory callbackFactory;
    private final StreamTaskManager taskManager;

    @Override
    @ChatRateLimit
    public void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter) {
        streamChat(question, conversationId, deepThinking, null, emitter);
    }

    @Override
    @ChatRateLimit
    public void streamChat(String question, String conversationId, Boolean deepThinking,
						   List<String> knowledgeBaseIds, SseEmitter emitter) {
        String actualConversationId = StrUtil.isBlank(conversationId) ? IdUtil.getSnowflakeNextIdStr() : conversationId;
        String taskId = StrUtil.isBlank(RagTraceContext.getTaskId())
                ? IdUtil.getSnowflakeNextIdStr()
                : RagTraceContext.getTaskId();
        log.info("开始流式对话，会话ID：{}，任务ID：{}，限定知识库：{}", actualConversationId, taskId, knowledgeBaseIds);
        boolean thinkingEnabled = Boolean.TRUE.equals(deepThinking);

        StreamCallback callback = callbackFactory.createChatEventHandler(emitter, actualConversationId, taskId);

        StreamChatContext ctx = StreamChatContext.builder()
                .question(question)
                .conversationId(actualConversationId)
                .taskId(taskId)
				.knowledgeBaseIds(knowledgeBaseIds)
                .deepThinking(thinkingEnabled)
                .userId(UserContext.getUserId())
                .callback(callback)
                .build();

        try {
            chatPipeline.execute(ctx);
        } catch (Exception e) {
            log.error("流式对话处理异常，会话ID：{}，任务ID：{}", actualConversationId, taskId, e);
            callback.onError(e);
        }
    }

    @Override
    public void stopTask(String taskId) {
        taskManager.cancel(taskId);
    }
}

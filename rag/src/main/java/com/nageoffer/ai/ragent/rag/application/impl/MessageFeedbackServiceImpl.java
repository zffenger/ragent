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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.framework.context.UserContext;
import com.nageoffer.ai.ragent.framework.exception.ClientException;
import com.nageoffer.ai.ragent.rag.interfaces.controller.request.MessageFeedbackRequest;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationMessage;
import com.nageoffer.ai.ragent.rag.domain.entity.MessageFeedback;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationMessageRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.MessageFeedbackRepository;
import com.nageoffer.ai.ragent.rag.application.MessageFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 消息反馈服务实现
 * 异步提交使用虚拟线程池执行，避免阻塞主请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageFeedbackServiceImpl implements MessageFeedbackService {

    private final MessageFeedbackRepository feedbackRepository;
    private final ConversationMessageRepository messageRepository;

    @Override
    public void submitFeedbackAsync(String messageId, MessageFeedbackRequest request) {
        String userId = UserContext.getUserId();
        Assert.notBlank(userId, () -> new ClientException("未获取到当前登录用户"));
        Assert.notBlank(messageId, () -> new ClientException("消息ID不能为空"));
        Assert.notNull(request, () -> new ClientException("反馈内容不能为空"));
        Integer vote = request.getVote();
        Assert.notNull(vote, () -> new ClientException("反馈值不能为空"));
        Assert.isTrue(vote == 1 || vote == -1, () -> new ClientException("反馈值必须为 1 或 -1"));

        long submitTime = System.currentTimeMillis();

        // 使用虚拟线程池异步执行
        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                doSubmitFeedback(messageId, userId, vote, request.getReason(), request.getComment(), submitTime);
                log.info("异步提交消息反馈成功, messageId={}, userId={}, vote={}", messageId, userId, vote);
            } catch (Exception e) {
                log.error("异步提交消息反馈失败, messageId={}, userId={}, vote={}", messageId, userId, vote, e);
            }
        });
    }

    @Override
    public void submitFeedback(String messageId, MessageFeedbackRequest request) {
        String userId = UserContext.getUserId();
        Assert.notBlank(userId, () -> new ClientException("未获取到当前登录用户"));
        Assert.notBlank(messageId, () -> new ClientException("消息ID不能为空"));
        Assert.notNull(request, () -> new ClientException("反馈内容不能为空"));

        Integer vote = request.getVote();
        Assert.notNull(vote, () -> new ClientException("反馈值不能为空"));
        Assert.isTrue(vote == 1 || vote == -1, () -> new ClientException("反馈值必须为 1 或 -1"));

        doSubmitFeedback(messageId, userId, vote, request.getReason(), request.getComment(), System.currentTimeMillis());
    }

    @Override
    public Map<String, Integer> getUserVotes(String userId, List<String> messageIds) {
        if (StrUtil.isBlank(userId) || CollUtil.isEmpty(messageIds)) {
            return Collections.emptyMap();
        }
        return feedbackRepository.findVotesByUserIdAndMessageIds(userId, messageIds);
    }

    private void doSubmitFeedback(String messageId, String userId, Integer vote, String reason, String comment, long submitTime) {
        ConversationMessage message = loadAssistantMessage(messageId, userId);
        doUpsertFeedback(messageId, userId, message.getConversationId(), vote, reason, comment, submitTime);
    }

    private ConversationMessage loadAssistantMessage(String messageId, String userId) {
        ConversationMessage message = messageRepository.findByIdAndUserId(messageId, userId);
        Assert.notNull(message, () -> new ClientException("消息不存在"));
        Assert.isTrue("assistant".equalsIgnoreCase(message.getRole()), () -> new ClientException("仅支持对助手消息反馈"));
        return message;
    }

    private void doUpsertFeedback(String messageId, String userId, String conversationId,
                                  Integer vote, String reason, String comment, long submitTime) {
        MessageFeedback existing = feedbackRepository.findByMessageIdAndUserId(messageId, userId);

        if (existing == null) {
            MessageFeedback feedback = MessageFeedback.builder()
                    .messageId(messageId)
                    .conversationId(conversationId)
                    .userId(userId)
                    .vote(vote)
                    .reason(reason)
                    .comment(comment)
                    .build();
            feedbackRepository.save(feedback);
        } else {
            // 仅当本次提交时间晚于记录最后更新时间时才覆盖，避免并发乱序
            MessageFeedback update = MessageFeedback.builder()
                    .id(existing.getId())
                    .vote(vote)
                    .reason(reason)
                    .comment(comment)
                    .build();
            feedbackRepository.update(update, new Date(submitTime));
        }
    }
}

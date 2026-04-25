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

import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.rag.domain.entity.Conversation;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationMessage;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationSummary;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationMessageRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationSummaryRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationRepository;
import com.nageoffer.ai.ragent.rag.application.ConversationGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationGroupServiceImpl implements ConversationGroupService {

    private final ConversationMessageRepository messageRepository;
    private final ConversationSummaryRepository summaryRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public List<ConversationMessage> listLatestUserOnlyMessages(String conversationId, String userId, int limit) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId) || limit <= 0) {
            return List.of();
        }
        return messageRepository.listLatestUserMessages(conversationId, userId, limit);
    }

    @Override
    public List<ConversationMessage> listMessagesBetweenIds(String conversationId, String userId, String afterId, String beforeId) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)) {
            return List.of();
        }
        return messageRepository.listBetweenIds(conversationId, userId, afterId, beforeId);
    }

    @Override
    public String findMaxMessageIdAtOrBefore(String conversationId, String userId, java.util.Date at) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId) || at == null) {
            return null;
        }
        return messageRepository.findMaxIdAtOrBefore(conversationId, userId, at);
    }

    @Override
    public long countUserMessages(String conversationId, String userId) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)) {
            return 0;
        }
        return messageRepository.countUserMessages(conversationId, userId);
    }

    @Override
    public ConversationSummary findLatestSummary(String conversationId, String userId) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)) {
            return null;
        }
        return summaryRepository.findLatestByConversationIdAndUserId(conversationId, userId);
    }

    @Override
    public Conversation findConversation(String conversationId, String userId) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)) {
            return null;
        }
        return conversationRepository.findByConversationIdAndUserId(conversationId, userId);
    }
}

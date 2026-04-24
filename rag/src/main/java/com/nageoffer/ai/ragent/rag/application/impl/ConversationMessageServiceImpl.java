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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.rag.interfaces.controller.vo.ConversationMessageVO;
import com.nageoffer.ai.ragent.rag.domain.entity.Conversation;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationMessage;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationSummary;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationMessageRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationSummaryRepository;
import com.nageoffer.ai.ragent.rag.domain.enums.ConversationMessageOrder;
import com.nageoffer.ai.ragent.rag.application.MessageFeedbackService;
import com.nageoffer.ai.ragent.rag.application.ConversationMessageService;
import com.nageoffer.ai.ragent.rag.application.bo.ConversationMessageBO;
import com.nageoffer.ai.ragent.rag.application.bo.ConversationSummaryBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {

    private final ConversationMessageRepository messageRepository;
    private final ConversationSummaryRepository summaryRepository;
    private final ConversationRepository conversationRepository;
    private final MessageFeedbackService feedbackService;

    @Override
    public String addMessage(ConversationMessageBO conversationMessage) {
        ConversationMessage message = BeanUtil.toBean(conversationMessage, ConversationMessage.class);
        return messageRepository.save(message);
    }

    @Override
    public List<ConversationMessageVO> listMessages(String conversationId, String userId, Integer limit, ConversationMessageOrder order) {
        if (StrUtil.isBlank(conversationId) || StrUtil.isBlank(userId)) {
            return List.of();
        }

        Conversation conversation = conversationRepository.findByConversationIdAndUserId(conversationId, userId);
        if (conversation == null) {
            return List.of();
        }

        boolean asc = order == null || order == ConversationMessageOrder.ASC;
        List<ConversationMessage> records = messageRepository.listByConversationIdAndUserId(conversationId, userId, limit, asc);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        if (!asc) {
            Collections.reverse(records);
        }

        List<String> assistantMessageIds = records.stream()
                .filter(record -> "assistant".equalsIgnoreCase(record.getRole()))
                .map(ConversationMessage::getId)
                .toList();
        Map<String, Integer> votesByMessageId = feedbackService.getUserVotes(userId, assistantMessageIds);

        List<ConversationMessageVO> result = new ArrayList<>();
        for (ConversationMessage record : records) {
            ConversationMessageVO vo = ConversationMessageVO.builder()
                    .id(String.valueOf(record.getId()))
                    .conversationId(record.getConversationId())
                    .role(record.getRole())
                    .content(record.getContent())
                    .thinkingContent(record.getThinkingContent())
                    .thinkingDuration(record.getThinkingDuration())
                    .vote(votesByMessageId.get(record.getId()))
                    .createTime(record.getCreateTime())
                    .build();
            result.add(vo);
        }

        return result;
    }

    @Override
    public void addMessageSummary(ConversationSummaryBO conversationSummary) {
        ConversationSummary summary = BeanUtil.toBean(conversationSummary, ConversationSummary.class);
        summaryRepository.save(summary);
    }
}

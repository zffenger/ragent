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

package com.nageoffer.ai.ragent.rag.infra.persistence.repository;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.entity.MessageFeedback;
import com.nageoffer.ai.ragent.rag.domain.repository.MessageFeedbackRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.MessageFeedbackMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.MessageFeedbackDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息反馈仓储实现
 */
@Repository
@RequiredArgsConstructor
public class MessageFeedbackRepositoryImpl implements MessageFeedbackRepository {

    private final MessageFeedbackMapper feedbackMapper;

    @Override
    public void save(MessageFeedback feedback) {
        MessageFeedbackDO record = toDO(feedback);
        feedbackMapper.insert(record);
    }

    @Override
    public void update(MessageFeedback feedback, Date beforeTime) {
        MessageFeedbackDO record = toDO(feedback);
        feedbackMapper.update(
                record,
                Wrappers.lambdaUpdate(MessageFeedbackDO.class)
                        .eq(MessageFeedbackDO::getId, record.getId())
                        .lt(MessageFeedbackDO::getUpdateTime, beforeTime)
        );
    }

    @Override
    public MessageFeedback findByMessageIdAndUserId(String messageId, String userId) {
        MessageFeedbackDO record = feedbackMapper.selectOne(
                Wrappers.lambdaQuery(MessageFeedbackDO.class)
                        .eq(MessageFeedbackDO::getMessageId, messageId)
                        .eq(MessageFeedbackDO::getUserId, userId)
                        .eq(MessageFeedbackDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public Map<String, Integer> findVotesByUserIdAndMessageIds(String userId, List<String> messageIds) {
        if (CollUtil.isEmpty(messageIds)) {
            return Collections.emptyMap();
        }
        List<MessageFeedbackDO> records = feedbackMapper.selectList(
                Wrappers.lambdaQuery(MessageFeedbackDO.class)
                        .eq(MessageFeedbackDO::getUserId, userId)
                        .eq(MessageFeedbackDO::getDeleted, 0)
                        .in(MessageFeedbackDO::getMessageId, messageIds)
        );
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyMap();
        }
        return records.stream()
                .collect(Collectors.toMap(
                        MessageFeedbackDO::getMessageId,
                        MessageFeedbackDO::getVote,
                        (first, second) -> first
                ));
    }

    private MessageFeedback toEntity(MessageFeedbackDO record) {
        if (record == null) {
            return null;
        }
        return BeanUtil.toBean(record, MessageFeedback.class);
    }

    private MessageFeedbackDO toDO(MessageFeedback entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.toBean(entity, MessageFeedbackDO.class);
    }
}

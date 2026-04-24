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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationMessage;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationMessageRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.ConversationMessageMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.ConversationMessageDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话消息仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ConversationMessageRepositoryImpl implements ConversationMessageRepository {

    private final ConversationMessageMapper messageMapper;

    @Override
    public String save(ConversationMessage message) {
        ConversationMessageDO messageDO = toDO(message);
        messageMapper.insert(messageDO);
        return messageDO.getId();
    }

    @Override
    public List<ConversationMessage> listByConversationIdAndUserId(String conversationId, String userId, Integer limit, boolean asc) {
        List<ConversationMessageDO> records = messageMapper.selectList(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getDeleted, 0)
                        .orderBy(true, asc, ConversationMessageDO::getCreateTime)
                        .last(limit != null, "limit " + limit)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> listLatestUserMessages(String conversationId, String userId, int limit) {
        List<ConversationMessageDO> records = messageMapper.selectList(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getRole, "user")
                        .eq(ConversationMessageDO::getDeleted, 0)
                        .orderByDesc(ConversationMessageDO::getCreateTime)
                        .last("limit " + limit)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> listBetweenIds(String conversationId, String userId, String afterId, String beforeId) {
        var query = Wrappers.lambdaQuery(ConversationMessageDO.class)
                .eq(ConversationMessageDO::getConversationId, conversationId)
                .eq(ConversationMessageDO::getUserId, userId)
                .in(ConversationMessageDO::getRole, "user", "assistant")
                .eq(ConversationMessageDO::getDeleted, 0);
        if (afterId != null) {
            query.gt(ConversationMessageDO::getId, afterId);
        }
        if (beforeId != null) {
            query.lt(ConversationMessageDO::getId, beforeId);
        }
        List<ConversationMessageDO> records = messageMapper.selectList(query.orderByAsc(ConversationMessageDO::getId));
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public String findMaxIdAtOrBefore(String conversationId, String userId, Date at) {
        ConversationMessageDO record = messageMapper.selectOne(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getDeleted, 0)
                        .le(ConversationMessageDO::getCreateTime, at)
                        .orderByDesc(ConversationMessageDO::getId)
                        .last("limit 1")
        );
        return record == null ? null : record.getId();
    }

    @Override
    public long countUserMessages(String conversationId, String userId) {
        return messageMapper.selectCount(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getRole, "user")
                        .eq(ConversationMessageDO::getDeleted, 0)
        );
    }

    @Override
    public ConversationMessage findByIdAndUserId(String messageId, String userId) {
        ConversationMessageDO record = messageMapper.selectOne(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getId, messageId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public void deleteByConversationIdAndUserId(String conversationId, String userId) {
        messageMapper.delete(
                Wrappers.lambdaQuery(ConversationMessageDO.class)
                        .eq(ConversationMessageDO::getConversationId, conversationId)
                        .eq(ConversationMessageDO::getUserId, userId)
                        .eq(ConversationMessageDO::getDeleted, 0)
        );
    }

    private ConversationMessage toEntity(ConversationMessageDO record) {
        if (record == null) {
            return null;
        }
        ConversationMessage entity = new ConversationMessage();
        entity.setId(record.getId());
        entity.setConversationId(record.getConversationId());
        entity.setUserId(record.getUserId());
        entity.setRole(record.getRole());
        entity.setContent(record.getContent());
        entity.setThinkingContent(record.getThinkingContent());
        entity.setThinkingDuration(record.getThinkingDuration());
        entity.setCreateTime(record.getCreateTime());
        entity.setUpdateTime(record.getUpdateTime());
        return entity;
    }

    private ConversationMessageDO toDO(ConversationMessage entity) {
        if (entity == null) {
            return null;
        }
        ConversationMessageDO record = new ConversationMessageDO();
        record.setId(entity.getId());
        record.setConversationId(entity.getConversationId());
        record.setUserId(entity.getUserId());
        record.setRole(entity.getRole());
        record.setContent(entity.getContent());
        record.setThinkingContent(entity.getThinkingContent());
        record.setThinkingDuration(entity.getThinkingDuration());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

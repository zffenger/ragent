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
import com.nageoffer.ai.ragent.rag.domain.entity.Conversation;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.ConversationMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.ConversationDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationMapper conversationMapper;

    @Override
    public List<Conversation> listByUserId(String userId) {
        List<ConversationDO> records = conversationMapper.selectList(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getDeleted, 0)
                        .orderByDesc(ConversationDO::getLastTime)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Conversation findByConversationIdAndUserId(String conversationId, String userId) {
        ConversationDO record = conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationDO.class)
                        .eq(ConversationDO::getConversationId, conversationId)
                        .eq(ConversationDO::getUserId, userId)
                        .eq(ConversationDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public void save(Conversation conversation) {
        ConversationDO record = toDO(conversation);
        conversationMapper.insert(record);
    }

    @Override
    public void update(Conversation conversation) {
        ConversationDO record = toDO(conversation);
        conversationMapper.updateById(record);
    }

    @Override
    public void deleteById(String id) {
        conversationMapper.deleteById(id);
    }

    private Conversation toEntity(ConversationDO record) {
        if (record == null) {
            return null;
        }
        Conversation entity = new Conversation();
        entity.setId(record.getId());
        entity.setConversationId(record.getConversationId());
        entity.setUserId(record.getUserId());
        entity.setTitle(record.getTitle());
        entity.setLastTime(record.getLastTime());
        entity.setCreateTime(record.getCreateTime());
        entity.setUpdateTime(record.getUpdateTime());
        return entity;
    }

    private ConversationDO toDO(Conversation entity) {
        if (entity == null) {
            return null;
        }
        ConversationDO record = new ConversationDO();
        record.setId(entity.getId());
        record.setConversationId(entity.getConversationId());
        record.setUserId(entity.getUserId());
        record.setTitle(entity.getTitle());
        record.setLastTime(entity.getLastTime());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

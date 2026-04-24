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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.entity.ConversationSummary;
import com.nageoffer.ai.ragent.rag.domain.repository.ConversationSummaryRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.ConversationSummaryMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.ConversationSummaryDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 会话摘要仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ConversationSummaryRepositoryImpl implements ConversationSummaryRepository {

    private final ConversationSummaryMapper summaryMapper;

    @Override
    public void save(ConversationSummary summary) {
        ConversationSummaryDO summaryDO = toDO(summary);
        summaryMapper.insert(summaryDO);
    }

    @Override
    public ConversationSummary findLatestByConversationIdAndUserId(String conversationId, String userId) {
        ConversationSummaryDO record = summaryMapper.selectOne(
                Wrappers.lambdaQuery(ConversationSummaryDO.class)
                        .eq(ConversationSummaryDO::getConversationId, conversationId)
                        .eq(ConversationSummaryDO::getUserId, userId)
                        .eq(ConversationSummaryDO::getDeleted, 0)
                        .orderByDesc(ConversationSummaryDO::getId)
                        .last("limit 1")
        );
        return toEntity(record);
    }

    @Override
    public void deleteByConversationIdAndUserId(String conversationId, String userId) {
        summaryMapper.delete(
                Wrappers.lambdaQuery(ConversationSummaryDO.class)
                        .eq(ConversationSummaryDO::getConversationId, conversationId)
                        .eq(ConversationSummaryDO::getUserId, userId)
                        .eq(ConversationSummaryDO::getDeleted, 0)
        );
    }

    private ConversationSummary toEntity(ConversationSummaryDO record) {
        if (record == null) {
            return null;
        }
        return BeanUtil.toBean(record, ConversationSummary.class);
    }

    private ConversationSummaryDO toDO(ConversationSummary entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.toBean(entity, ConversationSummaryDO.class);
    }
}

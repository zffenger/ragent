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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeBase;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeBaseRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeBaseMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeBaseDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库仓储实现
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public KnowledgeBase findById(String id) {
        KnowledgeBaseDO record = knowledgeBaseMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                        .eq(KnowledgeBaseDO::getId, id)
                        .eq(KnowledgeBaseDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<KnowledgeBase> findByIds(Collection<String> ids) {
        List<KnowledgeBaseDO> records = knowledgeBaseMapper.selectBatchIds(ids);
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeBase> findAll() {
        List<KnowledgeBaseDO> records = knowledgeBaseMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                        .eq(KnowledgeBaseDO::getDeleted, 0)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    private KnowledgeBase toEntity(KnowledgeBaseDO record) {
        if (record == null) {
            return null;
        }
        return BeanUtil.toBean(record, KnowledgeBase.class);
    }

    private KnowledgeBaseDO toDO(KnowledgeBase entity) {
        if (entity == null) {
            return null;
        }
        return BeanUtil.toBean(entity, KnowledgeBaseDO.class);
    }
}

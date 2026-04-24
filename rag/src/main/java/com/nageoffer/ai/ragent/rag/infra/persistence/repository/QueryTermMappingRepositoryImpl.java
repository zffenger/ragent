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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.entity.QueryTermMapping;
import com.nageoffer.ai.ragent.rag.domain.repository.QueryTermMappingRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.QueryTermMappingMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.QueryTermMappingDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询术语映射仓储实现
 */
@Repository
@RequiredArgsConstructor
public class QueryTermMappingRepositoryImpl implements QueryTermMappingRepository {

    private final QueryTermMappingMapper mappingMapper;

    @Override
    public void save(QueryTermMapping mapping) {
        QueryTermMappingDO record = toDO(mapping);
        mappingMapper.insert(record);
    }

    @Override
    public void update(QueryTermMapping mapping) {
        QueryTermMappingDO record = toDO(mapping);
        mappingMapper.updateById(record);
    }

    @Override
    public void deleteById(String id) {
        mappingMapper.deleteById(id);
    }

    @Override
    public QueryTermMapping findById(String id) {
        QueryTermMappingDO record = mappingMapper.selectById(id);
        return toEntity(record);
    }

    @Override
    public List<QueryTermMapping> findAllEnabled() {
        List<QueryTermMappingDO> records = mappingMapper.selectList(
                Wrappers.lambdaQuery(QueryTermMappingDO.class)
                        .eq(QueryTermMappingDO::getEnabled, 1)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<QueryTermMapping> pageQuery(IPage<QueryTermMapping> page, String keyword) {
        IPage<QueryTermMappingDO> doPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getCurrent(), page.getSize());
        IPage<QueryTermMappingDO> result = mappingMapper.selectPage(
                doPage,
                Wrappers.lambdaQuery(QueryTermMappingDO.class)
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(QueryTermMappingDO::getSourceTerm, keyword)
                                .or()
                                .like(QueryTermMappingDO::getTargetTerm, keyword))
                        .orderByAsc(QueryTermMappingDO::getPriority)
                        .orderByDesc(QueryTermMappingDO::getUpdateTime)
        );
        return result.convert(this::toEntity);
    }

    private QueryTermMapping toEntity(QueryTermMappingDO record) {
        if (record == null) {
            return null;
        }
        QueryTermMapping entity = new QueryTermMapping();
        entity.setId(record.getId());
        entity.setDomain(record.getDomain());
        entity.setSourceTerm(record.getSourceTerm());
        entity.setTargetTerm(record.getTargetTerm());
        entity.setMatchType(record.getMatchType());
        entity.setPriority(record.getPriority());
        entity.setEnabled(record.getEnabled());
        entity.setRemark(record.getRemark());
        entity.setCreateBy(record.getCreateBy());
        entity.setUpdateBy(record.getUpdateBy());
        entity.setCreateTime(record.getCreateTime());
        entity.setUpdateTime(record.getUpdateTime());
        return entity;
    }

    private QueryTermMappingDO toDO(QueryTermMapping entity) {
        if (entity == null) {
            return null;
        }
        QueryTermMappingDO record = new QueryTermMappingDO();
        record.setId(entity.getId());
        record.setDomain(entity.getDomain());
        record.setSourceTerm(entity.getSourceTerm());
        record.setTargetTerm(entity.getTargetTerm());
        record.setMatchType(entity.getMatchType());
        record.setPriority(entity.getPriority());
        record.setEnabled(entity.getEnabled());
        record.setRemark(entity.getRemark());
        record.setCreateBy(entity.getCreateBy());
        record.setUpdateBy(entity.getUpdateBy());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

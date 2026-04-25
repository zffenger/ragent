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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionPipeline;
import com.nageoffer.ai.ragent.rag.domain.repository.IngestionPipelineRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.IngestionPipelineMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.IngestionPipelineDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 摄取管道仓储实现
 */
@Repository
@RequiredArgsConstructor
public class IngestionPipelineRepositoryImpl implements IngestionPipelineRepository {

    private final IngestionPipelineMapper ingestionPipelineMapper;

    @Override
    public IngestionPipeline findById(String id) {
        IngestionPipelineDO record = ingestionPipelineMapper.selectOne(
                Wrappers.lambdaQuery(IngestionPipelineDO.class)
                        .eq(IngestionPipelineDO::getId, id)
                        .eq(IngestionPipelineDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<IngestionPipeline> findByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<IngestionPipelineDO> records = ingestionPipelineMapper.selectList(
                Wrappers.lambdaQuery(IngestionPipelineDO.class)
                        .in(IngestionPipelineDO::getId, ids)
                        .eq(IngestionPipelineDO::getDeleted, 0)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<IngestionPipeline> findPage(Page<?> page, String keyword) {
        Page<IngestionPipelineDO> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<IngestionPipelineDO> result = ingestionPipelineMapper.selectPage(mpPage,
                Wrappers.lambdaQuery(IngestionPipelineDO.class)
                        .eq(IngestionPipelineDO::getDeleted, 0)
                        .like(StringUtils.hasText(keyword), IngestionPipelineDO::getName, keyword)
                        .orderByDesc(IngestionPipelineDO::getUpdateTime)
        );
        Page<IngestionPipeline> entityPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        entityPage.setRecords(result.getRecords().stream()
                .map(this::toEntity)
                .collect(Collectors.toList()));
        return entityPage;
    }

    @Override
    public void save(IngestionPipeline pipeline) {
        IngestionPipelineDO record = toDO(pipeline);
        ingestionPipelineMapper.insert(record);
    }

    @Override
    public void update(IngestionPipeline pipeline) {
        IngestionPipelineDO record = toDO(pipeline);
        ingestionPipelineMapper.updateById(record);
    }

    @Override
    public void deleteById(String id) {
        ingestionPipelineMapper.deleteById(id);
    }

    private IngestionPipeline toEntity(IngestionPipelineDO record) {
        if (record == null) {
            return null;
        }
        return IngestionPipeline.builder()
                .id(record.getId())
                .name(record.getName())
                .description(record.getDescription())
                .createdBy(record.getCreatedBy())
                .updatedBy(record.getUpdatedBy())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private IngestionPipelineDO toDO(IngestionPipeline entity) {
        if (entity == null) {
            return null;
        }
        IngestionPipelineDO record = new IngestionPipelineDO();
        record.setId(entity.getId());
        record.setName(entity.getName());
        record.setDescription(entity.getDescription());
        record.setCreatedBy(entity.getCreatedBy());
        record.setUpdatedBy(entity.getUpdatedBy());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

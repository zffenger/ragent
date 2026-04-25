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
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionTask;
import com.nageoffer.ai.ragent.rag.domain.repository.IngestionTaskRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.IngestionTaskMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.IngestionTaskDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摄取任务仓储实现
 */
@Repository
@RequiredArgsConstructor
public class IngestionTaskRepositoryImpl implements IngestionTaskRepository {

    private final IngestionTaskMapper ingestionTaskMapper;

    @Override
    public IngestionTask findById(String id) {
        IngestionTaskDO record = ingestionTaskMapper.selectOne(
                Wrappers.lambdaQuery(IngestionTaskDO.class)
                        .eq(IngestionTaskDO::getId, id)
                        .eq(IngestionTaskDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<IngestionTask> findByPipelineId(String pipelineId) {
        List<IngestionTaskDO> records = ingestionTaskMapper.selectList(
                Wrappers.lambdaQuery(IngestionTaskDO.class)
                        .eq(IngestionTaskDO::getPipelineId, pipelineId)
                        .eq(IngestionTaskDO::getDeleted, 0)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<IngestionTask> findPage(Page<?> page, String status) {
        Page<IngestionTaskDO> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<IngestionTaskDO> result = ingestionTaskMapper.selectPage(mpPage,
                Wrappers.lambdaQuery(IngestionTaskDO.class)
                        .eq(IngestionTaskDO::getDeleted, 0)
                        .eq(StringUtils.hasText(status), IngestionTaskDO::getStatus, status)
                        .orderByDesc(IngestionTaskDO::getCreateTime)
        );
        Page<IngestionTask> entityPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        entityPage.setRecords(result.getRecords().stream()
                .map(this::toEntity)
                .collect(Collectors.toList()));
        return entityPage;
    }

    @Override
    public void save(IngestionTask task) {
        IngestionTaskDO record = toDO(task);
        ingestionTaskMapper.insert(record);
    }

    @Override
    public void update(IngestionTask task) {
        IngestionTaskDO record = toDO(task);
        ingestionTaskMapper.updateById(record);
    }

    private IngestionTask toEntity(IngestionTaskDO record) {
        if (record == null) {
            return null;
        }
        return IngestionTask.builder()
                .id(record.getId())
                .pipelineId(record.getPipelineId())
                .sourceType(record.getSourceType())
                .sourceLocation(record.getSourceLocation())
                .sourceFileName(record.getSourceFileName())
                .status(record.getStatus())
                .chunkCount(record.getChunkCount())
                .errorMessage(record.getErrorMessage())
                .logsJson(record.getLogsJson())
                .metadataJson(record.getMetadataJson())
                .startedAt(record.getStartedAt())
                .completedAt(record.getCompletedAt())
                .createdBy(record.getCreatedBy())
                .updatedBy(record.getUpdatedBy())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private IngestionTaskDO toDO(IngestionTask entity) {
        if (entity == null) {
            return null;
        }
        IngestionTaskDO record = new IngestionTaskDO();
        record.setId(entity.getId());
        record.setPipelineId(entity.getPipelineId());
        record.setSourceType(entity.getSourceType());
        record.setSourceLocation(entity.getSourceLocation());
        record.setSourceFileName(entity.getSourceFileName());
        record.setStatus(entity.getStatus());
        record.setChunkCount(entity.getChunkCount());
        record.setErrorMessage(entity.getErrorMessage());
        record.setLogsJson(entity.getLogsJson());
        record.setMetadataJson(entity.getMetadataJson());
        record.setStartedAt(entity.getStartedAt());
        record.setCompletedAt(entity.getCompletedAt());
        record.setCreatedBy(entity.getCreatedBy());
        record.setUpdatedBy(entity.getUpdatedBy());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

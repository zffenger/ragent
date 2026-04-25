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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentChunkLog;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentChunkLogRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeDocumentChunkLogMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeDocumentChunkLogDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档分块日志仓储实现
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentChunkLogRepositoryImpl implements KnowledgeDocumentChunkLogRepository {

    private final KnowledgeDocumentChunkLogMapper chunkLogMapper;

    @Override
    public KnowledgeDocumentChunkLog findById(String id) {
        KnowledgeDocumentChunkLogDO record = chunkLogMapper.selectById(id);
        return toEntity(record);
    }

    @Override
    public List<KnowledgeDocumentChunkLog> findByDocId(String docId) {
        List<KnowledgeDocumentChunkLogDO> records = chunkLogMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentChunkLogDO.class)
                        .eq(KnowledgeDocumentChunkLogDO::getDocId, docId)
                        .orderByDesc(KnowledgeDocumentChunkLogDO::getCreateTime)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public IPage<KnowledgeDocumentChunkLog> findPage(Page<?> page, String docId) {
        Page<KnowledgeDocumentChunkLogDO> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<KnowledgeDocumentChunkLogDO> result = chunkLogMapper.selectPage(mpPage,
                Wrappers.lambdaQuery(KnowledgeDocumentChunkLogDO.class)
                        .eq(KnowledgeDocumentChunkLogDO::getDocId, docId)
                        .orderByDesc(KnowledgeDocumentChunkLogDO::getCreateTime)
        );
        Page<KnowledgeDocumentChunkLog> entityPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        entityPage.setRecords(result.getRecords().stream().map(this::toEntity).collect(Collectors.toList()));
        return entityPage;
    }

    @Override
    public void save(KnowledgeDocumentChunkLog log) {
        KnowledgeDocumentChunkLogDO record = toDO(log);
        chunkLogMapper.insert(record);
    }

    @Override
    public void update(KnowledgeDocumentChunkLog log) {
        KnowledgeDocumentChunkLogDO record = toDO(log);
        chunkLogMapper.updateById(record);
    }

    @Override
    public void deleteByDocId(String docId) {
        chunkLogMapper.delete(
                Wrappers.lambdaQuery(KnowledgeDocumentChunkLogDO.class)
                        .eq(KnowledgeDocumentChunkLogDO::getDocId, docId)
        );
    }

    private KnowledgeDocumentChunkLog toEntity(KnowledgeDocumentChunkLogDO record) {
        if (record == null) {
            return null;
        }
        return KnowledgeDocumentChunkLog.builder()
                .id(record.getId())
                .docId(record.getDocId())
                .status(record.getStatus())
                .processMode(record.getProcessMode())
                .chunkStrategy(record.getChunkStrategy())
                .pipelineId(record.getPipelineId())
                .extractDuration(record.getExtractDuration())
                .chunkDuration(record.getChunkDuration())
                .embedDuration(record.getEmbedDuration())
                .persistDuration(record.getPersistDuration())
                .totalDuration(record.getTotalDuration())
                .chunkCount(record.getChunkCount())
                .errorMessage(record.getErrorMessage())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private KnowledgeDocumentChunkLogDO toDO(KnowledgeDocumentChunkLog entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentChunkLogDO record = new KnowledgeDocumentChunkLogDO();
        record.setId(entity.getId());
        record.setDocId(entity.getDocId());
        record.setStatus(entity.getStatus());
        record.setProcessMode(entity.getProcessMode());
        record.setChunkStrategy(entity.getChunkStrategy());
        record.setPipelineId(entity.getPipelineId());
        record.setExtractDuration(entity.getExtractDuration());
        record.setChunkDuration(entity.getChunkDuration());
        record.setEmbedDuration(entity.getEmbedDuration());
        record.setPersistDuration(entity.getPersistDuration());
        record.setTotalDuration(entity.getTotalDuration());
        record.setChunkCount(entity.getChunkCount());
        record.setErrorMessage(entity.getErrorMessage());
        record.setStartTime(entity.getStartTime());
        record.setEndTime(entity.getEndTime());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

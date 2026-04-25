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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocument;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeDocumentMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeDocumentDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档仓储实现
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public KnowledgeDocument findById(String id) {
        KnowledgeDocumentDO record = knowledgeDocumentMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                        .eq(KnowledgeDocumentDO::getId, id)
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<KnowledgeDocument> findByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<KnowledgeDocumentDO> records = knowledgeDocumentMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                        .in(KnowledgeDocumentDO::getId, ids)
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDocument> findByKbId(String kbId) {
        List<KnowledgeDocumentDO> records = knowledgeDocumentMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                        .eq(KnowledgeDocumentDO::getKbId, kbId)
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public IPage<KnowledgeDocument> findPage(Page<?> page, String kbId, String keyword, String status) {
        Page<KnowledgeDocumentDO> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<KnowledgeDocumentDO> result = knowledgeDocumentMapper.selectPage(mpPage,
                Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                        .eq(KnowledgeDocumentDO::getKbId, kbId)
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
                        .like(StringUtils.hasText(keyword), KnowledgeDocumentDO::getDocName, keyword)
                        .eq(StringUtils.hasText(status), KnowledgeDocumentDO::getStatus, status)
                        .orderByDesc(KnowledgeDocumentDO::getCreateTime)
        );
        Page<KnowledgeDocument> entityPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        entityPage.setRecords(result.getRecords().stream().map(this::toEntity).collect(Collectors.toList()));
        return entityPage;
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        List<KnowledgeDocumentDO> records = knowledgeDocumentMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                        .eq(KnowledgeDocumentDO::getDeleted, 0)
                        .like(KnowledgeDocumentDO::getDocName, keyword)
                        .orderByDesc(KnowledgeDocumentDO::getUpdateTime)
                        .last("LIMIT " + size)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void save(KnowledgeDocument document) {
        KnowledgeDocumentDO record = toDO(document);
        knowledgeDocumentMapper.insert(record);
    }

    @Override
    public void updateById(KnowledgeDocument document) {
        KnowledgeDocumentDO record = toDO(document);
        knowledgeDocumentMapper.updateById(record);
    }

    @Override
    public void deleteById(String id) {
        knowledgeDocumentMapper.deleteById(id);
    }

    @Override
    public void incrementChunkCount(String id, int delta) {
        String sql = delta >= 0
                ? "chunk_count = chunk_count + " + delta
                : "chunk_count = CASE WHEN chunk_count > 0 THEN chunk_count + " + delta + " ELSE 0 END";
        knowledgeDocumentMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeDocumentDO.class)
                        .setSql(sql)
                        .eq(KnowledgeDocumentDO::getId, id)
        );
    }

    @Override
    public boolean tryUpdateStatus(String id, String newStatus, String updatedBy) {
        int updated = knowledgeDocumentMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeDocumentDO.class)
                        .set(KnowledgeDocumentDO::getStatus, newStatus)
                        .set(KnowledgeDocumentDO::getUpdatedBy, updatedBy)
                        .eq(KnowledgeDocumentDO::getId, id)
                        .ne(KnowledgeDocumentDO::getStatus, newStatus)
        );
        return updated > 0;
    }

    @Override
    public void softDelete(String id, String updatedBy) {
        knowledgeDocumentMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeDocumentDO.class)
                        .set(KnowledgeDocumentDO::getDeleted, 1)
                        .set(KnowledgeDocumentDO::getUpdatedBy, updatedBy)
                        .eq(KnowledgeDocumentDO::getId, id)
        );
    }

    private KnowledgeDocument toEntity(KnowledgeDocumentDO record) {
        if (record == null) {
            return null;
        }
        return KnowledgeDocument.builder()
                .id(record.getId())
                .kbId(record.getKbId())
                .docName(record.getDocName())
                .sourceType(record.getSourceType())
                .sourceLocation(record.getSourceLocation())
                .scheduleEnabled(record.getScheduleEnabled())
                .scheduleCron(record.getScheduleCron())
                .enabled(record.getEnabled())
                .chunkCount(record.getChunkCount())
                .fileUrl(record.getFileUrl())
                .fileType(record.getFileType())
                .fileSize(record.getFileSize())
                .processMode(record.getProcessMode())
                .chunkStrategy(record.getChunkStrategy())
                .chunkConfig(record.getChunkConfig())
                .pipelineId(record.getPipelineId())
                .status(record.getStatus())
                .createdBy(record.getCreatedBy())
                .updatedBy(record.getUpdatedBy())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private KnowledgeDocumentDO toDO(KnowledgeDocument entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentDO record = new KnowledgeDocumentDO();
        record.setId(entity.getId());
        record.setKbId(entity.getKbId());
        record.setDocName(entity.getDocName());
        record.setSourceType(entity.getSourceType());
        record.setSourceLocation(entity.getSourceLocation());
        record.setScheduleEnabled(entity.getScheduleEnabled());
        record.setScheduleCron(entity.getScheduleCron());
        record.setEnabled(entity.getEnabled());
        record.setChunkCount(entity.getChunkCount());
        record.setFileUrl(entity.getFileUrl());
        record.setFileType(entity.getFileType());
        record.setFileSize(entity.getFileSize());
        record.setProcessMode(entity.getProcessMode());
        record.setChunkStrategy(entity.getChunkStrategy());
        record.setChunkConfig(entity.getChunkConfig());
        record.setPipelineId(entity.getPipelineId());
        record.setStatus(entity.getStatus());
        record.setCreatedBy(entity.getCreatedBy());
        record.setUpdatedBy(entity.getUpdatedBy());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

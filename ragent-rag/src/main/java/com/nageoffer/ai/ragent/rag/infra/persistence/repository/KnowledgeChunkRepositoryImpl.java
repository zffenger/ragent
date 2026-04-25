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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeChunk;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeChunkRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeChunkMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeChunkDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库分块仓储实现
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeChunkRepositoryImpl implements KnowledgeChunkRepository {

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    public KnowledgeChunk findById(String id) {
        KnowledgeChunkDO record = knowledgeChunkMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .eq(KnowledgeChunkDO::getId, id)
                        .eq(KnowledgeChunkDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<KnowledgeChunk> findByDocId(String docId) {
        List<KnowledgeChunkDO> records = knowledgeChunkMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .eq(KnowledgeChunkDO::getDocId, docId)
                        .eq(KnowledgeChunkDO::getDeleted, 0)
                        .orderByAsc(KnowledgeChunkDO::getChunkIndex)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeChunk> findByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<KnowledgeChunkDO> records = knowledgeChunkMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .in(KnowledgeChunkDO::getId, ids)
                        .eq(KnowledgeChunkDO::getDeleted, 0)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public IPage<KnowledgeChunk> findPage(Page<?> page, String docId) {
        Page<KnowledgeChunkDO> mpPage = new Page<>(page.getCurrent(), page.getSize());
        IPage<KnowledgeChunkDO> result = knowledgeChunkMapper.selectPage(mpPage,
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .eq(KnowledgeChunkDO::getDocId, docId)
                        .eq(KnowledgeChunkDO::getDeleted, 0)
                        .orderByAsc(KnowledgeChunkDO::getChunkIndex)
        );
        Page<KnowledgeChunk> entityPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        entityPage.setRecords(result.getRecords().stream().map(this::toEntity).collect(Collectors.toList()));
        return entityPage;
    }

    @Override
    public void save(KnowledgeChunk chunk) {
        KnowledgeChunkDO record = toDO(chunk);
        knowledgeChunkMapper.insert(record);
    }

    @Override
    public void saveAll(List<KnowledgeChunk> chunks) {
        for (KnowledgeChunk chunk : chunks) {
            KnowledgeChunkDO record = toDO(chunk);
            knowledgeChunkMapper.insert(record);
        }
    }

    @Override
    public void update(KnowledgeChunk chunk) {
        KnowledgeChunkDO record = toDO(chunk);
        knowledgeChunkMapper.updateById(record);
    }

    @Override
    public void deleteById(String id) {
        knowledgeChunkMapper.deleteById(id);
    }

    @Override
    public void deleteByDocId(String docId) {
        knowledgeChunkMapper.delete(
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .eq(KnowledgeChunkDO::getDocId, docId)
        );
    }

    @Override
    public void updateEnabled(String id, Integer enabled) {
        knowledgeChunkMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeChunkDO.class)
                        .set(KnowledgeChunkDO::getEnabled, enabled)
                        .eq(KnowledgeChunkDO::getId, id)
        );
    }

    @Override
    public void updateEnabledByDocId(String docId, Integer enabled) {
        knowledgeChunkMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeChunkDO.class)
                        .set(KnowledgeChunkDO::getEnabled, enabled)
                        .eq(KnowledgeChunkDO::getDocId, docId)
        );
    }

    @Override
    public void updateEnabledByIds(Collection<String> ids, Integer enabled, String updatedBy) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        knowledgeChunkMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeChunkDO.class)
                        .set(KnowledgeChunkDO::getEnabled, enabled)
                        .set(KnowledgeChunkDO::getUpdatedBy, updatedBy)
                        .in(KnowledgeChunkDO::getId, ids)
        );
    }

    @Override
    public KnowledgeChunk findLatestByDocId(String docId) {
        KnowledgeChunkDO record = knowledgeChunkMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .eq(KnowledgeChunkDO::getDocId, docId)
                        .eq(KnowledgeChunkDO::getDeleted, 0)
                        .orderByDesc(KnowledgeChunkDO::getChunkIndex)
                        .last("LIMIT 1")
        );
        return toEntity(record);
    }

    @Override
    public List<KnowledgeChunk> findByIdsAndNotEnabled(Collection<String> ids, Integer enabled) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<KnowledgeChunkDO> records = knowledgeChunkMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeChunkDO.class)
                        .in(KnowledgeChunkDO::getId, ids)
                        .ne(KnowledgeChunkDO::getEnabled, enabled)
                        .eq(KnowledgeChunkDO::getDeleted, 0)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    private KnowledgeChunk toEntity(KnowledgeChunkDO record) {
        if (record == null) {
            return null;
        }
        return KnowledgeChunk.builder()
                .id(record.getId())
                .kbId(record.getKbId())
                .docId(record.getDocId())
                .chunkIndex(record.getChunkIndex())
                .content(record.getContent())
                .contentHash(record.getContentHash())
                .charCount(record.getCharCount())
                .tokenCount(record.getTokenCount())
                .enabled(record.getEnabled())
                .createdBy(record.getCreatedBy())
                .updatedBy(record.getUpdatedBy())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private KnowledgeChunkDO toDO(KnowledgeChunk entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeChunkDO record = new KnowledgeChunkDO();
        record.setId(entity.getId());
        record.setKbId(entity.getKbId());
        record.setDocId(entity.getDocId());
        record.setChunkIndex(entity.getChunkIndex());
        record.setContent(entity.getContent());
        record.setContentHash(entity.getContentHash());
        record.setCharCount(entity.getCharCount());
        record.setTokenCount(entity.getTokenCount());
        record.setEnabled(entity.getEnabled());
        record.setCreatedBy(entity.getCreatedBy());
        record.setUpdatedBy(entity.getUpdatedBy());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentScheduleExec;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentScheduleExecRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeDocumentScheduleExecMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeDocumentScheduleExecDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档定时执行记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentScheduleExecRepositoryImpl implements KnowledgeDocumentScheduleExecRepository {

    private final KnowledgeDocumentScheduleExecMapper execMapper;

    @Override
    public KnowledgeDocumentScheduleExec findById(String id) {
        KnowledgeDocumentScheduleExecDO record = execMapper.selectById(id);
        return toEntity(record);
    }

    @Override
    public List<KnowledgeDocumentScheduleExec> findByScheduleId(String scheduleId) {
        List<KnowledgeDocumentScheduleExecDO> records = execMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentScheduleExecDO.class)
                        .eq(KnowledgeDocumentScheduleExecDO::getScheduleId, scheduleId)
                        .orderByDesc(KnowledgeDocumentScheduleExecDO::getCreateTime)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDocumentScheduleExec> findByDocId(String docId) {
        List<KnowledgeDocumentScheduleExecDO> records = execMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentScheduleExecDO.class)
                        .eq(KnowledgeDocumentScheduleExecDO::getDocId, docId)
                        .orderByDesc(KnowledgeDocumentScheduleExecDO::getCreateTime)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void save(KnowledgeDocumentScheduleExec exec) {
        KnowledgeDocumentScheduleExecDO record = toDO(exec);
        execMapper.insert(record);
    }

    @Override
    public void deleteByDocId(String docId) {
        execMapper.delete(
                Wrappers.lambdaQuery(KnowledgeDocumentScheduleExecDO.class)
                        .eq(KnowledgeDocumentScheduleExecDO::getDocId, docId)
        );
    }

    private KnowledgeDocumentScheduleExec toEntity(KnowledgeDocumentScheduleExecDO record) {
        if (record == null) {
            return null;
        }
        return KnowledgeDocumentScheduleExec.builder()
                .id(record.getId())
                .scheduleId(record.getScheduleId())
                .docId(record.getDocId())
                .kbId(record.getKbId())
                .status(record.getStatus())
                .message(record.getMessage())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .fileName(record.getFileName())
                .fileSize(record.getFileSize())
                .contentHash(record.getContentHash())
                .etag(record.getEtag())
                .lastModified(record.getLastModified())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private KnowledgeDocumentScheduleExecDO toDO(KnowledgeDocumentScheduleExec entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentScheduleExecDO record = new KnowledgeDocumentScheduleExecDO();
        record.setId(entity.getId());
        record.setScheduleId(entity.getScheduleId());
        record.setDocId(entity.getDocId());
        record.setKbId(entity.getKbId());
        record.setStatus(entity.getStatus());
        record.setMessage(entity.getMessage());
        record.setStartTime(entity.getStartTime());
        record.setEndTime(entity.getEndTime());
        record.setFileName(entity.getFileName());
        record.setFileSize(entity.getFileSize());
        record.setContentHash(entity.getContentHash());
        record.setEtag(entity.getEtag());
        record.setLastModified(entity.getLastModified());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

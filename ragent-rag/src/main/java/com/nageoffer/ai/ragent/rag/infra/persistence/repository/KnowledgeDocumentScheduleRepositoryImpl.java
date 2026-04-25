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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentSchedule;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentScheduleRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeDocumentScheduleMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeDocumentScheduleDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库文档定时任务仓储实现
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentScheduleRepositoryImpl implements KnowledgeDocumentScheduleRepository {

    private final KnowledgeDocumentScheduleMapper scheduleMapper;

    @Override
    public KnowledgeDocumentSchedule findById(String id) {
        KnowledgeDocumentScheduleDO record = scheduleMapper.selectById(id);
        return toEntity(record);
    }

    @Override
    public KnowledgeDocumentSchedule findByDocId(String docId) {
        KnowledgeDocumentScheduleDO record = scheduleMapper.selectOne(
                Wrappers.lambdaQuery(KnowledgeDocumentScheduleDO.class)
                        .eq(KnowledgeDocumentScheduleDO::getDocId, docId)
        );
        return toEntity(record);
    }

    @Override
    public List<KnowledgeDocumentSchedule> findAllEnabled() {
        List<KnowledgeDocumentScheduleDO> records = scheduleMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeDocumentScheduleDO.class)
                        .eq(KnowledgeDocumentScheduleDO::getEnabled, 1)
        );
        return records.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void save(KnowledgeDocumentSchedule schedule) {
        KnowledgeDocumentScheduleDO record = toDO(schedule);
        scheduleMapper.insert(record);
    }

    @Override
    public void update(KnowledgeDocumentSchedule schedule) {
        KnowledgeDocumentScheduleDO record = toDO(schedule);
        scheduleMapper.updateById(record);
    }

    @Override
    public void deleteByDocId(String docId) {
        scheduleMapper.delete(
                Wrappers.lambdaQuery(KnowledgeDocumentScheduleDO.class)
                        .eq(KnowledgeDocumentScheduleDO::getDocId, docId)
        );
    }

    @Override
    public boolean tryLock(String docId, String owner, Date lockUntil) {
        int updated = scheduleMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeDocumentScheduleDO.class)
                        .set(KnowledgeDocumentScheduleDO::getLockOwner, owner)
                        .set(KnowledgeDocumentScheduleDO::getLockUntil, lockUntil)
                        .eq(KnowledgeDocumentScheduleDO::getDocId, docId)
                        .and(w -> w.isNull(KnowledgeDocumentScheduleDO::getLockOwner)
                                .or()
                                .lt(KnowledgeDocumentScheduleDO::getLockUntil, new Date()))
        );
        return updated > 0;
    }

    @Override
    public void releaseLock(String docId, String owner) {
        scheduleMapper.update(null,
                Wrappers.lambdaUpdate(KnowledgeDocumentScheduleDO.class)
                        .set(KnowledgeDocumentScheduleDO::getLockOwner, null)
                        .set(KnowledgeDocumentScheduleDO::getLockUntil, null)
                        .eq(KnowledgeDocumentScheduleDO::getDocId, docId)
                        .eq(KnowledgeDocumentScheduleDO::getLockOwner, owner)
        );
    }

    private KnowledgeDocumentSchedule toEntity(KnowledgeDocumentScheduleDO record) {
        if (record == null) {
            return null;
        }
        return KnowledgeDocumentSchedule.builder()
                .id(record.getId())
                .docId(record.getDocId())
                .kbId(record.getKbId())
                .cronExpr(record.getCronExpr())
                .enabled(record.getEnabled())
                .nextRunTime(record.getNextRunTime())
                .lastRunTime(record.getLastRunTime())
                .lastSuccessTime(record.getLastSuccessTime())
                .lastStatus(record.getLastStatus())
                .lastError(record.getLastError())
                .lastEtag(record.getLastEtag())
                .lastModified(record.getLastModified())
                .lastContentHash(record.getLastContentHash())
                .lockOwner(record.getLockOwner())
                .lockUntil(record.getLockUntil())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private KnowledgeDocumentScheduleDO toDO(KnowledgeDocumentSchedule entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentScheduleDO record = new KnowledgeDocumentScheduleDO();
        record.setId(entity.getId());
        record.setDocId(entity.getDocId());
        record.setKbId(entity.getKbId());
        record.setCronExpr(entity.getCronExpr());
        record.setEnabled(entity.getEnabled());
        record.setNextRunTime(entity.getNextRunTime());
        record.setLastRunTime(entity.getLastRunTime());
        record.setLastSuccessTime(entity.getLastSuccessTime());
        record.setLastStatus(entity.getLastStatus());
        record.setLastError(entity.getLastError());
        record.setLastEtag(entity.getLastEtag());
        record.setLastModified(entity.getLastModified());
        record.setLastContentHash(entity.getLastContentHash());
        record.setLockOwner(entity.getLockOwner());
        record.setLockUntil(entity.getLockUntil());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

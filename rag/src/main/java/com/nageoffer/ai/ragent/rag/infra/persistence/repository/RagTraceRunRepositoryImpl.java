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
import com.nageoffer.ai.ragent.rag.domain.entity.RagTraceRun;
import com.nageoffer.ai.ragent.rag.domain.repository.RagTraceRunRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.RagTraceRunMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.RagTraceRunDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * RAG Trace 运行仓储实现
 */
@Repository
@RequiredArgsConstructor
public class RagTraceRunRepositoryImpl implements RagTraceRunRepository {

    private final RagTraceRunMapper runMapper;

    @Override
    public void save(RagTraceRun run) {
        RagTraceRunDO record = toDO(run);
        runMapper.insert(record);
    }

    @Override
    public void updateByTraceId(String traceId, RagTraceRun run) {
        RagTraceRunDO record = toDO(run);
        runMapper.update(
                record,
                Wrappers.lambdaUpdate(RagTraceRunDO.class)
                        .eq(RagTraceRunDO::getTraceId, traceId)
        );
    }

    private RagTraceRun toEntity(RagTraceRunDO record) {
        if (record == null) {
            return null;
        }
        RagTraceRun entity = new RagTraceRun();
        entity.setId(record.getId());
        entity.setTraceId(record.getTraceId());
        entity.setTraceName(record.getTraceName());
        entity.setEntryMethod(record.getEntryMethod());
        entity.setConversationId(record.getConversationId());
        entity.setTaskId(record.getTaskId());
        entity.setUserId(record.getUserId());
        entity.setStatus(record.getStatus());
        entity.setErrorMessage(record.getErrorMessage());
        entity.setStartTime(record.getStartTime());
        entity.setEndTime(record.getEndTime());
        entity.setDurationMs(record.getDurationMs());
        entity.setExtraData(record.getExtraData());
        entity.setCreateTime(record.getCreateTime());
        entity.setUpdateTime(record.getUpdateTime());
        return entity;
    }

    private RagTraceRunDO toDO(RagTraceRun entity) {
        if (entity == null) {
            return null;
        }
        RagTraceRunDO record = new RagTraceRunDO();
        record.setId(entity.getId());
        record.setTraceId(entity.getTraceId());
        record.setTraceName(entity.getTraceName());
        record.setEntryMethod(entity.getEntryMethod());
        record.setConversationId(entity.getConversationId());
        record.setTaskId(entity.getTaskId());
        record.setUserId(entity.getUserId());
        record.setStatus(entity.getStatus());
        record.setErrorMessage(entity.getErrorMessage());
        record.setStartTime(entity.getStartTime());
        record.setEndTime(entity.getEndTime());
        record.setDurationMs(entity.getDurationMs());
        record.setExtraData(entity.getExtraData());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

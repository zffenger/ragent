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
import com.nageoffer.ai.ragent.rag.domain.entity.RagTraceNode;
import com.nageoffer.ai.ragent.rag.domain.repository.RagTraceNodeRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.RagTraceNodeMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.RagTraceNodeDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * RAG Trace 节点仓储实现
 */
@Repository
@RequiredArgsConstructor
public class RagTraceNodeRepositoryImpl implements RagTraceNodeRepository {

    private final RagTraceNodeMapper nodeMapper;

    @Override
    public void save(RagTraceNode node) {
        RagTraceNodeDO record = toDO(node);
        nodeMapper.insert(record);
    }

    @Override
    public void updateByTraceIdAndNodeId(String traceId, String nodeId, RagTraceNode node) {
        RagTraceNodeDO record = toDO(node);
        nodeMapper.update(
                record,
                Wrappers.lambdaUpdate(RagTraceNodeDO.class)
                        .eq(RagTraceNodeDO::getTraceId, traceId)
                        .eq(RagTraceNodeDO::getNodeId, nodeId)
        );
    }

    private RagTraceNode toEntity(RagTraceNodeDO record) {
        if (record == null) {
            return null;
        }
        RagTraceNode entity = new RagTraceNode();
        entity.setId(record.getId());
        entity.setTraceId(record.getTraceId());
        entity.setNodeId(record.getNodeId());
        entity.setParentNodeId(record.getParentNodeId());
        entity.setDepth(record.getDepth());
        entity.setNodeType(record.getNodeType());
        entity.setNodeName(record.getNodeName());
        entity.setClassName(record.getClassName());
        entity.setMethodName(record.getMethodName());
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

    private RagTraceNodeDO toDO(RagTraceNode entity) {
        if (entity == null) {
            return null;
        }
        RagTraceNodeDO record = new RagTraceNodeDO();
        record.setId(entity.getId());
        record.setTraceId(entity.getTraceId());
        record.setNodeId(entity.getNodeId());
        record.setParentNodeId(entity.getParentNodeId());
        record.setDepth(entity.getDepth());
        record.setNodeType(entity.getNodeType());
        record.setNodeName(entity.getNodeName());
        record.setClassName(entity.getClassName());
        record.setMethodName(entity.getMethodName());
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

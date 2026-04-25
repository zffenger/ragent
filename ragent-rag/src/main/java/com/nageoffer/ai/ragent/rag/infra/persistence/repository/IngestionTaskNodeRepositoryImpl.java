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
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionTaskNode;
import com.nageoffer.ai.ragent.rag.domain.repository.IngestionTaskNodeRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.IngestionTaskNodeMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.IngestionTaskNodeDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摄取任务节点仓储实现
 */
@Repository
@RequiredArgsConstructor
public class IngestionTaskNodeRepositoryImpl implements IngestionTaskNodeRepository {

    private final IngestionTaskNodeMapper ingestionTaskNodeMapper;

    @Override
    public IngestionTaskNode findById(String id) {
        IngestionTaskNodeDO record = ingestionTaskNodeMapper.selectOne(
                Wrappers.lambdaQuery(IngestionTaskNodeDO.class)
                        .eq(IngestionTaskNodeDO::getId, id)
                        .eq(IngestionTaskNodeDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<IngestionTaskNode> findByTaskId(String taskId) {
        List<IngestionTaskNodeDO> records = ingestionTaskNodeMapper.selectList(
                Wrappers.lambdaQuery(IngestionTaskNodeDO.class)
                        .eq(IngestionTaskNodeDO::getTaskId, taskId)
                        .eq(IngestionTaskNodeDO::getDeleted, 0)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void save(IngestionTaskNode node) {
        IngestionTaskNodeDO record = toDO(node);
        ingestionTaskNodeMapper.insert(record);
    }


    private IngestionTaskNode toEntity(IngestionTaskNodeDO record) {
        if (record == null) {
            return null;
        }
        return IngestionTaskNode.builder()
                .id(record.getId())
                .taskId(record.getTaskId())
                .pipelineId(record.getPipelineId())
                .nodeId(record.getNodeId())
                .nodeType(record.getNodeType())
                .nodeOrder(record.getNodeOrder())
                .status(record.getStatus())
                .durationMs(record.getDurationMs())
                .message(record.getMessage())
                .errorMessage(record.getErrorMessage())
                .outputJson(record.getOutputJson())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private IngestionTaskNodeDO toDO(IngestionTaskNode entity) {
        if (entity == null) {
            return null;
        }
        IngestionTaskNodeDO record = new IngestionTaskNodeDO();
        record.setId(entity.getId());
        record.setTaskId(entity.getTaskId());
        record.setPipelineId(entity.getPipelineId());
        record.setNodeId(entity.getNodeId());
        record.setNodeType(entity.getNodeType());
        record.setNodeOrder(entity.getNodeOrder());
        record.setStatus(entity.getStatus());
        record.setDurationMs(entity.getDurationMs());
        record.setMessage(entity.getMessage());
        record.setErrorMessage(entity.getErrorMessage());
        record.setOutputJson(entity.getOutputJson());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

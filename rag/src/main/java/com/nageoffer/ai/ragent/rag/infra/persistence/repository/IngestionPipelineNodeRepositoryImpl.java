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
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionPipelineNode;
import com.nageoffer.ai.ragent.rag.domain.repository.IngestionPipelineNodeRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.IngestionPipelineNodeMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.IngestionPipelineNodeDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摄取管道节点仓储实现
 */
@Repository
@RequiredArgsConstructor
public class IngestionPipelineNodeRepositoryImpl implements IngestionPipelineNodeRepository {

    private final IngestionPipelineNodeMapper ingestionPipelineNodeMapper;

    @Override
    public IngestionPipelineNode findById(String id) {
        IngestionPipelineNodeDO record = ingestionPipelineNodeMapper.selectOne(
                Wrappers.lambdaQuery(IngestionPipelineNodeDO.class)
                        .eq(IngestionPipelineNodeDO::getId, id)
                        .eq(IngestionPipelineNodeDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public List<IngestionPipelineNode> findByPipelineId(String pipelineId) {
        List<IngestionPipelineNodeDO> records = ingestionPipelineNodeMapper.selectList(
                Wrappers.lambdaQuery(IngestionPipelineNodeDO.class)
                        .eq(IngestionPipelineNodeDO::getPipelineId, pipelineId)
                        .eq(IngestionPipelineNodeDO::getDeleted, 0)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void save(IngestionPipelineNode node) {
        IngestionPipelineNodeDO record = toDO(node);
        ingestionPipelineNodeMapper.insert(record);
    }

    @Override
    public void deleteByPipelineId(String pipelineId) {
        ingestionPipelineNodeMapper.delete(
                Wrappers.lambdaQuery(IngestionPipelineNodeDO.class)
                        .eq(IngestionPipelineNodeDO::getPipelineId, pipelineId)
        );
    }

    private IngestionPipelineNode toEntity(IngestionPipelineNodeDO record) {
        if (record == null) {
            return null;
        }
        return IngestionPipelineNode.builder()
                .id(record.getId())
                .pipelineId(record.getPipelineId())
                .nodeId(record.getNodeId())
                .nodeType(record.getNodeType())
                .nextNodeId(record.getNextNodeId())
                .settingsJson(record.getSettingsJson())
                .conditionJson(record.getConditionJson())
                .createdBy(record.getCreatedBy())
                .updatedBy(record.getUpdatedBy())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private IngestionPipelineNodeDO toDO(IngestionPipelineNode entity) {
        if (entity == null) {
            return null;
        }
        IngestionPipelineNodeDO record = new IngestionPipelineNodeDO();
        record.setId(entity.getId());
        record.setPipelineId(entity.getPipelineId());
        record.setNodeId(entity.getNodeId());
        record.setNodeType(entity.getNodeType());
        record.setNextNodeId(entity.getNextNodeId());
        record.setSettingsJson(entity.getSettingsJson());
        record.setConditionJson(entity.getConditionJson());
        record.setCreatedBy(entity.getCreatedBy());
        record.setUpdatedBy(entity.getUpdatedBy());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

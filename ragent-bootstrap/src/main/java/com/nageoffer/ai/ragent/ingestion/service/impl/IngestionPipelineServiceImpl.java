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

package com.nageoffer.ai.ragent.ingestion.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nageoffer.ai.ragent.ingestion.controller.request.IngestionPipelineCreateRequest;
import com.nageoffer.ai.ragent.ingestion.controller.request.IngestionPipelineNodeRequest;
import com.nageoffer.ai.ragent.ingestion.controller.request.IngestionPipelineUpdateRequest;
import com.nageoffer.ai.ragent.ingestion.controller.vo.IngestionPipelineNodeVO;
import com.nageoffer.ai.ragent.ingestion.controller.vo.IngestionPipelineVO;
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionPipeline;
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionPipelineNode;
import com.nageoffer.ai.ragent.rag.domain.repository.IngestionPipelineRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.IngestionPipelineNodeRepository;
import com.nageoffer.ai.ragent.framework.context.UserContext;
import com.nageoffer.ai.ragent.framework.exception.ClientException;
import com.nageoffer.ai.ragent.ingestion.domain.enums.IngestionNodeType;
import com.nageoffer.ai.ragent.ingestion.domain.pipeline.NodeConfig;
import com.nageoffer.ai.ragent.ingestion.domain.pipeline.PipelineDefinition;
import com.nageoffer.ai.ragent.ingestion.service.IngestionPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据清洗流水线业务逻辑实现
 */
@Service
@RequiredArgsConstructor
public class IngestionPipelineServiceImpl implements IngestionPipelineService {

    private final IngestionPipelineRepository pipelineRepository;
    private final IngestionPipelineNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestionPipelineVO create(IngestionPipelineCreateRequest request) {
        Assert.notNull(request, () -> new ClientException("请求不能为空"));
        IngestionPipeline pipeline = IngestionPipeline.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(UserContext.getUsername())
                .updatedBy(UserContext.getUsername())
                .build();
        try {
            pipelineRepository.save(pipeline);
        } catch (DuplicateKeyException dke) {
            throw new ClientException("流水线名称已存在");
        }
        upsertNodes(pipeline.getId(), request.getNodes());
        return toVO(pipeline, nodeRepository.findByPipelineId(pipeline.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestionPipelineVO update(String pipelineId, IngestionPipelineUpdateRequest request) {
        IngestionPipeline pipeline = pipelineRepository.findById(pipelineId);
        Assert.notNull(pipeline, () -> new ClientException("未找到流水线"));

        if (StringUtils.hasText(request.getName())) {
            pipeline.setName(request.getName());
        }
        if (request.getDescription() != null) {
            pipeline.setDescription(request.getDescription());
        }
        pipeline.setUpdatedBy(UserContext.getUsername());
        pipelineRepository.update(pipeline);

        if (request.getNodes() != null) {
            upsertNodes(pipeline.getId(), request.getNodes());
        }
        return toVO(pipeline, nodeRepository.findByPipelineId(pipeline.getId()));
    }

    @Override
    public IngestionPipelineVO get(String pipelineId) {
        IngestionPipeline pipeline = pipelineRepository.findById(pipelineId);
        Assert.notNull(pipeline, () -> new ClientException("未找到流水线"));
        return toVO(pipeline, nodeRepository.findByPipelineId(pipeline.getId()));
    }

    @Override
    public IPage<IngestionPipelineVO> page(Page<IngestionPipelineVO> page, String keyword) {
        IPage<IngestionPipeline> result = pipelineRepository.findPage(page, keyword);

        Page<IngestionPipelineVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(each -> toVO(each, nodeRepository.findByPipelineId(each.getId())))
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String pipelineId) {
        IngestionPipeline pipeline = pipelineRepository.findById(pipelineId);
        Assert.notNull(pipeline, () -> new ClientException("未找到流水线"));
        // 删除管道节点
        nodeRepository.deleteByPipelineId(pipelineId);
        // 删除管道（软删除）
        pipelineRepository.deleteById(pipelineId);
    }

    @Override
    public PipelineDefinition getDefinition(String pipelineId) {
        IngestionPipeline pipeline = pipelineRepository.findById(pipelineId);
        Assert.notNull(pipeline, () -> new ClientException("未找到流水线"));

        List<NodeConfig> nodes = nodeRepository.findByPipelineId(pipeline.getId()).stream()
                .map(this::toNodeConfig)
                .toList();
        return PipelineDefinition.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .description(pipeline.getDescription())
                .nodes(nodes)
                .build();
    }

    private void upsertNodes(String pipelineId, List<IngestionPipelineNodeRequest> nodes) {
        if (nodes == null) {
            return;
        }
        // 先删除旧节点
        nodeRepository.deleteByPipelineId(pipelineId);
        // 再插入新节点
        for (IngestionPipelineNodeRequest node : nodes) {
            if (node == null) {
                continue;
            }
            IngestionPipelineNode entity = IngestionPipelineNode.builder()
                    .pipelineId(pipelineId)
                    .nodeId(node.getNodeId())
                    .nodeType(normalizeNodeType(node.getNodeType()))
                    .nextNodeId(node.getNextNodeId())
                    .settingsJson(toJson(node.getSettings()))
                    .conditionJson(toJson(node.getCondition()))
                    .createdBy(UserContext.getUsername())
                    .updatedBy(UserContext.getUsername())
                    .build();
			nodeRepository.save(entity);
        }
    }

    private IngestionPipelineVO toVO(IngestionPipeline pipeline, List<IngestionPipelineNode> nodes) {
        IngestionPipelineVO vo = BeanUtil.toBean(pipeline, IngestionPipelineVO.class);
        vo.setNodes(nodes.stream().map(this::toNodeVO).toList());
        return vo;
    }

    private IngestionPipelineNodeVO toNodeVO(IngestionPipelineNode node) {
        IngestionPipelineNodeVO vo = BeanUtil.toBean(node, IngestionPipelineNodeVO.class);
        vo.setNodeType(normalizeNodeTypeForOutput(node.getNodeType()));
        vo.setSettings(parseJson(node.getSettingsJson()));
        vo.setCondition(parseJson(node.getConditionJson()));
        return vo;
    }

    private NodeConfig toNodeConfig(IngestionPipelineNode node) {
        return NodeConfig.builder()
                .nodeId(node.getNodeId())
                .nodeType(normalizeNodeType(node.getNodeType()))
                .settings(parseJson(node.getSettingsJson()))
                .condition(parseJson(node.getConditionJson()))
                .nextNodeId(node.getNextNodeId())
                .build();
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString();
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeNodeType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return nodeType;
        }
        try {
            return IngestionNodeType.fromValue(nodeType).getValue();
        } catch (IllegalArgumentException ex) {
            throw new ClientException("未知节点类型: " + nodeType);
        }
    }

    private String normalizeNodeTypeForOutput(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return nodeType;
        }
        try {
            return IngestionNodeType.fromValue(nodeType).getValue();
        } catch (IllegalArgumentException ex) {
            return nodeType;
        }
    }
}

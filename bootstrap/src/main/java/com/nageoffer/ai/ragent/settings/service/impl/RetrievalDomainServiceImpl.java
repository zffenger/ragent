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

package com.nageoffer.ai.ragent.settings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.knowledge.dao.entity.KnowledgeBaseDO;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.KnowledgeBaseMapper;
import com.nageoffer.ai.ragent.settings.controller.vo.RetrievalDomainVO;
import com.nageoffer.ai.ragent.chatbot.infra.persistence.po.ChatBotDO;
import com.nageoffer.ai.ragent.settings.dao.entity.DomainKnowledgeDO;
import com.nageoffer.ai.ragent.settings.dao.entity.RetrievalDomainDO;
import com.nageoffer.ai.ragent.chatbot.infra.persistence.mapper.ChatBotMapper;
import com.nageoffer.ai.ragent.settings.dao.mapper.DomainKnowledgeMapper;
import com.nageoffer.ai.ragent.settings.dao.mapper.RetrievalDomainMapper;
import com.nageoffer.ai.ragent.settings.service.RetrievalDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索域服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalDomainServiceImpl implements RetrievalDomainService {

    private final RetrievalDomainMapper retrievalDomainMapper;
    private final DomainKnowledgeMapper domainKnowledgeMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatBotMapper chatBotMapper;

    @Override
    public List<RetrievalDomainVO> listAll() {
        List<RetrievalDomainDO> domains = retrievalDomainMapper.selectList(
                new LambdaQueryWrapper<RetrievalDomainDO>().orderByAsc(RetrievalDomainDO::getName)
        );

        // 获取每个检索域关联的知识库
        List<String> domainIds = domains.stream().map(RetrievalDomainDO::getId).toList();
        Map<String, List<String>> domainKnowledgeMap = getDomainKnowledgeMap(domainIds);

        // 获取每个检索域绑定的机器人数量
        Map<String, Long> botCountMap = getBotCountMap(domainIds);

        return domains.stream().map(domain -> {
            List<String> knowledgeIds = domainKnowledgeMap.getOrDefault(domain.getId(), List.of());
            List<RetrievalDomainVO.KnowledgeBrief> knowledges = getKnowledgeBriefs(knowledgeIds);

            return RetrievalDomainVO.builder()
                    .id(domain.getId())
                    .name(domain.getName())
                    .description(domain.getDescription())
                    .enabled(Integer.valueOf(1).equals(domain.getEnabled()))
                    .knowledgeIds(knowledgeIds)
                    .knowledges(knowledges)
                    .botCount(botCountMap.getOrDefault(domain.getId(), 0L).intValue())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public RetrievalDomainVO getById(String id) {
        RetrievalDomainDO domain = retrievalDomainMapper.selectById(id);
        if (domain == null) {
            return null;
        }

        List<String> knowledgeIds = getKnowledgeIds(id);
        List<RetrievalDomainVO.KnowledgeBrief> knowledges = getKnowledgeBriefs(knowledgeIds);

        return RetrievalDomainVO.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .enabled(Integer.valueOf(1).equals(domain.getEnabled()))
                .knowledgeIds(knowledgeIds)
                .knowledges(knowledges)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetrievalDomainVO create(RetrievalDomainVO vo) {
        // 检查名称是否重复
        Long count = retrievalDomainMapper.selectCount(
                new LambdaQueryWrapper<RetrievalDomainDO>().eq(RetrievalDomainDO::getName, vo.getName())
        );
        if (count > 0) {
            throw new ServiceException("检索域名称已存在: " + vo.getName());
        }

        RetrievalDomainDO entity = RetrievalDomainDO.builder()
                .name(vo.getName())
                .description(vo.getDescription())
                .enabled(Boolean.TRUE.equals(vo.getEnabled()) ? 1 : 0)
                .build();
        retrievalDomainMapper.insert(entity);

        // 绑定知识库
        if (vo.getKnowledgeIds() != null && !vo.getKnowledgeIds().isEmpty()) {
            bindKnowledges(entity.getId(), vo.getKnowledgeIds());
        }

        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetrievalDomainVO update(String id, RetrievalDomainVO vo) {
        RetrievalDomainDO entity = retrievalDomainMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("检索域不存在: " + id);
        }

        // 检查名称是否重复（排除自己）
        if (vo.getName() != null && !vo.getName().equals(entity.getName())) {
            Long count = retrievalDomainMapper.selectCount(
                    new LambdaQueryWrapper<RetrievalDomainDO>()
                            .eq(RetrievalDomainDO::getName, vo.getName())
                            .ne(RetrievalDomainDO::getId, id)
            );
            if (count > 0) {
                throw new ServiceException("检索域名称已存在: " + vo.getName());
            }
            entity.setName(vo.getName());
        }

        if (vo.getDescription() != null) {
            entity.setDescription(vo.getDescription());
        }
        if (vo.getEnabled() != null) {
            entity.setEnabled(Boolean.TRUE.equals(vo.getEnabled()) ? 1 : 0);
        }
        retrievalDomainMapper.updateById(entity);

        // 更新知识库绑定
        if (vo.getKnowledgeIds() != null) {
            // 先删除旧的绑定
            domainKnowledgeMapper.delete(
                    new LambdaQueryWrapper<DomainKnowledgeDO>().eq(DomainKnowledgeDO::getDomainId, id)
            );
            // 再添加新的绑定
            if (!vo.getKnowledgeIds().isEmpty()) {
                bindKnowledges(id, vo.getKnowledgeIds());
            }
        }

        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        // 检查是否有机器人绑定
        Long botCount = chatBotMapper.selectCount(
                new LambdaQueryWrapper<ChatBotDO>().eq(ChatBotDO::getDomainId, id)
        );
        if (botCount > 0) {
            throw new ServiceException("该检索域已绑定机器人，无法删除");
        }

        // 删除知识库绑定
        domainKnowledgeMapper.delete(
                new LambdaQueryWrapper<DomainKnowledgeDO>().eq(DomainKnowledgeDO::getDomainId, id)
        );

        // 删除检索域
        retrievalDomainMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindKnowledges(String domainId, List<String> knowledgeIds) {
        // 验证检索域存在
        RetrievalDomainDO domain = retrievalDomainMapper.selectById(domainId);
        if (domain == null) {
            throw new ServiceException("检索域不存在: " + domainId);
        }

        // 验证知识库存在
        for (String knowledgeId : knowledgeIds) {
            KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(knowledgeId);
            if (kb == null) {
                throw new ServiceException("知识库不存在: " + knowledgeId);
            }
        }

        // 添加绑定（跳过已存在的）
        for (int i = 0; i < knowledgeIds.size(); i++) {
            String knowledgeId = knowledgeIds.get(i);
            // 检查是否已存在
            Long count = domainKnowledgeMapper.selectCount(
                    new LambdaQueryWrapper<DomainKnowledgeDO>()
                            .eq(DomainKnowledgeDO::getDomainId, domainId)
                            .eq(DomainKnowledgeDO::getKnowledgeId, knowledgeId)
            );
            if (count == 0) {
                DomainKnowledgeDO entity = DomainKnowledgeDO.builder()
                        .domainId(domainId)
                        .knowledgeId(knowledgeId)
                        .priority(100 + i)
                        .build();
                domainKnowledgeMapper.insert(entity);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindKnowledge(String domainId, String knowledgeId) {
        domainKnowledgeMapper.delete(
                new LambdaQueryWrapper<DomainKnowledgeDO>()
                        .eq(DomainKnowledgeDO::getDomainId, domainId)
                        .eq(DomainKnowledgeDO::getKnowledgeId, knowledgeId)
        );
    }

    @Override
    public List<String> getKnowledgeIds(String domainId) {
        List<DomainKnowledgeDO> relations = domainKnowledgeMapper.selectList(
                new LambdaQueryWrapper<DomainKnowledgeDO>()
                        .eq(DomainKnowledgeDO::getDomainId, domainId)
                        .orderByAsc(DomainKnowledgeDO::getPriority)
        );
        return relations.stream().map(DomainKnowledgeDO::getKnowledgeId).collect(Collectors.toList());
    }

    /**
     * 获取检索域-知识库映射
     */
    private Map<String, List<String>> getDomainKnowledgeMap(List<String> domainIds) {
        if (domainIds.isEmpty()) {
            return Map.of();
        }
        List<DomainKnowledgeDO> relations = domainKnowledgeMapper.selectList(
                new LambdaQueryWrapper<DomainKnowledgeDO>()
                        .in(DomainKnowledgeDO::getDomainId, domainIds)
                        .orderByAsc(DomainKnowledgeDO::getPriority)
        );
        return relations.stream().collect(Collectors.groupingBy(
                DomainKnowledgeDO::getDomainId,
                Collectors.mapping(DomainKnowledgeDO::getKnowledgeId, Collectors.toList())
        ));
    }

    /**
     * 获取机器人数量映射
     */
    private Map<String, Long> getBotCountMap(List<String> domainIds) {
        if (domainIds.isEmpty()) {
            return Map.of();
        }
        List<ChatBotDO> bots = chatBotMapper.selectList(
                new LambdaQueryWrapper<ChatBotDO>()
                        .in(ChatBotDO::getDomainId, domainIds)
        );
        return bots.stream().collect(Collectors.groupingBy(
                ChatBotDO::getDomainId,
                Collectors.counting()
        ));
    }

    /**
     * 获取知识库简要信息
     */
    private List<RetrievalDomainVO.KnowledgeBrief> getKnowledgeBriefs(List<String> knowledgeIds) {
        if (knowledgeIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<KnowledgeBaseDO> knowledgeBases = knowledgeBaseMapper.selectBatchIds(knowledgeIds);
        // 按传入顺序排序
        Map<String, KnowledgeBaseDO> kbMap = knowledgeBases.stream()
                .collect(Collectors.toMap(KnowledgeBaseDO::getId, kb -> kb));
        return knowledgeIds.stream()
                .map(kbMap::get)
                .filter(java.util.Objects::nonNull)
                .map(kb -> RetrievalDomainVO.KnowledgeBrief.builder()
                        .id(kb.getId())
                        .name(kb.getName())
                        .build())
                .collect(Collectors.toList());
    }
}

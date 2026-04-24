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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.llm.domain.repository.ModelConfigRepository;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelGroupConfigVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelProviderVO;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelCandidateDO;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelProviderDO;
import com.nageoffer.ai.ragent.llm.infra.persistence.mapper.ModelCandidateMapper;
import com.nageoffer.ai.ragent.llm.infra.persistence.mapper.ModelProviderMapper;
import com.nageoffer.ai.ragent.settings.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final ModelProviderMapper modelProviderMapper;
    private final ModelCandidateMapper modelCandidateMapper;
    private final StringRedisTemplate redisTemplate;
	private final ModelConfigRepository modelConfigRepository;

    // 配置键常量
    private static final String CONFIG_KEY_CHAT = "ai.chat";
    private static final String CONFIG_KEY_EMBEDDING = "ai.embedding";
    private static final String CONFIG_KEY_RERANK = "ai.rerank";

    // Redis 缓存键前缀
    private static final String REDIS_PREFIX_CONFIG = "config:";

    @Override
    public ModelGroupConfigVO getModelGroupConfig(ModelCandidateDO.ModelType modelType) {
        // 查询该类型的所有模型候选
        List<ModelCandidateDO> candidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, modelType.name())
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        // 找出默认模型和深度思考模型
        final String[] defaultModel = {null};
        final String[] deepThinkingModel = {null};

        List<ModelGroupConfigVO.ModelCandidateVO> candidateVOs = candidates.stream()
                .map(c -> {
                    if (Integer.valueOf(1).equals(c.getIsDefault())) {
                        defaultModel[0] = c.getModelId();
                    }
                    if (Integer.valueOf(1).equals(c.getIsDeepThinking())) {
                        deepThinkingModel[0] = c.getModelId();
                    }
                    return ModelGroupConfigVO.ModelCandidateVO.builder()
                            .id(c.getId())
                            .modelId(c.getModelId())
                            .provider(c.getProviderName())
                            .model(c.getModelName())
                            .url(c.getUrl())
                            .dimension(c.getDimension())
                            .priority(c.getPriority())
                            .enabled(Integer.valueOf(1).equals(c.getEnabled()))
                            .supportsThinking(Integer.valueOf(1).equals(c.getSupportsThinking()))
                            .isDefault(Integer.valueOf(1).equals(c.getIsDefault()))
                            .isDeepThinking(Integer.valueOf(1).equals(c.getIsDeepThinking()))
                            .build();
                })
                .collect(Collectors.toList());

        return ModelGroupConfigVO.builder()
                .defaultModel(defaultModel[0])
                .deepThinkingModel(deepThinkingModel[0])
                .candidates(candidateVOs)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModelGroupConfig(ModelCandidateDO.ModelType modelType, ModelGroupConfigVO config) {
        if (config == null || config.getCandidates() == null) {
            return;
        }

        // 更新候选列表
        for (ModelGroupConfigVO.ModelCandidateVO candidate : config.getCandidates()) {
            if (candidate.getId() != null) {
                // 更新现有候选
                ModelCandidateDO entity = new ModelCandidateDO();
                entity.setId(candidate.getId());
                entity.setModelId(candidate.getModelId());
                entity.setProviderName(candidate.getProvider());
                entity.setModelName(candidate.getModel());
                entity.setUrl(candidate.getUrl());
                entity.setDimension(candidate.getDimension());
                entity.setPriority(candidate.getPriority());
                entity.setEnabled(candidate.getEnabled() != null && candidate.getEnabled() ? 1 : 0);
                entity.setSupportsThinking(candidate.getSupportsThinking() != null && candidate.getSupportsThinking() ? 1 : 0);
                modelCandidateMapper.updateById(entity);
            } else {
                // 新增候选
                ModelCandidateDO entity = ModelCandidateDO.builder()
                        .modelId(candidate.getModelId())
                        .modelType(modelType.name())
                        .providerName(candidate.getProvider())
                        .modelName(candidate.getModel())
                        .url(candidate.getUrl())
                        .dimension(candidate.getDimension())
                        .priority(candidate.getPriority() != null ? candidate.getPriority() : 100)
                        .enabled(candidate.getEnabled() != null && candidate.getEnabled() ? 1 : 0)
                        .supportsThinking(candidate.getSupportsThinking() != null && candidate.getSupportsThinking() ? 1 : 0)
                        .isDefault(0)
                        .isDeepThinking(0)
                        .build();
                modelCandidateMapper.insert(entity);
            }
        }

        // 更新缓存
        refreshConfigCache();
    }

    @Override
    public List<ModelProviderVO> listModelProviders() {
        List<ModelProviderDO> providers = modelProviderMapper.selectList(
                new LambdaQueryWrapper<ModelProviderDO>().orderByAsc(ModelProviderDO::getName)
        );

        return providers.stream()
                .map(p -> {
                    Map<String, String> endpoints = null;
                    if (p.getEndpoints() != null && !p.getEndpoints().isBlank()) {
                        endpoints = JSON.parseObject(p.getEndpoints(), new TypeReference<Map<String, String>>() {});
                    }
                    return ModelProviderVO.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .url(p.getUrl())
                            .apiKey(p.getApiKey())
                            .endpoints(endpoints != null ? endpoints : new HashMap<>())
                            .enabled(Integer.valueOf(1).equals(p.getEnabled()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderVO createModelProvider(ModelProviderVO vo) {
        // 检查名称是否重复
        Long count = modelProviderMapper.selectCount(
                new LambdaQueryWrapper<ModelProviderDO>().eq(ModelProviderDO::getName, vo.getName())
        );
        if (count > 0) {
            throw new ServiceException("提供商名称已存在: " + vo.getName());
        }

        ModelProviderDO entity = ModelProviderDO.builder()
                .name(vo.getName())
                .url(vo.getUrl())
                .apiKey(vo.getApiKey())
                .endpoints(vo.getEndpoints() != null ? JSON.toJSONString(vo.getEndpoints()) : null)
                .enabled(vo.getEnabled() != null && vo.getEnabled() ? 1 : 0)
                .build();
        modelProviderMapper.insert(entity);

        refreshConfigCache();

        return ModelProviderVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .apiKey(entity.getApiKey())
                .endpoints(vo.getEndpoints())
                .enabled(Integer.valueOf(1).equals(entity.getEnabled()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelProviderVO updateModelProvider(String id, ModelProviderVO vo) {
        ModelProviderDO entity = modelProviderMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("提供商不存在: " + id);
        }

        entity.setUrl(vo.getUrl());
        entity.setApiKey(vo.getApiKey());
        entity.setEndpoints(vo.getEndpoints() != null ? JSON.toJSONString(vo.getEndpoints()) : null);
        entity.setEnabled(vo.getEnabled() != null && vo.getEnabled() ? 1 : 0);
        modelProviderMapper.updateById(entity);

        refreshConfigCache();

        return ModelProviderVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .apiKey(entity.getApiKey())
                .endpoints(vo.getEndpoints())
                .enabled(Integer.valueOf(1).equals(entity.getEnabled()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelProvider(String id) {
        modelProviderMapper.deleteById(id);
        refreshConfigCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultModel(String candidateId, ModelCandidateDO.ModelType modelType) {
        // 清除同类型其他模型的默认标记
        modelCandidateMapper.update(null,
                new LambdaUpdateWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, modelType.name())
                        .set(ModelCandidateDO::getIsDefault, 0)
        );

        // 设置新的默认模型
        ModelCandidateDO entity = new ModelCandidateDO();
        entity.setId(candidateId);
        entity.setIsDefault(1);
        modelCandidateMapper.updateById(entity);

        refreshConfigCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDeepThinkingModel(String candidateId) {
        // 清除其他模型的深度思考标记
        modelCandidateMapper.update(null,
                new LambdaUpdateWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.CHAT.name())
                        .set(ModelCandidateDO::getIsDeepThinking, 0)
        );

        // 设置新的深度思考模型
        ModelCandidateDO entity = new ModelCandidateDO();
        entity.setId(candidateId);
        entity.setIsDeepThinking(1);
        modelCandidateMapper.updateById(entity);

        refreshConfigCache();
    }

    @Override
    public void refreshConfigCache() {
        // 清除 Redis 缓存
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_CHAT);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_EMBEDDING);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_RERANK);

        // 触发配置刷新
		modelConfigRepository.refreshCache();

        log.info("配置缓存已刷新");
    }
}

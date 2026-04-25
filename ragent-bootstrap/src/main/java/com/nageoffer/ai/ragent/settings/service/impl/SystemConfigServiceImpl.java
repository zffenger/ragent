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

import com.nageoffer.ai.ragent.llm.domain.repository.ModelConfigRepository;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelCandidateConfig;
import com.nageoffer.ai.ragent.llm.domain.vo.ProviderConfig;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelCandidateVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelGroupConfigVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelProviderVO;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelCandidateDO;
import com.nageoffer.ai.ragent.settings.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final StringRedisTemplate redisTemplate;
    private final ModelConfigRepository modelConfigRepository;

    private static final String CONFIG_KEY_CHAT = "ai.chat";
    private static final String CONFIG_KEY_EMBEDDING = "ai.embedding";
    private static final String CONFIG_KEY_RERANK = "ai.rerank";
    private static final String REDIS_PREFIX_CONFIG = "config:";

    // ==================== 模型组配置查询 ====================

    @Override
    public ModelGroupConfigVO getChatModelGroupConfig() {
        List<ModelCandidateConfig> candidateConfigs = modelConfigRepository.getChatCandidates();

        List<ModelCandidateVO> candidateVOs = candidateConfigs.stream()
                .map(this::toGroupCandidateVO)
                .collect(Collectors.toList());

        return ModelGroupConfigVO.builder()
                .defaultModel(modelConfigRepository.getChatDefaultModel())
                .deepThinkingModel(modelConfigRepository.getChatDeepThinkingModel())
                .candidates(candidateVOs)
                .build();
    }

    @Override
    public ModelGroupConfigVO getEmbeddingModelGroupConfig() {
        List<ModelCandidateConfig> candidateConfigs = modelConfigRepository.getEmbeddingCandidates();

        List<ModelCandidateVO> candidateVOs = candidateConfigs.stream()
                .map(this::toGroupCandidateVO)
                .collect(Collectors.toList());

        return ModelGroupConfigVO.builder()
                .defaultModel(modelConfigRepository.getEmbeddingDefaultModel())
                .deepThinkingModel(null)
                .candidates(candidateVOs)
                .build();
    }

    @Override
    public ModelGroupConfigVO getRerankModelGroupConfig() {
        List<ModelCandidateConfig> candidateConfigs = modelConfigRepository.getRerankCandidates();

        List<ModelCandidateVO> candidateVOs = candidateConfigs.stream()
                .map(this::toGroupCandidateVO)
                .collect(Collectors.toList());

        return ModelGroupConfigVO.builder()
                .defaultModel(modelConfigRepository.getRerankDefaultModel())
                .deepThinkingModel(null)
                .candidates(candidateVOs)
                .build();
    }

    // ==================== 模型候选 CRUD ====================

    @Override
    public ModelCandidateVO createModelCandidate(ModelCandidateVO vo) {
        ModelCandidateConfig config = toCandidateConfig(vo);
        modelConfigRepository.createModelConfig(config);
        clearRedisCache();
        return vo;
    }

    @Override
    public ModelCandidateVO updateModelCandidate(String id, ModelCandidateVO vo) {
        vo.setId(id);
        ModelCandidateConfig config = toCandidateConfig(vo);
        modelConfigRepository.updateModelConfig(config);
        clearRedisCache();
        return vo;
    }

    @Override
    public void deleteModelCandidate(String id) {
        modelConfigRepository.deleteModelConfig(id);
        clearRedisCache();
    }

    @Override
    public void setDefaultModel(String candidateId, ModelCandidateDO.ModelType modelType) {
        modelConfigRepository.setDefaultModel(candidateId, modelType.name());
        clearRedisCache();
    }

    @Override
    public void setDeepThinkingModel(String candidateId) {
        modelConfigRepository.setDeepThinkingModel(candidateId);
        clearRedisCache();
    }

    // ==================== 模型提供商管理 ====================

    @Override
    public List<ModelProviderVO> listModelProviders() {
        return modelConfigRepository.listAllProviders().stream()
                .map(this::toProviderVO)
                .collect(Collectors.toList());
    }

    @Override
    public ModelProviderVO createModelProvider(ModelProviderVO vo) {
        ProviderConfig provider = new ProviderConfig(
                null,
                vo.getName(),
                vo.getUrl(),
                vo.getApiKey(),
                vo.getEndpoints() != null ? vo.getEndpoints() : new HashMap<>(),
                vo.getEnabled() != null && vo.getEnabled()
        );

        ProviderConfig created = modelConfigRepository.createProvider(provider);
        clearRedisCache();

        return toProviderVO(created);
    }

    @Override
    public ModelProviderVO updateModelProvider(String id, ModelProviderVO vo) {
        ProviderConfig provider = new ProviderConfig(
                id,
                null,
                vo.getUrl(),
                vo.getApiKey(),
                vo.getEndpoints() != null ? vo.getEndpoints() : new HashMap<>(),
                vo.getEnabled() != null && vo.getEnabled()
        );

        ProviderConfig updated = modelConfigRepository.updateProvider(provider);
        clearRedisCache();

        return toProviderVO(updated);
    }

    @Override
    public void deleteModelProvider(String id) {
        modelConfigRepository.deleteProvider(id);
        clearRedisCache();
    }

    // ==================== 配置刷新 ====================

    @Override
    public void refreshConfigCache() {
        clearRedisCache();
        modelConfigRepository.refreshCache();
        log.info("配置缓存已刷新");
    }

    // ==================== 私有方法 ====================

    private void clearRedisCache() {
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_CHAT);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_EMBEDDING);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_RERANK);
    }

    private ModelCandidateVO toGroupCandidateVO(ModelCandidateConfig config) {
        return ModelCandidateVO.builder()
                .id(config.id())
                .modelId(config.modelId())
                .provider(config.provider())
                .model(config.modelName())
                .url(config.url())
                .dimension(config.dimension())
                .priority(config.priority())
                .enabled(config.enabled())
                .supportsThinking(config.supportsThinking())
                .isDefault(config.isDefault())
                .isDeepThinking(config.isDeepThinking())
                .build();
    }

    private ModelCandidateConfig toCandidateConfig(ModelCandidateVO vo) {
        return ModelCandidateConfig.builder()
                .id(vo.getId())
                .modelId(vo.getModelId())
                .modelType(vo.getModelType())
                .provider(vo.getProvider())
                .modelName(vo.getModel())
                .url(vo.getUrl())
                .dimension(vo.getDimension())
                .priority(vo.getPriority())
                .enabled(vo.getEnabled() != null && vo.getEnabled())
                .supportsThinking(vo.getSupportsThinking() != null && vo.getSupportsThinking())
                .isDefault(false)
                .isDeepThinking(false)
                .build();
    }

    private ModelProviderVO toProviderVO(ProviderConfig config) {
        return ModelProviderVO.builder()
                .id(config.id())
                .name(config.name())
                .url(config.url())
                .apiKey(config.apiKey())
                .endpoints(config.endpoints() != null ? config.endpoints() : new HashMap<>())
                .enabled(config.enabled())
                .build();
    }
}

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

package com.nageoffer.ai.ragent.llm.infra.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.llm.domain.repository.ModelConfigRepository;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelCandidateConfig;
import com.nageoffer.ai.ragent.llm.domain.vo.ProviderConfig;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelCandidateDO;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelProviderDO;
import com.nageoffer.ai.ragent.llm.infra.persistence.mapper.ModelCandidateMapper;
import com.nageoffer.ai.ragent.llm.infra.persistence.mapper.ModelProviderMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型配置仓储实现
 * <p>
 * 从数据库加载模型配置，支持实时刷新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbModelConfigRepository implements ModelConfigRepository {

    private final ModelProviderMapper modelProviderMapper;
    private final ModelCandidateMapper modelCandidateMapper;

    // 缓存的配置（volatile 保证可见性）
    private volatile Map<String, ProviderConfig> cachedProviders;
    private volatile List<ModelCandidateConfig> cachedChatCandidates;
    private volatile List<ModelCandidateConfig> cachedEmbeddingCandidates;
    private volatile List<ModelCandidateConfig> cachedRerankCandidates;
    private volatile String chatDefaultModel;
    private volatile String chatDeepThinkingModel;
    private volatile String embeddingDefaultModel;
    private volatile String rerankDefaultModel;

    @PostConstruct
    public void init() {
        log.info("从数据库加载模型配置...");
        refreshCache();
    }

    @Override
    public Map<String, ProviderConfig> getProviders() {
        return cachedProviders;
    }

    @Override
    public List<ModelCandidateConfig> getChatCandidates() {
        return cachedChatCandidates;
    }

    @Override
    public List<ModelCandidateConfig> getEmbeddingCandidates() {
        return cachedEmbeddingCandidates;
    }

    @Override
    public List<ModelCandidateConfig> getRerankCandidates() {
        return cachedRerankCandidates;
    }

    @Override
    public String getChatDefaultModel() {
        return chatDefaultModel;
    }

    @Override
    public String getChatDeepThinkingModel() {
        return chatDeepThinkingModel;
    }

    @Override
    public String getEmbeddingDefaultModel() {
        return embeddingDefaultModel;
    }

    @Override
    public String getRerankDefaultModel() {
        return rerankDefaultModel;
    }

    @Override
    public void refreshCache() {
        // 加载启用的提供商配置
		List<ProviderConfig> providers = listAllProviders();

        Map<String, ProviderConfig> newProviders = new HashMap<>();
        for (ProviderConfig provider : providers) {
            newProviders.put(provider.name(), provider);
        }

        // 加载模型候选配置
        List<ModelCandidateDO> chatCandidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.CHAT.name())
                        .eq(ModelCandidateDO::getEnabled, 1)
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        List<ModelCandidateDO> embeddingCandidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.EMBEDDING.name())
                        .eq(ModelCandidateDO::getEnabled, 1)
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        List<ModelCandidateDO> rerankCandidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.RERANK.name())
                        .eq(ModelCandidateDO::getEnabled, 1)
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        // 原子更新缓存
        this.cachedProviders = newProviders;
        this.cachedChatCandidates = chatCandidates.stream()
                .map(this::toModelCandidateConfig)
                .collect(Collectors.toList());
        this.cachedEmbeddingCandidates = embeddingCandidates.stream()
                .map(this::toModelCandidateConfig)
                .collect(Collectors.toList());
        this.cachedRerankCandidates = rerankCandidates.stream()
                .map(this::toModelCandidateConfig)
                .collect(Collectors.toList());

        // 提取默认模型
        this.chatDefaultModel = findDefaultModel(chatCandidates);
        this.chatDeepThinkingModel = findDeepThinkingModel(chatCandidates);
        this.embeddingDefaultModel = findDefaultModel(embeddingCandidates);
        this.rerankDefaultModel = findDefaultModel(rerankCandidates);

        validateConfig();

        log.info("模型配置加载完成，提供商: {}, Chat模型: {}, Embedding模型: {}, Rerank模型: {}",
                cachedProviders.size(),
                cachedChatCandidates.size(),
                cachedEmbeddingCandidates.size(),
                cachedRerankCandidates.size());
    }

    // ==================== 提供商管理 ====================

    @Override
    public List<ProviderConfig> listAllProviders() {
		List<ModelProviderDO> providers = modelProviderMapper.selectList(
				new LambdaQueryWrapper<ModelProviderDO>()
						.eq(ModelProviderDO::getEnabled, 1)
		);
        return providers.stream()
                .map(this::toProviderConfig)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProviderConfig createProvider(ProviderConfig provider) {
        Long count = modelProviderMapper.selectCount(
                new LambdaQueryWrapper<ModelProviderDO>().eq(ModelProviderDO::getName, provider.name())
        );
        if (count > 0) {
            throw new ServiceException("提供商名称已存在: " + provider.name());
        }

        ModelProviderDO entity = ModelProviderDO.builder()
                .name(provider.name())
                .url(provider.url())
                .apiKey(provider.apiKey())
                .endpoints(provider.endpoints() != null ? JSON.toJSONString(provider.endpoints()) : null)
                .enabled(provider.enabled() ? 1 : 0)
                .build();
        modelProviderMapper.insert(entity);

        refreshCache();
        return toProviderConfig(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProviderConfig updateProvider(ProviderConfig provider) {
        ModelProviderDO entity = modelProviderMapper.selectById(provider.id());
        if (entity == null) {
            throw new ServiceException("提供商不存在: " + provider.id());
        }

        entity.setUrl(provider.url());
        entity.setApiKey(provider.apiKey());
        entity.setEndpoints(provider.endpoints() != null ? JSON.toJSONString(provider.endpoints()) : null);
        entity.setEnabled(provider.enabled() ? 1 : 0);
        modelProviderMapper.updateById(entity);

        refreshCache();
        return toProviderConfig(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(String id) {
        modelProviderMapper.deleteById(id);
        refreshCache();
    }

    // ==================== 模型候选管理 ====================


	@Override
	public void createModelConfig(ModelCandidateConfig candidate) {
		ModelCandidateDO entity = ModelCandidateDO.builder()
				.modelId(candidate.modelId())
				.modelType(candidate.modelType())
				.providerName(candidate.provider())
				.modelName(candidate.modelName())
				.url(candidate.url())
				.dimension(candidate.dimension())
				.priority(candidate.priority() != null ? candidate.priority() : 100)
				.enabled(candidate.enabled() ? 1 : 0)
				.supportsThinking(candidate.supportsThinking() ? 1 : 0)
				.isDefault(0)
				.isDeepThinking(0)
				.build();
		modelCandidateMapper.insert(entity);
		refreshCache();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateModelConfig(ModelCandidateConfig candidate) {
		ModelCandidateDO exist = modelCandidateMapper.selectById(candidate.id());
		if (exist == null) {
			throw new ServiceException("模型不存在: " + candidate.id());
		}
		ModelCandidateDO entity = new ModelCandidateDO();
		entity.setId(candidate.id());
		entity.setModelId(candidate.modelId());
		entity.setProviderName(candidate.provider());
		entity.setModelName(candidate.modelName());
		entity.setUrl(candidate.url());
		entity.setDimension(candidate.dimension());
		entity.setPriority(candidate.priority());
		entity.setEnabled(candidate.enabled() ? 1 : 0);
		entity.setSupportsThinking(candidate.supportsThinking() ? 1 : 0);
		modelCandidateMapper.updateById(entity);

		refreshCache();
	}

	@Override
	public void deleteModelConfig(String id) {
		modelCandidateMapper.deleteById(id);
		refreshCache();
	}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultModel(String candidateId, String modelType) {
        modelCandidateMapper.update(null,
                new LambdaUpdateWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, modelType)
                        .set(ModelCandidateDO::getIsDefault, 0)
        );

        ModelCandidateDO entity = new ModelCandidateDO();
        entity.setId(candidateId);
        entity.setIsDefault(1);
        modelCandidateMapper.updateById(entity);

        refreshCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDeepThinkingModel(String candidateId) {
        modelCandidateMapper.update(null,
                new LambdaUpdateWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.CHAT.name())
                        .set(ModelCandidateDO::getIsDeepThinking, 0)
        );

        ModelCandidateDO entity = new ModelCandidateDO();
        entity.setId(candidateId);
        entity.setIsDeepThinking(1);
        modelCandidateMapper.updateById(entity);

        refreshCache();
    }

    // ==================== 私有方法 ====================

    private String findDefaultModel(List<ModelCandidateDO> candidates) {
        return candidates.stream()
                .filter(c -> Integer.valueOf(1).equals(c.getIsDefault()))
                .findFirst()
                .map(ModelCandidateDO::getModelId)
                .orElse(null);
    }

    private String findDeepThinkingModel(List<ModelCandidateDO> candidates) {
        return candidates.stream()
                .filter(c -> Integer.valueOf(1).equals(c.getIsDeepThinking()))
                .findFirst()
                .map(ModelCandidateDO::getModelId)
                .orElse(null);
    }

    private ProviderConfig toProviderConfig(ModelProviderDO entity) {
        Map<String, String> endpoints = new HashMap<>();
        if (entity.getEndpoints() != null && !entity.getEndpoints().isBlank()) {
            endpoints = JSON.parseObject(entity.getEndpoints(), new TypeReference<Map<String, String>>() {});
        }
        return new ProviderConfig(
                entity.getId(),
                entity.getName(),
                entity.getUrl(),
                entity.getApiKey(),
                endpoints,
                Integer.valueOf(1).equals(entity.getEnabled())
        );
    }

    private ModelCandidateConfig toModelCandidateConfig(ModelCandidateDO entity) {
        return ModelCandidateConfig.builder()
                .id(entity.getId())
                .modelId(entity.getModelId())
                .modelType(entity.getModelType())
                .provider(entity.getProviderName())
                .modelName(entity.getModelName())
                .url(entity.getUrl())
                .dimension(entity.getDimension())
                .priority(entity.getPriority())
                .enabled(Integer.valueOf(1).equals(entity.getEnabled()))
                .supportsThinking(Integer.valueOf(1).equals(entity.getSupportsThinking()))
                .isDefault(Integer.valueOf(1).equals(entity.getIsDefault()))
                .isDeepThinking(Integer.valueOf(1).equals(entity.getIsDeepThinking()))
                .build();
    }

    private void validateConfig() {
        StringBuilder errors = new StringBuilder();

        if (cachedProviders.isEmpty()) {
            errors.append("- 没有启用的模型提供商 (t_model_provider)\n");
        }
        if (cachedChatCandidates.isEmpty()) {
            errors.append("- 没有启用的 Chat 模型 (t_model_candidate)\n");
        }
        if (cachedEmbeddingCandidates.isEmpty()) {
            errors.append("- 没有启用的 Embedding 模型 (t_model_candidate)\n");
        }

        if (!cachedChatCandidates.isEmpty() && chatDefaultModel == null) {
            errors.append("- Chat 模型未设置默认模型 (is_default=1)\n");
        }
        if (!cachedEmbeddingCandidates.isEmpty() && embeddingDefaultModel == null) {
            errors.append("- Embedding 模型未设置默认模型 (is_default=1)\n");
        }

        if (!errors.isEmpty()) {
            log.warn(
                    "\n========================================\n" +
                    "模型配置校验失败\n" +
                    "========================================\n" +
                    errors +
                    "========================================\n" +
                    "请在数据库中配置相关数据\n" +
                    "========================================\n"
            );
        }
    }
}

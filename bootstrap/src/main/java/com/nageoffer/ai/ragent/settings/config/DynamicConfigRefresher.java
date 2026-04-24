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

package com.nageoffer.ai.ragent.settings.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.nageoffer.ai.ragent.llm.interfaces.config.AIModelProperties;
import com.nageoffer.ai.ragent.settings.dao.entity.ModelCandidateDO;
import com.nageoffer.ai.ragent.settings.dao.entity.ModelProviderDO;
import com.nageoffer.ai.ragent.settings.dao.mapper.ModelCandidateMapper;
import com.nageoffer.ai.ragent.settings.dao.mapper.ModelProviderMapper;
import jakarta.annotation.PostConstruct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态配置加载器
 * <p>
 * 从数据库加载模型配置到 AIModelProperties，支持实时刷新。
 * 模型配置完全由数据库管理，不再使用 YAML 配置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicConfigRefresher {

    private final ModelProviderMapper modelProviderMapper;
    private final ModelCandidateMapper modelCandidateMapper;
    private final AIModelProperties aiModelProperties;

	/**
	 * -- GETTER --
	 *  获取缓存的提供商配置
	 */
	// 缓存的配置
    @Getter
	private volatile Map<String, AIModelProperties.ProviderConfig> cachedProviders;
	/**
	 * -- GETTER --
	 *  获取缓存的 Chat 模型候选
	 */
	@Getter
	private volatile List<ModelCandidateDO> cachedChatCandidates;
	/**
	 * -- GETTER --
	 *  获取缓存的 Embedding 模型候选
	 */
	@Getter
	private volatile List<ModelCandidateDO> cachedEmbeddingCandidates;
	/**
	 * -- GETTER --
	 *  获取缓存的 Rerank 模型候选
	 */
	@Getter
	private volatile List<ModelCandidateDO> cachedRerankCandidates;

    /**
     * 应用启动时加载配置
     */
    @PostConstruct
    public void init() {
        log.info("从数据库加载模型配置...");
        refreshFromDatabase();
    }

    /**
     * 从数据库刷新配置
     */
    public synchronized void refreshFromDatabase() {
        // 加载提供商配置
        List<ModelProviderDO> providers = modelProviderMapper.selectList(
                new LambdaQueryWrapper<ModelProviderDO>()
                        .eq(ModelProviderDO::getEnabled, 1)
        );

        cachedProviders = new HashMap<>();
        for (ModelProviderDO provider : providers) {
            Map<String, String> endpoints = new HashMap<>();
            if (provider.getEndpoints() != null && !provider.getEndpoints().isBlank()) {
                endpoints = JSON.parseObject(provider.getEndpoints(), new TypeReference<Map<String, String>>() {});
            }
            AIModelProperties.ProviderConfig providerConfig = new AIModelProperties.ProviderConfig();
            providerConfig.setUrl(provider.getUrl());
            providerConfig.setApiKey(provider.getApiKey());
            providerConfig.setEndpoints(endpoints);
            cachedProviders.put(provider.getName(), providerConfig);
        }

        // 加载模型候选配置
        cachedChatCandidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.CHAT.name())
                        .eq(ModelCandidateDO::getEnabled, 1)
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        cachedEmbeddingCandidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.EMBEDDING.name())
                        .eq(ModelCandidateDO::getEnabled, 1)
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        cachedRerankCandidates = modelCandidateMapper.selectList(
                new LambdaQueryWrapper<ModelCandidateDO>()
                        .eq(ModelCandidateDO::getModelType, ModelCandidateDO.ModelType.RERANK.name())
                        .eq(ModelCandidateDO::getEnabled, 1)
                        .orderByAsc(ModelCandidateDO::getPriority)
        );

        // 更新 AIModelProperties
        updateAIModelProperties();

        // 校验必需配置
        validateConfig();

        log.info("模型配置加载完成，提供商: {}, Chat模型: {}, Embedding模型: {}, Rerank模型: {}",
                cachedProviders.size(),
                cachedChatCandidates.size(),
                cachedEmbeddingCandidates.size(),
                cachedRerankCandidates.size());
    }

    /**
     * 更新 AIModelProperties
     */
    private void updateAIModelProperties() {
        // 更新提供商配置（直接替换）
        aiModelProperties.getProviders().clear();
        aiModelProperties.getProviders().putAll(cachedProviders);

        // 更新 Chat 模型配置
        AIModelProperties.ModelGroup chatGroup = new AIModelProperties.ModelGroup();
        chatGroup.setCandidates(cachedChatCandidates.stream()
                .map(this::toModelCandidate)
                .collect(Collectors.toList()));
        for (ModelCandidateDO c : cachedChatCandidates) {
            if (Integer.valueOf(1).equals(c.getIsDefault())) {
                chatGroup.setDefaultModel(c.getModelId());
            }
            if (Integer.valueOf(1).equals(c.getIsDeepThinking())) {
                chatGroup.setDeepThinkingModel(c.getModelId());
            }
        }
        aiModelProperties.setChat(chatGroup);

        // 更新 Embedding 模型配置
        AIModelProperties.ModelGroup embeddingGroup = new AIModelProperties.ModelGroup();
        embeddingGroup.setCandidates(cachedEmbeddingCandidates.stream()
                .map(this::toModelCandidate)
                .collect(Collectors.toList()));
        for (ModelCandidateDO c : cachedEmbeddingCandidates) {
            if (Integer.valueOf(1).equals(c.getIsDefault())) {
                embeddingGroup.setDefaultModel(c.getModelId());
            }
        }
        aiModelProperties.setEmbedding(embeddingGroup);

        // 更新 Rerank 模型配置
        AIModelProperties.ModelGroup rerankGroup = new AIModelProperties.ModelGroup();
        rerankGroup.setCandidates(cachedRerankCandidates.stream()
                .map(this::toModelCandidate)
                .collect(Collectors.toList()));
        for (ModelCandidateDO c : cachedRerankCandidates) {
            if (Integer.valueOf(1).equals(c.getIsDefault())) {
                rerankGroup.setDefaultModel(c.getModelId());
            }
        }
        aiModelProperties.setRerank(rerankGroup);
    }

    /**
     * 校验必需配置
     */
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

        // 校验默认模型
        if (!cachedChatCandidates.isEmpty() && aiModelProperties.getChat().getDefaultModel() == null) {
            errors.append("- Chat 模型未设置默认模型 (is_default=1)\n");
        }
        if (!cachedEmbeddingCandidates.isEmpty() && aiModelProperties.getEmbedding().getDefaultModel() == null) {
            errors.append("- Embedding 模型未设置默认模型 (is_default=1)\n");
        }

        if (errors.length() > 0) {
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

    /**
     * 转换 ModelCandidateDO 为 ModelCandidate
     */
    private AIModelProperties.ModelCandidate toModelCandidate(ModelCandidateDO entity) {
        AIModelProperties.ModelCandidate candidate = new AIModelProperties.ModelCandidate();
        candidate.setId(entity.getModelId());
        candidate.setProvider(entity.getProviderName());
        candidate.setModel(entity.getModelName());
        candidate.setUrl(entity.getUrl());
        candidate.setDimension(entity.getDimension());
        candidate.setPriority(entity.getPriority());
        candidate.setEnabled(true);
        candidate.setSupportsThinking(Integer.valueOf(1).equals(entity.getSupportsThinking()));
        return candidate;
    }

}

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
import com.google.gson.Gson;
import com.nageoffer.ai.ragent.infra.config.AIModelProperties;
import com.nageoffer.ai.ragent.settings.dao.entity.ModelCandidateDO;
import com.nageoffer.ai.ragent.settings.dao.entity.ModelProviderDO;
import com.nageoffer.ai.ragent.settings.dao.mapper.ModelCandidateMapper;
import com.nageoffer.ai.ragent.settings.dao.mapper.ModelProviderMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 动态配置刷新器
 * <p>
 * 从数据库加载配置并刷新到内存中，支持实时刷新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicConfigRefresher {

    private final ModelProviderMapper modelProviderMapper;
    private final ModelCandidateMapper modelCandidateMapper;
    private final StringRedisTemplate redisTemplate;
    private final AIModelProperties aiModelProperties;

    private static final Gson GSON = new Gson();

    private static final String REDIS_KEY_CHAT_CONFIG = "config:ai.chat";
    private static final String REDIS_KEY_EMBEDDING_CONFIG = "config:ai.embedding";
    private static final String REDIS_KEY_RERANK_CONFIG = "config:ai.rerank";
    private static final String REDIS_KEY_PROVIDERS = "config:ai.providers";

    // 缓存的配置
    private volatile Map<String, AIModelProperties.ProviderConfig> cachedProviders;
    private volatile List<ModelCandidateDO> cachedChatCandidates;
    private volatile List<ModelCandidateDO> cachedEmbeddingCandidates;
    private volatile List<ModelCandidateDO> cachedRerankCandidates;

    /**
     * 应用启动时加载配置
     */
    @PostConstruct
    public void init() {
        log.info("初始化动态配置加载...");
        try {
            refreshFromDatabase();
        } catch (Exception e) {
            log.warn("动态配置加载失败，将使用默认配置: {}", e.getMessage());
            // 不抛出异常，允许应用正常启动
        }
    }

    /**
     * 从数据库刷新配置
     */
    public synchronized void refreshFromDatabase() {
        try {
            // 加载提供商配置
            List<ModelProviderDO> providers = modelProviderMapper.selectList(
                    new LambdaQueryWrapper<ModelProviderDO>()
                            .eq(ModelProviderDO::getEnabled, 1)
            );

            cachedProviders = new HashMap<>();
            for (ModelProviderDO provider : providers) {
                Map<String, String> endpoints = new HashMap<>();
                if (provider.getEndpoints() != null && !provider.getEndpoints().isBlank()) {
                    endpoints = GSON.fromJson(provider.getEndpoints(), Map.class);
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

            log.info("动态配置刷新完成，提供商: {}, Chat模型: {}, Embedding模型: {}, Rerank模型: {}",
                    cachedProviders.size(),
                    cachedChatCandidates.size(),
                    cachedEmbeddingCandidates.size(),
                    cachedRerankCandidates.size());

        } catch (Exception e) {
            log.error("刷新动态配置失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新 AIModelProperties
     */
    private void updateAIModelProperties() {
        // 更新提供商配置
        if (cachedProviders != null && !cachedProviders.isEmpty()) {
            aiModelProperties.getProviders().putAll(cachedProviders);
        }

        // 更新 Chat 模型配置
        if (cachedChatCandidates != null && !cachedChatCandidates.isEmpty()) {
            AIModelProperties.ModelGroup chatGroup = aiModelProperties.getChat();
            if (chatGroup == null) {
                chatGroup = new AIModelProperties.ModelGroup();
                aiModelProperties.setChat(chatGroup);
            }

            List<AIModelProperties.ModelCandidate> candidates = cachedChatCandidates.stream()
                    .map(this::toModelCandidate)
                    .collect(Collectors.toList());
            chatGroup.setCandidates(candidates);

            // 设置默认模型
            for (ModelCandidateDO c : cachedChatCandidates) {
                if (Integer.valueOf(1).equals(c.getIsDefault())) {
                    chatGroup.setDefaultModel(c.getModelId());
                }
                if (Integer.valueOf(1).equals(c.getIsDeepThinking())) {
                    chatGroup.setDeepThinkingModel(c.getModelId());
                }
            }
        }

        // 更新 Embedding 模型配置
        if (cachedEmbeddingCandidates != null && !cachedEmbeddingCandidates.isEmpty()) {
            AIModelProperties.ModelGroup embeddingGroup = aiModelProperties.getEmbedding();
            if (embeddingGroup == null) {
                embeddingGroup = new AIModelProperties.ModelGroup();
                aiModelProperties.setEmbedding(embeddingGroup);
            }

            List<AIModelProperties.ModelCandidate> candidates = cachedEmbeddingCandidates.stream()
                    .map(this::toModelCandidate)
                    .collect(Collectors.toList());
            embeddingGroup.setCandidates(candidates);

            for (ModelCandidateDO c : cachedEmbeddingCandidates) {
                if (Integer.valueOf(1).equals(c.getIsDefault())) {
                    embeddingGroup.setDefaultModel(c.getModelId());
                }
            }
        }

        // 更新 Rerank 模型配置
        if (cachedRerankCandidates != null && !cachedRerankCandidates.isEmpty()) {
            AIModelProperties.ModelGroup rerankGroup = aiModelProperties.getRerank();
            if (rerankGroup == null) {
                rerankGroup = new AIModelProperties.ModelGroup();
                aiModelProperties.setRerank(rerankGroup);
            }

            List<AIModelProperties.ModelCandidate> candidates = cachedRerankCandidates.stream()
                    .map(this::toModelCandidate)
                    .collect(Collectors.toList());
            rerankGroup.setCandidates(candidates);

            for (ModelCandidateDO c : cachedRerankCandidates) {
                if (Integer.valueOf(1).equals(c.getIsDefault())) {
                    rerankGroup.setDefaultModel(c.getModelId());
                }
            }
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

    /**
     * 获取缓存的提供商配置
     */
    public Map<String, AIModelProperties.ProviderConfig> getCachedProviders() {
        if (cachedProviders == null) {
            refreshFromDatabase();
        }
        return cachedProviders;
    }

    /**
     * 获取缓存的 Chat 模型候选
     */
    public List<ModelCandidateDO> getCachedChatCandidates() {
        if (cachedChatCandidates == null) {
            refreshFromDatabase();
        }
        return cachedChatCandidates;
    }

    /**
     * 获取缓存的 Embedding 模型候选
     */
    public List<ModelCandidateDO> getCachedEmbeddingCandidates() {
        if (cachedEmbeddingCandidates == null) {
            refreshFromDatabase();
        }
        return cachedEmbeddingCandidates;
    }

    /**
     * 获取缓存的 Rerank 模型候选
     */
    public List<ModelCandidateDO> getCachedRerankCandidates() {
        if (cachedRerankCandidates == null) {
            refreshFromDatabase();
        }
        return cachedRerankCandidates;
    }
}

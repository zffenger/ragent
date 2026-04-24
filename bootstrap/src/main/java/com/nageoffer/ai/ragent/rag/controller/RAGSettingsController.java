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

package com.nageoffer.ai.ragent.rag.controller;

import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import com.nageoffer.ai.ragent.llm.domain.repository.ModelConfigRepository;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelCandidateConfig;
import com.nageoffer.ai.ragent.llm.domain.vo.ProviderConfig;
import com.nageoffer.ai.ragent.llm.infra.config.AISelectionProperties;
import com.nageoffer.ai.ragent.llm.infra.config.AIStreamProperties;
import com.nageoffer.ai.ragent.rag.config.MemoryProperties;
import com.nageoffer.ai.ragent.rag.config.RAGConfigProperties;
import com.nageoffer.ai.ragent.rag.config.RAGDefaultProperties;
import com.nageoffer.ai.ragent.rag.config.RAGRateLimitProperties;
import com.nageoffer.ai.ragent.rag.controller.vo.SystemSettingsVO;
import com.nageoffer.ai.ragent.rag.controller.vo.SystemSettingsVO.AISettings;
import com.nageoffer.ai.ragent.rag.controller.vo.SystemSettingsVO.DefaultSettings;
import com.nageoffer.ai.ragent.rag.controller.vo.SystemSettingsVO.MemorySettings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 设置控制器，负责系统 RAG、AI 模型等配置信息的查询
 */
@RestController
@RequiredArgsConstructor
public class RAGSettingsController {

    private final RAGDefaultProperties ragDefaultProperties;
    private final RAGConfigProperties ragConfigProperties;
    private final RAGRateLimitProperties ragRateLimitProperties;
    private final MemoryProperties memoryProperties;
    private final ModelConfigRepository modelConfigRepository;
    private final AISelectionProperties selectionProperties;
    private final AIStreamProperties streamProperties;

    @Value("${spring.servlet.multipart.max-file-size:50MB}")
    private DataSize maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:100MB}")
    private DataSize maxRequestSize;

    /**
     * 获取系统 RAG、AI 模型等配置信息
     */
    @GetMapping("/rag/settings")
    public Result<SystemSettingsVO> settings() {
        SystemSettingsVO response = SystemSettingsVO.builder()
                .upload(SystemSettingsVO.UploadSettings.builder()
                        .maxFileSize(maxFileSize.toBytes())
                        .maxRequestSize(maxRequestSize.toBytes())
                        .build())
                .rag(SystemSettingsVO.RagSettings.builder()
                        .defaultConfig(toDefaultSettings(ragDefaultProperties))
                        .queryRewrite(SystemSettingsVO.QueryRewriteSettings.builder()
                                .enabled(ragConfigProperties.getQueryRewriteEnabled())
                                .build())
                        .rateLimit(SystemSettingsVO.RateLimitSettings.builder()
                                .global(SystemSettingsVO.GlobalRateLimit.builder()
                                        .enabled(ragRateLimitProperties.getGlobalEnabled())
                                        .maxConcurrent(ragRateLimitProperties.getGlobalMaxConcurrent())
                                        .maxWaitSeconds(ragRateLimitProperties.getGlobalMaxWaitSeconds())
                                        .leaseSeconds(ragRateLimitProperties.getGlobalLeaseSeconds())
                                        .pollIntervalMs(ragRateLimitProperties.getGlobalPollIntervalMs())
                                        .build())
                                .build())
                        .memory(toMemorySettings(memoryProperties))
                        .build())
                .ai(toAISettings())
                .build();
        return Results.success(response);
    }

    private DefaultSettings toDefaultSettings(RAGDefaultProperties props) {
        return DefaultSettings.builder()
                .collectionName(props.getCollectionName())
                .dimension(props.getDimension())
                .metricType(props.getMetricType())
                .build();
    }

    private MemorySettings toMemorySettings(MemoryProperties props) {
        return MemorySettings.builder()
                .historyKeepTurns(props.getHistoryKeepTurns())
                .summaryEnabled(props.getSummaryEnabled())
                .summaryStartTurns(props.getSummaryStartTurns())
                .summaryMaxChars(props.getSummaryMaxChars())
                .titleMaxLength(props.getTitleMaxLength())
                .build();
    }

    private AISettings toAISettings() {
        Map<String, ProviderConfig> providers = modelConfigRepository.getProviders();
        Map<String, AISettings.ProviderConfig> providerConfigs = new HashMap<>();
        if (providers != null) {
            providers.forEach((k, v) -> providerConfigs.put(k, AISettings.ProviderConfig.builder()
                    .url(v.url())
                    .apiKey(v.apiKey())
                    .endpoints(v.endpoints())
                    .build()));
        }

        return AISettings.builder()
                .providers(providerConfigs)
                .chat(toModelGroup(
                        modelConfigRepository.getChatCandidates(),
                        modelConfigRepository.getChatDefaultModel(),
                        modelConfigRepository.getChatDeepThinkingModel()))
                .embedding(toModelGroup(
                        modelConfigRepository.getEmbeddingCandidates(),
                        modelConfigRepository.getEmbeddingDefaultModel(),
                        null))
                .rerank(toModelGroup(
                        modelConfigRepository.getRerankCandidates(),
                        modelConfigRepository.getRerankDefaultModel(),
                        null))
                .selection(selectionProperties == null ? null : AISettings.Selection.builder()
                        .failureThreshold(selectionProperties.getFailureThreshold())
                        .openDurationMs(selectionProperties.getOpenDurationMs())
                        .build())
                .stream(streamProperties == null ? null : AISettings.Stream.builder()
                        .messageChunkSize(streamProperties.getMessageChunkSize())
                        .build())
                .build();
    }

    private AISettings.ModelGroup toModelGroup(java.util.List<ModelCandidateConfig> candidates,
                                                String defaultModel,
                                                String deepThinkingModel) {
        if (candidates == null) {
            return null;
        }
        return AISettings.ModelGroup.builder()
                .defaultModel(defaultModel)
                .deepThinkingModel(deepThinkingModel)
                .candidates(candidates.stream()
                        .map(c -> AISettings.ModelCandidate.builder()
                                .id(c.id())
                                .provider(c.provider())
                                .model(c.model())
                                .url(c.url())
                                .dimension(c.dimension())
                                .priority(c.priority())
                                .enabled(c.enabled())
                                .supportsThinking(c.supportsThinking())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

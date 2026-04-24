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

package com.nageoffer.ai.ragent.llm.domain.service.route;

import cn.hutool.core.util.StrUtil;
import com.nageoffer.ai.ragent.llm.domain.repository.ModelConfigRepository;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelCandidateConfig;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelProvider;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelTarget;
import com.nageoffer.ai.ragent.llm.domain.vo.ProviderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 模型选择器
 * <p>
 * 负责根据配置和当前需求（如普通对话、深度思考、Embedding等）选择合适的模型候选列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ModelSelectorImpl implements ModelSelector {

    private final ModelConfigRepository configRepository;
    private final ModelHealthStore healthStore;

    @Override
    public List<ModelTarget> selectChatCandidates(boolean deepThinking) {
        List<ModelCandidateConfig> candidates = configRepository.getChatCandidates();
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        String firstChoiceModelId = resolveFirstChoiceModel(deepThinking);
        return selectCandidates(candidates, firstChoiceModelId, deepThinking);
    }

    @Override
    public List<ModelTarget> selectEmbeddingCandidates() {
        List<ModelCandidateConfig> candidates = configRepository.getEmbeddingCandidates();
        return selectCandidates(candidates, configRepository.getEmbeddingDefaultModel(), false);
    }

    @Override
    public List<ModelTarget> selectRerankCandidates() {
        List<ModelCandidateConfig> candidates = configRepository.getRerankCandidates();
        return selectCandidates(candidates, configRepository.getRerankDefaultModel(), false);
    }

    private String resolveFirstChoiceModel(boolean deepThinking) {
        if (deepThinking) {
            String deepModel = configRepository.getChatDeepThinkingModel();
            if (StrUtil.isNotBlank(deepModel)) {
                return deepModel;
            }
        }
        return configRepository.getChatDefaultModel();
    }

    private List<ModelTarget> selectCandidates(List<ModelCandidateConfig> candidates,
                                                String firstChoiceModelId,
                                                boolean deepThinking) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<ModelCandidateConfig> orderedCandidates =
                filterAndSortCandidates(candidates, firstChoiceModelId, deepThinking);

        return buildAvailableTargets(orderedCandidates);
    }

    /**
     * 过滤并排序候选模型列表
     */
    private List<ModelCandidateConfig> filterAndSortCandidates(List<ModelCandidateConfig> candidates,
                                                                String firstChoiceModelId,
                                                                boolean deepThinking) {
        List<ModelCandidateConfig> enabled = candidates.stream()
                .filter(c -> c != null && !Boolean.FALSE.equals(c.enabled()))
                .filter(c -> !deepThinking || Boolean.TRUE.equals(c.supportsThinking()))
                .sorted(Comparator
                        .comparing((ModelCandidateConfig c) ->
                                !Objects.equals(c.id(), firstChoiceModelId))
                        .thenComparing(ModelCandidateConfig::priority,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ModelCandidateConfig::id,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        if (deepThinking && enabled.isEmpty()) {
            log.warn("深度思考模式没有可用候选模型");
        }

        return enabled;
    }

    private List<ModelTarget> buildAvailableTargets(List<ModelCandidateConfig> candidates) {
        Map<String, ProviderConfig> providers = configRepository.getProviders();

        return candidates.stream()
                .map(candidate -> buildModelTarget(candidate, providers))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ModelTarget buildModelTarget(ModelCandidateConfig candidate, Map<String, ProviderConfig> providers) {
        String modelId = candidate.id();

        if (healthStore.isUnavailable(modelId)) {
            return null;
        }

        ProviderConfig provider = providers.get(candidate.provider());
        if (provider == null && !ModelProvider.NOOP.matches(candidate.provider())) {
            log.warn("Provider配置缺失: provider={}, modelId={}", candidate.provider(), modelId);
            return null;
        }

        return new ModelTarget(modelId, candidate, provider);
    }
}

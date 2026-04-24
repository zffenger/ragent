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

package com.nageoffer.ai.ragent.rag.domain.service.retrieve.channel;

import cn.hutool.core.collection.CollUtil;
import com.nageoffer.ai.ragent.framework.convention.RetrievedChunk;
import com.nageoffer.ai.ragent.rag.infra.config.SearchChannelProperties;
import com.nageoffer.ai.ragent.rag.domain.service.intent.NodeScore;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.RetrieverService;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.channel.strategy.CollectionParallelRetriever;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeBaseRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeBaseDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 向量全局检索通道
 */
@Slf4j
@Component
public class VectorGlobalSearchChannel implements SearchChannel {

    private final SearchChannelProperties properties;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final CollectionParallelRetriever parallelRetriever;

    public VectorGlobalSearchChannel(RetrieverService retrieverService,
                                     SearchChannelProperties properties,
                                     KnowledgeBaseRepository knowledgeBaseRepository,
                                     @Qualifier("ragInnerRetrievalThreadPoolExecutor") Executor innerRetrievalExecutor) {
        this.properties = properties;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.parallelRetriever = new CollectionParallelRetriever(retrieverService, innerRetrievalExecutor);
    }

    @Override
    public String getName() {
        return "VectorGlobalSearch";
    }

    @Override
    public int getPriority() {
        return 10;  // 较低优先级
    }

    @Override
    public boolean isEnabled(SearchContext context) {
        // 检查配置是否启用
        if (!properties.getChannels().getVectorGlobal().isEnabled()) {
            return false;
        }

        List<NodeScore> allScores = context.getIntents().stream()
                .flatMap(si -> si.nodeScores().stream())
                .toList();
        if (CollUtil.isEmpty(allScores)) {
            log.info("未识别出任何意图，启用全局检索");
            return true;
        }

        double maxScore = allScores.stream()
                .mapToDouble(NodeScore::getScore)
                .max()
                .orElse(0.0);

        double threshold = properties.getChannels().getVectorGlobal().getConfidenceThreshold();
        if (maxScore < threshold) {
            log.info("意图置信度过低（{}），启用全局检索", maxScore);
            return true;
        }

        double supplementThreshold = properties.getChannels().getVectorGlobal().getSingleIntentSupplementThreshold();
        if (allScores.size() == 1 && maxScore < supplementThreshold) {
            log.info("单一中等置信度意图（{}），启用补充全局检索", maxScore);
            return true;
        }

        return false;
    }

    @Override
    public SearchChannelResult search(SearchContext context) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("执行向量全局检索，问题：{}", context.getMainQuestion());

            // 获取 KB collection（根据知识库 ID 列表过滤）
            List<String> collections = getKBCollections(context.getKnowledgeBaseIds());

            if (collections.isEmpty()) {
                log.warn("未找到任何 KB collection，跳过全局检索");
                return SearchChannelResult.builder()
                        .channelType(SearchChannelType.VECTOR_GLOBAL)
                        .channelName(getName())
                        .chunks(List.of())
                        .latencyMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 并行在所有 collection 中检索
            int topKMultiplier = properties.getChannels().getVectorGlobal().getTopKMultiplier();
            List<RetrievedChunk> allChunks = retrieveFromAllCollections(
                    context.getMainQuestion(),
                    collections,
                    context.getTopK() * topKMultiplier,
                    context.getKnowledgeBaseIds()
            );

            long latency = System.currentTimeMillis() - startTime;

            log.info("向量全局检索完成，检索到 {} 个 Chunk，耗时 {}ms", allChunks.size(), latency);

            return SearchChannelResult.builder()
                    .channelType(SearchChannelType.VECTOR_GLOBAL)
                    .channelName(getName())
                    .chunks(allChunks)
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            log.error("向量全局检索失败", e);
            return SearchChannelResult.builder()
                    .channelType(SearchChannelType.VECTOR_GLOBAL)
                    .channelName(getName())
                    .chunks(List.of())
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 获取 KB 类型的 collection（支持知识库 ID 过滤）
     */
    private List<String> getKBCollections(List<String> knowledgeBaseIds) {
        Set<String> collections = new HashSet<>();

        // 如果指定了知识库 ID 列表，只获取这些知识库的 collection
        if (CollUtil.isNotEmpty(knowledgeBaseIds)) {
            List<KnowledgeBaseDO> kbList = knowledgeBaseRepository.findByIds(knowledgeBaseIds);
            for (KnowledgeBaseDO kb : kbList) {
                String collectionName = kb.getCollectionName();
                if (collectionName != null && !collectionName.isBlank()) {
                    collections.add(collectionName);
                }
            }
            return new ArrayList<>(collections);
        }

        // 否则从知识库表获取全量 collection（全局检索兜底）
        List<KnowledgeBaseDO> kbList = knowledgeBaseRepository.findAll();
        for (KnowledgeBaseDO kb : kbList) {
            String collectionName = kb.getCollectionName();
            if (collectionName != null && !collectionName.isBlank()) {
                collections.add(collectionName);
            }
        }

        return new ArrayList<>(collections);
    }

    /**
     * 并行在所有 collection 中检索
     */
    private List<RetrievedChunk> retrieveFromAllCollections(String question,
                                                            List<String> collections,
                                                            int topK,
                                                            List<String> knowledgeBaseIds) {
        // 使用模板方法执行并行检索，传入知识库 ID 列表用于过滤
        return parallelRetriever.executeParallelRetrieval(question, collections, topK, knowledgeBaseIds);
    }

    @Override
    public SearchChannelType getType() {
        return SearchChannelType.VECTOR_GLOBAL;
    }
}

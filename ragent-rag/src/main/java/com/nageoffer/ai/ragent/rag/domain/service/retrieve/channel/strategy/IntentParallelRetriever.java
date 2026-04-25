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

package com.nageoffer.ai.ragent.rag.domain.service.retrieve.channel.strategy;

import cn.hutool.core.collection.CollUtil;
import com.nageoffer.ai.ragent.framework.convention.RetrievedChunk;
import com.nageoffer.ai.ragent.rag.domain.entity.IntentNode;
import com.nageoffer.ai.ragent.rag.domain.entity.NodeScore;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.RetrieveRequest;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.RetrieverService;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.channel.AbstractParallelRetriever;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 意图并行检索器
 * 继承模板类，实现意图特定的检索逻辑
 */
@Slf4j
public class IntentParallelRetriever extends AbstractParallelRetriever<IntentParallelRetriever.IntentTask> {

    private final RetrieverService retrieverService;

    public record IntentTask(NodeScore nodeScore, int intentTopK) {
    }

    public IntentParallelRetriever(RetrieverService retrieverService,
                                   Executor executor) {
        super(executor);
        this.retrieverService = retrieverService;
    }

    /**
     * 执行并行检索（重载方法，支持动态 TopK 计算）
     */
    public List<RetrievedChunk> executeParallelRetrieval(String question,
                                                         List<NodeScore> targets,
                                                         int fallbackTopK,
                                                         int topKMultiplier) {
        return executeParallelRetrieval(question, targets, fallbackTopK, topKMultiplier, null);
    }

    /**
     * 执行并行检索（支持知识库 ID 过滤）
     */
    public List<RetrievedChunk> executeParallelRetrieval(String question,
                                                         List<NodeScore> targets,
                                                         int fallbackTopK,
                                                         int topKMultiplier,
                                                         List<String> knowledgeBaseIds) {
        List<IntentTask> intentTasks = targets.stream()
                .map(nodeScore -> new IntentTask(
                        nodeScore,
                        resolveIntentTopK(nodeScore, fallbackTopK, topKMultiplier)
                ))
                .toList();

        // 如果没有知识库 ID 限制，使用原有方法
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return super.executeParallelRetrieval(question, intentTasks, fallbackTopK);
        }

        // 创建带知识库 ID 过滤的检索任务
        record RetrievalFuture(IntentTask task, CompletableFuture<List<RetrievedChunk>> future) {
        }

        List<RetrievalFuture> futures = intentTasks.stream()
                .map(task -> {
                    CompletableFuture<List<RetrievedChunk>> future = CompletableFuture.supplyAsync(
                            () -> createRetrievalTaskWithKbFilter(question, task, knowledgeBaseIds),
                            getExecutor()
                    );
                    return new RetrievalFuture(task, future);
                })
                .toList();

        // 收集结果
        List<RetrievedChunk> allChunks = new ArrayList<>();
        for (RetrievalFuture future : futures) {
            try {
                List<RetrievedChunk> chunks = future.future.join();
                allChunks.addAll(chunks);
            } catch (Exception e) {
                log.error("意图检索获取结果失败 - {}", getTargetIdentifier(future.task), e);
            }
        }

        log.info("意图检索完成（知识库过滤），检索到 Chunk 总数: {}", allChunks.size());
        return allChunks;
    }

    @Override
    protected List<RetrievedChunk> createRetrievalTask(String question, IntentTask task, int ignoredTopK) {
        NodeScore nodeScore = task.nodeScore();
        IntentNode node = nodeScore.getNode();
        try {
            return retrieverService.retrieve(
                    RetrieveRequest.builder()
                            .collectionName(node.getCollectionName())
                            .query(question)
                            .topK(task.intentTopK())
                            .build()
            );
        } catch (Exception e) {
            log.error("意图检索失败 - 意图ID: {}, 意图名称: {}, Collection: {}, 错误: {}",
                    node.getId(), node.getName(), node.getCollectionName(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 创建带知识库 ID 过滤的检索任务
     */
    private List<RetrievedChunk> createRetrievalTaskWithKbFilter(String question, IntentTask task,
                                                                  List<String> knowledgeBaseIds) {
        NodeScore nodeScore = task.nodeScore();
        IntentNode node = nodeScore.getNode();
        try {
            return retrieverService.retrieve(
                    RetrieveRequest.builder()
                            .collectionName(node.getCollectionName())
                            .query(question)
                            .topK(task.intentTopK())
                            .knowledgeBaseIds(knowledgeBaseIds)
                            .build()
            );
        } catch (Exception e) {
            log.error("意图检索失败（知识库过滤） - 意图ID: {}, 意图名称: {}, Collection: {}, 错误: {}",
                    node.getId(), node.getName(), node.getCollectionName(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取执行器（供子类使用）
     */
    protected Executor getExecutor() {
        return executor;
    }

    @Override
    protected String getTargetIdentifier(IntentTask task) {
        NodeScore nodeScore = task.nodeScore();
        IntentNode node = nodeScore.getNode();
        return String.format("意图ID: %s, 意图名称: %s", node.getId(), node.getName());
    }

    @Override
    protected String getStatisticsName() {
        return "意图检索";
    }

    /**
     * 计算单个意图节点检索 TopK
     */
    private int resolveIntentTopK(NodeScore nodeScore, int fallbackTopK, int topKMultiplier) {
        int baseTopK = fallbackTopK;
        if (nodeScore != null && nodeScore.getNode() != null) {
            Integer nodeTopK = nodeScore.getNode().getTopK();
            if (nodeTopK != null && nodeTopK > 0) {
                baseTopK = nodeTopK;
            }
        }

        if (topKMultiplier <= 0) {
            log.warn("意图定向通道倍率配置异常: {}, 使用基础 TopK: {}", topKMultiplier, baseTopK);
            return baseTopK;
        }

        return baseTopK * topKMultiplier;
    }
}

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
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.RetrieveRequest;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.RetrieverService;
import com.nageoffer.ai.ragent.rag.domain.service.retrieve.channel.AbstractParallelRetriever;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Collection 并行检索器
 * 继承模板类，实现 Collection 特定的检索逻辑
 */
@Slf4j
public class CollectionParallelRetriever extends AbstractParallelRetriever<String> {

    private final RetrieverService retrieverService;

    public CollectionParallelRetriever(RetrieverService retrieverService, Executor executor) {
        super(executor);
        this.retrieverService = retrieverService;
    }

    /**
     * 并行检索（支持知识库 ID 过滤）
     */
    public List<RetrievedChunk> executeParallelRetrieval(String question,
                                                         List<String> collections,
                                                         int topK,
                                                         List<String> knowledgeBaseIds) {
        // 如果没有知识库 ID 限制，使用原有方法
        if (CollUtil.isEmpty(knowledgeBaseIds)) {
            return executeParallelRetrieval(question, collections, topK);
        }

        // 创建带知识库 ID 过滤的检索任务
        record RetrievalFuture(String collection, CompletableFuture<List<RetrievedChunk>> future) {
        }

        List<RetrievalFuture> futures = collections.stream()
                .map(collection -> {
                    CompletableFuture<List<RetrievedChunk>> future = CompletableFuture.supplyAsync(
                            () -> createRetrievalTaskWithKbFilter(question, collection, topK, knowledgeBaseIds),
                            getExecutor()
                    );
                    return new RetrievalFuture(collection, future);
                })
                .toList();

        // 收集结果
        List<RetrievedChunk> allChunks = new java.util.ArrayList<>();
        for (RetrievalFuture future : futures) {
            try {
                List<RetrievedChunk> chunks = future.future.join();
                allChunks.addAll(chunks);
            } catch (Exception e) {
                log.error("全局检索获取结果失败 - Collection: {}", future.collection, e);
            }
        }

        log.info("全局检索完成（知识库过滤），检索到 Chunk 总数: {}", allChunks.size());
        return allChunks;
    }

    @Override
    protected List<RetrievedChunk> createRetrievalTask(String question, String collectionName, int topK) {
        try {
            return retrieverService.retrieve(
                    RetrieveRequest.builder()
                            .collectionName(collectionName)
                            .query(question)
                            .topK(topK)
                            .build()
            );
        } catch (Exception e) {
            log.error("在 collection {} 中检索失败，错误: {}", collectionName, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 创建带知识库 ID 过滤的检索任务
     */
    private List<RetrievedChunk> createRetrievalTaskWithKbFilter(String question, String collectionName,
                                                                  int topK, List<String> knowledgeBaseIds) {
        try {
            return retrieverService.retrieve(
                    RetrieveRequest.builder()
                            .collectionName(collectionName)
                            .query(question)
                            .topK(topK)
                            .knowledgeBaseIds(knowledgeBaseIds)
                            .build()
            );
        } catch (Exception e) {
            log.error("在 collection {} 中检索失败（知识库过滤），错误: {}", collectionName, e.getMessage(), e);
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
    protected String getTargetIdentifier(String collectionName) {
        return "Collection: " + collectionName;
    }

    @Override
    protected String getStatisticsName() {
        return "全局检索";
    }
}

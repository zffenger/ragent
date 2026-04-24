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

package com.nageoffer.ai.ragent.llm.domain.repository;

import com.nageoffer.ai.ragent.llm.domain.vo.ModelCandidateConfig;
import com.nageoffer.ai.ragent.llm.domain.vo.ProviderConfig;

import java.util.List;
import java.util.Map;

/**
 * 模型配置仓储接口
 * <p>
 * 提供模型配置的查询能力，配置从数据库动态加载
 */
public interface ModelConfigRepository {

    /**
     * 获取所有启用的提供商配置
     *
     * @return 提供商名称 -> 配置映射
     */
    Map<String, ProviderConfig> getProviders();

    /**
     * 获取 Chat 模型候选列表
     */
    List<ModelCandidateConfig> getChatCandidates();

    /**
     * 获取 Embedding 模型候选列表
     */
    List<ModelCandidateConfig> getEmbeddingCandidates();

    /**
     * 获取 Rerank 模型候选列表
     */
    List<ModelCandidateConfig> getRerankCandidates();

    /**
     * 获取 Chat 默认模型 ID
     */
    String getChatDefaultModel();

    /**
     * 获取 Chat 深度思考模型 ID
     */
    String getChatDeepThinkingModel();

    /**
     * 获取 Embedding 默认模型 ID
     */
    String getEmbeddingDefaultModel();

    /**
     * 获取 Rerank 默认模型 ID
     */
    String getRerankDefaultModel();

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}

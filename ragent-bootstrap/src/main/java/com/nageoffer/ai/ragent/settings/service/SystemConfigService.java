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

package com.nageoffer.ai.ragent.settings.service;

import com.nageoffer.ai.ragent.settings.controller.vo.ModelCandidateVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelGroupConfigVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelProviderVO;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelCandidateDO;

import java.util.List;

/**
 * 系统配置服务接口
 * <p>
 * 提供模型配置的管理功能
 */
public interface SystemConfigService {

    // ==================== 模型组配置查询 ====================

    /**
     * 获取 Chat 模型组配置
     */
    ModelGroupConfigVO getChatModelGroupConfig();

    /**
     * 获取 Embedding 模型组配置
     */
    ModelGroupConfigVO getEmbeddingModelGroupConfig();

    /**
     * 获取 Rerank 模型组配置
     */
    ModelGroupConfigVO getRerankModelGroupConfig();

    // ==================== 模型候选 CRUD ====================

    /**
     * 创建模型候选
     *
     * @param vo 模型候选配置
     * @return 创建后的模型候选
     */
    ModelCandidateVO createModelCandidate(ModelCandidateVO vo);

    /**
     * 更新模型候选
     *
     * @param id 模型候选 ID
     * @param vo 模型候选配置
     * @return 更新后的模型候选
     */
    ModelCandidateVO updateModelCandidate(String id, ModelCandidateVO vo);

    /**
     * 删除模型候选
     *
     * @param id 模型候选 ID
     */
    void deleteModelCandidate(String id);

    /**
     * 设置默认模型
     *
     * @param candidateId 模型候选 ID
     * @param modelType   模型类型
     */
    void setDefaultModel(String candidateId, ModelCandidateDO.ModelType modelType);

    /**
     * 设置深度思考模型
     *
     * @param candidateId 模型候选 ID
     */
    void setDeepThinkingModel(String candidateId);

    // ==================== 模型提供商管理 ====================

    /**
     * 获取所有模型提供商
     *
     * @return 提供商列表
     */
    List<ModelProviderVO> listModelProviders();

    /**
     * 创建模型提供商
     *
     * @param vo 提供商配置
     * @return 创建后的提供商
     */
    ModelProviderVO createModelProvider(ModelProviderVO vo);

    /**
     * 更新模型提供商
     *
     * @param id 提供商 ID
     * @param vo 提供商配置
     * @return 更新后的提供商
     */
    ModelProviderVO updateModelProvider(String id, ModelProviderVO vo);

    /**
     * 删除模型提供商
     *
     * @param id 提供商 ID
     */
    void deleteModelProvider(String id);

    // ==================== 配置刷新 ====================

    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();
}

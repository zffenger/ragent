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

import com.nageoffer.ai.ragent.settings.controller.vo.ModelGroupConfigVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelProviderVO;
import com.nageoffer.ai.ragent.settings.dao.entity.ModelCandidateDO;

import java.util.List;

/**
 * 系统配置服务接口
 * <p>
 * 提供模型配置的管理功能
 */
public interface SystemConfigService {

    // ==================== 模型配置 ====================

    /**
     * 获取指定类型的模型组配置
     *
     * @param modelType 模型类型：CHAT/EMBEDDING/RERANK
     * @return 模型组配置
     */
    ModelGroupConfigVO getModelGroupConfig(ModelCandidateDO.ModelType modelType);

    /**
     * 更新指定类型的模型组配置
     *
     * @param modelType 模型类型
     * @param config    模型组配置
     */
    void updateModelGroupConfig(ModelCandidateDO.ModelType modelType, ModelGroupConfigVO config);

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
    ModelProviderVO updateModelProvider(Long id, ModelProviderVO vo);

    /**
     * 删除模型提供商
     *
     * @param id 提供商 ID
     */
    void deleteModelProvider(Long id);

    /**
     * 设置默认模型
     *
     * @param candidateId 模型候选 ID
     * @param modelType   模型类型
     */
    void setDefaultModel(Long candidateId, ModelCandidateDO.ModelType modelType);

    /**
     * 设置深度思考模型
     *
     * @param candidateId 模型候选 ID
     */
    void setDeepThinkingModel(Long candidateId);

    // ==================== 配置刷新 ====================

    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();
}

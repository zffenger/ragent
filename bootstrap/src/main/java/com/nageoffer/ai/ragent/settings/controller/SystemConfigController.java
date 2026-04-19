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

package com.nageoffer.ai.ragent.settings.controller;

import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelGroupConfigVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelProviderVO;
import com.nageoffer.ai.ragent.settings.dao.entity.ModelCandidateDO;
import com.nageoffer.ai.ragent.settings.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置管理控制器
 * <p>
 * 提供模型配置的管理API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settings")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    // ==================== 模型配置 API ====================

    /**
     * 获取 Chat 模型配置
     */
    @GetMapping("/ai/chat")
    public Result<ModelGroupConfigVO> getChatModelConfig() {
        return Results.success(systemConfigService.getModelGroupConfig(ModelCandidateDO.ModelType.CHAT));
    }

    /**
     * 更新 Chat 模型配置
     */
    @PutMapping("/ai/chat")
    public Result<Void> updateChatModelConfig(@RequestBody ModelGroupConfigVO config) {
        systemConfigService.updateModelGroupConfig(ModelCandidateDO.ModelType.CHAT, config);
        return Results.success();
    }

    /**
     * 获取 Embedding 模型配置
     */
    @GetMapping("/ai/embedding")
    public Result<ModelGroupConfigVO> getEmbeddingModelConfig() {
        return Results.success(systemConfigService.getModelGroupConfig(ModelCandidateDO.ModelType.EMBEDDING));
    }

    /**
     * 更新 Embedding 模型配置
     */
    @PutMapping("/ai/embedding")
    public Result<Void> updateEmbeddingModelConfig(@RequestBody ModelGroupConfigVO config) {
        systemConfigService.updateModelGroupConfig(ModelCandidateDO.ModelType.EMBEDDING, config);
        return Results.success();
    }

    /**
     * 获取 Rerank 模型配置
     */
    @GetMapping("/ai/rerank")
    public Result<ModelGroupConfigVO> getRerankModelConfig() {
        return Results.success(systemConfigService.getModelGroupConfig(ModelCandidateDO.ModelType.RERANK));
    }

    /**
     * 更新 Rerank 模型配置
     */
    @PutMapping("/ai/rerank")
    public Result<Void> updateRerankModelConfig(@RequestBody ModelGroupConfigVO config) {
        systemConfigService.updateModelGroupConfig(ModelCandidateDO.ModelType.RERANK, config);
        return Results.success();
    }

    // ==================== 模型提供商 API ====================

    /**
     * 获取模型提供商列表
     */
    @GetMapping("/model-providers")
    public Result<List<ModelProviderVO>> listModelProviders() {
        return Results.success(systemConfigService.listModelProviders());
    }

    /**
     * 创建模型提供商
     */
    @PostMapping("/model-providers")
    public Result<ModelProviderVO> createModelProvider(@RequestBody ModelProviderVO vo) {
        return Results.success(systemConfigService.createModelProvider(vo));
    }

    /**
     * 更新模型提供商
     */
    @PutMapping("/model-providers/{id}")
    public Result<ModelProviderVO> updateModelProvider(
            @PathVariable Long id,
            @RequestBody ModelProviderVO vo) {
        return Results.success(systemConfigService.updateModelProvider(id, vo));
    }

    /**
     * 删除模型提供商
     */
    @DeleteMapping("/model-providers/{id}")
    public Result<Void> deleteModelProvider(@PathVariable Long id) {
        systemConfigService.deleteModelProvider(id);
        return Results.success();
    }

    /**
     * 设置默认模型
     */
    @PutMapping("/model-candidates/{id}/default")
    public Result<Void> setDefaultModel(
            @PathVariable Long id,
            @RequestBody SetDefaultModelRequest request) {
        systemConfigService.setDefaultModel(id, ModelCandidateDO.ModelType.valueOf(request.getModelType()));
        return Results.success();
    }

    /**
     * 设置深度思考模型
     */
    @PutMapping("/model-candidates/{id}/deep-thinking")
    public Result<Void> setDeepThinkingModel(@PathVariable Long id) {
        systemConfigService.setDeepThinkingModel(id);
        return Results.success();
    }

    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        systemConfigService.refreshConfigCache();
        return Results.success();
    }

    /**
     * 设置默认模型请求
     */
    @lombok.Data
    public static class SetDefaultModelRequest {
        private String modelType;
    }
}

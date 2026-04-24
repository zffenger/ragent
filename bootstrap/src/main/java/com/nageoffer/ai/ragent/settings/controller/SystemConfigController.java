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
import com.nageoffer.ai.ragent.llm.domain.vo.ModelProvider;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelCandidateVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelGroupConfigVO;
import com.nageoffer.ai.ragent.settings.controller.vo.ModelProviderVO;
import com.nageoffer.ai.ragent.llm.infra.persistence.po.ModelCandidateDO;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    // ==================== 模型组配置查询 ====================

    /**
     * 获取 Chat 模型配置
     */
    @GetMapping("/ai/chat")
    public Result<ModelGroupConfigVO> getChatModelConfig() {
        return Results.success(systemConfigService.getChatModelGroupConfig());
    }

    /**
     * 获取 Embedding 模型配置
     */
    @GetMapping("/ai/embedding")
    public Result<ModelGroupConfigVO> getEmbeddingModelConfig() {
        return Results.success(systemConfigService.getEmbeddingModelGroupConfig());
    }

    /**
     * 获取 Rerank 模型配置
     */
    @GetMapping("/ai/rerank")
    public Result<ModelGroupConfigVO> getRerankModelConfig() {
        return Results.success(systemConfigService.getRerankModelGroupConfig());
    }

    // ==================== 模型候选 CRUD ====================

    /**
     * 创建模型候选
     */
    @PostMapping("/model-candidates")
    public Result<ModelCandidateVO> createModelCandidate(@RequestBody ModelCandidateVO vo) {
        return Results.success(systemConfigService.createModelCandidate(vo));
    }

    /**
     * 更新模型候选
     */
    @PutMapping("/model-candidates/{id}")
    public Result<ModelCandidateVO> updateModelCandidate(
            @PathVariable String id,
            @RequestBody ModelCandidateVO vo) {
        return Results.success(systemConfigService.updateModelCandidate(id, vo));
    }

    /**
     * 删除模型候选
     */
    @DeleteMapping("/model-candidates/{id}")
    public Result<Void> deleteModelCandidate(@PathVariable String id) {
        systemConfigService.deleteModelCandidate(id);
        return Results.success();
    }

    /**
     * 设置默认模型
     */
    @PutMapping("/model-candidates/{id}/default")
    public Result<Void> setDefaultModel(
            @PathVariable String id,
            @RequestBody SetDefaultModelRequest request) {
        systemConfigService.setDefaultModel(id, ModelCandidateDO.ModelType.valueOf(request.getModelType()));
        return Results.success();
    }

    /**
     * 设置深度思考模型
     */
    @PutMapping("/model-candidates/{id}/deep-thinking")
    public Result<Void> setDeepThinkingModel(@PathVariable String id) {
        systemConfigService.setDeepThinkingModel(id);
        return Results.success();
    }

    // ==================== 模型提供商 API ====================

    /**
     * 获取系统支持的模型提供商类型
     * <p>
     * 返回系统内置支持的提供商列表，前端创建模型时只能从中选择
     */
    @GetMapping("/supported-providers")
    public Result<List<SupportedProviderVO>> getSupportedProviders() {
        List<SupportedProviderVO> providers = Arrays.stream(ModelProvider.values())
                .filter(p -> p != ModelProvider.NOOP)  // 排除 NOOP
                .map(p -> new SupportedProviderVO(p.getId(), p.getId()))
                .collect(Collectors.toList());
        return Results.success(providers);
    }

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
            @PathVariable String id,
            @RequestBody ModelProviderVO vo) {
        return Results.success(systemConfigService.updateModelProvider(id, vo));
    }

    /**
     * 删除模型提供商
     */
    @DeleteMapping("/model-providers/{id}")
    public Result<Void> deleteModelProvider(@PathVariable String id) {
        systemConfigService.deleteModelProvider(id);
        return Results.success();
    }

    // ==================== 配置刷新 ====================

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

    /**
     * 支持的提供商 VO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SupportedProviderVO {
        /**
         * 提供商标识
         */
        private String id;

        /**
         * 提供商名称
         */
        private String name;
    }
}

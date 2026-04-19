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

package com.nageoffer.ai.ragent.settings.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型组配置 VO
 * <p>
 * 用于展示和编辑 Chat/Embedding/Rerank 模型组配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelGroupConfigVO {

    /**
     * 默认模型 ID
     */
    private String defaultModel;

    /**
     * 深度思考模型 ID（仅 Chat 模型）
     */
    private String deepThinkingModel;

    /**
     * 候选模型列表
     */
    private List<ModelCandidateVO> candidates;

    /**
     * 模型候选 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelCandidateVO {
        /**
         * 主键 ID
         */
        private String id;

        /**
         * 模型标识
         */
        private String modelId;

        /**
         * 提供商名称
         */
        private String provider;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 自定义 URL
         */
        private String url;

        /**
         * 向量维度（embedding 模型）
         */
        private Integer dimension;

        /**
         * 优先级
         */
        private Integer priority;

        /**
         * 是否启用
         */
        private Boolean enabled;

        /**
         * 是否支持思考链
         */
        private Boolean supportsThinking;

        /**
         * 是否默认模型
         */
        private Boolean isDefault;

        /**
         * 是否深度思考模型
         */
        private Boolean isDeepThinking;
    }
}

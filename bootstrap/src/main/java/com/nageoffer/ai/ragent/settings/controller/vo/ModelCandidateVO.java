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

/**
 * 模型候选 VO
 * <p>
 * 用于单个模型候选的创建和更新
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelCandidateVO {

    /**
     * 主键 ID（更新时必填）
     */
    private String id;

    /**
     * 模型标识
     */
    private String modelId;

    /**
     * 模型类型：CHAT, EMBEDDING, RERANK
     */
    private String modelType;

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
	 * 是否默认模型
	 */
	private Boolean isDefault;
    /**
     * 是否支持思考链
     */
    private Boolean supportsThinking;
	/**
	 * 是否深度思考模型
	 */
	private Boolean isDeepThinking;
}

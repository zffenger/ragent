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

package com.nageoffer.ai.ragent.infra.ai.domain.vo;

/**
 * 模型候选配置值对象
 * <p>
 * 用于封装单个候选模型的配置信息，是领域层的值对象
 */
public record ModelCandidateConfig(
		/**
		 * 模型唯一标识符
		 */
        String id,
		/**
		 * 模型提供商名称
		 */
        String provider,

        String model,
		/**
		 * 模型名称
		 */
        String url,
		/**
		 * 模型访问 URL
		 */
        Integer dimension,
		/**
		 * 向量维度（用于 embedding 模型）
		 */
        Integer priority,
		/**
		 * 模型优先级，数值越小优先级越高
		 */
        Boolean enabled,
		/**
		 * 是否启用该模型
		 */
        Boolean supportsThinking
) {
}

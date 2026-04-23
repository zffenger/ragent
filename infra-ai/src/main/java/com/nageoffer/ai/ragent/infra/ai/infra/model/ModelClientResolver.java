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

package com.nageoffer.ai.ragent.infra.ai.infra.model;

import com.nageoffer.ai.ragent.infra.ai.domain.repository.LLMClient;
import com.nageoffer.ai.ragent.infra.ai.domain.vo.ModelTarget;

/**
 * 模型客户端解析器
 * <p>
 * 根据模型目标解析出对应的客户端实例
 *
 * @param <C> 客户端类型
 */
@FunctionalInterface
public interface ModelClientResolver<C extends LLMClient> {
	/**
	 * 解析模型目标对应的客户端
	 *
	 * @param target 模型目标
	 * @return 客户端实例，如果不存在返回 null
	 */
	C resolve(ModelTarget target);
}

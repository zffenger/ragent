package com.nageoffer.ai.ragent.infra.model;

import com.nageoffer.ai.ragent.infra.core.LLMClient;

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

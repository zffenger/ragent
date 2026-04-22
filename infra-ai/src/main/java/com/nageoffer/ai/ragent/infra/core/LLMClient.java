package com.nageoffer.ai.ragent.infra.core;

public interface LLMClient {
	/**
	 * 获取服务提供商名称
	 *
	 * @return 服务提供商标识：{@link com.nageoffer.ai.ragent.infra.enums.ModelProvider}
	 */
	String provider();
}

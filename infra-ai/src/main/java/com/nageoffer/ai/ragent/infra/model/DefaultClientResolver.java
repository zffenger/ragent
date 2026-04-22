package com.nageoffer.ai.ragent.infra.model;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nageoffer.ai.ragent.infra.core.LLMClient;

public class DefaultClientResolver<C extends LLMClient> implements ModelClientResolver<C> {

	private final Map<String, C> clientsByProvider;

	public DefaultClientResolver(Collection<C> clients) {
		this.clientsByProvider = clients.stream()
				.collect(Collectors.toMap(C::provider, Function.identity()));
	}


	@Override
	public C resolve(ModelTarget target) {
		return clientsByProvider.get(target.candidate().getProvider());
	}
}

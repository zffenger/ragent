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

package com.nageoffer.ai.ragent.infra.ai.infra.routing;

import cn.hutool.core.collection.CollUtil;
import com.nageoffer.ai.ragent.framework.convention.ChatRequest;
import com.nageoffer.ai.ragent.framework.errorcode.BaseErrorCode;
import com.nageoffer.ai.ragent.framework.exception.RemoteException;
import com.nageoffer.ai.ragent.framework.trace.RagTraceNode;
import com.nageoffer.ai.ragent.infra.ai.domain.repository.ChatClient;
import com.nageoffer.ai.ragent.infra.ai.domain.service.LLMService;
import com.nageoffer.ai.ragent.infra.ai.domain.service.StreamCallback;
import com.nageoffer.ai.ragent.infra.ai.domain.service.StreamCancellationHandle;
import com.nageoffer.ai.ragent.infra.ai.domain.vo.ModelCapability;
import com.nageoffer.ai.ragent.infra.ai.domain.vo.ModelTarget;
import com.nageoffer.ai.ragent.infra.ai.infra.client.chat.ProbeStreamBridge;
import com.nageoffer.ai.ragent.infra.ai.infra.model.DefaultClientResolver;
import com.nageoffer.ai.ragent.infra.ai.infra.model.ModelClientResolver;
import com.nageoffer.ai.ragent.infra.ai.infra.model.ModelHealthStore;
import com.nageoffer.ai.ragent.infra.ai.infra.model.ModelRoutingExecutor;
import com.nageoffer.ai.ragent.infra.ai.infra.model.ModelSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 路由式 LLM 服务实现类
 */
@Slf4j
@Service
@Primary
public class RoutingLLMService implements LLMService {

    private static final int FIRST_PACKET_TIMEOUT_SECONDS = 60;
    private static final String STREAM_INTERRUPTED_MESSAGE = "流式请求被中断";
    private static final String STREAM_NO_PROVIDER_MESSAGE = "无可用大模型提供者";
    private static final String STREAM_START_FAILED_MESSAGE = "流式请求启动失败";
    private static final String STREAM_TIMEOUT_MESSAGE = "流式首包超时";
    private static final String STREAM_NO_CONTENT_MESSAGE = "流式请求未返回内容";
    private static final String STREAM_ALL_FAILED_MESSAGE = "大模型调用失败，请稍后再试...";

    private final ModelSelector selector;
    private final ModelHealthStore healthStore;
    private final ModelRoutingExecutor executor;
	private final ModelClientResolver<ChatClient> clientResolver;

    public RoutingLLMService(
            ModelSelector selector,
            ModelHealthStore healthStore,
            ModelRoutingExecutor executor,
            List<ChatClient> clients) {
        this.selector = selector;
        this.healthStore = healthStore;
        this.executor = executor;
		this.clientResolver = new DefaultClientResolver<>(clients);
    }

    @Override
    @RagTraceNode(name = "llm-chat-routing", type = "LLM_ROUTING")
    public String chat(ChatRequest request) {
        return executor.executeWithFallback(
                ModelCapability.CHAT,
                selector.selectChatCandidates(Boolean.TRUE.equals(request.getThinking())),
                this.clientResolver,
                (client, target) -> client.chat(request, target)
        );
    }

    @Override
    public String chat(ChatRequest request, String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return chat(request);
        }
        return executor.executeWithFallback(
                ModelCapability.CHAT,
                List.of(resolveTarget(modelId, Boolean.TRUE.equals(request.getThinking()))),
                this.clientResolver,
                (client, target) -> client.chat(request, target)
        );
    }

    @Override
    @RagTraceNode(name = "llm-stream-routing", type = "LLM_ROUTING")
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback) {
        List<ModelTarget> targets = selector.selectChatCandidates(Boolean.TRUE.equals(request.getThinking()));
        if (CollUtil.isEmpty(targets)) {
            throw new RemoteException(STREAM_NO_PROVIDER_MESSAGE);
        }

        String label = ModelCapability.CHAT.getDisplayName();
        Throwable lastError = null;

        for (ModelTarget target : targets) {
            ChatClient client = resolveClient(target, label);
            if (client == null) {
                continue;
            }
            if (!healthStore.allowCall(target.id())) {
                continue;
            }

            ProbeStreamBridge bridge = new ProbeStreamBridge(callback);

            StreamCancellationHandle handle;
            try {
                handle = client.streamChat(request, bridge, target);
            } catch (Exception e) {
                healthStore.markFailure(target.id());
                lastError = e;
                log.warn("{} 流式请求启动失败，切换下一个模型。modelId：{}，provider：{}",
                        label, target.id(), target.candidate().provider(), e);
                continue;
            }
            if (handle == null) {
                healthStore.markFailure(target.id());
                lastError = new RemoteException(STREAM_START_FAILED_MESSAGE, BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 流式请求未返回取消句柄，切换下一个模型。modelId：{}，provider：{}",
                        label, target.id(), target.candidate().provider());
                continue;
            }

            ProbeStreamBridge.ProbeResult result = awaitFirstPacket(bridge, handle, callback);

            if (result.isSuccess()) {
                healthStore.markSuccess(target.id());
                return handle;
            }

            // 失败处理
            healthStore.markFailure(target.id());
            handle.cancel();

            lastError = buildLastErrorAndLog(result, target, label);
        }

        // 所有模型都失败了，通知客户端错误
        throw notifyAllFailed(callback, lastError);
    }

    private ChatClient resolveClient(ModelTarget target, String label) {
        ChatClient client = clientResolver.resolve(target);
        if (client == null) {
            log.warn("{} 提供商客户端缺失: provider：{}，modelId：{}",
                    label, target.candidate().provider(), target.id());
        }
        return client;
    }

    private ProbeStreamBridge.ProbeResult awaitFirstPacket(ProbeStreamBridge bridge,
                                                           StreamCancellationHandle handle,
                                                           StreamCallback callback) {
        try {
            return bridge.awaitFirstPacket(FIRST_PACKET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handle.cancel();
            RemoteException interruptedException = new RemoteException(STREAM_INTERRUPTED_MESSAGE, e, BaseErrorCode.REMOTE_ERROR);
            callback.onError(interruptedException);
            throw interruptedException;
        }
    }

    private Throwable buildLastErrorAndLog(ProbeStreamBridge.ProbeResult result, ModelTarget target, String label) {
        switch (result.getType()) {
            case ERROR -> {
                Throwable error = result.getError() != null
                        ? result.getError()
                        : new RemoteException("流式请求失败", BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求失败，切换下一个模型",
                        label, target.id(), target.candidate().provider(), error);
                return error;
            }
            case TIMEOUT -> {
                RemoteException timeout = new RemoteException(STREAM_TIMEOUT_MESSAGE, BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求超时，切换下一个模型",
                        label, target.id(), target.candidate().provider());
                return timeout;
            }
            case NO_CONTENT -> {
                RemoteException noContent = new RemoteException(STREAM_NO_CONTENT_MESSAGE, BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求无内容完成，切换下一个模型",
                        label, target.id(), target.candidate().provider());
                return noContent;
            }
            default -> {
                RemoteException unknown = new RemoteException("流式请求失败", BaseErrorCode.REMOTE_ERROR);
                log.warn("{} 失败模型: modelId={}, provider={}，原因: 流式请求失败（未知类型），切换下一个模型",
                        label, target.id(), target.candidate().provider());
                return unknown;
            }
        }
    }

    private RemoteException notifyAllFailed(StreamCallback callback, Throwable lastError) {
        RemoteException finalException = new RemoteException(
                STREAM_ALL_FAILED_MESSAGE,
                lastError,
                BaseErrorCode.REMOTE_ERROR
        );
        callback.onError(finalException);
        return finalException;
    }

    private ModelTarget resolveTarget(String modelId, boolean deepThinking) {
        return selector.selectChatCandidates(deepThinking).stream()
                .filter(target -> modelId.equals(target.id()))
                .findFirst()
                .orElseThrow(() -> new RemoteException("Chat 模型不可用: " + modelId));
    }
}

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

package com.nageoffer.ai.ragent.chatbot.infra.adapter.feishu;

import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书 API 客户端工厂
 * <p>
 * 为每个机器人创建独立的 API 客户端，支持多机器人配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuApiClientFactory {

    private final okhttp3.OkHttpClient httpClient;
    private final StringRedisTemplate redisTemplate;

    /**
     * 客户端缓存（按机器人 ID 缓存）
     */
    private final Map<String, FeishuApiClient> clientCache = new ConcurrentHashMap<>();

    /**
     * 获取或创建飞书 API 客户端
     *
     * @param botConfig 机器人配置
     * @return API 客户端
     */
    public FeishuApiClient getClient(BotConfig botConfig) {
        if (botConfig == null) {
            throw new IllegalArgumentException("机器人配置不能为空");
        }

        return clientCache.computeIfAbsent(botConfig.getId(), id -> {
            log.info("创建飞书 API 客户端: botId={}, appId={}", id, botConfig.getAppId());
            return new FeishuApiClient(httpClient, redisTemplate, botConfig);
        });
    }

    /**
     * 清除指定机器人的客户端缓存
     *
     * @param botId 机器人 ID
     */
    public void clearClient(String botId) {
        FeishuApiClient client = clientCache.remove(botId);
        if (client != null) {
            client.clearTokenCache();
            log.info("清除飞书 API 客户端缓存: botId={}", botId);
        }
    }

    /**
     * 清除所有客户端缓存
     */
    public void clearAll() {
        clientCache.values().forEach(FeishuApiClient::clearTokenCache);
        clientCache.clear();
        log.info("清除所有飞书 API 客户端缓存");
    }
}

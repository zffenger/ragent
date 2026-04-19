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

package com.nageoffer.ai.ragent.chatbot.feishu;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nageoffer.ai.ragent.chatbot.common.BotException;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 飞书 API 客户端
 * <p>
 * 封装飞书开放平台的 API 调用，包括获取 Token、发送消息等
 */
@Slf4j
@RequiredArgsConstructor
public class FeishuApiClient {

    private final OkHttpClient httpClient;
    private final StringRedisTemplate redisTemplate;
    private final ChatbotProperties properties;

    private static final Gson GSON = new Gson();

    /**
     * 获取 tenant_access_token 的 URL
     */
    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/";

    /**
     * 发送消息的 URL
     */
    private static final String SEND_MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

    /**
     * Token 缓存的 Redis Key
     */
    private static final String TOKEN_CACHE_KEY = "chatbot:feishu:tenant_access_token";

    /**
     * 发送文本消息
     *
     * @param chatId  聊天 ID
     * @param content 消息内容
     */
    public void sendTextMessage(String chatId, String content) {
        String token = getTenantAccessToken();

        // 构建请求体
        JsonObject body = new JsonObject();
        body.addProperty("receive_id", chatId);
        body.addProperty("receive_id_type", "chat_id");
        body.addProperty("msg_type", "text");

        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("text", content);
        body.add("content", contentObj);

        Request request = new Request.Builder()
                .url(SEND_MESSAGE_URL + "?receive_id_type=chat_id")
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("发送飞书消息失败: chatId={}, code={}", chatId, response.code());
                throw new BotException("发送飞书消息失败: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonObject resp = JsonParser.parseString(responseBody).getAsJsonObject();
            int code = resp.has("code") ? resp.get("code").getAsInt() : -1;

            if (code != 0) {
                String msg = resp.has("msg") ? resp.get("msg").getAsString() : "Unknown error";
                log.error("发送飞书消息失败: code={}, msg={}", code, msg);
                throw new BotException("发送飞书消息失败: " + msg);
            }

            log.debug("发送飞书消息成功: chatId={}", chatId);
        } catch (IOException e) {
            log.error("发送飞书消息异常: {}", e.getMessage(), e);
            throw new BotException("发送飞书消息异常", e);
        }
    }

    /**
     * 获取 tenant_access_token
     * <p>
     * 优先从缓存获取，缓存不存在或过期则重新请求
     *
     * @return tenant_access_token
     */
    public String getTenantAccessToken() {
        // 从缓存获取
        String cachedToken = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cachedToken != null && !cachedToken.isBlank()) {
            return cachedToken;
        }

        // 请求新 token
        return requestTenantAccessToken();
    }

    /**
     * 请求新的 tenant_access_token
     *
     * @return token
     */
    private String requestTenantAccessToken() {
        JsonObject body = new JsonObject();
        body.addProperty("app_id", properties.getFeishu().getAppId());
        body.addProperty("app_secret", properties.getFeishu().getAppSecret());

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new BotException("获取飞书 Token 失败: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            JsonObject resp = JsonParser.parseString(responseBody).getAsJsonObject();

            int code = resp.has("code") ? resp.get("code").getAsInt() : -1;
            if (code != 0) {
                String msg = resp.has("msg") ? resp.get("msg").getAsString() : "Unknown error";
                throw new BotException("获取飞书 Token 失败: " + msg);
            }

            String token = resp.get("tenant_access_token").getAsString();
            int expire = resp.get("expire").getAsInt();

            // 缓存 token，提前 5 分钟过期
            long cacheExpire = Math.max(expire - 300, 60);
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, cacheExpire, TimeUnit.SECONDS);

            log.debug("获取飞书 Token 成功，缓存 {} 秒", cacheExpire);
            return token;
        } catch (IOException e) {
            throw new BotException("获取飞书 Token 异常", e);
        }
    }

    /**
     * 清除缓存的 Token
     */
    public void clearTokenCache() {
        redisTemplate.delete(TOKEN_CACHE_KEY);
        log.debug("清除飞书 Token 缓存");
    }
}

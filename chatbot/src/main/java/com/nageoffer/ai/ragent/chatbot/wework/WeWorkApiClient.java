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

package com.nageoffer.ai.ragent.chatbot.wework;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nageoffer.ai.ragent.chatbot.common.BotException;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import com.nageoffer.ai.ragent.chatbot.wework.dto.WeWorkMessage;
import com.nageoffer.ai.ragent.chatbot.wework.dto.WeWorkResponse;
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
 * 企业微信 API 客户端
 * <p>
 * 封装企业微信 API 调用，包括获取 Token、发送消息等
 */
@Slf4j
@RequiredArgsConstructor
public class WeWorkApiClient {

    private final OkHttpClient httpClient;
    private final StringRedisTemplate redisTemplate;
    private final ChatbotProperties properties;

    private static final Gson GSON = new Gson();

    /**
     * 获取 access_token 的 URL
     */
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";

    /**
     * 发送消息的 URL
     */
    private static final String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";

    /**
     * Token 缓存的 Redis Key
     */
    private static final String TOKEN_CACHE_KEY = "chatbot:wework:access_token";

    /**
     * 发送文本消息
     *
     * @param toUser  接收者 ID（用户 ID 或群聊 ID）
     * @param content 消息内容
     */
    public void sendTextMessage(String toUser, String content) {
        String token = getAccessToken();

        // 构建消息
        Integer agentId = null;
        try {
            agentId = Integer.parseInt(properties.getWework().getAgentId());
        } catch (NumberFormatException e) {
            throw new BotException("企微 AgentId 配置错误");
        }

        WeWorkMessage message = WeWorkMessage.text(toUser, agentId, content);

        Request request = new Request.Builder()
                .url(SEND_MESSAGE_URL + "?access_token=" + token)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(GSON.toJson(message), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("发送企微消息失败: toUser={}, code={}", toUser, response.code());
                throw new BotException("发送企微消息失败: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            WeWorkResponse resp = GSON.fromJson(responseBody, WeWorkResponse.class);

            if (!resp.isSuccess()) {
                log.error("发送企微消息失败: errcode={}, errmsg={}", resp.getErrcode(), resp.getErrmsg());
                throw new BotException("发送企微消息失败: " + resp.getErrmsg());
            }

            log.debug("发送企微消息成功: toUser={}, msgId={}", toUser, resp.getMsgid());
        } catch (IOException e) {
            log.error("发送企微消息异常: {}", e.getMessage(), e);
            throw new BotException("发送企微消息异常", e);
        }
    }

    /**
     * 获取 access_token
     * <p>
     * 优先从缓存获取，缓存不存在或过期则重新请求
     *
     * @return access_token
     */
    public String getAccessToken() {
        // 从缓存获取
        String cachedToken = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cachedToken != null && !cachedToken.isBlank()) {
            return cachedToken;
        }

        // 请求新 token
        return requestAccessToken();
    }

    /**
     * 请求新的 access_token
     *
     * @return token
     */
    private String requestAccessToken() {
        String corpId = properties.getWework().getCorpId();
        String secret = properties.getWework().getSecret();

        String url = TOKEN_URL + "?corpid=" + corpId + "&corpsecret=" + secret;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new BotException("获取企微 Token 失败: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            WeWorkResponse resp = GSON.fromJson(responseBody, WeWorkResponse.class);

            if (!resp.isSuccess()) {
                throw new BotException("获取企微 Token 失败: " + resp.getErrmsg());
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String token = json.get("access_token").getAsString();
            int expire = json.get("expires_in").getAsInt();

            // 缓存 token，提前 5 分钟过期
            long cacheExpire = Math.max(expire - 300, 60);
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, cacheExpire, TimeUnit.SECONDS);

            log.debug("获取企微 Token 成功，缓存 {} 秒", cacheExpire);
            return token;
        } catch (IOException e) {
            throw new BotException("获取企微 Token 异常", e);
        }
    }

    /**
     * 清除缓存的 Token
     */
    public void clearTokenCache() {
        redisTemplate.delete(TOKEN_CACHE_KEY);
        log.debug("清除企微 Token 缓存");
    }
}

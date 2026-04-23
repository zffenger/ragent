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

import com.alibaba.fastjson2.JSON;
import com.nageoffer.ai.ragent.chatbot.common.BotConfig;
import com.nageoffer.ai.ragent.chatbot.feishu.dto.FeishuEvent;
import com.nageoffer.ai.ragent.chatbot.feishu.dto.FeishuResponse;
import com.nageoffer.ai.ragent.chatbot.service.BotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 飞书 Webhook 控制器
 * <p>
 * 接收飞书开放平台推送的消息事件，支持多机器人配置
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/feishu")
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class FeishuWebhookController {

    private final FeishuMessageHandler messageHandler;
    private final BotConfigRepository botConfigRepository;

    @Qualifier("chatbotMessageExecutor")
    private final Executor messageExecutor;

    /**
     * 处理飞书 Webhook 请求
     *
     * @param timestamp 请求时间戳
     * @param nonce     请求随机字符串
     * @param signature 请求签名
     * @param body      请求体
     * @return 响应
     */
    @PostMapping
    public Object handleWebhook(
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Lark-Signature", required = false) String signature,
            @RequestBody String body) {

        log.debug("收到飞书 Webhook 请求: timestamp={}, nonce={}", timestamp, nonce);

        // 1. 解析事件
        FeishuEvent event;
        try {
            event = JSON.parseObject(body, FeishuEvent.class);
        } catch (Exception e) {
            log.error("解析飞书事件失败: {}", e.getMessage());
            return FeishuResponse.error("解析事件失败");
        }

        if (event == null || event.getType() == null) {
            log.warn("飞书事件为空或类型为空");
            return FeishuResponse.error("事件为空");
        }

        // 2. URL 验证（首次配置时飞书会发送）
        if ("url_verification".equals(event.getType())) {
            log.info("飞书 URL 验证请求: challenge={}", event.getChallenge());
            return Map.of("challenge", event.getChallenge());
        }

        // 3. 根据 appId 加载机器人配置
        String appId = event.getAppId();
        if (appId == null || appId.isBlank()) {
            log.warn("飞书事件缺少 appId");
            return FeishuResponse.error("缺少 appId");
        }

        BotConfig botConfig = botConfigRepository.getByAppId(appId);
        if (botConfig == null) {
            log.warn("未找到对应的机器人配置: appId={}", appId);
            return FeishuResponse.error("机器人未配置或未启用");
        }

        // 4. 签名验证（如果配置了 verificationToken）
        if (botConfig.getVerificationToken() != null && !botConfig.getVerificationToken().isBlank()) {
            FeishuSignatureValidator signatureValidator = new FeishuSignatureValidator(botConfig);
            if (!signatureValidator.validate(timestamp, nonce, body, signature)) {
                log.warn("飞书签名验证失败: appId={}", appId);
                return FeishuResponse.error("签名验证失败");
            }
        }

        // 5. 异步处理消息（快速响应飞书，避免超时）
        CompletableFuture.runAsync(() -> {
            try {
                messageHandler.handle(event, botConfig);
            } catch (Exception e) {
                log.error("处理飞书消息异常: {}", e.getMessage(), e);
            }
        }, messageExecutor);

        // 6. 快速返回成功响应
        return FeishuResponse.success();
    }

    /**
     * 健康检查接口
     */
    @PostMapping("/health")
    public FeishuResponse health() {
        return FeishuResponse.success("ok");
    }
}

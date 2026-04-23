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

package com.nageoffer.ai.ragent.chatbot.interfaces.controller;

import com.alibaba.fastjson2.JSON;
import com.nageoffer.ai.ragent.chatbot.application.MessageProcessApplication;
import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import com.nageoffer.ai.ragent.chatbot.domain.repository.BotConfigRepository;
import com.nageoffer.ai.ragent.chatbot.infra.adapter.feishu.FeishuSignatureValidator;
import com.nageoffer.ai.ragent.chatbot.interfaces.assembler.FeishuMessageAssembler;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.feishu.FeishuEventDTO;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.feishu.FeishuMessageDTO;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.feishu.FeishuResponseDTO;
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
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/feishu")
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class FeishuWebhookController {

    private final MessageProcessApplication messageProcessApplication;
    private final BotConfigRepository botConfigRepository;

    @Qualifier("chatbotMessageExecutor")
    private final Executor messageExecutor;

    @PostMapping
    public Object handleWebhook(
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Lark-Signature", required = false) String signature,
            @RequestBody String body) {

        log.debug("收到飞书 Webhook 请求: timestamp={}, nonce={}", timestamp, nonce);

        FeishuEventDTO event;
        try {
            event = JSON.parseObject(body, FeishuEventDTO.class);
        } catch (Exception e) {
            log.error("解析飞书事件失败: {}", e.getMessage());
            return FeishuResponseDTO.error("解析事件失败");
        }

        if (event == null || event.getType() == null) {
            log.warn("飞书事件为空或类型为空");
            return FeishuResponseDTO.error("事件为空");
        }

        if ("url_verification".equals(event.getType())) {
            log.info("飞书 URL 验证请求: challenge={}", event.getChallenge());
            return Map.of("challenge", event.getChallenge());
        }

        String appId = event.getAppId();
        if (appId == null || appId.isBlank()) {
            log.warn("飞书事件缺少 appId");
            return FeishuResponseDTO.error("缺少 appId");
        }

        BotConfig botConfig = botConfigRepository.getByAppId(appId);
        if (botConfig == null) {
            log.warn("未找到对应的机器人配置: appId={}", appId);
            return FeishuResponseDTO.error("机器人未配置或未启用");
        }

        if (botConfig.getVerificationToken() != null && !botConfig.getVerificationToken().isBlank()) {
            FeishuSignatureValidator signatureValidator = new FeishuSignatureValidator(botConfig);
            if (!signatureValidator.validate(timestamp, nonce, body, signature)) {
                log.warn("飞书签名验证失败: appId={}", appId);
                return FeishuResponseDTO.error("签名验证失败");
            }
        }

        CompletableFuture.runAsync(() -> processMessage(event, botConfig), messageExecutor);

        return FeishuResponseDTO.success();
    }

    private void processMessage(FeishuEventDTO event, BotConfig botConfig) {
        try {
            FeishuMessageDTO message = event.getEvent();
            if (message == null) {
                log.debug("飞书事件无消息内容，跳过处理");
                return;
            }

            if (!"text".equals(message.getMsgType())) {
                log.debug("非文本消息，跳过处理: msgType={}", message.getMsgType());
                return;
            }

            if (Boolean.TRUE.equals(message.getDeleted()) || Boolean.TRUE.equals(message.getRecalled())) {
                log.debug("消息已删除或撤回，跳过处理");
                return;
            }

            String content = FeishuMessageAssembler.extractTextContent(message.getContent());
            if (content == null || content.isBlank()) {
                log.debug("消息内容为空，跳过处理");
                return;
            }

            log.info("处理飞书消息: botId={}, chatType={}, chatId={}, content={}",
                    botConfig.getId(), message.getChatType(), message.getChatId(),
                    content.length() > 50 ? content.substring(0, 50) + "..." : content);

            MessageContext context = FeishuMessageAssembler.toMessageContext(message, content, botConfig);
            messageProcessApplication.process(content, context, botConfig);

        } catch (Exception e) {
            log.error("处理飞书消息异常: {}", e.getMessage(), e);
        }
    }

    @PostMapping("/health")
    public FeishuResponseDTO health() {
        return FeishuResponseDTO.success("ok");
    }
}

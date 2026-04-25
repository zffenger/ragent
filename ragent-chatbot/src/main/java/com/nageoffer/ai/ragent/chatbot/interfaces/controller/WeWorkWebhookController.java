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

import com.nageoffer.ai.ragent.chatbot.application.MessageProcessApplication;
import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import com.nageoffer.ai.ragent.chatbot.domain.repository.BotConfigRepository;
import com.nageoffer.ai.ragent.chatbot.domain.vo.BotPlatform;
import com.nageoffer.ai.ragent.chatbot.infra.adapter.wework.WeWorkResponseDTO;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.wework.WeWorkEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 企业微信 Webhook 控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/wework")
public class WeWorkWebhookController {

    private final MessageProcessApplication messageProcessApplication;
    private final BotConfigRepository botConfigRepository;

    @Qualifier("chatbotMessageExecutor")
    private final Executor messageExecutor;

    @GetMapping
    public String verifyUrl(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echoStr) {
        log.info("企微 URL 验证请求: msgSignature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);
        return echoStr;
    }

    @PostMapping
    public String handleWebhook(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String body) {

        log.debug("收到企微 Webhook 请求: msgSignature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);

        try {
            WeWorkEventDTO event = parseEvent(body);
            if (event == null) {
                log.warn("企微事件解析失败");
                return "success";
            }

            String agentId = event.getAgentId();
            if (agentId == null || agentId.isBlank()) {
                log.warn("企微事件缺少 AgentId");
                return "success";
            }

            BotConfig botConfig = findBotByAgentId(agentId);
            if (botConfig == null) {
                log.warn("未找到对应的机器人配置: agentId={}", agentId);
                return "success";
            }

            CompletableFuture.runAsync(() -> processMessage(event, botConfig), messageExecutor);

            return "success";

        } catch (Exception e) {
            log.error("处理企微回调异常: {}", e.getMessage(), e);
            return "success";
        }
    }

    private void processMessage(WeWorkEventDTO event, BotConfig botConfig) {
        try {
            if (!"text".equals(event.getMsgType())) {
                log.debug("非文本消息，跳过处理: msgType={}", event.getMsgType());
                return;
            }

            String content = event.getContent();
            if (content == null || content.isBlank()) {
                log.debug("消息内容为空，跳过处理");
                return;
            }

            log.info("处理企微消息: botId={}, chatType={}, fromUser={}, content={}",
                    botConfig.getId(), event.getChatType(), event.getFromUserName(),
                    content.length() > 50 ? content.substring(0, 50) + "..." : content);

            String chatType = "single".equals(event.getChatType()) ? "p2p" : "group";
            String botName = botConfig.getName() != null ? botConfig.getName() : "智能助手";
            boolean atBot = content.contains("<@") && content.contains(">");

            MessageContext context = MessageContext.builder()
                    .platform(BotPlatform.WEWORK)
                    .chatType(chatType)
                    .chatId(event.getChatId() != null ? event.getChatId() : event.getFromUserName())
                    .senderId(event.getFromUserName())
                    .atBot(atBot)
                    .botName(botName)
                    .rawContent(content)
                    .messageId(event.getMsgId())
                    .timestamp(event.getCreateTime())
                    .botConfig(botConfig)
                    .build();

            messageProcessApplication.process(content, context, botConfig);

        } catch (Exception e) {
            log.error("处理企微消息异常: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/health")
    public WeWorkResponseDTO health() {
        return WeWorkResponseDTO.success("ok");
    }

    private BotConfig findBotByAgentId(String agentId) {
        for (BotConfig config : botConfigRepository.listEnabledByPlatform("WEWORK")) {
            if (agentId.equals(config.getAgentId())) {
                return config;
            }
        }
        return null;
    }

    private WeWorkEventDTO parseEvent(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes()));

            WeWorkEventDTO event = new WeWorkEventDTO();
            event.setToUserName(getElementText(doc, "ToUserName"));
            event.setFromUserName(getElementText(doc, "FromUserName"));
            event.setCreateTime(parseLong(getElementText(doc, "CreateTime")));
            event.setMsgType(getElementText(doc, "MsgType"));
            event.setEvent(getElementText(doc, "Event"));
            event.setMsgId(getElementText(doc, "MsgId"));
            event.setAgentId(getElementText(doc, "AgentId"));
            event.setChatType(getElementText(doc, "ChatType"));
            event.setChatId(getElementText(doc, "ChatId"));
            event.setContent(getElementText(doc, "Content"));

            return event;
        } catch (Exception e) {
            log.error("解析企微事件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getElementText(org.w3c.dom.Document doc, String tagName) {
        try {
            org.w3c.dom.NodeList nodes = doc.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Long parseLong(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

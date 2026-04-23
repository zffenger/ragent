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

import com.nageoffer.ai.ragent.chatbot.common.BotConfig;
import com.nageoffer.ai.ragent.chatbot.service.BotConfigRepository;
import com.nageoffer.ai.ragent.chatbot.wework.dto.WeWorkEvent;
import com.nageoffer.ai.ragent.chatbot.wework.dto.WeWorkResponse;
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
 * <p>
 * 接收企业微信回调推送的消息事件，支持多机器人配置
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/wework")
public class WeWorkWebhookController {

    private final WeWorkMessageHandler messageHandler;
    private final WeWorkApiClientFactory apiClientFactory;
    private final BotConfigRepository botConfigRepository;

    @Qualifier("chatbotMessageExecutor")
    private final Executor messageExecutor;

    /**
     * URL 验证（GET 请求）
     * <p>
     * 企业微信配置回调 URL 时会发送 GET 请求进行验证
     */
    @GetMapping
    public String verifyUrl(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echoStr) {

        log.info("企微 URL 验证请求: msgSignature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);

        // URL 验证时无法确定具体机器人，返回原始 echoStr 让企微验证通过
        // 实际消息处理时会根据 AgentId 匹配机器人
        return echoStr;
    }

    /**
     * 处理企微回调消息（POST 请求）
     *
     * @param msgSignature 签名
     * @param timestamp    时间戳
     * @param nonce        随机字符串
     * @param body         请求体（加密的 XML）
     * @return 响应
     */
    @PostMapping
    public String handleWebhook(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String body) {

        log.debug("收到企微 Webhook 请求: msgSignature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);

        try {
            // 1. 解析事件
            WeWorkEvent event = parseEvent(body);
            if (event == null) {
                log.warn("企微事件解析失败");
                return "success";
            }

            // 2. 根据 AgentId 加载机器人配置
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

            // 3. 签名验证
            WeWorkSignatureValidator signatureValidator = new WeWorkSignatureValidator(botConfig);
            // TODO: 企微消息需要解密，这里暂时跳过签名验证
            // 如需完整实现，需要先从加密消息中提取 encryptMsg，再验证签名并解密

            // 4. 异步处理消息
            CompletableFuture.runAsync(() -> {
                try {
                    messageHandler.handle(event, botConfig, apiClientFactory.getClient(botConfig));
                } catch (Exception e) {
                    log.error("处理企微消息异常: {}", e.getMessage(), e);
                }
            }, messageExecutor);

            // 5. 快速返回成功响应
            return "success";

        } catch (Exception e) {
            log.error("处理企微回调异常: {}", e.getMessage(), e);
            return "success";
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public WeWorkResponse health() {
        return WeWorkResponse.success("ok");
    }

    /**
     * 根据 AgentId 查找机器人配置
     */
    private BotConfig findBotByAgentId(String agentId) {
        // 从所有启用的企微机器人中查找匹配的 AgentId
        for (BotConfig config : botConfigRepository.listEnabledByPlatform("WEWORK")) {
            if (agentId.equals(config.getAgentId())) {
                return config;
            }
        }
        return null;
    }

    /**
     * 解析 XML 事件
     */
    private WeWorkEvent parseEvent(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes()));

            WeWorkEvent event = new WeWorkEvent();
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

    /**
     * 获取 XML 元素文本
     */
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

    /**
     * 解析长整型
     */
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

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nageoffer.ai.ragent.chatbot.common.BotPlatform;
import com.nageoffer.ai.ragent.chatbot.common.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.common.MessageContext;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import com.nageoffer.ai.ragent.chatbot.core.QuestionDetector;
import com.nageoffer.ai.ragent.chatbot.feishu.dto.FeishuEvent;
import com.nageoffer.ai.ragent.chatbot.feishu.dto.FeishuMessage;
import com.nageoffer.ai.ragent.chatbot.service.AnswerGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 飞书消息处理器
 * <p>
 * 处理飞书 Webhook 推送的消息事件
 */
@Slf4j
@RequiredArgsConstructor
public class FeishuMessageHandler {

    private final QuestionDetector questionDetector;
    private final AnswerGenerator answerGenerator;
    private final FeishuApiClient feishuApiClient;
    private final ChatbotProperties properties;

    /**
     * 处理飞书事件
     *
     * @param event 飞书事件
     */
    public void handle(FeishuEvent event) {
        if (event == null) {
            return;
        }

        FeishuMessage message = event.getEvent();
        if (message == null) {
            log.debug("飞书事件无消息内容，跳过处理");
            return;
        }

        // 只处理文本消息
        if (!"text".equals(message.getMsgType())) {
            log.debug("非文本消息，跳过处理: msgType={}", message.getMsgType());
            return;
        }

        // 检查消息是否已删除或撤回
        if (Boolean.TRUE.equals(message.getDeleted()) || Boolean.TRUE.equals(message.getRecalled())) {
            log.debug("消息已删除或撤回，跳过处理");
            return;
        }

        try {
            // 提取文本内容
            String content = extractTextContent(message.getContent());
            if (content == null || content.isBlank()) {
                log.debug("消息内容为空，跳过处理");
                return;
            }

            log.info("处理飞书消息: chatType={}, chatId={}, content={}",
                    message.getChatType(), message.getChatId(),
                    content.length() > 50 ? content.substring(0, 50) + "..." : content);

            // 构建消息上下文
            MessageContext context = buildMessageContext(message, content);

            // 单聊直接回复
            if (context.isPrivateChat()) {
                handlePrivateMessage(content, context);
                return;
            }

            // 群聊需要检测是否为问题
            handleGroupMessage(content, context);

        } catch (Exception e) {
            log.error("处理飞书消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单聊消息
     */
    private void handlePrivateMessage(String content, MessageContext context) {
        log.debug("处理单聊消息");
        generateAndSendReply(content, context);
    }

    /**
     * 处理群聊消息
     */
    private void handleGroupMessage(String content, MessageContext context) {
        // 检测是否为问题
        DetectionResult detectionResult = questionDetector.detect(content, context);

        if (!detectionResult.isQuestion()) {
            log.debug("群聊消息非问题，跳过回复: {}", content);
            return;
        }

        // 使用提取出的问题（可能去除了 @ 等内容）
        String question = detectionResult.getExtractedQuestion();
        if (question == null || question.isBlank()) {
            question = content;
        }

        log.debug("群聊问题检测通过，置信度: {}", detectionResult.getConfidence());
        generateAndSendReply(question, context);
    }

    /**
     * 生成并发送回复
     */
    private void generateAndSendReply(String question, MessageContext context) {
        try {
            // 生成回答
            String answer = answerGenerator.generate(question, context);

            // 发送回复
            feishuApiClient.sendTextMessage(context.getChatId(), answer);

            log.info("飞书消息回复成功: chatId={}", context.getChatId());
        } catch (Exception e) {
            log.error("生成或发送回复失败: {}", e.getMessage(), e);
            // 尝试发送错误提示
            try {
                feishuApiClient.sendTextMessage(context.getChatId(), "抱歉，处理您的消息时出现错误，请稍后再试。");
            } catch (Exception ex) {
                log.error("发送错误提示失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 从消息内容中提取文本
     *
     * @param content JSON 格式的消息内容，如 {"text":"消息内容"}
     * @return 文本内容
     */
    private String extractTextContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        try {
            // 尝试解析 JSON
            if (content.startsWith("{")) {
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                if (json.has("text")) {
                    return json.get("text").getAsString();
                }
            }
            // 非 JSON 格式，直接返回
            return content;
        } catch (Exception e) {
            log.debug("解析消息内容失败，直接返回原始内容: {}", e.getMessage());
            return content;
        }
    }

    /**
     * 构建消息上下文
     */
    private MessageContext buildMessageContext(FeishuMessage message, String content) {
        String botName = properties.getFeishu().getBotName();

        return MessageContext.builder()
                .platform(BotPlatform.FEISHU)
                .chatType(message.getChatType())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .atBot(content.contains("@" + botName))
                .botName(botName)
                .rawContent(content)
                .messageId(message.getMessageId())
                .timestamp(message.getCreateTime())
                .build();
    }
}

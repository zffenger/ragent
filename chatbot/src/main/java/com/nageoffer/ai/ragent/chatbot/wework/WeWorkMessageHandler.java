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

import com.nageoffer.ai.ragent.chatbot.common.BotPlatform;
import com.nageoffer.ai.ragent.chatbot.common.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.common.MessageContext;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import com.nageoffer.ai.ragent.chatbot.core.QuestionDetector;
import com.nageoffer.ai.ragent.chatbot.service.AnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.wework.dto.WeWorkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 企业微信消息处理器
 * <p>
 * 处理企业微信回调推送的消息事件
 */
@Slf4j
@RequiredArgsConstructor
public class WeWorkMessageHandler {

    private final QuestionDetector questionDetector;
    private final AnswerGenerator answerGenerator;
    private final WeWorkApiClient weWorkApiClient;
    private final ChatbotProperties properties;

    /**
     * 处理企微事件
     *
     * @param event 企微事件
     */
    public void handle(WeWorkEvent event) {
        if (event == null) {
            return;
        }

        // 只处理文本消息
        if (!"text".equals(event.getMsgType())) {
            log.debug("非文本消息，跳过处理: msgType={}", event.getMsgType());
            return;
        }

        String content = event.getContent();
        if (content == null || content.isBlank()) {
            log.debug("消息内容为空，跳过处理");
            return;
        }

        log.info("处理企微消息: chatType={}, fromUser={}, content={}",
                event.getChatType(), event.getFromUserName(),
                content.length() > 50 ? content.substring(0, 50) + "..." : content);

        try {
            // 构建消息上下文
            MessageContext context = buildMessageContext(event, content);

            // 判断是单聊还是群聊
            if (context.isPrivateChat()) {
                handlePrivateMessage(content, context);
            } else {
                handleGroupMessage(content, context);
            }
        } catch (Exception e) {
            log.error("处理企微消息异常: {}", e.getMessage(), e);
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

        // 使用提取出的问题
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

            // 发送回复（企微群聊回复需要使用 chatId）
            String toUser = context.isGroupChat() ? context.getChatId() : context.getSenderId();
            weWorkApiClient.sendTextMessage(toUser, answer);

            log.info("企微消息回复成功: toUser={}", toUser);
        } catch (Exception e) {
            log.error("生成或发送回复失败: {}", e.getMessage(), e);
            // 尝试发送错误提示
            try {
                String toUser = context.isGroupChat() ? context.getChatId() : context.getSenderId();
                weWorkApiClient.sendTextMessage(toUser, "抱歉，处理您的消息时出现错误，请稍后再试。");
            } catch (Exception ex) {
                log.error("发送错误提示失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 构建消息上下文
     */
    private MessageContext buildMessageContext(WeWorkEvent event, String content) {
        String chatType = "single".equals(event.getChatType()) ? "p2p" : "group";
        String botName = properties.getWework().getBotName();

        // 检测是否 @机器人（企微格式：<@username>）
        boolean atBot = content.contains("<@") && content.contains(">");

        return MessageContext.builder()
                .platform(BotPlatform.WEWORK)
                .chatType(chatType)
                .chatId(event.getChatId() != null ? event.getChatId() : event.getFromUserName())
                .senderId(event.getFromUserName())
                .atBot(atBot)
                .botName(botName)
                .rawContent(content)
                .messageId(event.getMsgId())
                .timestamp(event.getCreateTime())
                .build();
    }
}

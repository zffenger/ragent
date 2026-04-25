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

package com.nageoffer.ai.ragent.chatbot.application;

import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import com.nageoffer.ai.ragent.chatbot.domain.service.AnswerGenerator;
import com.nageoffer.ai.ragent.chatbot.domain.service.QuestionDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息处理应用服务
 * <p>
 * 编排消息处理的完整流程：问题检测 -> 回答生成 -> 消息发送
 */
@Slf4j
@RequiredArgsConstructor
public class MessageProcessApplication {

    private final QuestionDetector questionDetector;
    private final AnswerGenerator answerGenerator;
    private final MessageSender messageSender;

    /**
     * 处理消息
     */
    public void process(String content, MessageContext context, BotConfig botConfig) {
        if (content == null || content.isBlank()) {
            log.debug("消息内容为空，跳过处理");
            return;
        }

        try {
            String question;

            if (context.isPrivateChat()) {
                question = content;
                log.debug("单聊消息，直接处理");
            } else {
                DetectionResult detectionResult = questionDetector.detect(content, context);
                if (!detectionResult.isQuestion()) {
                    log.debug("群聊消息非问题，跳过回复: {}", content);
                    return;
                }
                question = detectionResult.getExtractedQuestion();
                if (question == null || question.isBlank()) {
                    question = content;
                }
                log.debug("群聊问题检测通过，置信度: {}", detectionResult.getConfidence());
            }

            String answer = answerGenerator.generate(question, context);
            messageSender.send(context.getChatId(), answer, botConfig);

            log.info("消息处理完成: chatId={}", context.getChatId());

        } catch (Exception e) {
            log.error("处理消息异常: {}", e.getMessage(), e);
            try {
                messageSender.send(context.getChatId(), "抱歉，处理您的消息时出现错误，请稍后再试。", botConfig);
            } catch (Exception ex) {
                log.error("发送错误提示失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 消息发送器接口
     */
    public interface MessageSender {
        void send(String chatId, String content, BotConfig botConfig);
    }
}

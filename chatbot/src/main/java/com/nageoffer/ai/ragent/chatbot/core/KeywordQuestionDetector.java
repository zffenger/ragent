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

package com.nageoffer.ai.ragent.chatbot.core;

import com.nageoffer.ai.ragent.chatbot.common.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.common.MessageContext;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词问题检测器
 * <p>
 * 基于关键词匹配和 @机器人 触发来检测问题
 */
@Slf4j
@RequiredArgsConstructor
public class KeywordQuestionDetector implements QuestionDetector {

    private final ChatbotProperties properties;

    /**
     * @ 用户名的正则模式
     */
    private static final Pattern AT_PATTERN = Pattern.compile("@[^\\s]+\\s*");

    @Override
    public DetectionResult detect(String message, MessageContext context) {
        if (message == null || message.isBlank()) {
            return DetectionResult.notQuestion();
        }

        String trimmedMessage = message.trim();

        // 1. @机器人 直接触发
        if (properties.getDetection().isAtTriggerEnabled() && context.isAtBot()) {
            String question = removeAtMention(trimmedMessage, context.getBotName());
            log.debug("@触发问题检测: {}", question);
            return DetectionResult.questionByAt(question.trim());
        }

        // 2. 关键词匹配
        List<String> keywords = properties.getDetection().getKeywords();
        for (String keyword : keywords) {
            if (trimmedMessage.contains(keyword)) {
                log.debug("关键词 '{}' 触发问题检测: {}", keyword, trimmedMessage);
                return DetectionResult.questionByKeyword(trimmedMessage, 0.8);
            }
        }

        // 3. 检查是否以问号结尾
        if (trimmedMessage.endsWith("？") || trimmedMessage.endsWith("?")) {
            log.debug("问号结尾触发问题检测: {}", trimmedMessage);
            return DetectionResult.questionByKeyword(trimmedMessage, 0.75);
        }

        return DetectionResult.notQuestion();
    }

    /**
     * 移除 @提及 内容
     *
     * @param message 原始消息
     * @param botName 机器人名称
     * @return 移除 @后的消息
     */
    private String removeAtMention(String message, String botName) {
        if (botName == null || botName.isBlank()) {
            return message;
        }

        // 移除 @机器人名
        String result = message.replace("@" + botName, "");

        // 移除其他 @用户（飞书格式）
        Matcher matcher = AT_PATTERN.matcher(result);
        result = matcher.replaceAll("");

        return result.trim();
    }
}

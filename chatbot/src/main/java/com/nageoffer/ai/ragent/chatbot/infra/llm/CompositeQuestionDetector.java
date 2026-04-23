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

package com.nageoffer.ai.ragent.chatbot.infra.llm;

import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import com.nageoffer.ai.ragent.chatbot.domain.service.QuestionDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 组合问题检测器
 * <p>
 * 先使用关键词检测快速判断，如果不确定再使用 LLM 语义检测
 */
@Slf4j
@RequiredArgsConstructor
public class CompositeQuestionDetector implements QuestionDetector {

    private final KeywordQuestionDetector keywordDetector;
    private final LlmQuestionDetector llmDetector;

    /**
     * 关键词检测的高置信度阈值
     * 如果关键词检测置信度超过此值，直接返回结果，不调用 LLM
     */
    private static final double KEYWORD_HIGH_CONFIDENCE = 0.75;

    @Override
    public DetectionResult detect(String message, MessageContext context) {
        if (message == null || message.isBlank()) {
            return DetectionResult.notQuestion();
        }

        BotConfig botConfig = context.getBotConfig();

        // 1. @机器人 直接触发，不需要 LLM 判断
        boolean atTriggerEnabled = botConfig != null && botConfig.isAtTriggerEnabled();
        if (atTriggerEnabled && context.isAtBot()) {
            log.debug("@触发，跳过 LLM 检测");
            return keywordDetector.detect(message, context);
        }

        // 2. 先进行关键词检测
        DetectionResult keywordResult = keywordDetector.detect(message, context);

        // 3. 如果关键词检测高置信度，直接返回
        if (keywordResult.isQuestion() && keywordResult.getConfidence() >= KEYWORD_HIGH_CONFIDENCE) {
            log.debug("关键词高置信度检测: confidence={}", keywordResult.getConfidence());
            return keywordResult;
        }

        // 4. 如果关键词检测认为是问题但置信度不够高，也直接返回（避免过度调用 LLM）
        if (keywordResult.isQuestion()) {
            log.debug("关键词检测为问题，置信度: {}", keywordResult.getConfidence());
            return keywordResult;
        }

        // 5. 关键词检测不确定，使用 LLM 进行语义检测
        log.debug("关键词检测不确定，使用 LLM 进行语义检测");
        return llmDetector.detect(message, context);
    }
}

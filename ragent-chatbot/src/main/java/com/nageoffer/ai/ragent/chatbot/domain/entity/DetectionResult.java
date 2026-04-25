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

package com.nageoffer.ai.ragent.chatbot.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问题检测结果
 * <p>
 * 包含是否为问题的判断、置信度和提取出的核心问题
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {

    /**
     * 是否为问题
     */
    private boolean isQuestion;

    /**
     * 置信度（0-1）
     */
    private double confidence;

    /**
     * 提取出的核心问题（可能去除 @ 等无关内容）
     */
    private String extractedQuestion;

    /**
     * 检测方式
     */
    private String detectionMethod;

    /**
     * 创建非问题结果
     */
    public static DetectionResult notQuestion() {
        return new DetectionResult(false, 0.0, null, null);
    }

    /**
     * 创建问题结果（关键词检测）
     */
    public static DetectionResult questionByKeyword(String question, double confidence) {
        return new DetectionResult(true, confidence, question, "KEYWORD");
    }

    /**
     * 创建问题结果（@触发）
     */
    public static DetectionResult questionByAt(String question) {
        return new DetectionResult(true, 1.0, question, "AT_TRIGGER");
    }

    /**
     * 创建问题结果（LLM 检测）
     */
    public static DetectionResult questionByLlm(String question, double confidence) {
        return new DetectionResult(true, confidence, question, "LLM");
    }
}

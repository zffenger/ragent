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

package com.nageoffer.ai.ragent.chatbot.domain.service;

import com.nageoffer.ai.ragent.chatbot.domain.entity.DetectionResult;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;

/**
 * 问题识别器接口
 * <p>
 * 用于检测消息是否为需要回答的问题，支持多种检测策略
 */
public interface QuestionDetector {

    /**
     * 检测消息是否为问题
     *
     * @param message 消息内容
     * @param context 消息上下文（包含是否 @机器人 等信息）
     * @return 检测结果，包含是否为问题、置信度和提取出的问题
     */
    DetectionResult detect(String message, MessageContext context);
}

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

package com.nageoffer.ai.ragent.chatbot.service;

import com.nageoffer.ai.ragent.chatbot.common.MessageContext;

/**
 * 回答生成器接口
 * <p>
 * 用于生成对用户问题的回答，支持 LLM 直接生成和 RAG 检索增强生成
 */
public interface AnswerGenerator {

    /**
     * 生成回答
     *
     * @param question 用户问题
     * @param context  消息上下文
     * @return 生成的回答
     */
    String generate(String question, MessageContext context);
}

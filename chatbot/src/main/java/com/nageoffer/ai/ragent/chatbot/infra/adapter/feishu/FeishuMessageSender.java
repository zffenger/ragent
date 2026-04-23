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

package com.nageoffer.ai.ragent.chatbot.infra.adapter.feishu;

import com.nageoffer.ai.ragent.chatbot.application.MessageProcessApplication;
import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 飞书消息发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuMessageSender implements MessageProcessApplication.MessageSender {

    private final FeishuApiClientFactory apiClientFactory;

    @Override
    public void send(String chatId, String content, BotConfig botConfig) {
        FeishuApiClient apiClient = apiClientFactory.getClient(botConfig);
        apiClient.sendTextMessage(chatId, content);
        log.debug("飞书消息发送完成: chatId={}", chatId);
    }
}

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

package com.nageoffer.ai.ragent.chatbot.infra.adapter.wework;

import com.nageoffer.ai.ragent.chatbot.application.MessageProcessApplication;
import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.entity.MessageContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 企业微信消息发送器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeWorkMessageSender implements MessageProcessApplication.MessageSender {

    private final WeWorkApiClientFactory apiClientFactory;

    @Override
    public void send(String chatId, String content, BotConfig botConfig) {
        WeWorkApiClient apiClient = apiClientFactory.getClient(botConfig);
        apiClient.sendTextMessage(chatId, content);
        log.debug("企微消息发送完成: chatId={}", chatId);
    }
}

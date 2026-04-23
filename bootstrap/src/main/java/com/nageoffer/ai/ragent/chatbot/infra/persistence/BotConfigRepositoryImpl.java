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

package com.nageoffer.ai.ragent.chatbot.infra.persistence;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.repository.BotConfigRepository;
import com.nageoffer.ai.ragent.settings.dao.entity.ChatBotDO;
import com.nageoffer.ai.ragent.settings.dao.mapper.ChatBotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 机器人配置仓库实现
 * <p>
 * 从数据库加载机器人配置
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BotConfigRepositoryImpl implements BotConfigRepository {

    private final ChatBotMapper chatBotMapper;

    @Override
    public BotConfig getByAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return null;
        }

        ChatBotDO bot = chatBotMapper.selectOne(
                new LambdaQueryWrapper<ChatBotDO>()
                        .eq(ChatBotDO::getAppId, appId)
                        .eq(ChatBotDO::getEnabled, 1)
        );

        return convertToConfig(bot);
    }

    @Override
    public BotConfig getById(String botId) {
        if (botId == null || botId.isBlank()) {
            return null;
        }

        ChatBotDO bot = chatBotMapper.selectById(botId);
        if (bot == null || !Integer.valueOf(1).equals(bot.getEnabled())) {
            return null;
        }

        return convertToConfig(bot);
    }

    @Override
    public List<BotConfig> listEnabled() {
        List<ChatBotDO> bots = chatBotMapper.selectList(
                new LambdaQueryWrapper<ChatBotDO>()
                        .eq(ChatBotDO::getEnabled, 1)
        );

        return bots.stream()
                .map(this::convertToConfig)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<BotConfig> listEnabledByPlatform(String platform) {
        List<ChatBotDO> bots = chatBotMapper.selectList(
                new LambdaQueryWrapper<ChatBotDO>()
                        .eq(ChatBotDO::getPlatform, platform)
                        .eq(ChatBotDO::getEnabled, 1)
        );

        return bots.stream()
                .map(this::convertToConfig)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 转换数据库实体为配置对象
     */
    private BotConfig convertToConfig(ChatBotDO entity) {
        if (entity == null) {
            return null;
        }

        List<String> keywords = null;
        if (entity.getDetectionKeywords() != null && !entity.getDetectionKeywords().isBlank()) {
            keywords = JSON.parseArray(entity.getDetectionKeywords(), String.class);
        }

        return BotConfig.builder()
                .id(entity.getId())
                .name(entity.getName())
                .platform(entity.getPlatform())
                .appId(entity.getAppId())
                .appSecret(entity.getAppSecret())
                .encryptKey(entity.getEncryptKey())
                .verificationToken(entity.getVerificationToken())
                .corpId(entity.getCorpId())
                .agentId(entity.getAgentId())
                .token(entity.getToken())
                .encodingAesKey(entity.getEncodingAesKey())
                .botName(entity.getBotName())
                .domainId(entity.getDomainId())
                .detectionMode(entity.getDetectionMode())
                .detectionKeywords(keywords)
                .atTriggerEnabled(Integer.valueOf(1).equals(entity.getAtTriggerEnabled()))
                .llmThreshold(entity.getLlmThreshold() != null ? entity.getLlmThreshold() : new BigDecimal("0.70"))
                .answerMode(entity.getAnswerMode())
                .systemPrompt(entity.getSystemPrompt())
                .maxTokens(entity.getMaxTokens())
                .enabled(Integer.valueOf(1).equals(entity.getEnabled()))
                .build();
    }
}

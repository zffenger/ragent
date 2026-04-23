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

package com.nageoffer.ai.ragent.chatbot.settings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.alibaba.fastjson2.JSON;
import com.nageoffer.ai.ragent.chatbot.config.ChatbotProperties;
import com.nageoffer.ai.ragent.chatbot.settings.dao.entity.SystemConfigDO;
import com.nageoffer.ai.ragent.chatbot.settings.dao.mapper.SystemConfigMapper;
import com.nageoffer.ai.ragent.chatbot.settings.service.BotConfigService;
import com.nageoffer.ai.ragent.chatbot.settings.vo.AnswerConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.DetectionConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.FeishuBotConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.WeWorkBotConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 机器人配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotConfigServiceImpl implements BotConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final StringRedisTemplate redisTemplate;
    private final ChatbotProperties chatbotProperties;

    // 配置键常量
    private static final String CONFIG_KEY_FEISHU = "chatbot.feishu";
    private static final String CONFIG_KEY_WEWORK = "chatbot.wework";
    private static final String CONFIG_KEY_DETECTION = "chatbot.detection";
    private static final String CONFIG_KEY_ANSWER = "chatbot.answer";

    // Redis 缓存键前缀
    private static final String REDIS_PREFIX_CONFIG = "config:";

    @Override
    public FeishuBotConfigVO getFeishuBotConfig() {
        SystemConfigDO config = getConfigByKey(CONFIG_KEY_FEISHU);
        if (config == null) {
            return FeishuBotConfigVO.builder().enabled(false).build();
        }
        return JSON.parseObject(config.getConfigValue(), FeishuBotConfigVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeishuBotConfig(FeishuBotConfigVO config) {
        saveOrUpdateConfig(CONFIG_KEY_FEISHU, JSON.toJSONString(config), "飞书机器人配置");
        refreshConfigCache();
    }

    @Override
    public WeWorkBotConfigVO getWeWorkBotConfig() {
        SystemConfigDO config = getConfigByKey(CONFIG_KEY_WEWORK);
        if (config == null) {
            return WeWorkBotConfigVO.builder().enabled(false).build();
        }
        return JSON.parseObject(config.getConfigValue(), WeWorkBotConfigVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWeWorkBotConfig(WeWorkBotConfigVO config) {
        saveOrUpdateConfig(CONFIG_KEY_WEWORK, JSON.toJSONString(config), "企微机器人配置");
        refreshConfigCache();
    }

    @Override
    public DetectionConfigVO getDetectionConfig() {
        SystemConfigDO config = getConfigByKey(CONFIG_KEY_DETECTION);
        if (config == null) {
            return DetectionConfigVO.builder()
                    .mode("COMPOSITE")
                    .atTriggerEnabled(true)
                    .llmThreshold(0.7)
                    .build();
        }
        return JSON.parseObject(config.getConfigValue(), DetectionConfigVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDetectionConfig(DetectionConfigVO config) {
        saveOrUpdateConfig(CONFIG_KEY_DETECTION, JSON.toJSONString(config), "问题检测配置");
        refreshConfigCache();
    }

    @Override
    public AnswerConfigVO getAnswerConfig() {
        SystemConfigDO config = getConfigByKey(CONFIG_KEY_ANSWER);
        if (config == null) {
            return AnswerConfigVO.builder()
                    .mode("RAG")
                    .maxTokens(2000)
                    .build();
        }
        return JSON.parseObject(config.getConfigValue(), AnswerConfigVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAnswerConfig(AnswerConfigVO config) {
        saveOrUpdateConfig(CONFIG_KEY_ANSWER, JSON.toJSONString(config), "回答生成配置");
        refreshConfigCache();
    }

    @Override
    public void refreshConfigCache() {
        // 清除 Redis 缓存
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_FEISHU);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_WEWORK);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_DETECTION);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_ANSWER);

        // 更新内存中的 ChatbotProperties
        updateChatbotProperties();

        log.info("机器人配置缓存已刷新");
    }

    /**
     * 更新 ChatbotProperties
     */
    private void updateChatbotProperties() {
        // 加载飞书配置
        SystemConfigDO feishuConfig = getConfigByKey(CONFIG_KEY_FEISHU);
        if (feishuConfig != null) {
            FeishuBotConfigVO vo = JSON.parseObject(feishuConfig.getConfigValue(), FeishuBotConfigVO.class);
            ChatbotProperties.FeishuConfig feishu = chatbotProperties.getFeishu();
            feishu.setEnabled(vo.getEnabled() != null && vo.getEnabled());
            feishu.setAppId(vo.getAppId());
            feishu.setAppSecret(vo.getAppSecret());
            feishu.setEncryptKey(vo.getEncryptKey());
            feishu.setVerificationToken(vo.getVerificationToken());
            feishu.setBotName(vo.getBotName() != null ? vo.getBotName() : "智能助手");
        }

        // 加载企微配置
        SystemConfigDO weworkConfig = getConfigByKey(CONFIG_KEY_WEWORK);
        if (weworkConfig != null) {
            WeWorkBotConfigVO vo = JSON.parseObject(weworkConfig.getConfigValue(), WeWorkBotConfigVO.class);
            ChatbotProperties.WeWorkConfig wework = chatbotProperties.getWework();
            wework.setEnabled(vo.getEnabled() != null && vo.getEnabled());
            wework.setCorpId(vo.getCorpId());
            wework.setAgentId(vo.getAgentId());
            wework.setSecret(vo.getSecret());
            wework.setToken(vo.getToken());
            wework.setEncodingAesKey(vo.getEncodingAesKey());
            wework.setBotName(vo.getBotName() != null ? vo.getBotName() : "智能助手");
        }

        // 加载检测配置
        SystemConfigDO detectionConfig = getConfigByKey(CONFIG_KEY_DETECTION);
        if (detectionConfig != null) {
            DetectionConfigVO vo = JSON.parseObject(detectionConfig.getConfigValue(), DetectionConfigVO.class);
            ChatbotProperties.DetectionConfig detection = chatbotProperties.getDetection();
            if (vo.getMode() != null) {
                detection.setMode(ChatbotProperties.DetectionMode.valueOf(vo.getMode()));
            }
            if (vo.getKeywords() != null) {
                detection.setKeywords(vo.getKeywords());
            }
            detection.setAtTriggerEnabled(vo.getAtTriggerEnabled() != null ? vo.getAtTriggerEnabled() : true);
            detection.setLlmThreshold(vo.getLlmThreshold() != null ? vo.getLlmThreshold() : 0.7);
        }

        // 加载回答配置
        SystemConfigDO answerConfig = getConfigByKey(CONFIG_KEY_ANSWER);
        if (answerConfig != null) {
            AnswerConfigVO vo = JSON.parseObject(answerConfig.getConfigValue(), AnswerConfigVO.class);
            ChatbotProperties.AnswerConfig answer = chatbotProperties.getAnswer();
            if (vo.getMode() != null) {
                answer.setMode(ChatbotProperties.AnswerMode.valueOf(vo.getMode()));
            }
            if (vo.getDefaultSystemPrompt() != null) {
                answer.setDefaultSystemPrompt(vo.getDefaultSystemPrompt());
            }
            answer.setMaxTokens(vo.getMaxTokens() != null ? vo.getMaxTokens() : 2000);
        }

        // 更新总开关
        chatbotProperties.setEnabled(
                chatbotProperties.getFeishu().isEnabled() || chatbotProperties.getWework().isEnabled()
        );

        log.info("Chatbot配置刷新完成，飞书启用: {}, 企微启用: {}",
                chatbotProperties.getFeishu().isEnabled(),
                chatbotProperties.getWework().isEnabled());
    }

    /**
     * 根据配置键获取配置
     */
    private SystemConfigDO getConfigByKey(String configKey) {
        return systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfigDO>().eq(SystemConfigDO::getConfigKey, configKey)
        );
    }

    /**
     * 保存或更新配置
     */
    private void saveOrUpdateConfig(String configKey, String configValue, String description) {
        SystemConfigDO existing = getConfigByKey(configKey);
        if (existing != null) {
            existing.setConfigValue(configValue);
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfigDO entity = SystemConfigDO.builder()
                    .configKey(configKey)
                    .configValue(configValue)
                    .description(description)
                    .build();
            systemConfigMapper.insert(entity);
        }
    }
}

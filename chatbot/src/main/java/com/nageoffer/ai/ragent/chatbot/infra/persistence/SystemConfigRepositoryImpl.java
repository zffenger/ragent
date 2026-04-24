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
import com.nageoffer.ai.ragent.chatbot.domain.repository.SystemConfigRepository;
import com.nageoffer.ai.ragent.chatbot.domain.vo.AnswerConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.DetectionConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.FeishuBotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.WeWorkBotConfig;
import com.nageoffer.ai.ragent.chatbot.infra.persistence.mapper.SystemConfigMapper;
import com.nageoffer.ai.ragent.chatbot.infra.persistence.po.SystemConfigPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SystemConfigRepositoryImpl implements SystemConfigRepository {

    private final SystemConfigMapper systemConfigMapper;
    private final StringRedisTemplate redisTemplate;

    // 配置键常量
    private static final String CONFIG_KEY_FEISHU = "chatbot.feishu";
    private static final String CONFIG_KEY_WEWORK = "chatbot.wework";
    private static final String CONFIG_KEY_DETECTION = "chatbot.detection";
    private static final String CONFIG_KEY_ANSWER = "chatbot.answer";

    // Redis 缓存键前缀
    private static final String REDIS_PREFIX_CONFIG = "config:";

    @Override
    public FeishuBotConfig getFeishuBotConfig() {
        SystemConfigPO config = getConfigByKey(CONFIG_KEY_FEISHU);
        if (config == null) {
            return FeishuBotConfig.disabled();
        }
        return parseFeishuBotConfig(config.getConfigValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFeishuBotConfig(FeishuBotConfig config) {
        saveOrUpdateConfig(CONFIG_KEY_FEISHU, JSON.toJSONString(toFeishuJson(config)), "飞书机器人配置");
        refreshCache();
    }

    @Override
    public WeWorkBotConfig getWeWorkBotConfig() {
        SystemConfigPO config = getConfigByKey(CONFIG_KEY_WEWORK);
        if (config == null) {
            return WeWorkBotConfig.disabled();
        }
        return parseWeWorkBotConfig(config.getConfigValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWeWorkBotConfig(WeWorkBotConfig config) {
        saveOrUpdateConfig(CONFIG_KEY_WEWORK, JSON.toJSONString(toWeWorkJson(config)), "企微机器人配置");
        refreshCache();
    }

    @Override
    public DetectionConfig getDetectionConfig() {
        SystemConfigPO config = getConfigByKey(CONFIG_KEY_DETECTION);
        if (config == null) {
            return DetectionConfig.defaultConfig();
        }
        return parseDetectionConfig(config.getConfigValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDetectionConfig(DetectionConfig config) {
        saveOrUpdateConfig(CONFIG_KEY_DETECTION, JSON.toJSONString(toDetectionJson(config)), "问题检测配置");
        refreshCache();
    }

    @Override
    public AnswerConfig getAnswerConfig() {
        SystemConfigPO config = getConfigByKey(CONFIG_KEY_ANSWER);
        if (config == null) {
            return AnswerConfig.defaultConfig();
        }
        return parseAnswerConfig(config.getConfigValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswerConfig(AnswerConfig config) {
        saveOrUpdateConfig(CONFIG_KEY_ANSWER, JSON.toJSONString(toAnswerJson(config)), "回答生成配置");
        refreshCache();
    }

    @Override
    public void refreshCache() {
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_FEISHU);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_WEWORK);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_DETECTION);
        redisTemplate.delete(REDIS_PREFIX_CONFIG + CONFIG_KEY_ANSWER);
        log.info("机器人配置缓存已刷新");
    }

    // ==================== 解析方法 ====================

    private FeishuBotConfig parseFeishuBotConfig(String json) {
        FeishuJson dto = JSON.parseObject(json, FeishuJson.class);
        return new FeishuBotConfig(
                dto.enabled, dto.appId, dto.appSecret,
                dto.encryptKey, dto.verificationToken, dto.botName);
    }

    private WeWorkBotConfig parseWeWorkBotConfig(String json) {
        WeWorkJson dto = JSON.parseObject(json, WeWorkJson.class);
        return new WeWorkBotConfig(
                dto.enabled, dto.corpId, dto.agentId, dto.secret,
                dto.token, dto.encodingAesKey, dto.botName);
    }

    private DetectionConfig parseDetectionConfig(String json) {
        DetectionJson dto = JSON.parseObject(json, DetectionJson.class);
        return new DetectionConfig(dto.mode, dto.keywords, dto.atTriggerEnabled, dto.llmThreshold);
    }

    private AnswerConfig parseAnswerConfig(String json) {
        AnswerJson dto = JSON.parseObject(json, AnswerJson.class);
        return new AnswerConfig(dto.mode, dto.defaultSystemPrompt, dto.maxTokens);
    }

    // ==================== 转换方法 ====================

    private FeishuJson toFeishuJson(FeishuBotConfig config) {
        return new FeishuJson(
                config.enabled(), config.appId(), config.appSecret(),
                config.encryptKey(), config.verificationToken(), config.botName());
    }

    private WeWorkJson toWeWorkJson(WeWorkBotConfig config) {
        return new WeWorkJson(
                config.enabled(), config.corpId(), config.agentId(), config.secret(),
                config.token(), config.encodingAesKey(), config.botName());
    }

    private DetectionJson toDetectionJson(DetectionConfig config) {
        return new DetectionJson(config.mode(), config.keywords(), config.atTriggerEnabled(), config.llmThreshold());
    }

    private AnswerJson toAnswerJson(AnswerConfig config) {
        return new AnswerJson(config.mode(), config.defaultSystemPrompt(), config.maxTokens());
    }

    // ==================== 数据访问方法 ====================

    private SystemConfigPO getConfigByKey(String configKey) {
        return systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfigPO>().eq(SystemConfigPO::getConfigKey, configKey)
        );
    }

    private void saveOrUpdateConfig(String configKey, String configValue, String description) {
        SystemConfigPO existing = getConfigByKey(configKey);
        if (existing != null) {
            existing.setConfigValue(configValue);
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfigPO entity = SystemConfigPO.builder()
                    .configKey(configKey)
                    .configValue(configValue)
                    .description(description)
                    .build();
            systemConfigMapper.insert(entity);
        }
    }

    // ==================== 内部 JSON 类 ====================

    record FeishuJson(Boolean enabled, String appId, String appSecret,
                       String encryptKey, String verificationToken, String botName) {}

    record WeWorkJson(Boolean enabled, String corpId, String agentId, String secret,
                       String token, String encodingAesKey, String botName) {}

    record DetectionJson(String mode, List<String> keywords,
                          Boolean atTriggerEnabled, Double llmThreshold) {}

    record AnswerJson(String mode, String defaultSystemPrompt, Integer maxTokens) {}
}

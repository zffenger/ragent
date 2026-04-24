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

package com.nageoffer.ai.ragent.chatbot.interfaces.controller;

import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import com.nageoffer.ai.ragent.chatbot.domain.repository.SystemConfigRepository;
import com.nageoffer.ai.ragent.chatbot.domain.vo.AnswerConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.DetectionConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.FeishuBotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.WeWorkBotConfig;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.response.AnswerConfigVO;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.response.DetectionConfigVO;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.response.FeishuBotConfigVO;
import com.nageoffer.ai.ragent.chatbot.interfaces.dto.response.WeWorkBotConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机器人配置管理控制器
 * <p>
 * 提供飞书、企微机器人配置的管理API
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settings/chatbot")
public class BotConfigController {

    private final SystemConfigRepository systemConfigRepository;

    // ==================== 飞书机器人配置 ====================

    @GetMapping("/feishu")
    public Result<FeishuBotConfigVO> getFeishuBotConfig() {
        FeishuBotConfig config = systemConfigRepository.getFeishuBotConfig();
        return Results.success(toFeishuBotConfigVO(config));
    }

    @PutMapping("/feishu")
    public Result<Void> updateFeishuBotConfig(@RequestBody FeishuBotConfigVO vo) {
        systemConfigRepository.saveFeishuBotConfig(toFeishuBotConfig(vo));
        return Results.success();
    }

    // ==================== 企微机器人配置 ====================

    @GetMapping("/wework")
    public Result<WeWorkBotConfigVO> getWeWorkBotConfig() {
        WeWorkBotConfig config = systemConfigRepository.getWeWorkBotConfig();
        return Results.success(toWeWorkBotConfigVO(config));
    }

    @PutMapping("/wework")
    public Result<Void> updateWeWorkBotConfig(@RequestBody WeWorkBotConfigVO vo) {
        systemConfigRepository.saveWeWorkBotConfig(toWeWorkBotConfig(vo));
        return Results.success();
    }

    // ==================== 检测配置 ====================

    @GetMapping("/detection")
    public Result<DetectionConfigVO> getDetectionConfig() {
        DetectionConfig config = systemConfigRepository.getDetectionConfig();
        return Results.success(toDetectionConfigVO(config));
    }

    @PutMapping("/detection")
    public Result<Void> updateDetectionConfig(@RequestBody DetectionConfigVO vo) {
        systemConfigRepository.saveDetectionConfig(toDetectionConfig(vo));
        return Results.success();
    }

    // ==================== 回答配置 ====================

    @GetMapping("/answer")
    public Result<AnswerConfigVO> getAnswerConfig() {
        AnswerConfig config = systemConfigRepository.getAnswerConfig();
        return Results.success(toAnswerConfigVO(config));
    }

    @PutMapping("/answer")
    public Result<Void> updateAnswerConfig(@RequestBody AnswerConfigVO vo) {
        systemConfigRepository.saveAnswerConfig(toAnswerConfig(vo));
        return Results.success();
    }

    // ==================== 缓存刷新 ====================

    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        systemConfigRepository.refreshCache();
        return Results.success();
    }

    // ==================== DTO 转换方法 ====================

    private FeishuBotConfigVO toFeishuBotConfigVO(FeishuBotConfig config) {
        return FeishuBotConfigVO.builder()
                .enabled(config.enabled())
                .appId(config.appId())
                .appSecret(config.appSecret())
                .encryptKey(config.encryptKey())
                .verificationToken(config.verificationToken())
                .botName(config.botName())
                .build();
    }

    private FeishuBotConfig toFeishuBotConfig(FeishuBotConfigVO vo) {
        return new FeishuBotConfig(
                vo.getEnabled(), vo.getAppId(), vo.getAppSecret(),
                vo.getEncryptKey(), vo.getVerificationToken(), vo.getBotName());
    }

    private WeWorkBotConfigVO toWeWorkBotConfigVO(WeWorkBotConfig config) {
        return WeWorkBotConfigVO.builder()
                .enabled(config.enabled())
                .corpId(config.corpId())
                .agentId(config.agentId())
                .secret(config.secret())
                .token(config.token())
                .encodingAesKey(config.encodingAesKey())
                .botName(config.botName())
                .build();
    }

    private WeWorkBotConfig toWeWorkBotConfig(WeWorkBotConfigVO vo) {
        return new WeWorkBotConfig(
                vo.getEnabled(), vo.getCorpId(), vo.getAgentId(), vo.getSecret(),
                vo.getToken(), vo.getEncodingAesKey(), vo.getBotName());
    }

    private DetectionConfigVO toDetectionConfigVO(DetectionConfig config) {
        return DetectionConfigVO.builder()
                .mode(config.mode())
                .keywords(config.keywords())
                .atTriggerEnabled(config.atTriggerEnabled())
                .llmThreshold(config.llmThreshold())
                .build();
    }

    private DetectionConfig toDetectionConfig(DetectionConfigVO vo) {
        return new DetectionConfig(
                vo.getMode(), vo.getKeywords(), vo.getAtTriggerEnabled(), vo.getLlmThreshold());
    }

    private AnswerConfigVO toAnswerConfigVO(AnswerConfig config) {
        return AnswerConfigVO.builder()
                .mode(config.mode())
                .defaultSystemPrompt(config.defaultSystemPrompt())
                .maxTokens(config.maxTokens())
                .build();
    }

    private AnswerConfig toAnswerConfig(AnswerConfigVO vo) {
        return new AnswerConfig(
                vo.getMode(), vo.getDefaultSystemPrompt(), vo.getMaxTokens());
    }
}

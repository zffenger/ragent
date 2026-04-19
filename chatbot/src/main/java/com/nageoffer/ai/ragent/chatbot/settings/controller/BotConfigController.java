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

package com.nageoffer.ai.ragent.chatbot.settings.controller;

import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import com.nageoffer.ai.ragent.chatbot.settings.service.BotConfigService;
import com.nageoffer.ai.ragent.chatbot.settings.vo.AnswerConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.DetectionConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.FeishuBotConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.WeWorkBotConfigVO;
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

    private final BotConfigService botConfigService;

    // ==================== 飞书机器人配置 ====================

    /**
     * 获取飞书机器人配置
     */
    @GetMapping("/feishu")
    public Result<FeishuBotConfigVO> getFeishuBotConfig() {
        return Results.success(botConfigService.getFeishuBotConfig());
    }

    /**
     * 更新飞书机器人配置
     */
    @PutMapping("/feishu")
    public Result<Void> updateFeishuBotConfig(@RequestBody FeishuBotConfigVO config) {
        botConfigService.updateFeishuBotConfig(config);
        return Results.success();
    }

    // ==================== 企微机器人配置 ====================

    /**
     * 获取企微机器人配置
     */
    @GetMapping("/wework")
    public Result<WeWorkBotConfigVO> getWeWorkBotConfig() {
        return Results.success(botConfigService.getWeWorkBotConfig());
    }

    /**
     * 更新企微机器人配置
     */
    @PutMapping("/wework")
    public Result<Void> updateWeWorkBotConfig(@RequestBody WeWorkBotConfigVO config) {
        botConfigService.updateWeWorkBotConfig(config);
        return Results.success();
    }

    // ==================== 检测配置 ====================

    /**
     * 获取问题检测配置
     */
    @GetMapping("/detection")
    public Result<DetectionConfigVO> getDetectionConfig() {
        return Results.success(botConfigService.getDetectionConfig());
    }

    /**
     * 更新问题检测配置
     */
    @PutMapping("/detection")
    public Result<Void> updateDetectionConfig(@RequestBody DetectionConfigVO config) {
        botConfigService.updateDetectionConfig(config);
        return Results.success();
    }

    // ==================== 回答配置 ====================

    /**
     * 获取回答生成配置
     */
    @GetMapping("/answer")
    public Result<AnswerConfigVO> getAnswerConfig() {
        return Results.success(botConfigService.getAnswerConfig());
    }

    /**
     * 更新回答生成配置
     */
    @PutMapping("/answer")
    public Result<Void> updateAnswerConfig(@RequestBody AnswerConfigVO config) {
        botConfigService.updateAnswerConfig(config);
        return Results.success();
    }

    // ==================== 缓存刷新 ====================

    /**
     * 刷新配置缓存
     */
    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        botConfigService.refreshConfigCache();
        return Results.success();
    }
}

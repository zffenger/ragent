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

package com.nageoffer.ai.ragent.chatbot.settings.service;

import com.nageoffer.ai.ragent.chatbot.settings.vo.AnswerConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.DetectionConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.FeishuBotConfigVO;
import com.nageoffer.ai.ragent.chatbot.settings.vo.WeWorkBotConfigVO;

/**
 * 机器人配置服务接口
 * <p>
 * 提供飞书、企微机器人配置的管理功能
 */
public interface BotConfigService {

    /**
     * 获取飞书机器人配置
     *
     * @return 飞书机器人配置
     */
    FeishuBotConfigVO getFeishuBotConfig();

    /**
     * 更新飞书机器人配置
     *
     * @param config 飞书机器人配置
     */
    void updateFeishuBotConfig(FeishuBotConfigVO config);

    /**
     * 获取企微机器人配置
     *
     * @return 企微机器人配置
     */
    WeWorkBotConfigVO getWeWorkBotConfig();

    /**
     * 更新企微机器人配置
     *
     * @param config 企微机器人配置
     */
    void updateWeWorkBotConfig(WeWorkBotConfigVO config);

    /**
     * 获取问题检测配置
     *
     * @return 问题检测配置
     */
    DetectionConfigVO getDetectionConfig();

    /**
     * 更新问题检测配置
     *
     * @param config 问题检测配置
     */
    void updateDetectionConfig(DetectionConfigVO config);

    /**
     * 获取回答生成配置
     *
     * @return 回答生成配置
     */
    AnswerConfigVO getAnswerConfig();

    /**
     * 更新回答生成配置
     *
     * @param config 回答生成配置
     */
    void updateAnswerConfig(AnswerConfigVO config);

    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();
}

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

package com.nageoffer.ai.ragent.chatbot.domain.repository;

import com.nageoffer.ai.ragent.chatbot.domain.vo.AnswerConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.DetectionConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.FeishuBotConfig;
import com.nageoffer.ai.ragent.chatbot.domain.vo.WeWorkBotConfig;

/**
 * 系统配置仓储接口
 * <p>
 * 提供机器人配置的持久化能力
 */
public interface SystemConfigRepository {

    /**
     * 获取飞书机器人配置
     */
    FeishuBotConfig getFeishuBotConfig();

    /**
     * 保存飞书机器人配置
     */
    void saveFeishuBotConfig(FeishuBotConfig config);

    /**
     * 获取企微机器人配置
     */
    WeWorkBotConfig getWeWorkBotConfig();

    /**
     * 保存企微机器人配置
     */
    void saveWeWorkBotConfig(WeWorkBotConfig config);

    /**
     * 获取问题检测配置
     */
    DetectionConfig getDetectionConfig();

    /**
     * 保存问题检测配置
     */
    void saveDetectionConfig(DetectionConfig config);

    /**
     * 获取回答生成配置
     */
    AnswerConfig getAnswerConfig();

    /**
     * 保存回答生成配置
     */
    void saveAnswerConfig(AnswerConfig config);

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}

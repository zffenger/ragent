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

import com.nageoffer.ai.ragent.chatbot.domain.entity.BotConfig;

import java.util.List;

/**
 * 机器人配置仓库接口
 * <p>
 * 用于从数据库加载机器人配置
 */
public interface BotConfigRepository {

    /**
     * 根据应用 ID 获取机器人配置
     *
     * @param appId 应用 ID
     * @return 机器人配置，不存在返回 null
     */
    BotConfig getByAppId(String appId);

    /**
     * 根据机器人 ID 获取配置
     *
     * @param botId 机器人 ID
     * @return 机器人配置，不存在返回 null
     */
    BotConfig getById(String botId);

    /**
     * 获取所有启用的机器人配置
     *
     * @return 启用的机器人配置列表
     */
    List<BotConfig> listEnabled();

    /**
     * 获取指定平台所有启用的机器人配置
     *
     * @param platform 平台：FEISHU/WEWORK
     * @return 启用的机器人配置列表
     */
    List<BotConfig> listEnabledByPlatform(String platform);
}

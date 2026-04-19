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

package com.nageoffer.ai.ragent.settings.service;

import com.nageoffer.ai.ragent.settings.controller.vo.ChatBotVO;

import java.util.List;

/**
 * 聊天机器人服务接口
 */
public interface ChatBotService {

    /**
     * 获取所有机器人
     *
     * @return 机器人列表
     */
    List<ChatBotVO> listAll();

    /**
     * 按平台获取机器人列表
     *
     * @param platform 平台：FEISHU/WEWORK
     * @return 机器人列表
     */
    List<ChatBotVO> listByPlatform(String platform);

    /**
     * 获取机器人详情
     *
     * @param id 机器人 ID
     * @return 机器人详情
     */
    ChatBotVO getById(String id);

    /**
     * 创建机器人
     *
     * @param vo 机器人信息
     * @return 创建后的机器人
     */
    ChatBotVO create(ChatBotVO vo);

    /**
     * 更新机器人
     *
     * @param id 机器人 ID
     * @param vo 机器人信息
     * @return 更新后的机器人
     */
    ChatBotVO update(String id, ChatBotVO vo);

    /**
     * 删除机器人
     *
     * @param id 机器人 ID
     */
    void delete(String id);

    /**
     * 启用/禁用机器人
     *
     * @param id      机器人 ID
     * @param enabled 是否启用
     */
    void setEnabled(String id, boolean enabled);

    /**
     * 绑定检索域
     *
     * @param botId    机器人 ID
     * @param domainId 检索域 ID
     */
    void bindDomain(String botId, String domainId);

    /**
     * 获取机器人绑定的检索域 ID
     *
     * @param botId 机器人 ID
     * @return 检索域 ID，未绑定返回 null
     */
    String getDomainId(String botId);
}

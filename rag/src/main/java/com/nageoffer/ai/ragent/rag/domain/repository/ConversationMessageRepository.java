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

package com.nageoffer.ai.ragent.rag.domain.repository;

import com.nageoffer.ai.ragent.rag.infra.persistence.po.ConversationMessageDO;

import java.util.Date;
import java.util.List;

/**
 * 会话消息仓储接口
 */
public interface ConversationMessageRepository {

    /**
     * 保存消息
     *
     * @param message 消息信息
     * @return 消息ID
     */
    String save(ConversationMessageDO message);

    /**
     * 根据会话ID和用户ID查询消息列表
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param limit          限制数量
     * @param asc            是否升序
     * @return 消息列表
     */
    List<ConversationMessageDO> listByConversationIdAndUserId(String conversationId, String userId, Integer limit, boolean asc);

    /**
     * 根据会话ID和用户ID查询用户消息列表（倒序）
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param limit          限制数量
     * @return 用户消息列表
     */
    List<ConversationMessageDO> listLatestUserMessages(String conversationId, String userId, int limit);

    /**
     * 根据ID范围查询消息列表
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param afterId        起始消息ID（不包含）
     * @param beforeId       结束消息ID（不包含）
     * @return 消息列表
     */
    List<ConversationMessageDO> listBetweenIds(String conversationId, String userId, String afterId, String beforeId);

    /**
     * 查询指定时间点之前或当时的最大消息ID
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param at             指定的时间点
     * @return 最大消息ID
     */
    String findMaxIdAtOrBefore(String conversationId, String userId, Date at);

    /**
     * 统计用户在指定会话中的消息数量
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return 消息数量
     */
    long countUserMessages(String conversationId, String userId);

    /**
     * 根据消息ID和用户ID查询消息
     *
     * @param messageId 消息ID
     * @param userId    用户ID
     * @return 消息信息
     */
    ConversationMessageDO findByIdAndUserId(String messageId, String userId);

    /**
     * 根据会话ID和用户ID删除消息
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     */
    void deleteByConversationIdAndUserId(String conversationId, String userId);
}

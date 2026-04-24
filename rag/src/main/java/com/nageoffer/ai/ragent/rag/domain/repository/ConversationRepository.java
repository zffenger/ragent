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

import com.nageoffer.ai.ragent.rag.domain.entity.Conversation;

import java.util.List;

/**
 * 会话仓储接口
 */
public interface ConversationRepository {

    /**
     * 根据用户ID查询会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> listByUserId(String userId);

    /**
     * 根据会话ID和用户ID查询会话
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return 会话信息
     */
    Conversation findByConversationIdAndUserId(String conversationId, String userId);

    /**
     * 保存会话
     *
     * @param conversation 会话信息
     */
    void save(Conversation conversation);

    /**
     * 更新会话
     *
     * @param conversation 会话信息
     */
    void update(Conversation conversation);

    /**
     * 根据主键ID删除会话
     *
     * @param id 主键ID
     */
    void deleteById(String id);
}

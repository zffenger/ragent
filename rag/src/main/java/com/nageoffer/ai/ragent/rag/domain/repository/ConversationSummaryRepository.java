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

import com.nageoffer.ai.ragent.rag.infra.persistence.po.ConversationSummaryDO;

/**
 * 会话摘要仓储接口
 */
public interface ConversationSummaryRepository {

    /**
     * 保存摘要
     *
     * @param summary 摘要信息
     */
    void save(ConversationSummaryDO summary);

    /**
     * 根据会话ID和用户ID查询最新摘要
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return 最新摘要
     */
    ConversationSummaryDO findLatestByConversationIdAndUserId(String conversationId, String userId);

    /**
     * 根据会话ID和用户ID删除摘要
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     */
    void deleteByConversationIdAndUserId(String conversationId, String userId);
}

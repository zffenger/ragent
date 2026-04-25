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

import com.nageoffer.ai.ragent.rag.domain.entity.MessageFeedback;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 消息反馈仓储接口
 */
public interface MessageFeedbackRepository {

    /**
     * 保存反馈
     *
     * @param feedback 反馈信息
     */
    void save(MessageFeedback feedback);

    /**
     * 更新反馈
     *
     * @param feedback   反馈信息
     * @param beforeTime 更新时间限制（仅当记录更新时间早于此时间时才更新）
     */
    void update(MessageFeedback feedback, Date beforeTime);

    /**
     * 根据消息ID和用户ID查询反馈
     *
     * @param messageId 消息ID
     * @param userId    用户ID
     * @return 反馈信息
     */
    MessageFeedback findByMessageIdAndUserId(String messageId, String userId);

    /**
     * 根据用户ID和消息ID列表查询反馈
     *
     * @param userId     用户ID
     * @param messageIds 消息ID列表
     * @return messageId -> vote 映射
     */
    Map<String, Integer> findVotesByUserIdAndMessageIds(String userId, List<String> messageIds);
}

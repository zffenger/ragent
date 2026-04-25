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

import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentSchedule;

import java.util.List;

/**
 * 知识库文档定时任务仓储接口
 */
public interface KnowledgeDocumentScheduleRepository {

    /**
     * 根据ID查询定时任务
     *
     * @param id 定时任务ID
     * @return 定时任务信息
     */
    KnowledgeDocumentSchedule findById(String id);

    /**
     * 根据文档ID查询定时任务
     *
     * @param docId 文档ID
     * @return 定时任务信息
     */
    KnowledgeDocumentSchedule findByDocId(String docId);

    /**
     * 查询所有启用的定时任务
     *
     * @return 定时任务列表
     */
    List<KnowledgeDocumentSchedule> findAllEnabled();

    /**
     * 保存定时任务
     *
     * @param schedule 定时任务信息
     */
    void save(KnowledgeDocumentSchedule schedule);

    /**
     * 更新定时任务
     *
     * @param schedule 定时任务信息
     */
    void update(KnowledgeDocumentSchedule schedule);

    /**
     * 根据文档ID删除定时任务
     *
     * @param docId 文档ID
     */
    void deleteByDocId(String docId);

    /**
     * 尝试获取锁
     *
     * @param docId    文档ID
     * @param owner    锁持有者
     * @param lockUntil 锁到期时间
     * @return 是否获取成功
     */
    boolean tryLock(String docId, String owner, java.util.Date lockUntil);

    /**
     * 释放锁
     *
     * @param docId 文档ID
     * @param owner 锁持有者
     */
    void releaseLock(String docId, String owner);
}

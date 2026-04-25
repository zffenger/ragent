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

import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentScheduleExec;

import java.util.List;

/**
 * 知识库文档定时执行记录仓储接口
 */
public interface KnowledgeDocumentScheduleExecRepository {

    /**
     * 根据ID查询执行记录
     *
     * @param id 执行记录ID
     * @return 执行记录信息
     */
    KnowledgeDocumentScheduleExec findById(String id);

    /**
     * 根据定时任务ID查询执行记录列表
     *
     * @param scheduleId 定时任务ID
     * @return 执行记录列表
     */
    List<KnowledgeDocumentScheduleExec> findByScheduleId(String scheduleId);

    /**
     * 根据文档ID查询执行记录列表
     *
     * @param docId 文档ID
     * @return 执行记录列表
     */
    List<KnowledgeDocumentScheduleExec> findByDocId(String docId);

    /**
     * 保存执行记录
     *
     * @param exec 执行记录信息
     */
    void save(KnowledgeDocumentScheduleExec exec);

    /**
     * 根据文档ID删除所有执行记录
     *
     * @param docId 文档ID
     */
    void deleteByDocId(String docId);
}

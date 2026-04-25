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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentChunkLog;

import java.util.List;

/**
 * 知识库文档分块日志仓储接口
 */
public interface KnowledgeDocumentChunkLogRepository {

    /**
     * 根据ID查询日志
     *
     * @param id 日志ID
     * @return 日志信息
     */
    KnowledgeDocumentChunkLog findById(String id);

    /**
     * 根据文档ID查询日志列表
     *
     * @param docId 文档ID
     * @return 日志列表
     */
    List<KnowledgeDocumentChunkLog> findByDocId(String docId);

    /**
     * 分页查询日志
     *
     * @param page  分页参数
     * @param docId 文档ID
     * @return 分页结果
     */
    IPage<KnowledgeDocumentChunkLog> findPage(Page<?> page, String docId);

    /**
     * 保存日志
     *
     * @param log 日志信息
     */
    void save(KnowledgeDocumentChunkLog log);

    /**
     * 更新日志
     *
     * @param log 日志信息
     */
    void update(KnowledgeDocumentChunkLog log);

    /**
     * 根据文档ID删除所有日志
     *
     * @param docId 文档ID
     */
    void deleteByDocId(String docId);
}

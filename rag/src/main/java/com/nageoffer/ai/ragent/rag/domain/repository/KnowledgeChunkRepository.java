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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeChunk;

import java.util.Collection;
import java.util.List;

/**
 * 知识库分块仓储接口
 */
public interface KnowledgeChunkRepository {

    /**
     * 根据ID查询分块
     *
     * @param id 分块ID
     * @return 分块信息
     */
    KnowledgeChunk findById(String id);

    /**
     * 根据ID列表批量查询分块
     *
     * @param ids 分块ID列表
     * @return 分块列表
     */
    List<KnowledgeChunk> findByIds(Collection<String> ids);

    /**
     * 根据文档ID查询所有分块
     *
     * @param docId 文档ID
     * @return 分块列表
     */
    List<KnowledgeChunk> findByDocId(String docId);

    /**
     * 分页查询分块
     *
     * @param page  分页参数
     * @param docId 文档ID
     * @return 分页结果
     */
    IPage<KnowledgeChunk> findPage(Page<?> page, String docId);

    /**
     * 保存分块
     *
     * @param chunk 分块信息
     */
    void save(KnowledgeChunk chunk);

    /**
     * 批量保存分块
     *
     * @param chunks 分块列表
     */
    void saveAll(List<KnowledgeChunk> chunks);

    /**
     * 更新分块
     *
     * @param chunk 分块信息
     */
    void update(KnowledgeChunk chunk);

    /**
     * 删除分块（软删除）
     *
     * @param id 分块ID
     */
    void deleteById(String id);

    /**
     * 根据文档ID删除所有分块（软删除）
     *
     * @param docId 文档ID
     */
    void deleteByDocId(String docId);

    /**
     * 更新分块启用状态
     *
     * @param id      分块ID
     * @param enabled 是否启用
     */
    void updateEnabled(String id, Integer enabled);

    /**
     * 批量更新文档下所有分块的启用状态
     *
     * @param docId   文档ID
     * @param enabled 是否启用
     */
    void updateEnabledByDocId(String docId, Integer enabled);

    /**
     * 批量更新指定分块的启用状态
     *
     * @param ids     分块ID列表
     * @param enabled 是否启用
     * @param updatedBy 更新人
     */
    void updateEnabledByIds(Collection<String> ids, Integer enabled, String updatedBy);

    /**
     * 查询文档下最新的分块（按chunkIndex降序）
     *
     * @param docId 文档ID
     * @return 最新的分块
     */
    KnowledgeChunk findLatestByDocId(String docId);

    /**
     * 查询指定ID列表中启用状态不等于给定值的分块
     *
     * @param ids     分块ID列表
     * @param enabled 启用状态
     * @return 分块列表
     */
    List<KnowledgeChunk> findByIdsAndNotEnabled(Collection<String> ids, Integer enabled);
}

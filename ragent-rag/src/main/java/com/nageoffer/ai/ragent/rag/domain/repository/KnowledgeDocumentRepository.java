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
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocument;

import java.util.Collection;
import java.util.List;

/**
 * 知识库文档仓储接口
 */
public interface KnowledgeDocumentRepository {

    /**
     * 根据ID查询文档
     *
     * @param id 文档ID
     * @return 文档信息
     */
    KnowledgeDocument findById(String id);

    /**
     * 根据ID列表批量查询文档
     *
     * @param ids 文档ID列表
     * @return 文档列表
     */
    List<KnowledgeDocument> findByIds(Collection<String> ids);

    /**
     * 根据知识库ID查询文档列表
     *
     * @param kbId 知识库ID
     * @return 文档列表
     */
    List<KnowledgeDocument> findByKbId(String kbId);

    /**
     * 分页查询文档
     *
     * @param page    分页参数
     * @param kbId    知识库ID
     * @param keyword 关键字（可选）
     * @param status  状态（可选）
     * @return 分页结果
     */
    IPage<KnowledgeDocument> findPage(Page<?> page, String kbId, String keyword, String status);

    /**
     * 搜索文档（按名称模糊匹配）
     *
     * @param keyword 关键字
     * @param limit   数量限制
     * @return 文档列表
     */
    List<KnowledgeDocument> search(String keyword, int limit);

    /**
     * 保存文档
     *
     * @param document 文档信息
     */
    void save(KnowledgeDocument document);

    /**
     * 更新文档（根据ID更新非空字段）
     *
     * @param document 文档信息
     */
    void updateById(KnowledgeDocument document);

    /**
     * 删除文档（软删除）
     *
     * @param id 文档ID
     */
    void deleteById(String id);

    /**
     * 增加分块计数
     *
     * @param id    文档ID
     * @param delta 增量
     */
    void incrementChunkCount(String id, int delta);

    /**
     * 尝试更新文档状态（乐观锁）
     *
     * @param id        文档ID
     * @param newStatus 新状态
     * @param updatedBy 更新人
     * @return 是否更新成功
     */
    boolean tryUpdateStatus(String id, String newStatus, String updatedBy);

    /**
     * 软删除文档
     *
     * @param id        文档ID
     * @param updatedBy 更新人
     */
    void softDelete(String id, String updatedBy);
}

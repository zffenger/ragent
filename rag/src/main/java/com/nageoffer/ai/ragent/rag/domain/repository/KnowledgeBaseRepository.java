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

import com.nageoffer.ai.ragent.rag.infra.persistence.po.KnowledgeBaseDO;

import java.util.Collection;
import java.util.List;

/**
 * 知识库仓储接口
 */
public interface KnowledgeBaseRepository {

    /**
     * 根据ID查询知识库
     *
     * @param id 知识库ID
     * @return 知识库信息
     */
    KnowledgeBaseDO findById(String id);

    /**
     * 根据ID列表查询知识库
     *
     * @param ids 知识库ID列表
     * @return 知识库列表
     */
    List<KnowledgeBaseDO> findByIds(Collection<String> ids);

    /**
     * 查询所有知识库
     *
     * @return 知识库列表
     */
    List<KnowledgeBaseDO> findAll();
}

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

import com.nageoffer.ai.ragent.rag.domain.entity.IngestionPipelineNode;

import java.util.List;

/**
 * 摄取管道节点仓储接口
 */
public interface IngestionPipelineNodeRepository {

    /**
     * 根据ID查询节点
     *
     * @param id 节点ID
     * @return 节点信息
     */
    IngestionPipelineNode findById(String id);

    /**
     * 根据管道ID查询所有节点
     *
     * @param pipelineId 管道ID
     * @return 节点列表
     */
    List<IngestionPipelineNode> findByPipelineId(String pipelineId);

    /**
     * 保存节点
     *
     * @param node 节点信息
     */
    void save(IngestionPipelineNode node);

    /**
     * 根据管道ID删除所有节点（软删除）
     *
     * @param pipelineId 管道ID
     */
    void deleteByPipelineId(String pipelineId);
}

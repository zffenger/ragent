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
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionPipeline;

import java.util.Collection;
import java.util.List;

/**
 * 摄取管道仓储接口
 */
public interface IngestionPipelineRepository {

    /**
     * 根据ID查询管道
     *
     * @param id 管道ID
     * @return 管道信息
     */
    IngestionPipeline findById(String id);

    /**
     * 根据ID列表批量查询管道
     *
     * @param ids 管道ID列表
     * @return 管道列表
     */
    List<IngestionPipeline> findByIds(Collection<String> ids);
    /**
     * 分页查询管道
     *
     * @param page    分页参数
     * @param keyword 关键字（可选，用于模糊匹配名称）
     * @return 分页结果
     */
    IPage<IngestionPipeline> findPage(Page<?> page, String keyword);

    /**
     * 保存管道
     *
     * @param pipeline 管道信息
     */
    void save(IngestionPipeline pipeline);

    /**
     * 更新管道
     *
     * @param pipeline 管道信息
     */
    void update(IngestionPipeline pipeline);

    /**
     * 删除管道（软删除）
     *
     * @param id 管道ID
     */
    void deleteById(String id);
}

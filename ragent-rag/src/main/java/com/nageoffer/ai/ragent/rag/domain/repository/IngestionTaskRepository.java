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
import com.nageoffer.ai.ragent.rag.domain.entity.IngestionTask;

import java.util.List;

/**
 * 摄取任务仓储接口
 */
public interface IngestionTaskRepository {

    /**
     * 根据ID查询任务
     *
     * @param id 任务ID
     * @return 任务信息
     */
    IngestionTask findById(String id);

    /**
     * 根据管道ID查询任务列表
     *
     * @param pipelineId 管道ID
     * @return 任务列表
     */
    List<IngestionTask> findByPipelineId(String pipelineId);

    /**
     * 分页查询任务
     *
     * @param page   分页参数
     * @param status 任务状态（可选）
     * @return 分页结果
     */
    IPage<IngestionTask> findPage(Page<?> page, String status);

    /**
     * 保存任务
     *
     * @param task 任务信息
     */
    void save(IngestionTask task);

    /**
     * 更新任务
     *
     * @param task 任务信息
     */
    void update(IngestionTask task);
}

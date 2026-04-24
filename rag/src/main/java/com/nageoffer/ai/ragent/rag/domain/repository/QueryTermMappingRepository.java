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
import com.nageoffer.ai.ragent.rag.infra.persistence.po.QueryTermMappingDO;

import java.util.List;

/**
 * 查询术语映射仓储接口
 */
public interface QueryTermMappingRepository {

    /**
     * 保存映射规则
     *
     * @param mapping 映射规则
     */
    void save(QueryTermMappingDO mapping);

    /**
     * 更新映射规则
     *
     * @param mapping 映射规则
     */
    void update(QueryTermMappingDO mapping);

    /**
     * 根据ID删除映射规则
     *
     * @param id 主键ID
     */
    void deleteById(String id);

    /**
     * 根据ID查询映射规则
     *
     * @param id 主键ID
     * @return 映射规则
     */
    QueryTermMappingDO findById(String id);

    /**
     * 查询所有启用的映射规则
     *
     * @return 映射规则列表
     */
    List<QueryTermMappingDO> findAllEnabled();

    /**
     * 分页查询映射规则
     *
     * @param page    分页参数
     * @param keyword 关键词
     * @return 分页结果
     */
    IPage<QueryTermMappingDO> pageQuery(IPage<QueryTermMappingDO> page, String keyword);
}

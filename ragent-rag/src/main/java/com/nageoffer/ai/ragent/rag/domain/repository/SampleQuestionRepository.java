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
import com.nageoffer.ai.ragent.rag.domain.entity.SampleQuestion;

import java.util.List;

/**
 * 示例问题仓储接口
 */
public interface SampleQuestionRepository {

    /**
     * 保存示例问题
     *
     * @param question 示例问题
     */
    void save(SampleQuestion question);

    /**
     * 更新示例问题
     *
     * @param question 示例问题
     */
    void update(SampleQuestion question);

    /**
     * 根据ID删除示例问题
     *
     * @param id 主键ID
     */
    void deleteById(String id);

    /**
     * 根据ID查询示例问题
     *
     * @param id 主键ID
     * @return 示例问题
     */
    SampleQuestion findById(String id);

    /**
     * 分页查询示例问题
     *
     * @param page    分页参数
     * @param keyword 关键词
     * @return 分页结果
     */
    IPage<SampleQuestion> pageQuery(IPage<SampleQuestion> page, String keyword);

    /**
     * 随机查询示例问题
     *
     * @param limit 数量限制
     * @return 示例问题列表
     */
    List<SampleQuestion> listRandom(int limit);
}

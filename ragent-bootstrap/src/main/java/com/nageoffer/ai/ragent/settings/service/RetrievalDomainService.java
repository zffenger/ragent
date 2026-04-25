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

package com.nageoffer.ai.ragent.settings.service;

import com.nageoffer.ai.ragent.settings.controller.vo.RetrievalDomainVO;

import java.util.List;

/**
 * 检索域服务接口
 */
public interface RetrievalDomainService {

    /**
     * 获取所有检索域
     *
     * @return 检索域列表
     */
    List<RetrievalDomainVO> listAll();

    /**
     * 获取检索域详情
     *
     * @param id 检索域 ID
     * @return 检索域详情
     */
    RetrievalDomainVO getById(String id);

    /**
     * 创建检索域
     *
     * @param vo 检索域信息
     * @return 创建后的检索域
     */
    RetrievalDomainVO create(RetrievalDomainVO vo);

    /**
     * 更新检索域
     *
     * @param id 检索域 ID
     * @param vo 检索域信息
     * @return 更新后的检索域
     */
    RetrievalDomainVO update(String id, RetrievalDomainVO vo);

    /**
     * 删除检索域
     *
     * @param id 检索域 ID
     */
    void delete(String id);

    /**
     * 绑定知识库到检索域
     *
     * @param domainId     检索域 ID
     * @param knowledgeIds 知识库 ID 列表
     */
    void bindKnowledges(String domainId, List<String> knowledgeIds);

    /**
     * 解绑知识库
     *
     * @param domainId    检索域 ID
     * @param knowledgeId 知识库 ID
     */
    void unbindKnowledge(String domainId, String knowledgeId);

    /**
     * 获取检索域关联的知识库 ID 列表
     *
     * @param domainId 检索域 ID
     * @return 知识库 ID 列表
     */
    List<String> getKnowledgeIds(String domainId);
}

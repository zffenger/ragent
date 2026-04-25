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

package com.nageoffer.ai.ragent.settings.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索域 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrievalDomainVO {

    /**
     * 主键 ID
     */
    private String id;

    /**
     * 检索域名称
     */
    private String name;

    /**
     * 检索域描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 关联的知识库 ID 列表
     */
    private List<String> knowledgeIds;

    /**
     * 关联的知识库简要信息
     */
    private List<KnowledgeBrief> knowledges;

    /**
     * 绑定的机器人数量
     */
    private Integer botCount;

    /**
     * 知识库简要信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KnowledgeBrief {
        private String id;
        private String name;
        private Integer documentCount;
    }
}

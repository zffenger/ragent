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

package com.nageoffer.ai.ragent.rag.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 意图节点实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentNode {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 知识库ID
     */
    private String kbId;

    /**
     * 业务唯一标识
     */
    private String intentCode;

    /**
     * 展示名称
     */
    private String name;

    /**
     * 层级：0=DOMAIN,1=CATEGORY,2=TOPIC
     */
    private Integer level;

    /**
     * 父节点的intentCode
     */
    private String parentCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 示例问题（JSON数组）
     */
    private String examples;

    /**
     * 向量集合名称
     */
    private String collectionName;

    /**
     * MCP工具ID
     */
    private String mcpToolId;

    /**
     * 节点级检索TopK
     */
    private Integer topK;

    /**
     * 类型：0=KB(RAG)，1=SYSTEM，2=MCP
     */
    private Integer kind;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 短规则片段
     */
    private String promptSnippet;

    /**
     * 完整Prompt模板
     */
    private String promptTemplate;

    /**
     * 参数提取提示词模板
     */
    private String paramPromptTemplate;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}

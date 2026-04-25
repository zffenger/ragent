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

package com.nageoffer.ai.ragent.settings.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 检索域-知识库关联实体
 * <p>
 * 多对多关系：一个检索域可包含多个知识库，一个知识库可属于多个检索域
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_domain_knowledge")
public class DomainKnowledgeDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 检索域 ID
     */
    private String domainId;

    /**
     * 知识库 ID
     */
    private String knowledgeId;

    /**
     * 优先级，越小越高
     */
    private Integer priority;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 是否删除：0-正常，1-删除
     */
    @TableLogic
    private Integer deleted;
}

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

package com.nageoffer.ai.ragent.llm.infra.persistence.po;

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
 * 模型候选实体
 * <p>
 * 存储各类模型（Chat/Embedding/Rerank）的候选配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_model_candidate")
public class ModelCandidateDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 模型标识
     */
    private String modelId;

    /**
     * 模型类型：CHAT/EMBEDDING/RERANK
     */
    private String modelType;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 自定义 URL
     */
    private String url;

    /**
     * 向量维度（embedding 模型）
     */
    private Integer dimension;

    /**
     * 优先级，越小越高
     */
    private Integer priority;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 是否支持思考链：0-否，1-是
     */
    private Integer supportsThinking;

    /**
     * 是否默认模型：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 是否深度思考模型：0-否，1-是
     */
    private Integer isDeepThinking;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除：0-正常，1-删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 模型类型枚举
     */
    public enum ModelType {
        CHAT,
        EMBEDDING,
        RERANK
    }
}

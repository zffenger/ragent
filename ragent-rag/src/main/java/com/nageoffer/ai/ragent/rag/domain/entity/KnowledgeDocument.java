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
 * 知识库文档领域实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeDocument {

    private String id;

    /**
     * 所属知识库 ID
     */
    private String kbId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 来源类型：file / url
     */
    private String sourceType;

    /**
     * 来源位置（URL）
     */
    private String sourceLocation;

    /**
     * 是否开启定时拉取：1-启用，0-禁用
     */
    private Integer scheduleEnabled;

    /**
     * 定时表达式（cron）
     */
    private String scheduleCron;

    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer enabled;

    /**
     * 分块数（chunk 数量）
     */
    private Integer chunkCount;

    /**
     * 文件地址（存 OSS / NFS 等路径）
     */
    private String fileUrl;

    /**
     * 文件类型：pdf / markdown / docx 等
     */
    private String fileType;

    /**
     * 文件大小（单位字节）
     */
    private Long fileSize;

    /**
     * 处理模式：chunk / pipeline
     */
    private String processMode;

    /**
     * 分块策略
     */
    private String chunkStrategy;

    /**
     * 分块参数配置（JSON）
     */
    private String chunkConfig;

    /**
     * 数据通道（Pipeline）ID
     */
    private String pipelineId;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 修改人
     */
    private String updatedBy;

    private Date createTime;

    private Date updateTime;
}

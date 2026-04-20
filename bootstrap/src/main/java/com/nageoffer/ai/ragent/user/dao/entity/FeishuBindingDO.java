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

package com.nageoffer.ai.ragent.user.dao.entity;

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
 * 飞书账号绑定表
 * <p>
 * 存储用户与飞书账号的绑定关系，支持飞书 OAuth 登录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_feishu_binding")
public class FeishuBindingDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 关联的用户 ID
     */
    private String userId;

    /**
     * 飞书 Open ID（用户在应用内的唯一标识）
     */
    private String feishuOpenId;

    /**
     * 飞书 User ID（用户在企业内的唯一标识）
     */
    private String feishuUserId;

    /**
     * 飞书用户名称
     */
    private String feishuName;

    /**
     * 飞书用户头像 URL
     */
    private String feishuAvatar;

    /**
     * 绑定时间
     */
    private Date bindTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}

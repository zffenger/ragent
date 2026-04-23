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

package com.nageoffer.ai.ragent.chatbot.infra.adapter.wework;

import lombok.Data;

/**
 * 企业微信响应 DTO
 * <p>
 * 企业微信 API 的响应结构
 */
@Data
public class WeWorkResponseDTO {

    /**
     * 错误码
     * 0 表示成功
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

    /**
     * 响应数据
     */
    private Object data;

    /**
     * 消息 ID（发送消息成功时返回）
     */
    private String msgid;

    /**
     * 创建成功响应
     */
    public static WeWorkResponseDTO success() {
        WeWorkResponseDTO response = new WeWorkResponseDTO();
        response.setErrcode(0);
        response.setErrmsg("ok");
        return response;
    }

    /**
     * 创建成功响应（带数据）
     */
    public static WeWorkResponseDTO success(Object data) {
        WeWorkResponseDTO response = new WeWorkResponseDTO();
        response.setErrcode(0);
        response.setErrmsg("ok");
        response.setData(data);
        return response;
    }

    /**
     * 创建错误响应
     */
    public static WeWorkResponseDTO error(String message) {
        WeWorkResponseDTO response = new WeWorkResponseDTO();
        response.setErrcode(-1);
        response.setErrmsg(message);
        return response;
    }

    /**
     * 创建错误响应（带错误码）
     */
    public static WeWorkResponseDTO error(Integer code, String message) {
        WeWorkResponseDTO response = new WeWorkResponseDTO();
        response.setErrcode(code);
        response.setErrmsg(message);
        return response;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return errcode != null && errcode == 0;
    }
}

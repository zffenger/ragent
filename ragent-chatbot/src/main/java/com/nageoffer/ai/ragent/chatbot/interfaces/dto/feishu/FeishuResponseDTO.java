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

package com.nageoffer.ai.ragent.chatbot.interfaces.dto.feishu;

import lombok.Data;

/**
 * 飞书响应 DTO
 * <p>
 * 飞书 API 的响应结构
 */
@Data
public class FeishuResponseDTO {

    /**
     * 响应码
     * 0 表示成功
     */
    private int code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private Object data;

    /**
     * 创建成功响应
     */
    public static FeishuResponseDTO success() {
        FeishuResponseDTO response = new FeishuResponseDTO();
        response.setCode(0);
        response.setMsg("success");
        return response;
    }

    /**
     * 创建成功响应（带数据）
     */
    public static FeishuResponseDTO success(Object data) {
        FeishuResponseDTO response = new FeishuResponseDTO();
        response.setCode(0);
        response.setMsg("success");
        response.setData(data);
        return response;
    }

    /**
     * 创建错误响应
     */
    public static FeishuResponseDTO error(String message) {
        FeishuResponseDTO response = new FeishuResponseDTO();
        response.setCode(-1);
        response.setMsg(message);
        return response;
    }

    /**
     * 创建错误响应（带错误码）
     */
    public static FeishuResponseDTO error(int code, String message) {
        FeishuResponseDTO response = new FeishuResponseDTO();
        response.setCode(code);
        response.setMsg(message);
        return response;
    }
}

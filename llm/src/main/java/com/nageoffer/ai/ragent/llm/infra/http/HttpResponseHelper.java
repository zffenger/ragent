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

package com.nageoffer.ai.ragent.llm.infra.http;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.nageoffer.ai.ragent.llm.domain.vo.ModelTarget;
import com.nageoffer.ai.ragent.llm.domain.vo.ProviderConfig;
import lombok.NoArgsConstructor;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 响应处理工具类
 * 集中管理 OkHttp 响应读取、JSON 解析以及模型目标校验等公共逻辑
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class HttpResponseHelper {

    /**
     * 读取响应体原始字符串
     */
    public static String readBody(ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return new String(body.bytes(), StandardCharsets.UTF_8);
    }

    /**
     * 将响应体解析为 JSONObject
     *
     * @param body  OkHttp 响应体
     * @param label 提供商标签，用于异常消息
     * @return 解析后的 JSONObject
     */
    public static JSONObject parseJson(ResponseBody body, String label) throws IOException {
        if (body == null) {
            throw new ModelClientException(label + " 响应为空", ModelClientErrorType.INVALID_RESPONSE, null);
        }
        String content = body.string();
        return JSON.parseObject(content);
    }

    /**
     * 校验并返回提供商配置
     */
    public static ProviderConfig requireProvider(ModelTarget target, String label) {
        if (target == null || target.provider() == null) {
            throw new IllegalStateException(label + " 提供商配置缺失");
        }
        return target.provider();
    }

    /**
     * 校验提供商 API 密钥
     */
    public static void requireApiKey(ProviderConfig provider, String label) {
        if (provider.apiKey() == null || provider.apiKey().isBlank()) {
            throw new IllegalStateException(label + " API密钥缺失");
        }
    }

    /**
     * 校验并返回模型名称
     */
    public static String requireModel(ModelTarget target, String label) {
        if (target == null || target.candidate() == null || target.candidate().modelName() == null) {
            throw new IllegalStateException(label + " 模型名称缺失");
        }
        return target.candidate().modelName();
    }
}

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

package com.nageoffer.ai.ragent.llm.infra.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型配置属性类
 * <p>
 * 模型配置（providers、chat、embedding、rerank）从数据库加载，
 * 由 DynamicConfigRefresher 在启动时填充。
 * <p>
 * 策略配置由独立的配置类管理：
 * - AISelectionProperties: 模型选择策略（ai.selection）
 * - AIStreamProperties: 流式响应配置（ai.stream）
 */
@Data
@Component
public class AIModelProperties {

    /**
     * AI 提供商配置映射
     * key: 提供商名称，value: 提供商配置信息
     * 从数据库加载
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /**
     * 聊天模型组配置
     * 从数据库加载
     */
    private ModelGroup chat = new ModelGroup();

    /**
     * 向量嵌入模型组配置
     * 从数据库加载
     */
    private ModelGroup embedding = new ModelGroup();

    /**
     * 重排序模型组配置
     * 从数据库加载
     */
    private ModelGroup rerank = new ModelGroup();

    /**
     * 模型组配置类
     * 包含默认模型、深度思考模型以及候选模型列表
     */
    @Data
    public static class ModelGroup {
        /**
         * 默认使用的模型标识
         */
        private String defaultModel;

        /**
         * 深度思考模型标识
         */
        private String deepThinkingModel;

        /**
         * 候选模型列表
         */
        private List<ModelCandidate> candidates = new ArrayList<>();
    }

    /**
     * 模型候选配置类
     * 定义单个候选模型的详细配置信息
     */
    @Data
    public static class ModelCandidate {

        /**
         * 模型唯一标识符
         */
        private String id;

        /**
         * 模型提供商名称
         */
        private String provider;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 模型访问 URL
         */
        private String url;

        /**
         * 向量维度（用于 embedding 模型）
         */
        private Integer dimension;

        /**
         * 模型优先级，数值越小优先级越高
         */
        private Integer priority = 100;

        /**
         * 是否启用该模型
         */
        private Boolean enabled = true;

        /**
         * 是否支持思考链功能
         */
        private Boolean supportsThinking = false;
    }

    /**
     * 提供商配置类
     * 包含提供商的基本连接信息和端点配置
     */
    @Data
    public static class ProviderConfig {

        /**
         * 提供商基础 URL
         */
        private String url;

        /**
         * API 密钥
         */
        private String apiKey;

        /**
         * 端点映射配置
         * key: 端点类型，value: 端点路径
         */
        private Map<String, String> endpoints = new HashMap<>();
    }
}

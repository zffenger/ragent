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

package com.nageoffer.ai.ragent.chatbot.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问题检测配置 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionConfigVO {

    /**
     * 检测模式：KEYWORD/LLM/COMPOSITE
     */
    private String mode;

    /**
     * 触发关键词列表
     */
    private List<String> keywords;

    /**
     * 是否启用 @机器人 触发
     */
    private Boolean atTriggerEnabled;

    /**
     * LLM 检测的置信度阈值
     */
    private Double llmThreshold;
}

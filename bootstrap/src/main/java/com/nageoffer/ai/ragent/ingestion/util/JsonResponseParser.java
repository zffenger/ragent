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

package com.nageoffer.ai.ragent.ingestion.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nageoffer.ai.ragent.llm.infra.util.LLMResponseCleaner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON 响应解析器，用于解析 LLM 返回的 JSON 字符串
 */
public final class JsonResponseParser {

    private JsonResponseParser() {
    }

    public static List<String> parseStringList(String raw) {
        String cleaned = cleanAndExtractJson(raw);
        if (cleaned == null) {
            return List.of();
        }
		JSONArray array = JSON.parseArray(cleaned);
		if (array == null) {
			return List.of();
		}
		return array.toJavaList(String.class);
    }

    public static Map<String, Object> parseObject(String raw) {
        String cleaned = cleanAndExtractJson(raw);
        if (cleaned == null) {
            return Collections.emptyMap();
        }
		JSONObject obj = JSON.parseObject(cleaned);
		if (obj == null) {
			return Collections.emptyMap();
		}
		return obj;
    }

    private static String cleanAndExtractJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String cleaned = LLMResponseCleaner.stripMarkdownCodeFence(raw);
        return extractJsonBody(cleaned);
    }

    private static String extractJsonBody(String raw) {
        int objStart = raw.indexOf('{');
        int arrStart = raw.indexOf('[');
        int start;
        if (objStart < 0) {
            start = arrStart;
        } else if (arrStart < 0) {
            start = objStart;
        } else {
            start = Math.min(objStart, arrStart);
        }
        if (start < 0) {
            return raw;
        }
        int objEnd = raw.lastIndexOf('}');
        int arrEnd = raw.lastIndexOf(']');
        int end = Math.max(objEnd, arrEnd);
        if (end < 0 || end <= start) {
            return raw.substring(start);
        }
        return raw.substring(start, end + 1);
    }
}

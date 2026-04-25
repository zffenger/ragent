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

package com.nageoffer.ai.ragent.ingestion.strategy.fetcher;

import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.ingestion.domain.context.DocumentSource;
import com.nageoffer.ai.ragent.ingestion.domain.enums.SourceType;
import com.nageoffer.ai.ragent.ingestion.util.MimeTypeDetector;
import com.nageoffer.ai.ragent.rag.application.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * OSS 对象存储文档提取器
 * 支持从阿里云 OSS 对象存储中获取文档，示例路径：oss://biz/5fb28010e16c4083ab07ca41f29804b0.md
 */
@Component
@RequiredArgsConstructor
public class OssFetcher implements DocumentFetcher {

    private final FileStorageService fileStorageService;

    @Override
    public SourceType supportedType() {
        return SourceType.OSS;
    }

    @Override
    public FetchResult fetch(DocumentSource source) {
        String location = source.getLocation();
        if (!StringUtils.hasText(location)) {
            throw new ServiceException("OSS 路径不能为空");
        }

        if (!location.startsWith("oss://")) {
            throw new ServiceException("无效的 OSS 路径格式，应以 oss:// 开头: " + location);
        }

        try {
            byte[] bytes;
            try (InputStream is = fileStorageService.openStream(location)) {
                bytes = is.readAllBytes();
            }

            String fileName = source.getFileName();
            if (!StringUtils.hasText(fileName)) {
                fileName = extractFileName(location);
            }

            String mimeType = MimeTypeDetector.detect(bytes, fileName);
            return new FetchResult(bytes, mimeType, fileName);
        } catch (Exception e) {
            throw new ServiceException("从 OSS 读取文件失败: " + location + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 从 OSS 路径中提取文件名
     * 例如：oss://biz/5fb28010e16c4083ab07ca41f29804b0.md -> 5fb28010e16c4083ab07ca41f29804b0.md
     */
    private String extractFileName(String location) {
        int idx = location.lastIndexOf('/');
        return idx >= 0 ? location.substring(idx + 1) : location;
    }
}

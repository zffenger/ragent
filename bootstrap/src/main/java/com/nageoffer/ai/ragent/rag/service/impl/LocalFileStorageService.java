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

package com.nageoffer.ai.ragent.rag.service.impl;

import cn.hutool.core.lang.Assert;
import com.nageoffer.ai.ragent.rag.dto.StoredFileDTO;
import com.nageoffer.ai.ragent.rag.service.FileStorageService;
import com.nageoffer.ai.ragent.rag.util.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 * 将文件存储在本地文件系统中，适用于开发测试或小规模部署场景
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    private static final Tika TIKA = new Tika();

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, MultipartFile file) {
        validateBucketName(bucketName);
        Assert.isFalse(file == null || file.isEmpty(), "上传文件不能为空");

        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();

        String detectedContentType;
        try (InputStream is = file.getInputStream()) {
            detectedContentType = TIKA.detect(is, originalFilename);
        }

        String objectKey = generateObjectKey(originalFilename);
        Path targetPath = resolvePath(bucketName, objectKey);

        Files.createDirectories(targetPath.getParent());
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = toLocalUrl(bucketName, objectKey);
        log.info("本地存储上传文件成功, path={}, size={}", targetPath, size);

        return buildStoredFileDTO(url, originalFilename, detectedContentType, size);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, InputStream content, long size, String originalFilename, String contentType) {
        validateBucketName(bucketName);
        Assert.notNull(content, "上传内容不能为空");
        Assert.isTrue(size >= 0, "上传内容大小不能小于 0");

        String detected = resolveContentType(originalFilename, contentType);
        String objectKey = generateObjectKey(originalFilename);

        Path targetPath = resolvePath(bucketName, objectKey);
        Files.createDirectories(targetPath.getParent());
        Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);

        String url = toLocalUrl(bucketName, objectKey);
        log.info("本地存储上传文件成功, path={}, size={}", targetPath, size);

        return buildStoredFileDTO(url, originalFilename, detected, size);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType) {
        validateBucketName(bucketName);
        Assert.notNull(content, "上传内容不能为空");

        String detected = resolveContentType(originalFilename, contentType);
        String objectKey = generateObjectKey(originalFilename);

        Path targetPath = resolvePath(bucketName, objectKey);
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, content);

        String url = toLocalUrl(bucketName, objectKey);
        log.info("本地存储上传文件成功, path={}, size={}", targetPath, content.length);

        return buildStoredFileDTO(url, originalFilename, detected, content.length);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO reliableUpload(String bucketName, InputStream content, long size, String originalFilename, String contentType) {
        // 本地存储无需特殊重试机制，直接调用 upload 方法
        return upload(bucketName, content, size, originalFilename, contentType);
    }

    @Override
    @SneakyThrows
    public InputStream openStream(String url) {
        LocalLocation loc = parseLocalUrl(url);
        Path path = resolvePath(loc.bucket(), loc.key());
        return new FileInputStream(path.toFile());
    }

    @Override
    @SneakyThrows
    public void deleteByUrl(String url) {
        LocalLocation loc = parseLocalUrl(url);
        Path path = resolvePath(loc.bucket(), loc.key());
        Files.deleteIfExists(path);
        log.info("本地存储删除文件成功, path={}", path);
    }

    private Path resolvePath(String bucketName, String objectKey) {
        return Paths.get(basePath, bucketName, objectKey).toAbsolutePath();
    }

    private String toLocalUrl(String bucket, String key) {
        return "local://" + bucket + "/" + key;
    }

    private LocalLocation parseLocalUrl(String url) {
        URI uri = URI.create(url);
        if (!"local".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("不支持的 URL 协议: " + url);
        }
        String bucket = uri.getHost();
        String path = uri.getPath();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("无效的本地存储 URL (缺少 bucket): " + url);
        }
        String key = (path != null && path.startsWith("/")) ? path.substring(1) : path;
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("无效的本地存储 URL (缺少 key): " + url);
        }
        return new LocalLocation(bucket, key);
    }

    private record LocalLocation(String bucket, String key) {
    }

    private String extractSuffix(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return (idx < 0 || idx == filename.length() - 1) ? "" : filename.substring(idx + 1).trim();
    }

    private String generateObjectKey(String originalFilename) {
        String suffix = extractSuffix(originalFilename);
        UUID uuid = UUID.randomUUID();
        String key = String.format("%016x%016x", uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        return suffix.isBlank() ? key : key + "." + suffix;
    }

    private void validateBucketName(String bucketName) {
        Assert.notBlank(bucketName, "bucketName 不能为空");
    }

    private StoredFileDTO buildStoredFileDTO(String url, String originalFilename, String contentType, long size) {
        String detectedType = FileTypeDetector.detectType(originalFilename, contentType);
        return StoredFileDTO.builder()
                .url(url)
                .detectedType(detectedType)
                .size(size)
                .originalFilename(originalFilename)
                .build();
    }

    private String resolveContentType(String originalFilename, String contentType) {
        if (contentType != null && !contentType.isBlank()) return contentType;
        if (originalFilename != null && !originalFilename.isBlank()) return TIKA.detect(originalFilename);
        return "application/octet-stream";
    }
}

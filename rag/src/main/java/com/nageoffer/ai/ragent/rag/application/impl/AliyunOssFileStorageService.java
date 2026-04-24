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

package com.nageoffer.ai.ragent.rag.application.impl;

import cn.hutool.core.lang.Assert;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.nageoffer.ai.ragent.rag.infra.config.AliyunOssConfig;
import com.nageoffer.ai.ragent.rag.domain.vo.StoredFileDTO;
import com.nageoffer.ai.ragent.rag.application.FileStorageService;
import com.nageoffer.ai.ragent.rag.infra.util.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * 阿里云 OSS 文件存储服务实现
 * 支持文件的流式上传、下载和删除操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "oss", matchIfMissing = false)
public class AliyunOssFileStorageService implements FileStorageService {

    private final OSS ossClient;
    private final AliyunOssConfig ossConfig;

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

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(detectedContentType);
        metadata.setContentLength(size);

        try (InputStream is = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, is, metadata);
            ossClient.putObject(request);
        }

        String url = toOssUrl(bucketName, objectKey);
        log.info("OSS 上传文件成功, bucket={}, key={}, size={}", bucketName, objectKey, size);

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

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(detected);
        metadata.setContentLength(size);

        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, content, metadata);
        ossClient.putObject(request);

        String url = toOssUrl(bucketName, objectKey);
        log.info("OSS 上传文件成功, bucket={}, key={}, size={}", bucketName, objectKey, size);

        return buildStoredFileDTO(url, originalFilename, detected, size);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType) {
        validateBucketName(bucketName);
        Assert.notNull(content, "上传内容不能为空");

        String detected = resolveContentType(originalFilename, contentType);
        String objectKey = generateObjectKey(originalFilename);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(detected);
        metadata.setContentLength(content.length);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(content)) {
            PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, bis, metadata);
            ossClient.putObject(request);
        }

        String url = toOssUrl(bucketName, objectKey);
        log.info("OSS 上传文件成功, bucket={}, key={}, size={}", bucketName, objectKey, content.length);

        return buildStoredFileDTO(url, originalFilename, detected, content.length);
    }

    @Override
    @SneakyThrows
    public StoredFileDTO reliableUpload(String bucketName, InputStream content, long size, String originalFilename, String contentType) {
        // 阿里云 OSS SDK 本身支持重试，直接调用 upload 方法
        return upload(bucketName, content, size, originalFilename, contentType);
    }

    @Override
    public InputStream openStream(String url) {
        OssLocation loc = parseOssUrl(url);
        OSSObject object = ossClient.getObject(loc.bucket(), loc.key());
        return object.getObjectContent();
    }

    @Override
    public void deleteByUrl(String url) {
        OssLocation loc = parseOssUrl(url);
        ossClient.deleteObject(loc.bucket(), loc.key());
        log.info("OSS 删除文件成功, bucket={}, key={}", loc.bucket(), loc.key());
    }

    private String toOssUrl(String bucket, String key) {
        return "oss://" + bucket + "/" + key;
    }

    private OssLocation parseOssUrl(String url) {
        URI uri = URI.create(url);
        if (!"oss".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("不支持的 URL 协议: " + url);
        }
        String bucket = uri.getHost();
        String path = uri.getPath();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("无效的 OSS URL (缺少 bucket): " + url);
        }
        String key = (path != null && path.startsWith("/")) ? path.substring(1) : path;
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("无效的 OSS URL (缺少 key): " + url);
        }
        return new OssLocation(bucket, key);
    }

    private record OssLocation(String bucket, String key) {
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

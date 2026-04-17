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

package com.nageoffer.ai.ragent.rag.service;

import com.nageoffer.ai.ragent.rag.dto.StoredFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    /**
     * 上传文件（流式，低内存）
     * <p>
     * 通过流式方式上传文件，堆内存开销近似为零
     * 适用于大文件上传、高并发场景
     */
    StoredFileDTO upload(String bucketName, MultipartFile file);

    /**
     * 上传文件（流式，低内存）
     * <p>
     * 通过流式方式上传文件，堆内存开销近似为零
     * 适用于大文件上传、高并发场景
     */
    StoredFileDTO upload(String bucketName, InputStream content, long size, String originalFilename, String contentType);

    /**
     * 上传文件（流式，低内存）
     * <p>
     * 通过流式方式上传文件，堆内存开销近似为零
     * 适用于大文件上传、高并发场景
     */
    StoredFileDTO upload(String bucketName, byte[] content, String originalFilename, String contentType);

    /**
     * 上传文件（可靠上传，带自动重试）
     * <p>
     * 通过存储 SDK 的可靠上传机制上传文件，具备内置的自动重试机制
     * 适用于对重试可靠性要求高的场景
     */
    StoredFileDTO reliableUpload(String bucketName, InputStream content, long size, String originalFilename, String contentType);

    InputStream openStream(String url);

    void deleteByUrl(String url);
}

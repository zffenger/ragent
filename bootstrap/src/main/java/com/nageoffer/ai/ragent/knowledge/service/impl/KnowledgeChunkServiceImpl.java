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

package com.nageoffer.ai.ragent.knowledge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.ai.ragent.llm.domain.service.EmbeddingService;
import com.nageoffer.ai.ragent.llm.domain.service.token.TokenCounterService;
import com.nageoffer.ai.ragent.knowledge.controller.request.KnowledgeChunkBatchRequest;
import com.nageoffer.ai.ragent.knowledge.controller.request.KnowledgeChunkCreateRequest;
import com.nageoffer.ai.ragent.knowledge.controller.request.KnowledgeChunkPageRequest;
import com.nageoffer.ai.ragent.knowledge.controller.request.KnowledgeChunkUpdateRequest;
import com.nageoffer.ai.ragent.knowledge.controller.vo.KnowledgeChunkVO;
import com.nageoffer.ai.ragent.core.chunk.VectorChunk;
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeBase;
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeChunk;
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocument;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeBaseRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeChunkRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentRepository;
import com.nageoffer.ai.ragent.framework.context.UserContext;
import com.nageoffer.ai.ragent.framework.exception.ClientException;
import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.knowledge.enums.DocumentStatus;
import com.nageoffer.ai.ragent.rag.domain.service.vector.VectorStoreService;
import com.nageoffer.ai.ragent.knowledge.service.KnowledgeChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StringUtils;

import cn.hutool.crypto.SecureUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库 Chunk 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingService embeddingService;
    private final TokenCounterService tokenCounterService;
    private final VectorStoreService vectorStoreService;
    private final TransactionOperations transactionOperations;

    @Override
    public IPage<KnowledgeChunkVO> pageQuery(String docId, KnowledgeChunkPageRequest requestParam) {
        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));

        Page<KnowledgeChunk> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<KnowledgeChunk> result = chunkRepository.findPage(page, docId);
        return result.convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeChunkVO create(String docId, KnowledgeChunkCreateRequest requestParam) {
        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));
        if (DocumentStatus.RUNNING.getCode().equals(document.getStatus())) {
            throw new ClientException("文档正在分块处理中，暂不支持新增 Chunk");
        }
        if (!Integer.valueOf(1).equals(document.getEnabled())) {
            throw new ClientException("文档未启用，暂不支持新增 Chunk");
        }

        String content = requestParam.getContent();
        Assert.notBlank(content, () -> new ClientException("Chunk 内容不能为空"));

        KnowledgeChunk latest = chunkRepository.findLatestByDocId(docId);
        int chunkIndex = requestParam.getIndex() != null
                ? requestParam.getIndex()
                : (latest != null ? latest.getChunkIndex() + 1 : 0);

        String contentHash = SecureUtil.sha256(content);
        int charCount = content.length();
        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId());
        String embeddingModel = kb.getEmbeddingModel();
        String collectionName = kb.getCollectionName();
        Integer tokenCount = resolveTokenCount(content);

        KnowledgeChunk chunk = KnowledgeChunk.builder()
                .id(requestParam.getChunkId())
                .kbId(document.getKbId())
                .docId(docId)
                .chunkIndex(chunkIndex)
                .content(content)
                .contentHash(contentHash)
                .charCount(charCount)
                .tokenCount(tokenCount)
                .enabled(1)
                .createdBy(UserContext.getUsername())
                .updatedBy(UserContext.getUsername())
                .build();

        chunkRepository.save(chunk);
        log.info("新增 Chunk 成功, kbId={}, docId={}, chunkId={}, chunkIndex={}", document.getKbId(), docId, chunk.getId(), chunkIndex);

        documentRepository.incrementChunkCount(docId, 1);

        // 同步写入向量库
        syncChunkToVector(collectionName, docId, chunk, embeddingModel);

        return toVO(chunk);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(String docId, List<KnowledgeChunkCreateRequest> requestParams) {
        batchCreate(docId, requestParams, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(String docId, List<KnowledgeChunkCreateRequest> requestParams, boolean writeVector) {
        if (CollUtil.isEmpty(requestParams)) {
            return;
        }

        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));

        boolean needAutoIndex = requestParams.stream().anyMatch(request -> request.getIndex() == null);
        int nextIndex = 0;
        if (needAutoIndex) {
            KnowledgeChunk latest = chunkRepository.findLatestByDocId(docId);
            nextIndex = latest != null && latest.getChunkIndex() != null ? latest.getChunkIndex() + 1 : 0;
        }

        String kbId = document.getKbId();
        String username = UserContext.getUsername();
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        String embeddingModel = kb.getEmbeddingModel();
        String collectionName = kb.getCollectionName();
        List<KnowledgeChunk> chunkList = new ArrayList<>(requestParams.size());

        for (KnowledgeChunkCreateRequest request : requestParams) {
            String content = request.getContent();
            Assert.notBlank(content, () -> new ClientException("Chunk 内容不能为空"));

            Integer chunkIndex = request.getIndex();
            if (chunkIndex == null) {
                chunkIndex = nextIndex++;
            }

            String chunkId = request.getChunkId();
            if (!StringUtils.hasText(chunkId)) {
                chunkId = IdUtil.getSnowflakeNextIdStr();
            }

            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .id(chunkId)
                    .kbId(kbId)
                    .docId(docId)
                    .chunkIndex(chunkIndex)
                    .content(content)
                    .contentHash(SecureUtil.sha256(content))
                    .charCount(content.length())
                    .tokenCount(resolveTokenCount(content))
                    .enabled(1)
                    .createdBy(username)
                    .updatedBy(username)
                    .build();
            chunkList.add(chunk);
        }

        // 批量写入数据库，向量索引由上层统一处理以避免重复计算
        chunkRepository.saveAll(chunkList);

        documentRepository.incrementChunkCount(docId, chunkList.size());

        if (writeVector) {
            List<VectorChunk> vectorChunks = chunkList.stream()
                    .map(each -> VectorChunk.builder()
                            .chunkId(String.valueOf(each.getId()))
                            .content(each.getContent())
                            .index(each.getChunkIndex())
                            .build())
                    .toList();
            if (CollUtil.isNotEmpty(vectorChunks)) {
                attachEmbeddings(vectorChunks, embeddingModel);
                vectorStoreService.indexDocumentChunks(collectionName, docId, vectorChunks);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String docId, String chunkId, KnowledgeChunkUpdateRequest requestParam) {
        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));
        if (DocumentStatus.RUNNING.getCode().equals(document.getStatus())) {
            throw new ClientException("文档正在分块处理中，暂不支持修改 Chunk");
        }

        KnowledgeChunk chunk = chunkRepository.findById(chunkId);
        Assert.notNull(chunk, () -> new ClientException("Chunk 不存在"));
        Assert.isTrue(chunk.getDocId().equals(docId), () -> new ClientException("Chunk 不属于该文档"));

        String newContent = requestParam.getContent();
        Assert.notBlank(newContent, () -> new ClientException("Chunk 内容不能为空"));

        if (newContent.equals(chunk.getContent())) {
            return;
        }

        chunk.setContent(newContent);
        chunk.setContentHash(SecureUtil.sha256(newContent));
        chunk.setCharCount(newContent.length());
        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId());
        String embeddingModel = kb.getEmbeddingModel();
        String collectionName = kb.getCollectionName();
        chunk.setTokenCount(resolveTokenCount(newContent));
        chunk.setUpdatedBy(UserContext.getUsername());

        chunkRepository.update(chunk);

        log.info("更新 Chunk 成功, kbId={}, docId={}, chunkId={}", document.getKbId(), docId, chunkId);

        // 同步向量数据库
        vectorStoreService.updateChunk(
                collectionName,
                docId,
                VectorChunk.builder()
                        .chunkId(chunkId)
                        .content(newContent)
                        .index(chunk.getChunkIndex())
                        .embedding(toArray(embedContent(newContent, embeddingModel)))
                        .build()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String docId, String chunkId) {
        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));
        if (DocumentStatus.RUNNING.getCode().equals(document.getStatus())) {
            throw new ClientException("文档正在分块处理中，暂不支持删除 Chunk");
        }

        KnowledgeChunk chunk = chunkRepository.findById(chunkId);
        Assert.notNull(chunk, () -> new ClientException("Chunk 不存在"));
        Assert.isTrue(chunk.getDocId().equals(docId), () -> new ClientException("Chunk 不属于该文档"));

        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId());
        Assert.notNull(kb, () -> new ServiceException("知识库不存在"));
        String collectionName = kb.getCollectionName();

        chunkRepository.deleteById(chunkId);

        documentRepository.incrementChunkCount(docId, -1);

        log.info("删除 Chunk 成功, kbId={}, docId={}, chunkId={}", document.getKbId(), docId, chunkId);

        deleteChunkFromVector(collectionName, chunkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableChunk(String docId, String chunkId, boolean enabled) {
        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));
        if (DocumentStatus.RUNNING.getCode().equals(document.getStatus())) {
            throw new ClientException("文档正在分块处理中，暂不支持修改 Chunk 状态");
        }
        validateDocumentEnabledForChunkEnable(document, enabled);

        KnowledgeChunk chunk = chunkRepository.findById(chunkId);
        Assert.notNull(chunk, () -> new ClientException("Chunk 不存在"));
        Assert.isTrue(chunk.getDocId().equals(docId), () -> new ClientException("Chunk 不属于该文档"));

        // 如果状态没变，直接返回
        int enabledValue = enabled ? 1 : 0;
        if (chunk.getEnabled().equals(enabledValue)) {
            return;
        }

        chunk.setEnabled(enabledValue);
        chunk.setUpdatedBy(UserContext.getUsername());
        chunkRepository.update(chunk);

        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId());
        String collectionName = kb.getCollectionName();
        log.info("{}Chunk 成功, kbId={}, docId={}, chunkId={}", enabled ? "启用" : "禁用", document.getKbId(), docId, chunkId);

        if (enabled) {
            String embeddingModel = kb.getEmbeddingModel();
            syncChunkToVector(collectionName, docId, chunk, embeddingModel);
        } else {
            deleteChunkFromVector(collectionName, chunkId);
        }
    }

    @Override
    public void batchToggleEnabled(String docId, KnowledgeChunkBatchRequest requestParam, boolean enabled) {
        if (requestParam == null || CollUtil.isEmpty(requestParam.getChunkIds())) {
            throw new ClientException("请指定需要操作的 Chunk，全量启用/禁用请使用文档启用接口");
        }
        List<String> requestedIds = requestParam.getChunkIds();
        if (requestedIds.size() > 500) {
            throw new ClientException("单次批量操作 Chunk 数量不能超过 500");
        }

        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));
        if (DocumentStatus.RUNNING.getCode().equals(document.getStatus())) {
            throw new ClientException("文档正在分块处理中，暂不支持批量修改 Chunk 状态");
        }
        validateDocumentEnabledForChunkEnable(document, enabled);

        List<KnowledgeChunk> found = chunkRepository.findByIds(requestedIds);
        if (found.size() != requestedIds.size()) {
            throw new ClientException("存在无效的 Chunk ID，请求 " + requestedIds.size() + " 个，实际找到 " + found.size() + " 个");
        }
        found.forEach(c -> {
            if (!c.getDocId().equals(docId)) {
                throw new ClientException("Chunk " + c.getId() + " 不属于文档 " + docId);
            }
        });
        List<String> targetIds = found.stream().map(KnowledgeChunk::getId).collect(Collectors.toList());

        if (CollUtil.isEmpty(targetIds)) {
            return;
        }

        int enabledValue = enabled ? 1 : 0;
        List<KnowledgeChunk> needUpdateChunks = chunkRepository.findByIdsAndNotEnabled(targetIds, enabledValue);
        List<String> needUpdateIds = needUpdateChunks.stream().map(KnowledgeChunk::getId).collect(Collectors.toList());

        if (CollUtil.isEmpty(needUpdateIds)) {
            throw new ClientException(enabled ? "所有 Chunk 已全部启用，无需重复操作" : "所有 Chunk 已全部禁用，无需重复操作");
        }

        KnowledgeBase kb = knowledgeBaseRepository.findById(document.getKbId());
        String collectionName = kb.getCollectionName();

        if (enabled) {
            List<VectorChunk> vectorChunks = needUpdateChunks.stream()
                    .map(c -> VectorChunk.builder()
                            .chunkId(c.getId())
                            .content(c.getContent())
                            .index(c.getChunkIndex())
                            .build())
                    .collect(Collectors.toList());
            attachEmbeddings(vectorChunks, kb.getEmbeddingModel());

            transactionOperations.executeWithoutResult(status -> {
                chunkRepository.updateEnabledByIds(needUpdateIds, 1, UserContext.getUsername());
                vectorStoreService.indexDocumentChunks(collectionName, docId, vectorChunks);
            });
        } else {
            transactionOperations.executeWithoutResult(status -> {
                chunkRepository.updateEnabledByIds(needUpdateIds, 0, UserContext.getUsername());
                vectorStoreService.deleteChunksByIds(collectionName, needUpdateIds);
            });
        }

        log.info("批量{}Chunk 成功, kbId={}, docId={}, count={}", enabled ? "启用" : "禁用",
                document.getKbId(), docId, needUpdateIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabledByDocId(String docId, String kbId, boolean enabled) {
        int enabledValue = enabled ? 1 : 0;
        chunkRepository.updateEnabledByDocId(docId, enabledValue);
        log.info("根据文档ID更新所有Chunk启用状态, kbId={}, docId={}, enabled={}", kbId, docId, enabled);
    }

    @Override
    public List<KnowledgeChunkVO> listByDocId(String docId) {
        KnowledgeDocument document = documentRepository.findById(docId);
        Assert.notNull(document, () -> new ClientException("文档不存在"));

        List<KnowledgeChunk> chunkList = chunkRepository.findByDocId(docId);

        return chunkList.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDocId(String docId) {
        if (docId == null) {
            return;
        }
        chunkRepository.deleteByDocId(docId);
    }

    // ==================== 私有方法 ====================

    private KnowledgeChunkVO toVO(KnowledgeChunk chunk) {
        if (chunk == null) {
            return null;
        }
        KnowledgeChunkVO vo = new KnowledgeChunkVO();
        vo.setId(chunk.getId());
        vo.setKbId(chunk.getKbId());
        vo.setDocId(chunk.getDocId());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setContentHash(chunk.getContentHash());
        vo.setCharCount(chunk.getCharCount());
        vo.setTokenCount(chunk.getTokenCount());
        vo.setEnabled(chunk.getEnabled());
        vo.setCreateTime(chunk.getCreateTime() != null ? chunk.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        vo.setUpdateTime(chunk.getUpdateTime() != null ? chunk.getUpdateTime().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        return vo;
    }

    /**
     * 启用 chunk 前必须保证所属文档为启用状态
     */
    private void validateDocumentEnabledForChunkEnable(KnowledgeDocument document, boolean enableChunk) {
        if (!enableChunk) {
            return;
        }
        if (!Integer.valueOf(1).equals(document.getEnabled())) {
            throw new ClientException("文档未启用，无法启用Chunk，请先启用文档");
        }
    }

    /**
     * 将单个 chunk 同步到向量库
     */
    private void syncChunkToVector(String collectionName, String docId, KnowledgeChunk chunk, String embeddingModel) {
        List<Float> embedding = embedContent(chunk.getContent(), embeddingModel);
        float[] vector = toArray(embedding);

        VectorChunk vectorChunk = VectorChunk.builder()
                .index(chunk.getChunkIndex())
                .content(chunk.getContent())
                .chunkId(String.valueOf(chunk.getId()))
                .embedding(vector)
                .build();
        vectorStoreService.indexDocumentChunks(collectionName, docId, List.of(vectorChunk));

        log.debug("同步 Chunk 到向量库成功, collectionName={}, docId={}, chunkId={}", collectionName, docId, chunk.getId());
    }

    /**
     * 从向量库删除单个 chunk
     */
    private void deleteChunkFromVector(String collectionName, String chunkId) {
        vectorStoreService.deleteChunkById(collectionName, chunkId);
        log.debug("从向量库删除 Chunk, collectionName={}, chunkId={}", collectionName, chunkId);
    }

    /**
     * List<Float> 转 float[]
     */
    private static float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private void attachEmbeddings(List<VectorChunk> chunks, String embeddingModel) {
        if (CollUtil.isEmpty(chunks)) {
            return;
        }
        List<String> texts = chunks.stream().map(VectorChunk::getContent).toList();
        List<List<Float>> vectors = embedBatch(texts, embeddingModel);
        if (vectors == null || vectors.size() != chunks.size()) {
            throw new ServiceException("向量结果数量不匹配");
        }
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(toArray(vectors.get(i)));
        }
    }

    private List<Float> embedContent(String content, String embeddingModel) {
        return StrUtil.isBlank(embeddingModel)
                ? embeddingService.embed(content)
                : embeddingService.embed(content, embeddingModel);
    }

    private List<List<Float>> embedBatch(List<String> texts, String embeddingModel) {
        return StrUtil.isBlank(embeddingModel)
                ? embeddingService.embedBatch(texts)
                : embeddingService.embedBatch(texts, embeddingModel);
    }

    private Integer resolveTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return tokenCounterService.countTokens(content);
    }
}

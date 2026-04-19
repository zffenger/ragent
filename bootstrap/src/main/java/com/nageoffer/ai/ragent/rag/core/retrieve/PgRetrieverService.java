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

package com.nageoffer.ai.ragent.rag.core.retrieve;

import cn.hutool.core.collection.CollUtil;
import com.nageoffer.ai.ragent.framework.convention.RetrievedChunk;
import com.nageoffer.ai.ragent.infra.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PgRetrieverService implements RetrieverService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    @Override
    public List<RetrievedChunk> retrieve(RetrieveRequest request) {
        List<Float> embedding = embeddingService.embed(request.getQuery());
        float[] vector = normalize(toArray(embedding));
        return retrieveByVector(vector, request);
    }

    @Override
    public List<RetrievedChunk> retrieveByVector(float[] vector, RetrieveRequest request) {
        // 设置ef_search提升召回率
        // noinspection SqlDialectInspection,SqlNoDataSourceInspection
        jdbcTemplate.execute("SET hnsw.ef_search = 200");

        String vectorLiteral = toVectorLiteral(vector);

        // 如果指定了知识库 ID 列表，需要通过子查询过滤
        if (CollUtil.isNotEmpty(request.getKnowledgeBaseIds())) {
            return retrieveWithKbFilter(vectorLiteral, request);
        }

        // noinspection SqlDialectInspection,SqlNoDataSourceInspection
        return jdbcTemplate.query("SELECT id, content, 1 - (embedding <=> ?::vector) AS score FROM t_knowledge_vector WHERE metadata->>'collection_name' = ? ORDER BY embedding <=> ?::vector LIMIT ?",
                (rs, rowNum) -> RetrievedChunk.builder()
                        .id(rs.getString("id"))
                        .text(rs.getString("content"))
                        .score(rs.getFloat("score"))
                        .build(),
                vectorLiteral, request.getCollectionName(), vectorLiteral, request.getTopK()
        );
    }

    /**
     * 带知识库 ID 过滤的检索
     * 通过关联知识库表来过滤
     */
    // noinspection SqlDialectInspection,SqlNoDataSourceInspection
    private List<RetrievedChunk> retrieveWithKbFilter(String vectorLiteral, RetrieveRequest request) {
        // 构建知识库 ID 占位符
        String kbIdPlaceholders = request.getKnowledgeBaseIds().stream()
                .map(id -> "?")
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String sql = "SELECT v.id, v.content, 1 - (v.embedding <=> ?::vector) AS score " +
                "FROM t_knowledge_vector v " +
                "INNER JOIN t_knowledge_base kb ON kb.collection_name = v.metadata->>'collection_name' " +
                "WHERE v.metadata->>'collection_name' = ? AND kb.id IN (" + kbIdPlaceholders + ") " +
                "ORDER BY v.embedding <=> ?::vector LIMIT ?";

        // 构建参数数组
        Object[] params = new Object[3 + request.getKnowledgeBaseIds().size()];
        params[0] = vectorLiteral;
        params[1] = request.getCollectionName();
        for (int i = 0; i < request.getKnowledgeBaseIds().size(); i++) {
            params[2 + i] = request.getKnowledgeBaseIds().get(i);
        }
        params[params.length - 2] = vectorLiteral;
        params[params.length - 1] = request.getTopK();

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> RetrievedChunk.builder()
                        .id(rs.getString("id"))
                        .text(rs.getString("content"))
                        .score(rs.getFloat("score"))
                        .build(),
                params
        );
    }

    private float[] normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    private float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        return sb.append("]").toString();
    }
}

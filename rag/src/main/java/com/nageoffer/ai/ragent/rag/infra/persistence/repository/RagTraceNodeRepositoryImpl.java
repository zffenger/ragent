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

package com.nageoffer.ai.ragent.rag.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.repository.RagTraceNodeRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.RagTraceNodeMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.RagTraceNodeDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * RAG Trace 节点仓储实现
 */
@Repository
@RequiredArgsConstructor
public class RagTraceNodeRepositoryImpl implements RagTraceNodeRepository {

    private final RagTraceNodeMapper nodeMapper;

    @Override
    public void save(RagTraceNodeDO node) {
        nodeMapper.insert(node);
    }

    @Override
    public void updateByTraceIdAndNodeId(String traceId, String nodeId, RagTraceNodeDO node) {
        nodeMapper.update(
                node,
                Wrappers.lambdaUpdate(RagTraceNodeDO.class)
                        .eq(RagTraceNodeDO::getTraceId, traceId)
                        .eq(RagTraceNodeDO::getNodeId, nodeId)
        );
    }
}

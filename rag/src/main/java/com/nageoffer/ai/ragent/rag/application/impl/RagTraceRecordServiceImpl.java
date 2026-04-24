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

import com.nageoffer.ai.ragent.rag.infra.persistence.po.RagTraceNodeDO;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.RagTraceRunDO;
import com.nageoffer.ai.ragent.rag.domain.repository.RagTraceNodeRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.RagTraceRunRepository;
import com.nageoffer.ai.ragent.rag.application.RagTraceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * RAG Trace 记录服务实现
 */
@Service
@RequiredArgsConstructor
public class RagTraceRecordServiceImpl implements RagTraceRecordService {

    private final RagTraceRunRepository runRepository;
    private final RagTraceNodeRepository nodeRepository;

    @Override
    public void startRun(RagTraceRunDO run) {
        runRepository.save(run);
    }

    @Override
    public void finishRun(String traceId, String status, String errorMessage, Date endTime, long durationMs) {
        RagTraceRunDO update = RagTraceRunDO.builder()
                .status(status)
                .errorMessage(errorMessage)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
        runRepository.updateByTraceId(traceId, update);
    }

    @Override
    public void startNode(RagTraceNodeDO node) {
        nodeRepository.save(node);
    }

    @Override
    public void finishNode(String traceId, String nodeId, String status, String errorMessage, Date endTime, long durationMs) {
        RagTraceNodeDO update = RagTraceNodeDO.builder()
                .status(status)
                .errorMessage(errorMessage)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
        nodeRepository.updateByTraceIdAndNodeId(traceId, nodeId, update);
    }
}

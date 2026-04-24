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

package com.nageoffer.ai.ragent.rag.domain.repository;

import com.nageoffer.ai.ragent.rag.domain.entity.RagTraceNode;

/**
 * RAG Trace 节点仓储接口
 */
public interface RagTraceNodeRepository {

    /**
     * 保存节点
     *
     * @param node 节点信息
     */
    void save(RagTraceNode node);

    /**
     * 更新节点状态
     *
     * @param traceId     链路ID
     * @param nodeId      节点ID
     * @param node        更新内容
     */
    void updateByTraceIdAndNodeId(String traceId, String nodeId, RagTraceNode node);
}

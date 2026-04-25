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

package com.nageoffer.ai.ragent.settings.controller;

import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import com.nageoffer.ai.ragent.settings.controller.vo.RetrievalDomainVO;
import com.nageoffer.ai.ragent.settings.service.RetrievalDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 检索域管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settings/retrieval-domains")
public class RetrievalDomainController {

    private final RetrievalDomainService retrievalDomainService;

    /**
     * 获取所有检索域
     */
    @GetMapping
    public Result<List<RetrievalDomainVO>> listAll() {
        return Results.success(retrievalDomainService.listAll());
    }

    /**
     * 获取检索域详情
     */
    @GetMapping("/{id}")
    public Result<RetrievalDomainVO> getById(@PathVariable String id) {
        return Results.success(retrievalDomainService.getById(id));
    }

    /**
     * 创建检索域
     */
    @PostMapping
    public Result<RetrievalDomainVO> create(@RequestBody RetrievalDomainVO vo) {
        return Results.success(retrievalDomainService.create(vo));
    }

    /**
     * 更新检索域
     */
    @PutMapping("/{id}")
    public Result<RetrievalDomainVO> update(@PathVariable String id, @RequestBody RetrievalDomainVO vo) {
        return Results.success(retrievalDomainService.update(id, vo));
    }

    /**
     * 删除检索域
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        retrievalDomainService.delete(id);
        return Results.success();
    }

    /**
     * 绑定知识库到检索域
     */
    @PostMapping("/{id}/knowledges")
    public Result<Void> bindKnowledges(
            @PathVariable String id,
            @RequestBody BindKnowledgeRequest request) {
        retrievalDomainService.bindKnowledges(id, request.getKnowledgeIds());
        return Results.success();
    }

    /**
     * 解绑知识库
     */
    @DeleteMapping("/{id}/knowledges/{knowledgeId}")
    public Result<Void> unbindKnowledge(
            @PathVariable String id,
            @PathVariable String knowledgeId) {
        retrievalDomainService.unbindKnowledge(id, knowledgeId);
        return Results.success();
    }

    /**
     * 绑定知识库请求
     */
    @lombok.Data
    public static class BindKnowledgeRequest {
        private List<String> knowledgeIds;
    }
}

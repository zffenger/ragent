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
import com.nageoffer.ai.ragent.settings.controller.vo.ChatBotVO;
import com.nageoffer.ai.ragent.settings.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天机器人管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settings/chat-bots")
public class ChatBotController {

    private final ChatBotService chatBotService;

    /**
     * 获取所有机器人
     */
    @GetMapping
    public Result<List<ChatBotVO>> listAll(
            @RequestParam(required = false) String platform) {
        if (platform != null && !platform.isBlank()) {
            return Results.success(chatBotService.listByPlatform(platform));
        }
        return Results.success(chatBotService.listAll());
    }

    /**
     * 获取机器人详情
     */
    @GetMapping("/{id}")
    public Result<ChatBotVO> getById(@PathVariable String id) {
        return Results.success(chatBotService.getById(id));
    }

    /**
     * 创建机器人
     */
    @PostMapping
    public Result<ChatBotVO> create(@RequestBody ChatBotVO vo) {
        return Results.success(chatBotService.create(vo));
    }

    /**
     * 更新机器人
     */
    @PutMapping("/{id}")
    public Result<ChatBotVO> update(@PathVariable String id, @RequestBody ChatBotVO vo) {
        return Results.success(chatBotService.update(id, vo));
    }

    /**
     * 删除机器人
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        chatBotService.delete(id);
        return Results.success();
    }

    /**
     * 启用/禁用机器人
     */
    @PutMapping("/{id}/enabled")
    public Result<Void> setEnabled(
            @PathVariable String id,
            @RequestBody SetEnabledRequest request) {
        chatBotService.setEnabled(id, request.getEnabled());
        return Results.success();
    }

    /**
     * 绑定检索域
     */
    @PutMapping("/{id}/domain")
    public Result<Void> bindDomain(
            @PathVariable String id,
            @RequestBody BindDomainRequest request) {
        chatBotService.bindDomain(id, request.getDomainId());
        return Results.success();
    }

    /**
     * 设置启用状态请求
     */
    @lombok.Data
    public static class SetEnabledRequest {
        private Boolean enabled;
    }

    /**
     * 绑定检索域请求
     */
    @lombok.Data
    public static class BindDomainRequest {
        private String domainId;
    }
}

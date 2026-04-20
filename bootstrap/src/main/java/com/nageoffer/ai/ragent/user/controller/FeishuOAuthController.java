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

package com.nageoffer.ai.ragent.user.controller;

import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import com.nageoffer.ai.ragent.user.controller.vo.FeishuUserVO;
import com.nageoffer.ai.ragent.user.controller.vo.LoginVO;
import com.nageoffer.ai.ragent.user.service.FeishuOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 飞书 OAuth 控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/feishu")
public class FeishuOAuthController {

    private final FeishuOAuthService feishuOAuthService;

    /**
     * 获取飞书 OAuth 授权 URL
     */
    @GetMapping("/authorize-url")
    public Result<String> getAuthorizeUrl(
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {
        if (state == null || state.isBlank()) {
            state = java.util.UUID.randomUUID().toString().replace("-", "");
        }
        return Results.success(feishuOAuthService.getAuthorizationUrl(redirectUri, state));
    }

    /**
     * 飞书 OAuth 回调接口
     */
    @PostMapping("/callback")
    public Result<LoginVO> callback(@RequestBody FeishuOAuthCallbackRequest request) {
        return Results.success(feishuOAuthService.loginWithFeishu(request.getCode()));
    }

    /**
     * 绑定飞书账号
     */
    @PostMapping("/bind")
    public Result<Void> bind(@RequestBody FeishuOAuthCallbackRequest request) {
        feishuOAuthService.bindFeishuAccount(request.getCode());
        return Results.success();
    }

    /**
     * 解绑飞书账号
     */
    @DeleteMapping("/bind")
    public Result<Void> unbind() {
        feishuOAuthService.unbindFeishuAccount();
        return Results.success();
    }

    /**
     * 获取当前用户的飞书绑定信息
     */
    @GetMapping("/binding")
    public Result<FeishuUserVO> getBinding() {
        return Results.success(feishuOAuthService.getFeishuBinding());
    }

    /**
     * 飞书 OAuth 回调请求
     */
    @lombok.Data
    public static class FeishuOAuthCallbackRequest {
        private String code;
        private String state;
    }
}

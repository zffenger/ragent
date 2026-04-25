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

package com.nageoffer.ai.ragent.user.service;

import com.nageoffer.ai.ragent.user.controller.vo.FeishuUserVO;
import com.nageoffer.ai.ragent.user.controller.vo.LoginVO;

/**
 * 飞书 OAuth 服务接口
 */
public interface FeishuOAuthService {

    /**
     * 获取飞书 OAuth 授权 URL
     *
     * @param redirectUri 回调地址
     * @param state       状态参数
     * @return 授权 URL
     */
    String getAuthorizationUrl(String redirectUri, String state);

    /**
     * 处理飞书 OAuth 回调，获取用户信息
     *
     * @param code        授权码
     * @param redirectUri 回调地址（必须与授权时使用的地址一致）
     * @return 飞书用户信息
     */
    FeishuUserVO handleOAuthCallback(String code, String redirectUri);

    /**
     * 绑定飞书账号到当前登录用户
     *
     * @param code        授权码
     * @param redirectUri 回调地址
     */
    void bindFeishuAccount(String code, String redirectUri);

    /**
     * 解绑当前用户的飞书账号
     */
    void unbindFeishuAccount();

    /**
     * 获取当前用户的飞书绑定信息
     *
     * @return 飞书用户信息，未绑定返回 null
     */
    FeishuUserVO getFeishuBinding();

    /**
     * 使用飞书账号登录
     *
     * @param code        授权码
     * @param redirectUri 回调地址
     * @return 登录结果
     */
    LoginVO loginWithFeishu(String code, String redirectUri);
}

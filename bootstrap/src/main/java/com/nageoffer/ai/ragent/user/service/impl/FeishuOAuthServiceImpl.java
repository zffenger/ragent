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

package com.nageoffer.ai.ragent.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.user.controller.vo.FeishuUserVO;
import com.nageoffer.ai.ragent.user.controller.vo.LoginVO;
import com.nageoffer.ai.ragent.user.dao.entity.FeishuBindingDO;
import com.nageoffer.ai.ragent.user.dao.entity.UserDO;
import com.nageoffer.ai.ragent.user.dao.mapper.FeishuBindingMapper;
import com.nageoffer.ai.ragent.user.dao.mapper.UserMapper;
import com.nageoffer.ai.ragent.user.service.FeishuOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 飞书 OAuth 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuOAuthServiceImpl implements FeishuOAuthService {

    private final UserMapper userMapper;
    private final FeishuBindingMapper feishuBindingMapper;
    private final OkHttpClient httpClient;
    private final StringRedisTemplate redisTemplate;

    @Value("${feishu.oauth.app-id:}")
    private String appId;

    @Value("${feishu.oauth.app-secret:}")
    private String appSecret;

    @Value("${feishu.oauth.enabled:false}")
    private Boolean enabled;

    private static final Gson GSON = new Gson();
    private static final String TOKEN_CACHE_KEY = "feishu:access_token";
    private static final String AUTHORIZE_URL = "https://open.feishu.cn/open-apis/authen/v1/authorize";
    private static final String ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v1/accessible_token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    @Override
    public String getAuthorizationUrl(String redirectUri, String state) {
        checkEnabled();
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return String.format("%s?app_id=%s&redirect_uri=%s&state=%s",
                AUTHORIZE_URL, appId, encodedRedirectUri, state);
    }

    @Override
    public FeishuUserVO handleOAuthCallback(String code) {
        checkEnabled();
        JsonObject tokenResult = getAccessToken(code);
        String accessToken = tokenResult.get("access_token").getAsString();

        // 获取用户信息
        JsonObject userInfo = getUserInfo(accessToken);
        return FeishuUserVO.builder()
                .openId(userInfo.get("open_id").getAsString())
                .userId(userInfo.has("user_id") ? userInfo.get("user_id").getAsString() : null)
                .name(userInfo.has("name") ? userInfo.get("name").getAsString() : null)
                .avatar(userInfo.has("avatar_url") ? userInfo.get("avatar_url").getAsString() : null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindFeishuAccount(String code) {
        checkEnabled();
        String currentUserId = StpUtil.getLoginIdAsString();

        FeishuUserVO feishuUser = handleOAuthCallback(code);

        // 检查该飞书账号是否已被其他用户绑定
        FeishuBindingDO existingBinding = feishuBindingMapper.selectOne(
                new LambdaQueryWrapper<FeishuBindingDO>()
                        .eq(FeishuBindingDO::getFeishuOpenId, feishuUser.getOpenId())
                        .ne(FeishuBindingDO::getUserId, currentUserId)
        );
        if (existingBinding != null) {
            throw new ServiceException("该飞书账号已被其他用户绑定");
        }

        // 检查当前用户是否已绑定飞书账号
        FeishuBindingDO currentBinding = feishuBindingMapper.selectOne(
                new LambdaQueryWrapper<FeishuBindingDO>()
                        .eq(FeishuBindingDO::getUserId, currentUserId)
        );

        if (currentBinding != null) {
            // 更新绑定
            currentBinding.setFeishuOpenId(feishuUser.getOpenId());
            currentBinding.setFeishuUserId(feishuUser.getUserId());
            currentBinding.setFeishuName(feishuUser.getName());
            currentBinding.setFeishuAvatar(feishuUser.getAvatar());
            currentBinding.setBindTime(new Date());
            feishuBindingMapper.updateById(currentBinding);
        } else {
            // 新建绑定
            FeishuBindingDO binding = FeishuBindingDO.builder()
                    .userId(currentUserId)
                    .feishuOpenId(feishuUser.getOpenId())
                    .feishuUserId(feishuUser.getUserId())
                    .feishuName(feishuUser.getName())
                    .feishuAvatar(feishuUser.getAvatar())
                    .bindTime(new Date())
                    .build();
            feishuBindingMapper.insert(binding);
        }

        log.info("用户 {} 成功绑定飞书账号: {}", currentUserId, feishuUser.getOpenId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindFeishuAccount() {
        String currentUserId = StpUtil.getLoginIdAsString();

        UserDO user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        FeishuBindingDO binding = feishuBindingMapper.selectOne(
                new LambdaQueryWrapper<FeishuBindingDO>()
                        .eq(FeishuBindingDO::getUserId, currentUserId)
        );

        if (binding == null) {
            throw new ServiceException("当前用户未绑定飞书账号");
        }

        // 检查是否有密码，如果没有密码且解绑飞书后将无法登录
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ServiceException("请先设置密码后再解绑飞书账号，否则将无法登录");
        }

        feishuBindingMapper.deleteById(binding.getId());

        log.info("用户 {} 成功解绑飞书账号", currentUserId);
    }

    @Override
    public FeishuUserVO getFeishuBinding() {
        String currentUserId = StpUtil.getLoginIdAsString();

        FeishuBindingDO binding = feishuBindingMapper.selectOne(
                new LambdaQueryWrapper<FeishuBindingDO>()
                        .eq(FeishuBindingDO::getUserId, currentUserId)
        );

        if (binding == null) {
            return null;
        }

        UserDO user = userMapper.selectById(currentUserId);

        return FeishuUserVO.builder()
                .openId(binding.getFeishuOpenId())
                .userId(binding.getFeishuUserId())
                .name(binding.getFeishuName())
                .avatar(binding.getFeishuAvatar())
                .bound(true)
                .username(user != null ? user.getUsername() : null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginWithFeishu(String code) {
        checkEnabled();

        FeishuUserVO feishuUser = handleOAuthCallback(code);

        // 查找绑定了该飞书账号的用户
        FeishuBindingDO binding = feishuBindingMapper.selectOne(
                new LambdaQueryWrapper<FeishuBindingDO>()
                        .eq(FeishuBindingDO::getFeishuOpenId, feishuUser.getOpenId())
        );

        if (binding == null) {
            throw new ServiceException("该飞书账号未绑定系统用户，请先登录后在个人设置中绑定");
        }

        UserDO user = userMapper.selectById(binding.getUserId());
        if (user == null) {
            throw new ServiceException("绑定用户不存在");
        }

        // 更新绑定信息中的头像（如果有变化）
        if (feishuUser.getAvatar() != null && !feishuUser.getAvatar().equals(binding.getFeishuAvatar())) {
            binding.setFeishuAvatar(feishuUser.getAvatar());
            binding.setFeishuName(feishuUser.getName());
            feishuBindingMapper.updateById(binding);
        }

        // 执行登录
        StpUtil.login(user.getId());

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .token(StpUtil.getTokenValue())
                .avatar(user.getAvatar())
                .build();
    }

    /**
     * 检查飞书 OAuth 是否启用
     */
    private void checkEnabled() {
        if (!Boolean.TRUE.equals(enabled)) {
            throw new ServiceException("飞书登录功能未启用");
        }
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new ServiceException("飞书 OAuth 配置不完整");
        }
    }

    /**
     * 获取飞书 Access Token
     */
    private JsonObject getAccessToken(String code) {
        // 先获取 tenant_access_token
        String tenantAccessToken = getTenantAccessToken();

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .build();

        Request request = new Request.Builder()
                .url(ACCESS_TOKEN_URL)
                .header("Authorization", "Bearer " + tenantAccessToken)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException("获取飞书 Access Token 失败: " + response.code());
            }
            JsonObject result = GSON.fromJson(response.body().string(), JsonObject.class);
            if (result.get("code").getAsInt() != 0) {
                throw new ServiceException("飞书 API 错误: " + result.get("msg").getAsString());
            }
            return result.getAsJsonObject("data");
        } catch (Exception e) {
            log.error("获取飞书 Access Token 失败", e);
            throw new ServiceException("获取飞书 Access Token 失败: " + e.getMessage());
        }
    }

    /**
     * 获取飞书用户信息
     */
    private JsonObject getUserInfo(String accessToken) {
        Request request = new Request.Builder()
                .url(USER_INFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException("获取飞书用户信息失败: " + response.code());
            }
            JsonObject result = GSON.fromJson(response.body().string(), JsonObject.class);
            if (result.get("code").getAsInt() != 0) {
                throw new ServiceException("飞书 API 错误: " + result.get("msg").getAsString());
            }
            return result.getAsJsonObject("data");
        } catch (Exception e) {
            log.error("获取飞书用户信息失败", e);
            throw new ServiceException("获取飞书用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取飞书 Tenant Access Token
     */
    private String getTenantAccessToken() {
        // 先从缓存获取
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("app_id", appId);
        bodyJson.addProperty("app_secret", appSecret);

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException("获取飞书 Tenant Access Token 失败: " + response.code());
            }
            JsonObject result = GSON.fromJson(response.body().string(), JsonObject.class);
            if (result.get("code").getAsInt() != 0) {
                throw new ServiceException("飞书 API 错误: " + result.get("msg").getAsString());
            }

            String token = result.get("tenant_access_token").getAsString();
            int expire = result.get("expire").getAsInt();

            // 缓存 token（提前 5 分钟过期）
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, expire - 300, TimeUnit.SECONDS);

            return token;
        } catch (Exception e) {
            log.error("获取飞书 Tenant Access Token 失败", e);
            throw new ServiceException("获取飞书 Tenant Access Token 失败: " + e.getMessage());
        }
    }
}

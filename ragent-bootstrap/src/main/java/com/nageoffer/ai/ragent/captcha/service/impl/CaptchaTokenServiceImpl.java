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

package com.nageoffer.ai.ragent.captcha.service.impl;

import com.nageoffer.ai.ragent.captcha.service.CaptchaTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码令牌服务实现
 */
@Service
@RequiredArgsConstructor
public class CaptchaTokenServiceImpl implements CaptchaTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_PREFIX = "captcha:token:";

    @Override
    public void storeToken(String token, long expireSeconds) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean verifyAndConsumeToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 验证后删除，一次性使用
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}

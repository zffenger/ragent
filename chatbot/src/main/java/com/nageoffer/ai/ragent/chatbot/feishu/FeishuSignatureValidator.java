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

package com.nageoffer.ai.ragent.chatbot.feishu;

import com.nageoffer.ai.ragent.chatbot.common.BotConfig;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 飞书签名验证器
 * <p>
 * 用于验证飞书 Webhook 请求的签名，确保请求来自飞书服务器
 * <p>
 * 签名算法：HMAC-SHA256(timestamp + nonce + body, secret)
 */
@Slf4j
public class FeishuSignatureValidator {

    private final BotConfig botConfig;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 使用 BotConfig 构造
     */
    public FeishuSignatureValidator(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    /**
     * 验证签名
     *
     * @param timestamp 请求时间戳
     * @param nonce     请求随机字符串
     * @param body      请求体
     * @param signature 请求签名
     * @return 签名是否有效
     */
    public boolean validate(String timestamp, String nonce, String body, String signature) {
        // 如果未配置验证令牌，跳过验证（不推荐生产环境使用）
        String verificationToken = botConfig.getVerificationToken();
        if (verificationToken == null || verificationToken.isBlank()) {
            log.warn("未配置飞书验证令牌，跳过签名验证: botId={}", botConfig.getId());
            return true;
        }

        if (timestamp == null || nonce == null || body == null || signature == null) {
            log.warn("签名验证参数不完整");
            return false;
        }

        try {
            // 构造签名字符串
            String stringToSign = timestamp + nonce + body;

            // 计算签名
            String calculatedSignature = calculateHmacSha256(stringToSign, verificationToken);

            // 比较签名
            boolean valid = calculatedSignature.equals(signature);
            if (!valid) {
                log.warn("飞书签名验证失败: botId={}, expected={}, actual={}", botConfig.getId(), calculatedSignature, signature);
            }
            return valid;
        } catch (Exception e) {
            log.error("飞书签名验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 计算 HMAC-SHA256 签名
     *
     * @param data   待签名数据
     * @param secret 密钥
     * @return Base64 编码的签名
     */
    private String calculateHmacSha256(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}

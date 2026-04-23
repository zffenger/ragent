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

package com.nageoffer.ai.ragent.chatbot.wework;

import com.nageoffer.ai.ragent.chatbot.common.BotConfig;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企业微信签名验证器
 * <p>
 * 用于验证企业微信回调请求的签名，并解密消息内容
 * <p>
 * 企业微信使用 AES-256-CBC 加密消息，签名使用 SHA-1
 * TODO: 待改造为数据库配置
 */
@Slf4j
public class WeWorkSignatureValidator {

    private final BotConfig botConfig;

    private static final String AES = "AES";
    private static final String AES_CBC_PKCS7 = "AES/CBC/PKCS7Padding";
    private static final String SHA1 = "SHA-1";

    public WeWorkSignatureValidator(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    /**
     * 验证签名
     *
     * @param msgSignature 签名
     * @param timestamp    时间戳
     * @param nonce        随机字符串
     * @param encryptMsg   加密消息
     * @return 签名是否有效
     */
    public boolean validate(String msgSignature, String timestamp, String nonce, String encryptMsg) {
        if (botConfig == null) {
            log.warn("企微机器人配置为空，跳过签名验证");
            return true;
        }

        String token = botConfig.getToken();
        if (token == null || token.isBlank()) {
            log.warn("未配置企微 Token，跳过签名验证: botId={}", botConfig.getId());
            return true;
        }

        if (msgSignature == null || timestamp == null || nonce == null || encryptMsg == null) {
            log.warn("签名验证参数不完整");
            return false;
        }

        try {
            // 构造签名字符串：token + timestamp + nonce + encryptMsg
            String[] arr = new String[]{token, timestamp, nonce, encryptMsg};
            Arrays.sort(arr);

            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                sb.append(s);
            }

            // 计算 SHA-1
            String calculated = sha1(sb.toString());

            // 比较签名
            boolean valid = calculated.equals(msgSignature);
            if (!valid) {
                log.warn("企微签名验证失败: expected={}, actual={}", calculated, msgSignature);
            }
            return valid;
        } catch (Exception e) {
            log.error("企微签名验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 解密消息
     *
     * @param encryptMsg 加密消息（Base64 编码）
     * @return 解密后的 XML 消息
     */
    public String decrypt(String encryptMsg) {
        if (botConfig == null) {
            throw new IllegalArgumentException("企微机器人配置为空");
        }

        String encodingAesKey = botConfig.getEncodingAesKey();
        if (encodingAesKey == null || encodingAesKey.isBlank()) {
            throw new IllegalArgumentException("未配置企微 EncodingAESKey: botId=" + botConfig.getId());
        }

        try {
            // EncodingAESKey 是 43 位，补 "=" 后 Base64 解码得到 32 字节 AES Key
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");

            // Base64 解码加密消息
            byte[] encrypted = Base64.getDecoder().decode(encryptMsg);

            // AES 解密
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS7);
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, AES);
            IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);

            // 去除补位字符
            int len = decrypted.length;
            int padLen = decrypted[len - 1] & 0xff;
            byte[] result = new byte[len - padLen];
            System.arraycopy(decrypted, 0, result, 0, result.length);

            // 解析消息格式：random(16) + msgLen(4) + msg + receiveid
            // 前 16 字节是随机数，后 4 字节是消息长度
            int msgLen = ((result[16] & 0xff) << 24) |
                    ((result[17] & 0xff) << 16) |
                    ((result[18] & 0xff) << 8) |
                    (result[19] & 0xff);

            return new String(result, 20, msgLen, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("企微消息解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("企微消息解密失败", e);
        }
    }

    /**
     * 计算 SHA-1
     */
    private String sha1(String str) throws Exception {
        MessageDigest md = MessageDigest.getInstance(SHA1);
        byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * 验证 URL（首次配置回调时）
     *
     * @param msgSignature 签名
     * @param timestamp    时间戳
     * @param nonce        随机字符串
     * @param echoStr      加密的响应字符串
     * @return 解密后的 echoStr，用于响应
     */
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echoStr) {
        if (!validate(msgSignature, timestamp, nonce, echoStr)) {
            throw new RuntimeException("企微 URL 验证签名失败");
        }
        return decrypt(echoStr);
    }
}

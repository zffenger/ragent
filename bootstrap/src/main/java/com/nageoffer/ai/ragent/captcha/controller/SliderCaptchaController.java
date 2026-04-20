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

package com.nageoffer.ai.ragent.captcha.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.TACBuilder;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.cache.impl.LocalCacheStore;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.generator.impl.transform.Base64ImageTransform;
import cloud.tianai.captcha.interceptor.EmptyCaptchaInterceptor;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.nageoffer.ai.ragent.captcha.service.CaptchaTokenService;
import com.nageoffer.ai.ragent.framework.convention.Result;
import com.nageoffer.ai.ragent.framework.web.Results;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 滑块验证码控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/captcha")
public class SliderCaptchaController {

	private final CaptchaTokenService captchaTokenService;

	private final ImageCaptchaApplication imageCaptchaApplication= TACBuilder.builder()
			// 设置资源存储器，默认是 LocalMemoryResourceStore
			.setResourceStore(new LocalMemoryResourceStore())
			// 加载系统自带的默认资源(系统内置了几个滑块验证码缺口模板图，调用此函数加载)
			.addDefaultTemplate()
			// 设置验证码过期时间, 单位毫秒, default 是默认验证码过期时间，当前设置为10秒,
			// 可以自定义某些验证码类型单独的过期时间， 比如把点选验证码的过期时间设置为60秒
			.expire("default", 10000L)
			// 设置拦截器，默认是 EmptyCaptchaInterceptor.INSTANCE
			.setInterceptor(EmptyCaptchaInterceptor.INSTANCE)
			// 添加验证码背景图片
			// arg1 验证码类型(SLIDER、WORD_IMAGE_CLICK、ROTATE、CONCAT),
			// arg2 验证码背景图片资源
			// 背景图宽高为 600x360
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/a.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/b.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/c.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/d.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/e.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/g.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/h.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/i.jpg"))
			.addResource(CaptchaTypeConstant.SLIDER, new Resource("classpath", "captchaimgs/j.jpg"))
			// 添加验证码模板图片
			// 设置缓存器,可提前生成验证码，用于增加并发性，可以不设置，默认是不开启缓存
			.cached(10, 1000, 5000, 10000L,null)
			// 添加字体包，用于给文字点选验证码提供字体
			.setCacheStore(new LocalCacheStore())
			// 图片转换器，默认是将图片转换成base64格式， 背景图为jpg， 模板图为png， 如果想要扩展，可替换成自己实现的
			.setTransform(new Base64ImageTransform())
			.build();

    /**
     * 生成滑块验证码
     */
    @GetMapping("/generate")
    public Result<ImageCaptchaVO> generate() {
        try {
            ApiResponse<ImageCaptchaVO> response = imageCaptchaApplication.generateCaptcha(CaptchaTypeConstant.SLIDER);
            return Results.success(response.getData());
        } catch (Exception e) {
            log.error("生成验证码失败", e);
            return Results.failure("生成验证码失败","生成验证码失败");
        }
    }

    /**
     * 验证滑块
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> check(@RequestBody CaptchaCheckRequest request) {
        if (request.getId() == null || request.getId().isBlank() || request.getData() == null) {
            return Results.success(Map.of("success", false, "message", "参数错误"));
        }

        try {
            ApiResponse<?> response = imageCaptchaApplication.matching(request.getId(), request.getData());

            Map<String, Object> result = new HashMap<>();
            if (response.isSuccess()) {
                // 验证成功，生成一个验证token供后续校验
                String token = UUID.randomUUID().toString().replace("-", "");
                // 缓存token，有效期5分钟
                captchaTokenService.storeToken(token, 300);
                result.put("success", true);
                result.put("token", token);
            } else {
                result.put("success", false);
                result.put("message", "验证失败");
            }

            return Results.success(result);
        } catch (Exception e) {
            log.error("验证滑块失败", e);
            return Results.success(Map.of("success", false, "message", "验证异常: " + e.getMessage()));
        }
    }

    /**
     * 验证码校验请求
     */
    @lombok.Data
    public static class CaptchaCheckRequest {
        /** 验证码ID */
        private String id;
        /** 验证码轨迹数据 */
        private ImageCaptchaTrack data;
    }
}

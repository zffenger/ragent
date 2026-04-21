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

package com.nageoffer.ai.ragent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.PlaceholderResolutionException;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 环境变量自动校验器
 * <p>
 * 在 Spring Boot 启动最早阶段扫描所有配置源，
 * 复用 Spring 的 PropertyPlaceholderHelper 占位符解析逻辑，
 * 识别未设置的必需环境变量。
 * <p>
 * 配置开关：app.env-validation-enabled=true
 */
@Slf4j
public class EnvVarValidationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	/**
	 * Spring 占位符解析器（复用 Spring 内置逻辑）
	 */
	private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER =
			new PropertyPlaceholderHelper(
					SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX,
					SystemPropertyUtils.VALUE_SEPARATOR,
					SystemPropertyUtils.ESCAPE_CHARACTER,
					false);

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment env = event.getEnvironment();

		// 检查开关
		boolean enabled = Boolean.parseBoolean(env.getProperty("app.env-validation-enabled", "false"));
		if (!enabled) {
			log.info("配置文件中的环境变量校验未开启，跳过");
			return;
		}

		// 扫描所有配置源，提取未解析的环境变量
		Set<String> missingVars = new LinkedHashSet<>();

		for (PropertySource<?> ps : env.getPropertySources()) {
			scanPropertySource(ps, env, missingVars);
		}

		// 校验
		if (!missingVars.isEmpty()) {
			String message = buildErrorMessage(missingVars);
			throw new IllegalStateException(message);
		}

		log.info("配置文件中的环境变量校验通过");
	}

	/**
	 * 扫描 PropertySource 中未解析的占位符
	 * <p>
	 * 只扫描来自 application*.yml 配置文件的属性源
	 */
	private void scanPropertySource(PropertySource<?> ps, ConfigurableEnvironment env, Set<String> missing) {
		// 只处理来自 application 配置文件的 PropertySource
		// Spring Boot 配置文件的名称格式: "applicationConfig: [classpath:/application.yaml]"
		if (!isApplicationConfigSource(ps)) {
			return;
		}

		if (ps instanceof EnumerablePropertySource<?> eps) {
			for (String name : eps.getPropertyNames()) {
				Object value = eps.getProperty(name);
				if (value instanceof String str) {
					try {
						PLACEHOLDER_HELPER.replacePlaceholders(str, env::getProperty);
					} catch (PlaceholderResolutionException ex) {
						missing.add(ex.getPlaceholder());
					}
				}
			}
		}
		// 递归处理组合 PropertySource
		if (ps.getSource() instanceof Iterable<?> sources) {
			for (Object source : sources) {
				if (source instanceof PropertySource<?> childPs) {
					scanPropertySource(childPs, env, missing);
				}
			}
		}
	}

	/**
	 * 判断是否为 application 配置文件的 PropertySource
	 */
	private static boolean isApplicationConfigSource(PropertySource<?> ps) {
		String name = ps.getName();
		// Spring Boot 配置文件名称 for example：Config resource 'class path resource [application.yaml]' via location 'optional:classpath:/'
		return name.contains("Config resource") && name.contains("application") && name.contains("via location");
	}

	/**
	 * 构建错误消息
	 */
	private String buildErrorMessage(Set<String> missing) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("========================================\n");
		sb.append("应用启动失败：缺少必需环境变量\n");
		sb.append("========================================\n");
		for (String var : missing) {
			sb.append("  - ").append(var).append("\n");
		}
		sb.append("========================================\n");
		sb.append("请设置以上环境变量后重新启动\n");
		sb.append("========================================\n");
		return sb.toString();
	}
}

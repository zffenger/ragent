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

package com.nageoffer.ai.ragent.rag.infra.persistence.repository;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.enums.IntentKind;
import com.nageoffer.ai.ragent.rag.domain.enums.IntentLevel;
import com.nageoffer.ai.ragent.rag.domain.repository.IntentNodeRepository;
import com.nageoffer.ai.ragent.rag.domain.entity.IntentNode;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.IntentNodeMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.IntentNodeDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图节点仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class IntentNodeRepositoryImpl implements IntentNodeRepository {

    private final IntentNodeMapper intentNodeMapper;

    @Override
    public List<IntentNode> loadAllIntentTree() {
        return loadIntentTreeFromDB();
    }

	private List<IntentNodeDO> doLoadAllFromDb() {
		return intentNodeMapper.selectList(
				Wrappers.lambdaQuery(IntentNodeDO.class)
						.eq(IntentNodeDO::getEnabled, 1)
						.eq(IntentNodeDO::getDeleted, 0)
		);
	}

	private List<IntentNode> loadIntentTreeFromDB() {
		// 1. 查出所有启用的节点（扁平结构）
		List<IntentNodeDO> entityList = doLoadAllFromDb();

		if (entityList.isEmpty()) {
			return List.of();
		}

		// 2. Entity -> IntentNode（第一遍：先把所有节点建出来，放到 map 里）
		Map<String, IntentNode> id2Node = new HashMap<>();
		for (IntentNodeDO entity : entityList) {
			IntentNode node = IntentNode.builder()
					.id(entity.getIntentCode())
					.kbId(entity.getKbId())
					.name(entity.getName())
					.description(entity.getDescription())
					.level(IntentLevel.fromCode(entity.getLevel()))
					.parentId(entity.getParentCode())
					.collectionName(entity.getCollectionName())
					.mcpToolId(entity.getMcpToolId())
					.topK(entity.getTopK())
					.promptSnippet(entity.getPromptSnippet())
					.promptTemplate(entity.getPromptTemplate())
					.paramPromptTemplate(entity.getParamPromptTemplate())
					.kind(IntentKind.fromCode(entity.getKind()))
					.examples(entity.getExamples() != null ? parseExamples(entity.getExamples()) : new ArrayList<>())
					.children(new ArrayList<>())
					.build();
			id2Node.put(node.getId(), node);
		}

		// 3. 第二遍：根据 parentId 组装 parent -> children
		List<IntentNode> roots = new ArrayList<>();
		for (IntentNode node : id2Node.values()) {
			String parentId = node.getParentId();
			if (parentId == null || parentId.isBlank()) {
				// 没有 parentId，当作根节点
				roots.add(node);
				continue;
			}

			IntentNode parent = id2Node.get(parentId);
			if (parent == null) {
				// 找不到父节点，兜底也当作根节点，避免节点丢失
				roots.add(node);
				continue;
			}

			// 追加到父节点的 children
			if (parent.getChildren() == null) {
				parent.setChildren(new ArrayList<>());
			}
			parent.getChildren().add(node);
		}

		// 4. 填充 fullPath（跟你原来的 fillFullPath 一样的逻辑）
		fillFullPath(roots, null);

		return roots;
	}



	/**
	 * 解析 examples JSON 字符串为 List
	 */
	private List<String> parseExamples(String examplesJson) {
		if (examplesJson == null || examplesJson.isBlank()) {
			return new ArrayList<>();
		}
		try {
			return JSON.parseArray(examplesJson, String.class);
		} catch (Exception e) {
			log.warn("解析 examples JSON 失败: {}", examplesJson, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 填充 fullPath 字段，效果类似：
	 * - 集团信息化
	 * - 集团信息化 > 人事
	 * - 业务系统 > OA系统 > 系统介绍
	 */
	private void fillFullPath(List<IntentNode> nodes, IntentNode parent) {
		if (nodes == null) return;

		for (IntentNode node : nodes) {
			if (parent == null) {
				node.setFullPath(node.getName());
			} else {
				node.setFullPath(parent.getFullPath() + " > " + node.getName());
			}

			if (node.getChildren() != null && !node.getChildren().isEmpty()) {
				fillFullPath(node.getChildren(), node);
			}
		}
	}
}

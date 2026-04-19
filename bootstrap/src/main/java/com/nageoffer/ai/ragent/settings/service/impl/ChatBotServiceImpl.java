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

package com.nageoffer.ai.ragent.settings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nageoffer.ai.ragent.framework.exception.ServiceException;
import com.nageoffer.ai.ragent.settings.controller.vo.ChatBotVO;
import com.nageoffer.ai.ragent.settings.dao.entity.ChatBotDO;
import com.nageoffer.ai.ragent.settings.dao.entity.RetrievalDomainDO;
import com.nageoffer.ai.ragent.settings.dao.mapper.ChatBotMapper;
import com.nageoffer.ai.ragent.settings.dao.mapper.RetrievalDomainMapper;
import com.nageoffer.ai.ragent.settings.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天机器人服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final ChatBotMapper chatBotMapper;
    private final RetrievalDomainMapper retrievalDomainMapper;

    private static final Gson GSON = new Gson();

    @Override
    public List<ChatBotVO> listAll() {
        List<ChatBotDO> bots = chatBotMapper.selectList(
                new LambdaQueryWrapper<ChatBotDO>().orderByAsc(ChatBotDO::getName)
        );
        return convertToVOList(bots);
    }

    @Override
    public List<ChatBotVO> listByPlatform(String platform) {
        List<ChatBotDO> bots = chatBotMapper.selectList(
                new LambdaQueryWrapper<ChatBotDO>()
                        .eq(ChatBotDO::getPlatform, platform)
                        .orderByAsc(ChatBotDO::getName)
        );
        return convertToVOList(bots);
    }

    @Override
    public ChatBotVO getById(String id) {
        ChatBotDO bot = chatBotMapper.selectById(id);
        if (bot == null) {
            return null;
        }
        return convertToVO(bot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatBotVO create(ChatBotVO vo) {
        // 检查名称是否重复
        Long count = chatBotMapper.selectCount(
                new LambdaQueryWrapper<ChatBotDO>().eq(ChatBotDO::getName, vo.getName())
        );
        if (count > 0) {
            throw new ServiceException("机器人名称已存在: " + vo.getName());
        }

        // 验证检索域
        if (vo.getDomainId() != null) {
            RetrievalDomainDO domain = retrievalDomainMapper.selectById(vo.getDomainId());
            if (domain == null) {
                throw new ServiceException("检索域不存在: " + vo.getDomainId());
            }
        }

        ChatBotDO entity = convertToEntity(vo);
        chatBotMapper.insert(entity);

        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatBotVO update(String id, ChatBotVO vo) {
        ChatBotDO entity = chatBotMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("机器人不存在: " + id);
        }

        // 检查名称是否重复（排除自己）
        if (vo.getName() != null && !vo.getName().equals(entity.getName())) {
            Long count = chatBotMapper.selectCount(
                    new LambdaQueryWrapper<ChatBotDO>()
                            .eq(ChatBotDO::getName, vo.getName())
                            .ne(ChatBotDO::getId, id)
            );
            if (count > 0) {
                throw new ServiceException("机器人名称已存在: " + vo.getName());
            }
            entity.setName(vo.getName());
        }

        // 验证检索域
        if (vo.getDomainId() != null) {
            RetrievalDomainDO domain = retrievalDomainMapper.selectById(vo.getDomainId());
            if (domain == null) {
                throw new ServiceException("检索域不存在: " + vo.getDomainId());
            }
            entity.setDomainId(vo.getDomainId());
        }

        // 更新平台配置
        if (vo.getPlatform() != null) {
            entity.setPlatform(vo.getPlatform());
        }
        if (vo.getDescription() != null) {
            entity.setDescription(vo.getDescription());
        }
        if (vo.getAppId() != null) {
            entity.setAppId(vo.getAppId());
        }
        if (vo.getAppSecret() != null) {
            entity.setAppSecret(vo.getAppSecret());
        }
        if (vo.getEncryptKey() != null) {
            entity.setEncryptKey(vo.getEncryptKey());
        }
        if (vo.getVerificationToken() != null) {
            entity.setVerificationToken(vo.getVerificationToken());
        }
        if (vo.getCorpId() != null) {
            entity.setCorpId(vo.getCorpId());
        }
        if (vo.getAgentId() != null) {
            entity.setAgentId(vo.getAgentId());
        }
        if (vo.getToken() != null) {
            entity.setToken(vo.getToken());
        }
        if (vo.getEncodingAesKey() != null) {
            entity.setEncodingAesKey(vo.getEncodingAesKey());
        }
        if (vo.getBotName() != null) {
            entity.setBotName(vo.getBotName());
        }

        // 更新检索配置
        if (vo.getDetectionMode() != null) {
            entity.setDetectionMode(vo.getDetectionMode());
        }
        if (vo.getDetectionKeywords() != null) {
            entity.setDetectionKeywords(GSON.toJson(vo.getDetectionKeywords()));
        }
        if (vo.getAtTriggerEnabled() != null) {
            entity.setAtTriggerEnabled(Boolean.TRUE.equals(vo.getAtTriggerEnabled()) ? 1 : 0);
        }
        if (vo.getLlmThreshold() != null) {
            entity.setLlmThreshold(vo.getLlmThreshold());
        }
        if (vo.getAnswerMode() != null) {
            entity.setAnswerMode(vo.getAnswerMode());
        }
        if (vo.getSystemPrompt() != null) {
            entity.setSystemPrompt(vo.getSystemPrompt());
        }
        if (vo.getMaxTokens() != null) {
            entity.setMaxTokens(vo.getMaxTokens());
        }
        if (vo.getEnabled() != null) {
            entity.setEnabled(Boolean.TRUE.equals(vo.getEnabled()) ? 1 : 0);
        }

        chatBotMapper.updateById(entity);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        chatBotMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(String id, boolean enabled) {
        chatBotMapper.update(null,
                new LambdaUpdateWrapper<ChatBotDO>()
                        .eq(ChatBotDO::getId, id)
                        .set(ChatBotDO::getEnabled, enabled ? 1 : 0)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDomain(String botId, String domainId) {
        if (domainId != null) {
            RetrievalDomainDO domain = retrievalDomainMapper.selectById(domainId);
            if (domain == null) {
                throw new ServiceException("检索域不存在: " + domainId);
            }
        }
        chatBotMapper.update(null,
                new LambdaUpdateWrapper<ChatBotDO>()
                        .eq(ChatBotDO::getId, botId)
                        .set(ChatBotDO::getDomainId, domainId)
        );
    }

    @Override
    public String getDomainId(String botId) {
        ChatBotDO bot = chatBotMapper.selectById(botId);
        return bot != null ? bot.getDomainId() : null;
    }

    /**
     * 转换实体列表为 VO 列表
     */
    private List<ChatBotVO> convertToVOList(List<ChatBotDO> bots) {
        // 获取所有检索域 ID
        List<String> domainIds = bots.stream()
                .map(ChatBotDO::getDomainId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // 查询检索域名称
        Map<String, String> domainNameMap = Map.of();
        if (!domainIds.isEmpty()) {
            List<RetrievalDomainDO> domains = retrievalDomainMapper.selectBatchIds(domainIds);
            domainNameMap = domains.stream()
                    .collect(Collectors.toMap(RetrievalDomainDO::getId, RetrievalDomainDO::getName));
        }

        Map<String, String> finalDomainNameMap = domainNameMap;
        return bots.stream().map(bot -> convertToVO(bot, finalDomainNameMap)).collect(Collectors.toList());
    }

    /**
     * 转换实体为 VO
     */
    private ChatBotVO convertToVO(ChatBotDO entity) {
        return convertToVO(entity, Map.of());
    }

    /**
     * 转换实体为 VO
     */
    private ChatBotVO convertToVO(ChatBotDO entity, Map<String, String> domainNameMap) {
        List<String> keywords = null;
        if (entity.getDetectionKeywords() != null && !entity.getDetectionKeywords().isBlank()) {
            keywords = GSON.fromJson(entity.getDetectionKeywords(), new TypeToken<List<String>>() {}.getType());
        }

        return ChatBotVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .platform(entity.getPlatform())
                .description(entity.getDescription())
                .appId(entity.getAppId())
                .appSecret(entity.getAppSecret())
                .encryptKey(entity.getEncryptKey())
                .verificationToken(entity.getVerificationToken())
                .corpId(entity.getCorpId())
                .agentId(entity.getAgentId())
                .token(entity.getToken())
                .encodingAesKey(entity.getEncodingAesKey())
                .botName(entity.getBotName())
                .domainId(entity.getDomainId())
                .domainName(entity.getDomainId() != null ? domainNameMap.get(entity.getDomainId()) : null)
                .detectionMode(entity.getDetectionMode())
                .detectionKeywords(keywords)
                .atTriggerEnabled(Integer.valueOf(1).equals(entity.getAtTriggerEnabled()))
                .llmThreshold(entity.getLlmThreshold() != null ? entity.getLlmThreshold() : new BigDecimal("0.70"))
                .answerMode(entity.getAnswerMode())
                .systemPrompt(entity.getSystemPrompt())
                .maxTokens(entity.getMaxTokens())
                .enabled(Integer.valueOf(1).equals(entity.getEnabled()))
                .build();
    }

    /**
     * 转换 VO 为实体
     */
    private ChatBotDO convertToEntity(ChatBotVO vo) {
        return ChatBotDO.builder()
                .name(vo.getName())
                .platform(vo.getPlatform())
                .description(vo.getDescription())
                .appId(vo.getAppId())
                .appSecret(vo.getAppSecret())
                .encryptKey(vo.getEncryptKey())
                .verificationToken(vo.getVerificationToken())
                .corpId(vo.getCorpId())
                .agentId(vo.getAgentId())
                .token(vo.getToken())
                .encodingAesKey(vo.getEncodingAesKey())
                .botName(vo.getBotName() != null ? vo.getBotName() : "智能助手")
                .domainId(vo.getDomainId())
                .detectionMode(vo.getDetectionMode() != null ? vo.getDetectionMode() : "COMPOSITE")
                .detectionKeywords(vo.getDetectionKeywords() != null ? GSON.toJson(vo.getDetectionKeywords()) : null)
                .atTriggerEnabled(Boolean.TRUE.equals(vo.getAtTriggerEnabled()) ? 1 : 0)
                .llmThreshold(vo.getLlmThreshold() != null ? vo.getLlmThreshold() : new BigDecimal("0.70"))
                .answerMode(vo.getAnswerMode() != null ? vo.getAnswerMode() : "RAG")
                .systemPrompt(vo.getSystemPrompt())
                .maxTokens(vo.getMaxTokens() != null ? vo.getMaxTokens() : 2000)
                .enabled(Boolean.TRUE.equals(vo.getEnabled()) ? 1 : 0)
                .build();
    }
}

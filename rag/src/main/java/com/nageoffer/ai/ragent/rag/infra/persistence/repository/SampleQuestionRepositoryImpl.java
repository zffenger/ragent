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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.entity.SampleQuestion;
import com.nageoffer.ai.ragent.rag.domain.repository.SampleQuestionRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.SampleQuestionMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.SampleQuestionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 示例问题仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SampleQuestionRepositoryImpl implements SampleQuestionRepository {

    private final SampleQuestionMapper questionMapper;

    @Override
    public void save(SampleQuestion question) {
        SampleQuestionDO record = toDO(question);
        questionMapper.insert(record);
    }

    @Override
    public void update(SampleQuestion question) {
        SampleQuestionDO record = toDO(question);
        questionMapper.updateById(record);
    }

    @Override
    public void deleteById(String id) {
        questionMapper.deleteById(id);
    }

    @Override
    public SampleQuestion findById(String id) {
        SampleQuestionDO record = questionMapper.selectOne(
                Wrappers.lambdaQuery(SampleQuestionDO.class)
                        .eq(SampleQuestionDO::getId, id)
                        .eq(SampleQuestionDO::getDeleted, 0)
        );
        return toEntity(record);
    }

    @Override
    public IPage<SampleQuestion> pageQuery(IPage<SampleQuestion> page, String keyword) {
        IPage<SampleQuestionDO> doPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getCurrent(), page.getSize());
        IPage<SampleQuestionDO> result = questionMapper.selectPage(
                doPage,
                Wrappers.lambdaQuery(SampleQuestionDO.class)
                        .eq(SampleQuestionDO::getDeleted, 0)
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(SampleQuestionDO::getTitle, keyword)
                                .or()
                                .like(SampleQuestionDO::getDescription, keyword)
                                .or()
                                .like(SampleQuestionDO::getQuestion, keyword))
                        .orderByDesc(SampleQuestionDO::getUpdateTime)
        );
        return result.convert(this::toEntity);
    }

    @Override
    public List<SampleQuestion> listRandom(int limit) {
        List<SampleQuestionDO> records = questionMapper.selectList(
                Wrappers.lambdaQuery(SampleQuestionDO.class)
                        .eq(SampleQuestionDO::getDeleted, 0)
                        .last("ORDER BY RANDOM() LIMIT " + limit)
        );
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    private SampleQuestion toEntity(SampleQuestionDO record) {
        if (record == null) {
            return null;
        }
        SampleQuestion entity = new SampleQuestion();
        entity.setId(record.getId());
        entity.setTitle(record.getTitle());
        entity.setDescription(record.getDescription());
        entity.setQuestion(record.getQuestion());
        entity.setCreateTime(record.getCreateTime());
        entity.setUpdateTime(record.getUpdateTime());
        return entity;
    }

    private SampleQuestionDO toDO(SampleQuestion entity) {
        if (entity == null) {
            return null;
        }
        SampleQuestionDO record = new SampleQuestionDO();
        record.setId(entity.getId());
        record.setTitle(entity.getTitle());
        record.setDescription(entity.getDescription());
        record.setQuestion(entity.getQuestion());
        record.setCreateTime(entity.getCreateTime());
        record.setUpdateTime(entity.getUpdateTime());
        return record;
    }
}

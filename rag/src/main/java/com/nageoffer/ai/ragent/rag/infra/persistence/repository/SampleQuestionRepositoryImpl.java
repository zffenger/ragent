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
import com.nageoffer.ai.ragent.rag.domain.repository.SampleQuestionRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.SampleQuestionMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.SampleQuestionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 示例问题仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SampleQuestionRepositoryImpl implements SampleQuestionRepository {

    private final SampleQuestionMapper questionMapper;

    @Override
    public void save(SampleQuestionDO question) {
        questionMapper.insert(question);
    }

    @Override
    public void update(SampleQuestionDO question) {
        questionMapper.updateById(question);
    }

    @Override
    public void deleteById(String id) {
        questionMapper.deleteById(id);
    }

    @Override
    public SampleQuestionDO findById(String id) {
        return questionMapper.selectOne(
                Wrappers.lambdaQuery(SampleQuestionDO.class)
                        .eq(SampleQuestionDO::getId, id)
                        .eq(SampleQuestionDO::getDeleted, 0)
        );
    }

    @Override
    public IPage<SampleQuestionDO> pageQuery(IPage<SampleQuestionDO> page, String keyword) {
        return questionMapper.selectPage(
                page,
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
    }

    @Override
    public List<SampleQuestionDO> listRandom(int limit) {
        return questionMapper.selectList(
                Wrappers.lambdaQuery(SampleQuestionDO.class)
                        .eq(SampleQuestionDO::getDeleted, 0)
                        .last("ORDER BY RANDOM() LIMIT " + limit)
        );
    }
}

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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.ai.ragent.rag.domain.repository.QueryTermMappingRepository;
import com.nageoffer.ai.ragent.rag.infra.persistence.mapper.QueryTermMappingMapper;
import com.nageoffer.ai.ragent.rag.infra.persistence.po.QueryTermMappingDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询术语映射仓储实现
 */
@Repository
@RequiredArgsConstructor
public class QueryTermMappingRepositoryImpl implements QueryTermMappingRepository {

    private final QueryTermMappingMapper mappingMapper;

    @Override
    public void save(QueryTermMappingDO mapping) {
        mappingMapper.insert(mapping);
    }

    @Override
    public void update(QueryTermMappingDO mapping) {
        mappingMapper.updateById(mapping);
    }

    @Override
    public void deleteById(String id) {
        mappingMapper.deleteById(id);
    }

    @Override
    public QueryTermMappingDO findById(String id) {
        return mappingMapper.selectById(id);
    }

    @Override
    public List<QueryTermMappingDO> findAllEnabled() {
        return mappingMapper.selectList(
                Wrappers.lambdaQuery(QueryTermMappingDO.class)
                        .eq(QueryTermMappingDO::getEnabled, 1)
        );
    }

    @Override
    public IPage<QueryTermMappingDO> pageQuery(IPage<QueryTermMappingDO> page, String keyword) {
        return mappingMapper.selectPage(
                page,
                Wrappers.lambdaQuery(QueryTermMappingDO.class)
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(QueryTermMappingDO::getSourceTerm, keyword)
                                .or()
                                .like(QueryTermMappingDO::getTargetTerm, keyword))
                        .orderByAsc(QueryTermMappingDO::getPriority)
                        .orderByDesc(QueryTermMappingDO::getUpdateTime)
        );
    }
}

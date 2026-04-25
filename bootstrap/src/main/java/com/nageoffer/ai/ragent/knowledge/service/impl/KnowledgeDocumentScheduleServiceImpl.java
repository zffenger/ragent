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

package com.nageoffer.ai.ragent.knowledge.service.impl;

import com.nageoffer.ai.ragent.framework.exception.ClientException;
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocument;
import com.nageoffer.ai.ragent.rag.domain.entity.KnowledgeDocumentSchedule;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentScheduleExecRepository;
import com.nageoffer.ai.ragent.rag.domain.repository.KnowledgeDocumentScheduleRepository;
import com.nageoffer.ai.ragent.knowledge.enums.SourceType;
import com.nageoffer.ai.ragent.knowledge.schedule.CronScheduleHelper;
import com.nageoffer.ai.ragent.knowledge.service.KnowledgeDocumentScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentScheduleServiceImpl implements KnowledgeDocumentScheduleService {

    private final KnowledgeDocumentScheduleRepository scheduleRepository;
    private final KnowledgeDocumentScheduleExecRepository scheduleExecRepository;
    @Value("${rag.knowledge.schedule.min-interval-seconds:60}")
    private long scheduleMinIntervalSeconds;

    @Override
    public void upsertSchedule(KnowledgeDocument document) {
        syncSchedule(document, true);
    }

    @Override
    public void syncScheduleIfExists(KnowledgeDocument document) {
        syncSchedule(document, false);
    }

    private void syncSchedule(KnowledgeDocument document, boolean allowCreate) {
        if (document == null) {
            return;
        }
        if (document.getId() == null || document.getKbId() == null) {
            return;
        }
        if (!SourceType.URL.getValue().equalsIgnoreCase(document.getSourceType())) {
            return;
        }
        boolean docEnabled = document.getEnabled() == null || document.getEnabled() == 1;
        String cron = document.getScheduleCron();
        boolean enabled = document.getScheduleEnabled() != null && document.getScheduleEnabled() == 1;
        if (!StringUtils.hasText(cron)) {
            enabled = false;
        }
        if (!docEnabled) {
            enabled = false;
        }

        Date nextRunTime = null;
        if (enabled) {
            try {
                if (CronScheduleHelper.isIntervalLessThan(cron, new Date(), scheduleMinIntervalSeconds)) {
                    throw new ClientException("定时周期不能小于 " + scheduleMinIntervalSeconds + " 秒");
                }
                nextRunTime = CronScheduleHelper.nextRunTime(cron, new Date());
            } catch (IllegalArgumentException e) {
                throw new ClientException("定时表达式不合法");
            }
        }

        KnowledgeDocumentSchedule existing = scheduleRepository.findByDocId(document.getId());

        if (existing == null) {
            if (!allowCreate) {
                return;
            }
            KnowledgeDocumentSchedule schedule = KnowledgeDocumentSchedule.builder()
                    .docId(document.getId())
                    .kbId(document.getKbId())
                    .cronExpr(cron)
                    .enabled(enabled ? 1 : 0)
                    .nextRunTime(nextRunTime)
                    .build();
            scheduleRepository.save(schedule);
        } else {
            existing.setCronExpr(cron);
            existing.setEnabled(enabled ? 1 : 0);
            existing.setNextRunTime(nextRunTime);
            scheduleRepository.update(existing);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDocId(String docId) {
        if (!StringUtils.hasText(docId)) {
            return;
        }
        scheduleExecRepository.deleteByDocId(docId);
        scheduleRepository.deleteByDocId(docId);
    }
}

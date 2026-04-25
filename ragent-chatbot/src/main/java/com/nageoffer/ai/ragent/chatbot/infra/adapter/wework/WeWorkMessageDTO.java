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

package com.nageoffer.ai.ragent.chatbot.infra.adapter.wework;

import lombok.Data;

/**
 * 企业微信消息 DTO
 * <p>
 * 用于构建发送消息的请求
 */
@Data
public class WeWorkMessageDTO {

    /**
     * 接收消息的成员 ID 列表（逗号分隔）
     */
    private String touser;

    /**
     * 接收消息的部门 ID 列表（逗号分隔）
     */
    private String toparty;

    /**
     * 接收消息的标签 ID 列表（逗号分隔）
     */
    private String totag;

    /**
     * 消息类型
     * text, markdown, image, news 等
     */
    private String msgtype;

    /**
     * 应用 AgentId
     */
    private Integer agentid;

    /**
     * 文本消息内容
     */
    private TextContent text;

    /**
     * Markdown 消息内容
     */
    private MarkdownContent markdown;

    /**
     * 是否是保密消息
     */
    private Integer safe;

    /**
     * 是否启用 ID 转译
     */
    private Integer enableIdTrans;

    /**
     * 是否开启重复消息检查
     */
    private Integer enableDuplicateCheck;

    /**
     * 重复消息检查的时间间隔
     */
    private Integer duplicateCheckInterval;

    /**
     * 文本消息内容
     */
    @Data
    public static class TextContent {
        private String content;
    }

    /**
     * Markdown 消息内容
     */
    @Data
    public static class MarkdownContent {
        private String content;
    }

    /**
     * 创建文本消息
     */
    public static WeWorkMessageDTO text(String toUser, Integer agentId, String content) {
        WeWorkMessageDTO message = new WeWorkMessageDTO();
        message.setTouser(toUser);
        message.setAgentid(agentId);
        message.setMsgtype("text");
        message.setSafe(0);

        TextContent textContent = new TextContent();
        textContent.setContent(content);
        message.setText(textContent);

        return message;
    }

    /**
     * 创建 Markdown 消息
     */
    public static WeWorkMessageDTO markdown(String toUser, Integer agentId, String content) {
        WeWorkMessageDTO message = new WeWorkMessageDTO();
        message.setTouser(toUser);
        message.setAgentid(agentId);
        message.setMsgtype("markdown");
        message.setSafe(0);

        MarkdownContent markdownContent = new MarkdownContent();
        markdownContent.setContent(content);
        message.setMarkdown(markdownContent);

        return message;
    }
}

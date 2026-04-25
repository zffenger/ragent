# 机器人模块配置示例

## 配置说明

在 `application.yml` 中添加以下配置：

```yaml
chatbot:
  # 是否启用机器人模块
  enabled: true

  # 飞书机器人配置
  feishu:
    enabled: true
    app-id: ${FEISHU_APP_ID}           # 飞书应用 ID
    app-secret: ${FEISHU_APP_SECRET}   # 飞书应用密钥
    encrypt-key: ${FEISHU_ENCRYPT_KEY:}          # 加密密钥（可选）
    verification-token: ${FEISHU_VERIFICATION_TOKEN:}  # 验证令牌（可选）
    bot-name: 智能助手                  # 机器人名称

  # 企业微信机器人配置
  wework:
    enabled: false
    corp-id: ${WEWORK_CORP_ID}         # 企业 ID
    agent-id: ${WEWORK_AGENT_ID}       # 应用 AgentId
    secret: ${WEWORK_SECRET}           # 应用 Secret
    token: ${WEWORK_TOKEN}             # 回调 Token
    encoding-aes-key: ${WEWORK_ENCODING_AES_KEY}  # 回调 EncodingAESKey

  # 问题检测配置
  detection:
    # 检测模式：KEYWORD（关键词）、LLM（大模型）、COMPOSITE（组合）
    mode: COMPOSITE
    # 触发关键词列表
    keywords:
      - "?"
      - "？"
      - "请"
      - "怎么"
      - "如何"
      - "什么"
      - "为什么"
      - "能不能"
      - "可以吗"
      - "吗"
    # 是否启用 @机器人 触发
    at-trigger-enabled: true
    # LLM 检测的置信度阈值（0-1）
    llm-threshold: 0.7

  # 回答生成配置
  answer:
    # 回答生成模式：LLM（直接调用大模型）、RAG（检索增强生成）
    mode: RAG
    # 默认系统提示词（LLM 模式使用）
    default-system-prompt: 你是一个智能客服助手，请简洁准确地回答用户问题。
    # 最大 Token 数
    max-tokens: 2000
```

## Webhook URL 配置

### 飞书

在飞书开放平台配置机器人回调 URL：
```
https://your-domain.com/webhook/feishu
```

### 企业微信

在企业微信管理后台配置回调 URL：
```
https://your-domain.com/webhook/wework
```

## 功能说明

### 问题识别

机器人支持三种问题识别模式：

1. **KEYWORD**：仅使用关键词检测
   - 检查消息是否包含预设关键词
   - 检查消息是否以问号结尾
   - 性能高，但可能误判

2. **LLM**：仅使用大模型语义检测
   - 使用 LLM 分析消息是否为问题
   - 准确率高，但响应慢、成本高

3. **COMPOSITE**：组合模式（推荐）
   - 先使用关键词快速判断
   - 关键词不确定时使用 LLM 语义检测
   - 平衡性能和准确率

### 回答生成

支持两种回答生成模式：

1. **LLM**：直接调用 LLM 生成回答
   - 简单直接，响应快
   - 不依赖知识库

2. **RAG**：检索增强生成（推荐）
   - 先从知识库检索相关内容
   - 基于检索结果生成回答
   - 回答更准确、更有针对性

### 单聊与群聊

- **单聊**：所有消息都会触发回答
- **群聊**：
  - 只有被识别为问题的消息才会触发回答
  - @机器人 可以直接触发回答

-- 检索域表
-- 定义机器人的检索范围，一个检索域可以包含多个知识库
CREATE TABLE IF NOT EXISTS t_retrieval_domain (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    enabled INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_retrieval_domain_name ON t_retrieval_domain(name) WHERE deleted = 0;

COMMENT ON TABLE t_retrieval_domain IS '检索域表';
COMMENT ON COLUMN t_retrieval_domain.id IS '主键ID';
COMMENT ON COLUMN t_retrieval_domain.name IS '检索域名称';
COMMENT ON COLUMN t_retrieval_domain.description IS '检索域描述';
COMMENT ON COLUMN t_retrieval_domain.enabled IS '是否启用：0-禁用，1-启用';
COMMENT ON COLUMN t_retrieval_domain.create_time IS '创建时间';
COMMENT ON COLUMN t_retrieval_domain.update_time IS '更新时间';
COMMENT ON COLUMN t_retrieval_domain.deleted IS '是否删除：0-正常，1-删除';

-- 检索域-知识库关联表
-- 多对多关系：一个检索域可包含多个知识库，一个知识库可属于多个检索域
CREATE TABLE IF NOT EXISTS t_domain_knowledge (
    id VARCHAR(64) PRIMARY KEY,
    domain_id VARCHAR(64) NOT NULL,
    knowledge_id VARCHAR(64) NOT NULL,
    priority INT DEFAULT 100,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_domain_knowledge_domain ON t_domain_knowledge(domain_id);
CREATE INDEX IF NOT EXISTS idx_domain_knowledge_knowledge ON t_domain_knowledge(knowledge_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_domain_knowledge_unique ON t_domain_knowledge(domain_id, knowledge_id) WHERE deleted = 0;

COMMENT ON TABLE t_domain_knowledge IS '检索域-知识库关联表';
COMMENT ON COLUMN t_domain_knowledge.id IS '主键ID';
COMMENT ON COLUMN t_domain_knowledge.domain_id IS '检索域ID';
COMMENT ON COLUMN t_domain_knowledge.knowledge_id IS '知识库ID';
COMMENT ON COLUMN t_domain_knowledge.priority IS '优先级，越小越高';
COMMENT ON COLUMN t_domain_knowledge.create_time IS '创建时间';
COMMENT ON COLUMN t_domain_knowledge.deleted IS '是否删除：0-正常，1-删除';

-- 聊天机器人表
-- 存储各个平台的机器人配置，每个机器人绑定一个检索域
CREATE TABLE IF NOT EXISTS t_chat_bot (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    description VARCHAR(500),

    -- 平台配置
    app_id VARCHAR(100),
    app_secret VARCHAR(500),
    encrypt_key VARCHAR(500),
    verification_token VARCHAR(500),
    corp_id VARCHAR(100),
    agent_id VARCHAR(100),
    token VARCHAR(500),
    encoding_aes_key VARCHAR(500),
    bot_name VARCHAR(100) DEFAULT '智能助手',

    -- 检索配置
    domain_id VARCHAR(64),
    detection_mode VARCHAR(20) DEFAULT 'COMPOSITE',
    detection_keywords TEXT,
    at_trigger_enabled INT DEFAULT 1,
    llm_threshold DECIMAL(3,2) DEFAULT 0.70,
    answer_mode VARCHAR(20) DEFAULT 'RAG',
    system_prompt TEXT,
    max_tokens INT DEFAULT 2000,

    -- 状态
    enabled INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_bot_name ON t_chat_bot(name) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_chat_bot_platform ON t_chat_bot(platform);
CREATE INDEX IF NOT EXISTS idx_chat_bot_domain ON t_chat_bot(domain_id);

COMMENT ON TABLE t_chat_bot IS '聊天机器人表';
COMMENT ON COLUMN t_chat_bot.id IS '主键ID';
COMMENT ON COLUMN t_chat_bot.name IS '机器人名称';
COMMENT ON COLUMN t_chat_bot.platform IS '平台：FEISHU/WEWORK';
COMMENT ON COLUMN t_chat_bot.description IS '机器人描述';
COMMENT ON COLUMN t_chat_bot.app_id IS '应用ID（飞书）';
COMMENT ON COLUMN t_chat_bot.app_secret IS '应用密钥';
COMMENT ON COLUMN t_chat_bot.encrypt_key IS '加密密钥';
COMMENT ON COLUMN t_chat_bot.verification_token IS '验证令牌';
COMMENT ON COLUMN t_chat_bot.corp_id IS '企业ID（企微）';
COMMENT ON COLUMN t_chat_bot.agent_id IS '应用AgentId（企微）';
COMMENT ON COLUMN t_chat_bot.token IS '回调Token（企微）';
COMMENT ON COLUMN t_chat_bot.encoding_aes_key IS 'EncodingAESKey（企微）';
COMMENT ON COLUMN t_chat_bot.bot_name IS '机器人显示名称';
COMMENT ON COLUMN t_chat_bot.domain_id IS '绑定的检索域ID';
COMMENT ON COLUMN t_chat_bot.detection_mode IS '问题检测模式：KEYWORD/LLM/COMPOSITE';
COMMENT ON COLUMN t_chat_bot.detection_keywords IS '检测关键词，JSON数组';
COMMENT ON COLUMN t_chat_bot.at_trigger_enabled IS '是否启用@触发：0-否，1-是';
COMMENT ON COLUMN t_chat_bot.llm_threshold IS 'LLM检测置信度阈值';
COMMENT ON COLUMN t_chat_bot.answer_mode IS '回答模式：LLM/RAG';
COMMENT ON COLUMN t_chat_bot.system_prompt IS '系统提示词';
COMMENT ON COLUMN t_chat_bot.max_tokens IS '最大Token数';
COMMENT ON COLUMN t_chat_bot.enabled IS '是否启用：0-禁用，1-启用';
COMMENT ON COLUMN t_chat_bot.create_time IS '创建时间';
COMMENT ON COLUMN t_chat_bot.update_time IS '更新时间';
COMMENT ON COLUMN t_chat_bot.deleted IS '是否删除：0-正常，1-删除';

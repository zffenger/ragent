-- 系统配置表
-- 用于存储各类系统配置，以 JSON 格式存储复杂配置
CREATE TABLE IF NOT EXISTS t_system_config (
    id VARCHAR(64) PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_system_config_key ON t_system_config(config_key) WHERE deleted = 0;

COMMENT ON TABLE t_system_config IS '系统配置表';
COMMENT ON COLUMN t_system_config.id IS '主键ID';
COMMENT ON COLUMN t_system_config.config_key IS '配置键，如 ai.chat, chatbot.feishu';
COMMENT ON COLUMN t_system_config.config_value IS '配置值，JSON格式';
COMMENT ON COLUMN t_system_config.description IS '配置描述';
COMMENT ON COLUMN t_system_config.create_time IS '创建时间';
COMMENT ON COLUMN t_system_config.update_time IS '更新时间';
COMMENT ON COLUMN t_system_config.deleted IS '是否删除：0-正常，1-删除';

-- 模型提供商表
-- 存储各AI模型提供商的连接配置
CREATE TABLE IF NOT EXISTS t_model_provider (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500),
    api_key VARCHAR(500),
    endpoints TEXT,
    enabled INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_model_provider_name ON t_model_provider(name) WHERE deleted = 0;

COMMENT ON TABLE t_model_provider IS '模型提供商表';
COMMENT ON COLUMN t_model_provider.id IS '主键ID';
COMMENT ON COLUMN t_model_provider.name IS '提供商名称，如 openai, siliconflow, ollama';
COMMENT ON COLUMN t_model_provider.url IS '基础URL';
COMMENT ON COLUMN t_model_provider.api_key IS 'API密钥';
COMMENT ON COLUMN t_model_provider.endpoints IS '端点配置，JSON格式';
COMMENT ON COLUMN t_model_provider.enabled IS '是否启用：0-禁用，1-启用';
COMMENT ON COLUMN t_model_provider.create_time IS '创建时间';
COMMENT ON COLUMN t_model_provider.update_time IS '更新时间';
COMMENT ON COLUMN t_model_provider.deleted IS '是否删除：0-正常，1-删除';

-- 模型候选表
-- 存储各类模型(Chat/Embedding/Rerank)的候选配置
CREATE TABLE IF NOT EXISTS t_model_candidate (
    id VARCHAR(64) PRIMARY KEY,
    model_id VARCHAR(100) NOT NULL,
    model_type VARCHAR(20) NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    url VARCHAR(500),
    dimension INT,
    priority INT DEFAULT 100,
    enabled INT DEFAULT 1,
    supports_thinking INT DEFAULT 0,
    is_default INT DEFAULT 0,
    is_deep_thinking INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_model_candidate_type ON t_model_candidate(model_type);
CREATE INDEX IF NOT EXISTS idx_model_candidate_provider ON t_model_candidate(provider_name);

COMMENT ON TABLE t_model_candidate IS '模型候选表';
COMMENT ON COLUMN t_model_candidate.id IS '主键ID';
COMMENT ON COLUMN t_model_candidate.model_id IS '模型唯一标识';
COMMENT ON COLUMN t_model_candidate.model_type IS '模型类型: CHAT/EMBEDDING/RERANK';
COMMENT ON COLUMN t_model_candidate.provider_name IS '提供商名称';
COMMENT ON COLUMN t_model_candidate.model_name IS '模型名称';
COMMENT ON COLUMN t_model_candidate.url IS '自定义URL';
COMMENT ON COLUMN t_model_candidate.dimension IS '向量维度（embedding模型）';
COMMENT ON COLUMN t_model_candidate.priority IS '优先级，越小越高';
COMMENT ON COLUMN t_model_candidate.enabled IS '是否启用：0-禁用，1-启用';
COMMENT ON COLUMN t_model_candidate.supports_thinking IS '是否支持思考链：0-否，1-是';
COMMENT ON COLUMN t_model_candidate.is_default IS '是否默认模型：0-否，1-是';
COMMENT ON COLUMN t_model_candidate.is_deep_thinking IS '是否深度思考模型：0-否，1-是';
COMMENT ON COLUMN t_model_candidate.create_time IS '创建时间';
COMMENT ON COLUMN t_model_candidate.update_time IS '更新时间';
COMMENT ON COLUMN t_model_candidate.deleted IS '是否删除：0-正常，1-删除';

-- 初始化默认配置数据（可选）
-- 这些配置可以从 application.yml 迁移过来

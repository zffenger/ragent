-- 创建飞书账号绑定表
CREATE TABLE IF NOT EXISTS t_feishu_binding (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    feishu_open_id VARCHAR(64) NOT NULL,
    feishu_user_id VARCHAR(64),
    feishu_name VARCHAR(128),
    feishu_avatar VARCHAR(512),
    bind_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

-- 创建索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_feishu_binding_user ON t_feishu_binding(user_id) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS idx_feishu_binding_open_id ON t_feishu_binding(feishu_open_id) WHERE deleted = 0;

-- 添加表注释
COMMENT ON TABLE t_feishu_binding IS '飞书账号绑定表';
COMMENT ON COLUMN t_feishu_binding.id IS '主键ID';
COMMENT ON COLUMN t_feishu_binding.user_id IS '关联的用户ID';
COMMENT ON COLUMN t_feishu_binding.feishu_open_id IS '飞书Open ID';
COMMENT ON COLUMN t_feishu_binding.feishu_user_id IS '飞书User ID';
COMMENT ON COLUMN t_feishu_binding.feishu_name IS '飞书用户名称';
COMMENT ON COLUMN t_feishu_binding.feishu_avatar IS '飞书用户头像URL';
COMMENT ON COLUMN t_feishu_binding.bind_time IS '绑定时间';
COMMENT ON COLUMN t_feishu_binding.create_time IS '创建时间';
COMMENT ON COLUMN t_feishu_binding.update_time IS '更新时间';
COMMENT ON COLUMN t_feishu_binding.deleted IS '删除标记';

-- 从用户表中移除飞书相关字段（如果存在）
ALTER TABLE t_user DROP COLUMN IF EXISTS feishu_open_id;
ALTER TABLE t_user DROP COLUMN IF EXISTS feishu_user_id;
ALTER TABLE t_user DROP COLUMN IF EXISTS feishu_name;
ALTER TABLE t_user DROP COLUMN IF EXISTS feishu_avatar;

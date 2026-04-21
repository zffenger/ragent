#!/bin/bash
#
# 环境变量配置文件
# 用法: source env.sh 或 . env.sh
#

# ============ Java 环境 ============
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# ============ 应用配置 ============
export APP_PORT=9090
export JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# ============ 数据库配置 ============
export DB_HOST=127.0.0.1
export DB_PORT=5432
export DB_NAME=ragent
export DB_USERNAME=postgres
export DB_PASSWORD=your_db_password

# ============ Redis 配置 ============
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password

# ============ AI 服务 API Key ============
export BAILIAN_API_KEY=your_bailian_api_key
export SILICONFLOW_API_KEY=your_siliconflow_api_key

# ============ Ollama 配置 ============
export OLLAMA_URL=http://localhost:11434

# ============ 存储配置 ============
export STORAGE_TYPE=local
export STORAGE_PATH=/data/ragent/storage
export ALIYUN_OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
export ALIYUN_ACCESS_KEY_ID=111
export ALIYUN_ACCESS_KEY_SECRET=111
export ALIYUN_OSS_BUCKET=111

# ============ 飞书 OAuth 配置 ============
export FEISHU_OAUTH_ENABLED=true
export FEISHU_APP_ID=
export FEISHU_APP_SECRET=

# ============ MCP 服务器 ============
export MCP_SERVER_URL=http://localhost:9099

# ============ 其他配置 ============
export DEFAULT_CHAT_MODEL=qwen3-max

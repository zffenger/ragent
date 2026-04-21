# Ragent 部署指南

## 目录结构

```
ragent/
├── bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar  # 应用 JAR
├── deploy/
│   ├── deploy.sh                    # 部署脚本
│   ├── env.sh.example               # 环境变量模板
│   ├── application-prod.yaml.example # 生产配置模板
│   ├── ragent.service               # Systemd 服务文件
│   └── logs/                        # 日志目录
```

## 快速部署

### 1. 构建应用

```bash
# 在项目根目录执行
mvn clean package -DskipTests
```

### 2. 配置环境变量

```bash
cd deploy
cp env.sh env.sh
vim env.sh  # 修改配置
source env.sh
```

### 3. 配置生产参数

```bash
cp application-prod.yaml application-prod.yaml
vim application-prod.yaml  # 修改配置
```

### 4. 启动服务

```bash
./deploy.sh start
```

## 管理命令

```bash
./deploy.sh start    # 启动服务
./deploy.sh stop     # 停止服务
./deploy.sh restart  # 重启服务
./deploy.sh status   # 查看状态
./deploy.sh logs     # 查看日志
```


## 配置注入方式

### 方式一：环境变量

所有敏感配置通过环境变量注入：

```bash
export DB_PASSWORD=your_password
export BAILIAN_API_KEY=your_api_key
```

### 方式二：外部配置文件

复制 `application-prod.yaml.example` 为 `application-prod.yaml`，修改后启动时会自动加载。

### 方式三：启动参数

```bash
java -jar bootstrap.jar \
    --spring.datasource.password=your_password \
    --ai.providers.bailian.api-key=your_key
```

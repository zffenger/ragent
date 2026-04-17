# 项目概况

企业级 Agentic RAG 智能体，覆盖文档解析、多路检索、意图识别、问题重写、会话记忆、MCP 工具调用与深度思考全链路

## 核心能力

包含以下核心能力：

多路检索引擎：意图定向检索 + 全局向量检索并行执行，结果经去重、重排序后处理，兼顾精准度与召回率。

意图识别与引导：树形多级意图分类（领域→类目→话题），置信度不足时主动引导澄清，而非硬猜答案。

问题重写与拆分：多轮对话自动补全上下文，复杂问题拆为子问题分别检索，解决"说的不是想问的"。

会话记忆管理：保留近 N 轮对话，超限自动摘要压缩，控 Token 成本不丢上下文。

模型路由与容错：多模型优先级调度、首包探测、健康检查、自动降级，单模型故障不影响服务。

MCP 工具集成：意图非知识检索时自动提参调用业务工具，检索与工具调用无缝融合。

文档入库ETL：节点编排 Pipeline，从抓取、解析、增强、分块、向量化到写入数据库，灵活配置可扩展。

全链路追踪：重写、意图、检索、生成每个环节均有 Trace 记录，排查与调优有据可依。

管理后台：React 管理界面，覆盖知识库管理、意图树编辑、入库监控、链路追踪、系统设置。

## 技术规范

### 技术栈

**后端**: Java 17、Spring Boot 3.5.7、MyBatis Plus
**前端**: React 18、Vite、TypeScript
**数据库**: Postgres
**缓存/限流**:Redis + Redisson
**文档解析**:Apache Tika 3.2
**认证鉴权**: Sa-Token

### 后端代码风格

使用 Lombok 注解简化 POJO（@Data, @Builder,
@Slf4j）                                                                                                                                                                                                                                           
统一异常处理：抛出 `ClientException` 或 `ServiceException`
，不要直接抛原始异常                                                                                                                                                                                                                 
API 响应使用 `Result<T>` 包装，成功用 `Result.success()`，失败用
`Result.fail()`                                                                                                                                                                                                               
链路追踪使用 `@RagTraceNode`
注解                                                                                                                                                                                                                                                              
线程池执行必须使用 TTL 包装，确保上下文透传

### TypeScript/React

使用 ES modules（import/export），不使用
CommonJS                                                                                                                                                                                                                                              
组件使用函数式组件 +
Hooks                                                                                                                                                                                                                                                                     
状态管理使用
Zustand                                                                                                                                                                                                                                                                           
表单使用 react-hook-form + zod
校验                                                                                                                                                                                                                                                            
UI 组件基于 Radix UI

### 必需的依赖服务

| 服务           | 默认端口  | 用途                       |                                                                                                                                                                                                                                                                       
|--------------|-------|--------------------------|                                                                                                                                                                                                                                                                       
| PostgreSQL   | 5432  | 关系数据 + pgvector          |                                                                                                                                                                                                                                                      
| Redis        | 6379  | 缓存、Session、限流            |                                                                                                                                                                                                                                                           
| RocketMQ     | 9876  | 异步消息队列                   |                                                                                                                                                                                                                                                               
| pgvector       | 5432 | 向量数据库 |                                                                                                                                                                                                                                       
| RustFS/MinIO | 9000  | 对象存储（文档文件）               |  
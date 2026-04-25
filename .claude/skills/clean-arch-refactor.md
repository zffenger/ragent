# Clean Architecture Refactoring Skill

将现有模块按 Clean Architecture (Clean-Arch) 原则进行重构，确保分层清晰、依赖方向正确。

## 重构原则

### 分层结构

```
module/
  ├── domain/                          # 领域层（核心，无外部依赖）
  │   ├── entity/                      # 实体（Entity）
  │   ├── vo/                          # 值对象（Value Object）
  │   ├── service/                     # 领域服务接口
  │   ├── repository/                  # Repository 接口
  │   ├── event/                       # 领域事件
  │   └── exception/                   # 领域异常
  │
  ├── application/                     # 应用层（编排领域服务）
  │   ├── XxxApplication.java          # 应用服务（命名规范）
  │   └── assembler/                   # 应用层组装器
  │
  ├── infra/                           # 基础设施层（实现接口）
  │   ├── persistence/                 # 持久化 实现
  │   │   ├── converter/               # DO ↔ Entity 转换
  │   │   ├── po/                      # 持久化对象（DO/PO）
  │   │   ├── mapper/                  # MyBatis Mapper
  │   ├── adapter/                     # 领域层接口实现
  │   │   └── client/                  # 外部客户端实现
  │   ├── config/                      # 内部配置
  │   └── util/                        # 工具类
  │
  └── interfaces/                      # 接口层（外部交互）
      ├── controller/                  # REST 控制器
      ├── dto/                         # 数据传输对象
      │   ├── request/
      │   └── response/
      ├── assembler/                   # DTO 组装器
      ├── facade/                      # 外部服务门面
      └── config/                      # 接口层配置
```

### 依赖规则（关键！）
- application 层依赖 domain 层，application层是编排层，作用是编排多个领域服务
- domain层不依赖任何其他层
- interfaces/infra可以依赖domain和application，
- interfaces可以依赖infra，infra不可以依赖interfaces
- 禁止： domain/application 依赖 infra/interfaces

### 命名规范
```
// 领域层
domain/entity/User.java                    // 实体
domain/vo/UserId.java                      // 值对象
domain/service/UserService.java            // 领域服务
domain/repository/UserRepository.java      // Repository 接口
domain/event/XxxEvent.java                 // 领域层事件
domain/event/XxxEventHandler.java          // 领域层事件处理器
domain/exception/XxxBizException.java      // 领域层异常

// 应用层
application/UserApplication.java           // 应用服务
application/assembler/XxxAssembler.java     // 应用层组装器

// 基础设施层
infra/persistence/UserRepositoryImpl.java     // Repository 实现
infra/persistence/po/UserPO.java              // 持久化对象
infra/persistence/mapper/UserMapper.java      // MyBatis Mapper
infra/adapter/BaiduMapAdapter.java           // 领域层接口实现
infra/adapter/client/BaiduMapClient.java     // 外部服务的client封装，非必须

// 接口层
interfaces/controller/UserController.java              // 控制器
interfaces/dto/request/CreateUserRequest.java          // 请求 DTO
interfaces/dto/response/UserResponse.java              // 响应 DTO
interfaces/facade/UserFacade.java                      // 外部服务门面
```

### 其他要求
- 不要遗留TODO
- 各种PO/VO值对象与Entity之间的互转不要使用BeanUtil.copy()
- 在将 Mapper 重构为 Repository 层时，必须保持原有的查询语义
  - 分页查询必须在数据库层面完成，不能用 findAll() 后内存分页
  - 不要用循环查询单个替代批量查询

## 重构步骤

### 1. 分析现有结构

首先理解现有代码的组织方式和依赖关系：

```bash
# 列出现有目录结构
find module/src/main/java -type d | head -20

# 检查现有依赖关系
grep -r "import" module/src/main/java --include="*.java" | grep -v "^Binary"
```

### 2. 创建目标目录结构

```bash
# 创建 domain 层目录
mkdir -p module/src/main/java/.../domain/service
mkdir -p module/src/main/java/.../domain/repository
mkdir -p module/src/main/java/.../domain/vo

# 创建 infra 层目录
mkdir -p module/src/main/java/.../infra/persistence
mkdir -p module/src/main/java/.../infra/adapter

# 创建 interfaces 层目录
mkdir -p module/src/main/java/.../interfaces/config
```

### 3. 移动文件（使用 git mv 保留历史）

**重要**：必须使用 `git mv` 而非创建新文件，以保留 git 历史追踪。

```bash
# 移动领域服务接口到 domain/service
git mv old/path/XXXService.java new/path/domain/service/

# 移动 Repository 接口到 domain/repository
git mv old/path/XXXRepository.java new/path/domain/repository/

# 移动值对象到 domain/vo
git mv old/path/ModelTarget.java new/path/domain/vo/
```

### 4. 更新包声明

每个移动后的文件需要更新 package 声明：

```java
// domain/service/XXXService.java
package com.xxx.module.domain.service;

// domain/repository/xxxRepository.java
package com.xxx.module.domain.repository;

// domain/vo/ModelTarget.java
package com.xxx.module.domain.vo;
```

### 5. 更新 import 语句

批量更新引用这些类的文件的 import：

```bash
# 查找需要更新的文件
grep -r "import com.xxx.old.path" --include="*.java"

# 批量替换 import
sed -i '' 's/import com.xxx.old.path.LLMService/import com.xxx.module.domain.service.LLMService/g' **/*.java
```

### 6. 处理依赖倒置问题

**关键问题**：如果 domain 层的值对象引用了 interfaces 层的配置类，违反了 Clean Architecture。

**解决方案**：在 domain 层创建独立的值对象。

**Before（违反规则）**:
```java
// domain/vo/ModelTarget.java - 引用了 interfaces 层
package com.xxx.module.domain.vo;
import com.xxx.module.interfaces.config.AIModelProperties; // ❌ 违规

public record ModelTarget(
    String id,
    AIModelProperties.ModelCandidate candidate, // ❌
    AIModelProperties.ProviderConfig provider   // ❌
) {}
```

**After（符合规则）**:
```java
// domain/vo/ModelCandidateConfig.java - 独立值对象
package com.xxx.module.domain.vo;
public record ModelCandidateConfig(
    String id, String provider, String model,
    String url, Integer dimension, Integer priority,
    Boolean enabled, Boolean supportsThinking
) {}

// domain/vo/ProviderConfig.java - 独立值对象
package com.xxx.module.domain.vo;
public record ProviderConfig(
    String url, String apiKey, Map<String, String> endpoints
) {}

// domain/vo/ModelTarget.java - 使用 domain 层值对象
package com.xxx.module.domain.vo;
public record ModelTarget(
    String id,
    ModelCandidateConfig candidate, // ✓ domain 层类型
    ProviderConfig provider          // ✓ domain 层类型
) {}
```

**转换逻辑放在 infra 或 interfaces **:
```java
// infra/model/ModelSelector.java
private ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate, ...) {
    return new ModelTarget(
        modelId,
        toCandidateConfig(candidate),  // 转换
        toProviderConfig(provider)     // 转换
    );
}

private ModelCandidateConfig toCandidateConfig(AIModelProperties.ModelCandidate c) {
    return new ModelCandidateConfig(
        c.getId(), c.getProvider(), c.getModel(),
        c.getUrl(), c.getDimension(), c.getPriority(),
        c.getEnabled(), c.getSupportsThinking()
    );
}
```

### 7. Record 类访问器注意事项

Java Record 的访问器方法是字段名本身，而非 `getXxx()`：

```java
// Record 定义
public record ModelCandidateConfig(String id, String provider) {}

// 正确访问方式
config.id()       // ✓
config.provider() // ✓
config.getId()    // ❌ 编译错误
config.getProvider() // ❌ 编译错误

// 从 Lombok @Data 类迁移时需要更新调用
// Before: candidate.getProvider()
// After:  candidate.provider()
```

### 8. 验证依赖规则

重构完成后验证分层正确性：

```bash
# 检查 domain 层是否依赖 infra/interfaces
grep -r "import com.xxx.module.infra" domain/
grep -r "import com.xxx.module.interfaces" domain/
# 应该无输出

# 检查 infra 层是否正确依赖 domain
grep -r "import com.xxx.module.domain" infra/
# 应该有输出
```

## 常见问题

### Q: 为什么 Repository 接口放在 domain 层？

A: Repository 接口是领域层对数据访问的抽象，属于领域概念。实现类放在 infra 层，遵循依赖倒置原则（DIP）。

### Q: 值对象（VO）和 DTO 有什么区别？

A:
- **VO（Value Object）**：领域层概念，不可变，代表业务概念
- **DTO（Data Transfer Object）**：接口层概念，用于跨层传输数据

### Q: 配置类应该放在哪层？

A: Spring 配置类（如 `@ConfigurationProperties`）放在 interfaces/config 层，因为它们处理外部配置输入。

### Q: 如何处理跨层转换？

A: 在调用边界处进行转换：
- interfaces → domain：在 assembler 或 controller 中转换
- domain → infra：在 infra 层实现中转换（如上面的 ModelSelector 例子）

## 重构检查清单

- [ ] 目录结构符合 Clean Architecture 分层
- [ ] domain 层无 infra/interfaces 依赖
- [ ] application 层无 infra/interfaces 依赖
- [ ] Repository 接口在 domain/repository
- [ ] 值对象使用 `vo` package 命名
- [ ] Application 服务命名为 `xxxApplication`
- [ ] 使用 `git mv` 保留文件历史
- [ ] Record 访问器使用字段名而非 getter
- [ ] 编译通过：`mvn compile -DskipTests`
- [ ] 测试通过：`mvn test`

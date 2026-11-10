# CampusOverflow · 后端工程（Lab 4）

> 《软件工程》课程项目 · 第 12 组 · Sprint 2 · 第 7 周
>
> **一个按 DDD 限界上下文物理分包的 Spring Boot 多模块工程**：6 个 Maven 模块、每个模块内部 domain / application / infrastructure / interfaces 四层，
> 首个端到端接口 `POST /api/v1/questions` 已经打通 Controller → ApplicationService → Repository → MySQL。

| 交付物 | 位置 | 状态 |
|---|---|---|
| ① 可运行的多模块 Maven 工程（`mvn clean install` 全绿） | 仓库根目录 | ✅ |
| ② 通过 `application.yml` 启动并连接数据库 | [`co-bootstrap/src/main/resources/application.yml`](co-bootstrap/src/main/resources/application.yml) | ✅ |
| ③ 分模块决策记录 | [`docs/adr/ADR-002-package-structure.md`](docs/adr/ADR-002-package-structure.md) | ✅ |
| ④ Postman Collection（含 `POST /api/v1/questions` 成功用例） | [`docs/api/CampusOverflow-Lab4.postman_collection.json`](docs/api/CampusOverflow-Lab4.postman_collection.json) | ✅ |
| 附：本地 MySQL 实测记录（截图级证据） | [`docs/lab4/验证记录.md`](docs/lab4/验证记录.md) | ✅ |

---

## 1. 团队与分工

| 角色 | 成员 | GitHub |
|---|---|---|
| PO（产品负责人） | 倪昊宇 | — |
| SM（Scrum Master） | 陈玉豪 | https://github.com/215cyh |
| SM（Scrum Master） | 刘铭洋 | https://github.com/215lmy |
| Developer | 陶浩峰 | https://github.com/7WenL7 |
| Developer | 程靖轩 | https://github.com/Cheng-7-28 |

作业提交仓库：<https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow>

Lab 4 的主责划分（完整 WBS、PR 拆分、评审配对与现场节奏见 [`docs/lab4/小组分工.md`](docs/lab4/小组分工.md)）：

| 成员 | 角色 | Lab 4 主责 |
|---|---|---|
| 倪昊宇 | PO | ADR-002、最终验收、报告"设计决策"章节 |
| 陶浩峰 | Developer | `co-qa` 领域模型（Question/Answer/Vote + 27 个单测）与 `POST /api/v1/questions` |
| 程靖轩 | Developer | 父 POM + 六模块骨架 + `co-identity` |
| 陈玉豪 | SM | Flyway / 数据库配置 / 全局异常 / JSON 日志，构建绿灯值守 |
| 刘铭洋 | SM | JPA 映射、Postman 集合、真库验证记录、看板 |

---

## 2. 技术栈

| 项 | 版本 / 选择 | 说明 |
|---|---|---|
| JDK | 21（Temurin） | 父 POM `maven.compiler.release=21` |
| Spring Boot | 3.5.6 | 通过 `spring-boot-dependencies` BOM 统一版本 |
| 构建 | Maven 3.9+ | 多模块 reactor |
| 持久化 | Spring Data JPA + Hibernate 6 | `ddl-auto=none`，schema 由 Flyway 管 |
| 数据库 | MySQL 8.x | 本机 `campus_overflow` 库 |
| 迁移 | Flyway 11（`flyway-core` + `flyway-mysql`） | `V1__init_schema.sql` |
| 校验 | Jakarta Bean Validation | 接口层第一道防线 |
| 日志 | Logback + logstash-logback-encoder | 默认 JSON，`local` profile 转人类可读 |
| 测试 | JUnit 5 + MockMvc + H2 | 不依赖本地 MySQL 即可全绿 |

---

## 3. 目录结构

```
campus-overflow/
├── pom.xml                          # 父 POM：模块清单、依赖版本、插件版本
├── co-shared-kernel/                # 共享内核（SK）：聚合根基类、领域事件、UserId、Tag 词表
├── co-identity/                     # 身份与访问上下文
│   └── src/main/java/com/campusoverflow/identity/
│       ├── domain/                  #   User 聚合、Email/Nickname/PasswordHash、UserRepository 接口
│       ├── application/             #   UserApplicationService、RegisterUserCommand
│       ├── infrastructure/          #   UserJpaEntity、UserRepositoryAdapter、BCrypt 适配器
│       └── interfaces/rest/         #   UserController + DTO（包名用 interfaces：interface 是 Java 保留字）
├── co-qa/                           # 问答核心上下文
│   └── src/main/java/com/campusoverflow/qa/
│       ├── domain/                  #   Question 聚合根、Answer 聚合、Vote 值对象、不变式常量
│       ├── application/             #   QuestionApplicationService、命令对象
│       ├── infrastructure/          #   QuestionJpaEntity、Mapper、RepositoryAdapter
│       └── interfaces/rest/         #   QuestionController + DTO
├── co-reputation/                   # 声誉上下文（骨架：ReputationAccount + 仓储接口）
├── co-discovery/                    # 发现与检索（骨架：QuestionSummary 读模型 + 仓储接口）
├── co-bootstrap/                    # 装配层：启动入口、Bean 装配、Flyway、全局异常、日志
│   └── src/main/resources/
│       ├── application.yml
│       ├── logback-spring.xml
│       └── db/migration/V1__init_schema.sql
└── docs/
    ├── adr/ADR-002-package-structure.md
    ├── api/CampusOverflow-Lab4.postman_collection.json
    └── lab4/验证记录.md
```

依赖方向（评审按这条打回）：

```
interfaces ──► application ──► domain ◄── infrastructure
                                 ▲
                       shared-kernel（所有上下文可依赖）
co-bootstrap ──► 所有上下文（唯一的装配点）
```

> 第四层的包名是 `interfaces` 而不是 `interface`：`interface` 是 Java 保留字，不能作为包名（这是编译期就撞过的坑，记录在 ADR-002 §3）。

---

## 4. 快速开始

### 4.1 准备数据库（MySQL 8）

```sql
CREATE DATABASE campus_overflow
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

表由 Flyway 在应用启动时自动创建，**不需要手工执行建表语句**。

### 4.2 配置连接

默认值写在 `co-bootstrap/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://127.0.0.1:3306/campus_overflow?...}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}     # 本机开发默认值，团队按自己环境覆盖
```

推荐用环境变量覆盖，不要把各自机器的密码提交进仓库：

```powershell
$env:DB_PASSWORD = "你的密码"
```

### 4.3 构建 + 测试

```bash
mvn clean install
```

预期：`BUILD SUCCESS`，各模块测试全部通过（聚合单元测试 38 个 + 接口集成测试 6 个，共 44 个）。
测试用 H2 内存库，**不要求本机装 MySQL**。

### 4.4 启动

```bash
# 方式一：Maven 直接跑
mvn -pl co-bootstrap -am spring-boot:run

# 方式二：打好的可执行 jar
java -jar co-bootstrap/target/campus-overflow.jar

# 本地开发想看人类可读日志
java -jar co-bootstrap/target/campus-overflow.jar --spring.profiles.active=local
```

启动成功的标志（JSON 日志里能看到）：

```json
{"timestamp":"...","level":"INFO","message":"Successfully applied 1 migration to schema `campus_overflow`"}
{"timestamp":"...","level":"INFO","message":"Tomcat started on port 8080"}
```

### 4.5 冒烟测试

```bash
# 1) 注册（初始 pending）
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email":"tao@hbuas.edu.cn","nickname":"陶浩峰","password":"abc12345"}'

# 2) 验证邮箱（拿到上一步的 id）
curl -X POST http://localhost:8080/api/v1/users/<userId>/email-verification

# 3) 发布问题（Lab 4 的核心接口）
curl -X POST http://localhost:8080/api/v1/questions \
  -H "Content-Type: application/json" \
  -d '{"askerId":"<userId>","title":"Flyway 和 JPA 的初始化顺序应该怎么配？","body":"启动时 Hibernate 先跑，说表不存在。","tags":["flyway","spring-data-jpa"]}'
```

最后一步应返回 `201 Created` + `Location: /api/v1/questions/<questionId>`，数据库里能查到对应行。

---

## 5. API

### 5.1 接口清单

| 方法 | 路径 | 用途 | 对应故事 |
|---|---|---|---|
| POST | `/api/v1/users` | 注册账号（初始 `pending`） | US-01 |
| POST | `/api/v1/users/{id}/email-verification` | 模拟点击验证链接（`pending → active`） | US-01 |
| GET | `/api/v1/users/{id}` | 查账号 | US-02 前置 |
| **POST** | **`/api/v1/questions`** | **发布问题（Lab 4 端到端接口）** | **US-04** |
| GET | `/api/v1/questions/{id}` | 问题详情 | US-05 |
| POST | `/api/v1/questions/{id}/acceptance` | 采纳答案 | US-08 |

### 5.2 `POST /api/v1/questions`

请求：

```json
{
  "askerId": "3f1c8f7e-1a2b-4c3d-9e8f-0123456789ab",
  "title": "Flyway 和 JPA 的初始化顺序应该怎么配？",
  "body": "启动时 Hibernate 先跑，报错说表不存在。",
  "tags": ["flyway", "spring-data-jpa"]
}
```

成功（`201 Created`，`Location: /api/v1/questions/{id}`）：

```json
{
  "id": "9a7e2c11-4b5d-4e6f-8a9b-0c1d2e3f4a5b",
  "askerId": "3f1c8f7e-1a2b-4c3d-9e8f-0123456789ab",
  "title": "Flyway 和 JPA 的初始化顺序应该怎么配？",
  "body": "启动时 Hibernate 先跑，报错说表不存在。",
  "tags": ["flyway", "spring-data-jpa"],
  "status": "open",
  "voteCount": 0,
  "createdAt": "2026-11-09T02:11:35.123Z",
  "updatedAt": "2026-11-09T02:11:35.123Z"
}
```

### 5.3 统一错误响应

所有 4xx/5xx 都是同一结构，前端按 `code` 分支，不解析中文：

```json
{
  "code": "INV-Q-02",
  "message": "标签「我自己造的词」不在共享词表 SK-01 中，请先申请新增词条",
  "traceId": "1f2e3d4c5b6a7988",
  "timestamp": "2026-11-09T02:12:03.456Z",
  "path": "/api/v1/questions",
  "details": []
}
```

| HTTP | code | 触发场景 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 请求字段格式错误（`details` 里逐字段说明） |
| 400 | `INV-Q-01` | 标题/正文长度不合规（聚合层兜底） |
| 400 | `INV-Q-02` | 标签超过 5 个 / 重复 / 不在 SK-01 词表 |
| 400 | `INV-U-02` | 账号未验证邮箱或已被禁用，不能发布内容 |
| 400 | `INV-U-03` | 密码不符合策略（≥8 位且含字母与数字） |
| 400 | `INV-Q-05` | 问题已下架，不能再编辑/采纳/投票 |
| 404 | `NOT_FOUND` | 问题 / 回答 / 用户不存在 |
| 409 | `INV-U-01` | 邮箱已注册 |
| 422 | 其他领域错误码 | 领域规则拒绝但不属于上述分类 |
| 500 | `INTERNAL_ERROR` | 未预期异常（日志里有完整堆栈，响应里不暴露） |

`traceId` 同时出现在响应头和 JSON 日志里（`X-Trace-Id`），排查时可直接反查整条链路。

---

## 6. 设计要点（为什么代码长这样）

完整决策见 [ADR-002](docs/adr/ADR-002-package-structure.md)，这里只列最容易被问到的三点：

1. **聚合没有 setter**：`Question` 的字段全私有，只能通过 `post / edit / acceptAnswer / hide / applyVoteProjection` 改变，
   每个方法内联了对应的不变式守卫（`INV-Q-01 ~ INV-Q-05`）。
2. **领域层不依赖框架**：`co-qa` 的 `domain` 包里没有一条 Spring/JPA import，所以聚合测试是纯内存的毫秒级测试。
3. **跨上下文只传 ID**：`co-qa` 的 pom 里没有 `co-identity`，判断"提问人能不能发帖"走 `AskerVerifier` 端口，
   由 `co-bootstrap` 的 `IdentityAskerVerifier` 适配。

---

## 7. 测试

```bash
mvn test                 # 全部
mvn -pl co-qa test       # 只跑问答上下文
```

| 测试类 | 覆盖内容 |
|---|---|
| `UserTest`（6） | 密码策略、状态单向流转、未验证不能发布 |
| `QuestionTest`（10） | INV-Q-01 ~ INV-Q-05、改判留痕、幂等采纳、票数投影 |
| `AnswerTest`（7） | INV-A-01、INV-A-04、仅作者可操作、下架后不可编辑 |
| `VoteTest`（5） | INV-V-02 自投拒绝、INV-V-03 改票净变化 ±2 |
| `TagSetTest`（5） | 词表外标签、重复标签、CSV 往返 |
| `ReputationAccountTest`（5） | INV-R-01 不低于 1 分、INV-R-02 eventId 幂等 |
| `QuestionApiIntegrationTest`（6） | 注册→验证→发帖→读回（H2），以及 4 类错误分支 |

> 如果 `mvn test` 在某些环境报 Mockito 自附加失败（`Could not self-attach to current VM`），
> 父 POM 已经加了 `-Djdk.attach.allowAttachSelf=true`；再不行就在本地 `.mvn/jvm.config` 里补同样参数。

---

## 8. 已知边界（Lab 4 范围内刻意不做）

| 项 | 为什么现在不做 | 何时做 |
|---|---|---|
| 登录鉴权 / JWT | US-02 在 Sprint 2 末尾；现在 `askerId` 由请求体显式传入，接入后改由会话解析，命令对象不变 | Sprint 2 末 |
| 回答接口 `POST /questions/{id}/answers` | Lab 4 只要求一个端到端接口；`Answer` 聚合与表已就绪 | Sprint 2 末（US-06） |
| 问题列表 / 分页 | `co-discovery` 读模型仍是骨架 | Sprint 2 末（US-03） |
| 投票接口 | `Vote` 目前是值对象，Lab 3 结论是它要独立成聚合（避免热点行锁） | Sprint 3（US-07） |
| 标签检索 | 标签暂存 CSV，等 US-10 再拆 `question_tag` 表 | Sprint 3（US-10） |
| 声誉持久化 | `co-reputation` 只有领域模型与单元测试 | Sprint 3（US-11/12） |
| 治理（举报/下架审核） | 没有用例，暂由 `Question.hide()` 承接 | Sprint 4（US-24） |

---

## 9. 文档索引

- [ADR-002 · 分模块与分层方案](docs/adr/ADR-002-package-structure.md)
- [Postman Collection](docs/api/CampusOverflow-Lab4.postman_collection.json)
- [本地 MySQL 实测记录](docs/lab4/验证记录.md)
- [小组分工（WBS / PR 拆分 / 评审配对）](docs/lab4/小组分工.md)
- [提交与 PR 说明（UPLOAD）](UPLOAD.md)
- Lab 3 产出（同仓库 `docs/ddd/`）：聚合识别清单、上下文映射、统一语言术语表

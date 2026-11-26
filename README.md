# CampusOverflow · 后端（Lab 4）+ 前端（Lab 5）+ Sprint 3 核心特性（Lab 6）+ 测试门禁（Lab 7）+ 上线（Lab 8）

[![CI](https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/actions/workflows/ci.yml/badge.svg)](https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/actions/workflows/ci.yml)

> 《软件工程》课程项目 · 第 12 组 · Sprint 2–4
>
> **后端**：按 DDD 限界上下文物理分包的 Spring Boot 多模块工程（8 个 Maven 模块，domain / application / infrastructure / interfaces 四层），
> `POST /api/v1/questions` 打通 Controller → ApplicationService → Repository → MySQL。
> **前端**：React 18 + TypeScript 5 + Vite 6，已与后端端到端贯通「提问 → 列表显示 → 详情查看」（见 [`frontend/README.md`](frontend/README.md)）。
> **Sprint 3**：投票、采纳、评论、标签、声誉计算全部落地，声誉由问答领域事件在事务提交后驱动
> （见 [`ADR-003`](docs/adr/ADR-003-sprint3-core-features.md)）。
> **质量与上线**：112 个测试 + 覆盖率 93.1% + Checkstyle 门禁（Lab 7）；Docker 多阶段镜像、compose 六服务、
> Prometheus + Grafana 监控与 CI/CD 自动部署（Lab 8）。

| 交付物 | 位置 | 状态 |
|---|---|---|
| ① 可运行的多模块 Maven 工程（`mvn clean install` 全绿） | 仓库根目录 | ✅ |
| ② 通过 `application.yml` 启动并连接数据库 | [`co-bootstrap/src/main/resources/application.yml`](co-bootstrap/src/main/resources/application.yml) | ✅ |
| ③ 分模块决策记录 | [`docs/adr/ADR-002-package-structure.md`](docs/adr/ADR-002-package-structure.md) | ✅ |
| ④ Postman Collection（含 `POST /api/v1/questions` 成功用例） | [`docs/api/CampusOverflow-Lab4.postman_collection.json`](docs/api/CampusOverflow-Lab4.postman_collection.json) | ✅ |
| 附：本地 MySQL 实测记录（截图级证据） | [`docs/lab4/验证记录.md`](docs/lab4/验证记录.md) | ✅ |
| **Lab 5** 前端工程（`pnpm dev` / lint / typecheck / E2E） | [`frontend/`](frontend/README.md) | ✅ |
| 附：Lab 5 验证记录（真实浏览器 + 录像 + SELECT） | [`docs/lab5/验证记录.md`](docs/lab5/验证记录.md) | ✅ |
| **Lab 6** Sprint 3 增量（投票 / 采纳 / 评论 / 标签 / 声誉） | `co-qa`、`co-reputation`、`V2__sprint3_schema.sql` | ✅ |
| ⑥ 关键设计 ADR | [`docs/adr/ADR-003-sprint3-core-features.md`](docs/adr/ADR-003-sprint3-core-features.md) | ✅ |
| ⑦ Sprint 3 回顾纪要 | [`docs/retro/sprint-3.md`](docs/retro/sprint-3.md) | ✅ |
| 附：Lab 6 验证记录（真库 + E2E + 跨模块 BUG） | [`docs/lab6/验证记录.md`](docs/lab6/验证记录.md) | ✅ |
| **Lab 7** 测试与质量门禁（覆盖率 / 契约 / CI） | `pom.xml`、`co-coverage/`、`.github/workflows/ci.yml` | ✅ |
| ⑧ 团队代码评审清单 v1.0 + 5 份评审记录 | [`docs/team/review-checklist.md`](docs/team/review-checklist.md)、[`docs/review/`](docs/review/) | ✅ |
| 附：Lab 7 验证记录（覆盖率与门禁输出） | [`docs/lab7/验证记录.md`](docs/lab7/验证记录.md) | ✅ |
| **Lab 8** 容器化 + CI/CD + 监控（compose 六服务） | `Dockerfile`、`frontend/Dockerfile`、`docker-compose.yml`、`deploy/`、`.github/workflows/ci.yml` | ✅ |
| ⑨ 运维手册 v1.0（启停/回滚/故障/备份） | [`docs/lab8/运维手册.md`](docs/lab8/运维手册.md) | ✅ |
| 附：Lab 8 部署方案与验证记录 | [`docs/lab8/部署方案.md`](docs/lab8/部署方案.md)、[`docs/lab8/验证记录.md`](docs/lab8/验证记录.md) | ✅ |

---

## 1. 团队与分工

| 角色 | 成员 | GitHub |
|---|---|---|
| PO（产品负责人） | 倪昊宇 | https://github.com/ennonhy |
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
├── frontend/                        # Lab 5 前端：React 18 + TS 5 + Vite 6（详见 frontend/README.md）
└── docs/
    ├── adr/ADR-002-package-structure.md
    ├── api/CampusOverflow-Lab4.postman_collection.json
    ├── api/openapi.yaml             # 前后端契约（前端据此生成客户端）
    ├── lab4/验证记录.md
    └── lab5/验证记录.md
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

### 4.0 一条命令启动全套（Lab 8）

```bash
cp .env.example .env        # 改 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD / GRAFANA_ADMIN_PASSWORD
docker compose up -d --build
docker compose ps           # mysql → app → nginx 依次 healthy
curl -s localhost/actuator/health
# 前端 http://localhost/ ；Grafana：ssh -L 3000:127.0.0.1:3000 user@server → http://localhost:3000
```

> 本机没有 Docker 时按 4.1–4.3 手动启动。容器结构、监控与回滚见
> [`docs/lab8/部署方案.md`](docs/lab8/部署方案.md) 与 [`docs/lab8/运维手册.md`](docs/lab8/运维手册.md)。

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
| POST / GET | `/api/v1/answers` | 提交回答 / 某问题的回答列表 | US-06 |
| POST / DELETE | `/api/v1/votes` | 投票、改票、撤票 | US-07 |
| POST / GET / DELETE | `/api/v1/comments`、`/api/v1/comments/{id}` | 评论 | US-09 |
| GET | `/api/v1/tags` | 标签词表（可扩展） | US-10 |
| GET | `/api/v1/questions?tag=` | 按标签筛选 | US-10 |
| GET | `/api/v1/users/{id}/reputation` | 声誉分与明细 | US-11 |

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

## 7. 测试与质量门禁（Lab 7）

```bash
# 一次跑完：编译 → Checkstyle → 测试 → JaCoCo → 覆盖率聚合报告
mvn clean verify

# 覆盖率门禁（整体 ≥70%、核心聚合包 ≥90%）
node tools/coverage-gate.mjs

# 前端：类型 + lint + 构建（CI 同款）
cd frontend && pnpm lint && pnpm typecheck && pnpm build
```

| 门禁 | 工具 | 阈值 / 行为 |
|---|---|---|
| 静态检查 | Checkstyle 10（`config/checkstyle/checkstyle.xml`） | 0 violation 才继续构建 |
| 覆盖率 | JaCoCo 0.8.12 + `co-coverage` 聚合模块 | 整体行覆盖 ≥70%；`*/domain/model` ≥90% |
| 契约 | `OpenApiContractTest` | 所有 MVC 映射必须出现在 `docs/api/openapi.yaml`，响应结构符合 schema |
| 集成 | Testcontainers（`MySqlContainerIntegrationTest`） | 无 Docker 自动跳过；CI 的 ubuntu runner 真实执行 |
| 持续集成 | GitHub Actions（`.github/workflows/ci.yml`） | 后端 verify + 前端 lint/typecheck/build；有 `SONAR_TOKEN` 时推送 SonarQube |

SonarQube（可选，需自备服务）：

```bash
docker compose -f docker-compose.sonarqube.yml up -d      # 首次启动约 1–2 分钟
export SONAR_TOKEN=<token>
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000
```

> 注意：`.github/workflows/ci.yml` 与 Checkstyle 的配置路径都假设**在仓库根目录执行 mvn**；
> 用 `mvn -f <path>` 从其他目录调用会找不到 `config/checkstyle/checkstyle.xml`。

### 测试清单

| 测试类 | 覆盖内容 |
|---|---|
| `UserTest` / `UserApplicationServiceTest` | 密码策略、状态单向流转、注册/验证事件（ArgumentCaptor） |
| `QuestionTest` / `QuestionApplicationServiceTest` | INV-Q-01~05、采纳与改判、限流、分页夹紧 |
| `AnswerTest` / `QaValueObjectTest` | INV-A-01/04、值对象与枚举边界 |
| `VoteTest` / `VoteApplicationServiceTest` | INV-V-02/03、票数投影与声誉增量 |
| `CommentApplicationServiceTest` | INV-C-01~03、目标存在性 |
| `ReputationAccountTest` / `ReputationApplicationServiceTest` | INV-R-01/02、幂等与最小分 |
| `QuestionApiIntegrationTest` / `Sprint3ApiIntegrationTest` | 接口级链路（H2） |
| `OpenApiContractTest` | OpenAPI 契约完整性与响应结构 |
| `MySqlContainerIntegrationTest` | 真实 MySQL + Flyway + `ddl-auto=validate` |

> 如果 `mvn test` 在某些环境报 Mockito 自附加失败（`Could not self-attach to current VM`），
> 父 POM 已经加了 `-Djdk.attach.allowAttachSelf=true`；再不行就在本地 `.mvn/jvm.config` 里补同样参数。

---

## 8. 边界与已知缺口（Sprint 3 结项时）

| 项 | 现状 | 何时补 |
|---|---|---|
| 登录鉴权 / JWT | 前端用自动演示账号获取 `askerId`；后端未校验身份 | Sprint 4（US-02 增强） |
| 通知（被回答/被采纳/被评论） | 事件已发出，但没有订阅方写通知表 | Sprint 4（US-14） |
| 标签治理（同义词合并、敏感词） | 词表已可扩展，但无审核/合并 | Sprint 4（US-10 增强） |
| 排行榜 / 徽章 | 声誉明细已就绪，未做聚合榜 | Sprint 4（US-17/18） |
| 治理（举报/下架审核） | 暂由 `Question.hide()` 承接 | Sprint 4（US-19/24） |
| 票数/声誉重算任务 | 目前只写不纠偏 | Sprint 4 运维任务 |
| 测试环境与 MySQL 的差异 | `vote.value` 保留字问题已修，但 DDL 仍是两套（Flyway + H2 ddl-auto） | Sprint 4 引入 Testcontainers |

> Sprint 2 的缺口（回答、列表、投票、标签、声誉）已在 Sprint 3 全部补齐；
> 完整实现见 [ADR-003](docs/adr/ADR-003-sprint3-core-features.md)。

---

## 9. 文档索引

- [ADR-002 · 分模块与分层方案](docs/adr/ADR-002-package-structure.md)
- [Postman Collection](docs/api/CampusOverflow-Lab4.postman_collection.json)
- [OpenAPI 契约](docs/api/openapi.yaml)
- [本地 MySQL 实测记录](docs/lab4/验证记录.md)
- [小组分工（WBS / PR 拆分 / 评审配对）](docs/lab4/小组分工.md)
- [提交与 PR 说明（UPLOAD）](UPLOAD.md)
- **Lab 5**：[前端 README](frontend/README.md)、[Lab 5 验证记录](docs/lab5/验证记录.md)、[Lab 5 小组分工](docs/lab5/小组分工.md)
- **Lab 6**：[ADR-003](docs/adr/ADR-003-sprint3-core-features.md)、[Sprint 3 回顾](docs/retro/sprint-3.md)、[Lab 6 验证记录](docs/lab6/验证记录.md)、[Lab 6 小组分工](docs/lab6/小组分工.md)
- **Lab 7**：[评审清单 v1.0](docs/team/review-checklist.md)、[评审记录](docs/review/)、[Lab 7 验证记录](docs/lab7/验证记录.md)、[Lab 7 小组分工](docs/lab7/小组分工.md)
- **Lab 8**：[部署方案](docs/lab8/部署方案.md)、[运维手册 v1.0](docs/lab8/运维手册.md)、[Lab 8 验证记录](docs/lab8/验证记录.md)、[Lab 8 小组分工](docs/lab8/小组分工.md)
- Lab 3 产出（同仓库 `docs/ddd/`）：聚合识别清单、上下文映射、统一语言术语表

---

## 10. 部署与可观测性（Lab 8）

### 10.1 容器与编排

| 组件 | 镜像 | 说明 |
|---|---|---|
| `nginx` | `nginx:1.27-alpine`（多阶段构建产物） | 静态资源 + `/api` 反代 + SPA fallback |
| `app` | `eclipse-temurin:21-jre-alpine`（多阶段） | 非 root `app` 用户，健康检查 `/actuator/health/readiness` |
| `mysql` | `mysql:8.4` | 数据卷 `mysql-data`，Flyway 自动迁移 |
| `redis` | `redis:7-alpine` | Sprint 4 预留缓存（当前业务不依赖） |
| `prometheus` | `prom/prometheus:v2.54.1` | 抓取 `app:8080/actuator/prometheus` |
| `grafana` | `grafana/grafana:11.2.0` | 看板「CampusOverflow · 生产概览」自动加载 |

`docker compose up -d --build` 一条命令启动；依赖顺序由 `depends_on: condition: service_healthy` 保证。

### 10.2 可观测指标（10+）

| 类别 | 指标 |
|---|---|
| 业务 | `campusoverflow_questions_posted_total`、`answers_submitted_total`、`answers_accepted_total`、`votes_cast_total{value}`、`comments_posted_total` |
| HTTP | `http_server_requests_seconds_count/{bucket}`（QPS、p50/p95、5xx 比例） |
| JVM | `jvm_memory_used_bytes`、`jvm_gc_*`、`process_cpu_usage` |
| 数据库 | `hikaricp_connections_active/idle/pending` |

告警规则（`deploy/prometheus/alerts.yml`）：p95 > 500ms、5xx > 1%、实例下线、堆占用 > 90%。

### 10.3 CI/CD

```
push(main) → backend(mvn verify) + frontend(lint/typecheck/build)
           → images(docker build & push GHCR)
           → deploy(SSH: compose pull + up -d --no-deps)
           → smoke test(/actuator/health/readiness，最长 90s)
```

未配置 `DEPLOY_HOST` 时部署步骤自动跳过；配置方法见 [`docs/lab8/部署方案.md`](docs/lab8/部署方案.md) §4。

已发布的镜像（main 每次构建自动推送）：

```text
ghcr.io/hbuas-se-2026fall/cs2412-team12-campus-overflow/backend:sha-<commit>
ghcr.io/hbuas-se-2026fall/cs2412-team12-campus-overflow/frontend:sha-<commit>
```

CI 运行记录：<https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/actions/workflows/ci.yml>
（`backend` 用 MySQL service 容器跑真实库集成测试；`images` 构建双镜像；`deploy` 未配服务器时跳过）。

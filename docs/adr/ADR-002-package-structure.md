# ADR-002 · 后端工程的分模块与分层方案

| 项目 | 内容 |
|---|---|
| 编号 | ADR-002 |
| 状态 | 已接受（Accepted） |
| 日期 | 第 7 周 · Sprint 2（Lab 4） |
| 决策人 | 第 12 组全员：倪昊宇（PO）、陈玉豪（SM）、刘铭洋（SM）、陶浩峰（Dev）、程靖轩（Dev） |
| 相关文档 | [Lab 3 聚合清单](../ddd/aggregates.md)、[统一语言术语表](../ddd/ubiquitous-language.md)、`docs/adr/ADR-001`（待补） |
| 影响范围 | 整个后端仓库的目录结构、模块依赖、持久化映射、数据库迁移归属 |

---

## 1. 我们面对的问题

Lab 3 事件风暴产出了 8 个聚合、6 个限界上下文和 53 条统一语言术语，但这些还只是"墙上的便利贴"。
Lab 4 要把它们变成能被 `mvn clean install` 编译、能被 `POST /api/v1/questions` 跑通、并且**别人接手时不会写错层**的代码。

具体要回答四个问题：

1. **按什么切 Maven 模块？** 按技术分层（controller/service/dao）还是按业务边界？
2. **每个模块内部再怎么分层？** 四层之间允许谁依赖谁？
3. **领域对象和 JPA 实体是一个类还是两个类？**
4. **哪些东西放共享内核、哪些必须各上下文自己定义？**

---

## 2. 决策一：Maven 模块 = 限界上下文，不按技术分层切

### 决策

```
campus-overflow（父 POM：统一 Spring Boot 3.5 / JDK 21 / JUnit 5 版本）
├── co-shared-kernel   共享内核：聚合根基类、领域事件、UserId、Tag 词表 SK-01
├── co-identity        身份与访问：User 聚合（INV-U-01~04）
├── co-qa              问答核心：Question / Answer 聚合 + Vote 值对象（INV-Q/A/V）
├── co-reputation      声誉：ReputationAccount 聚合骨架（INV-R-01~04）
├── co-discovery       发现与检索：QuestionSummary 读模型骨架
└── co-bootstrap       装配：启动入口、Bean 装配、Flyway、全局异常、日志
```

一共 6 个模块，与 Lab 4 Step 1 的要求一致。

### 为什么不是 8 个模块

Lab 3 识别出 8 个聚合，但**聚合数 ≠ 模块数**：

- **Moderation（治理）在 Sprint 2 没有任何用例**：举报（US-19）、审核队列与下架（US-24）都排在 Sprint 4。
  现在建一个空模块，只会让 `mvn` 多一个永远不编译的目录。它的概念暂时由 `Question.hide()` / `Answer.hide()` 承接，
  等 US-24 立项时再抽 `co-moderation`，届时 `ModerationCase` 通过防腐层调用 `HideQuestion` 命令。
- **Reputation / Discovery 保留为骨架模块**：它们已经有明确的领域词汇（声誉分、规则码、读模型），
  且 Sprint 3 就要落地，提前建模块可以让 `QuestionPosted` / `AnswerAccepted` 事件有明确的订阅方。

> 一句话：模块数量对齐"**本 Sprint 真的会写代码的上下文**"，不是对齐便利贴的数量。

### 为什么模块名带 `co-` 前缀

`co` = CampusOverflow。避免与 Spring 官方模块（`spring-boot-*`）或将来接入的第三方 jar 撞名；
同时 `mvn` 构建输出里一眼能看出"这是我自己的上下文"。

### 被否掉的备选

| 备选方案 | 否掉的理由 |
|---|---|
| 单模块 + 包分层（`controller/service/repository/domain`） | 编译期无法阻止"问答上下文直接 new 一个 User"；跨上下文调用会悄悄变成同进程内的方法调用，Lab 3 的边界白画了 |
| 每个聚合一个模块（8 个以上） | 聚合之间的强关联（Question ↔ Answer）会被模块边界切断，反而要写更多转换代码；模块过多也会拖慢构建 |
| 微服务（每个上下文一个进程） | 课程项目 5 人 2 学时，分布式事务、服务发现、链路追踪的成本远大于收益；物理分包已经能拿到 90% 的边界收益 |
| 按技术分模块（`web`、`service`、`dao`） | 这是"分层"不是"分界"，业务边界仍然不可见；改一个聚合要动三个模块 |

---

## 3. 决策二：模块内部固定四层，依赖方向单向

### 结构（以 co-qa 为例）

```
co-qa/src/main/java/com/campusoverflow/qa/
├── domain/                    ← 最内层，零框架依赖
│   ├── model/                 Question、Answer、Vote、Title、Body、TagSet、各 Invariants
│   ├── event/                 QuestionPosted、AnswerAccepted…
│   ├── repository/            QuestionRepository、AnswerRepository（只有接口）
│   └── port/                  AskerVerifier（跨上下文端口的接口）
├── application/               ← 用例编排，依赖 domain
│   ├── QuestionApplicationService
│   └── command/               CreateQuestionCommand、AcceptAnswerCommand
├── infrastructure/            ← 依赖 domain，实现其接口
│   └── persistence/           QuestionJpaEntity、QuestionRepositoryAdapter、Mapper
└── interfaces/               ← 依赖 application，只做 HTTP 适配（包名 interfaces，见下方说明）
    └── rest/                  QuestionController、Request/Response DTO
```

依赖规则（评审时按这条打回）：

```
interfaces ──► application ──► domain ◄── infrastructure
                                 ▲
                    shared-kernel ┘（所有模块都可以依赖，且只能依赖它）
```

- `domain` **不允许** import `org.springframework.*`、`jakarta.persistence.*`、`com.fasterxml.*`；
- `application` 允许 Spring 注解（`@Service`、`@Transactional`），但**不允许**出现 JPA 实体或 HTTP DTO；
- `infrastructure` **不允许**被 `interfaces` 直接引用；
- 跨模块只允许 `co-bootstrap` 依赖所有上下文；上下文之间**编译期零依赖**（靠 `shared-kernel` + 端口）。

> **为什么第四层叫 `interfaces` 而不是 `interface`**：`interface` 是 Java 保留字，不能作为包名。
> 我们第一次建包时用了 `com.campusoverflow.qa.interface.rest`，`mvn` 直接报
> "需要 class、interface、enum 或 record"（编译器把 `interface` 当关键字解析）。
> 于是改为复数 `interfaces`，语义不变（"接口层/适配层"），并保留这条记录避免下次再撞。

### 为什么 application 可以用 Spring 注解

把 `@Service` 换成手写工厂也能做到，但收益只是"纯度"，代价是每个用例都要在装配层手写一个 `@Bean`。
我们选择"domain 绝对纯净、application 允许框架注解"的折中：
真正容易写错的是领域规则（被 JPA 注解污染后无法脱离数据库测试），而不是 `@Service` 这一行。

### 怎么证明依赖方向没被破坏

- `co-qa/pom.xml` 只依赖 `co-shared-kernel`，不依赖 `co-identity` → 想 `import User` 会直接编译失败；
- 集成测试里 `co-bootstrap` 用 `IdentityAskerVerifier` 把两者接起来，说明边界是"真的能拆"。

---

## 4. 决策三：领域模型与持久化模型**分离**（两套类 + 一个 Mapper）

### 决策

`Question`（领域）与 `QuestionJpaEntity`（持久化）是两个类，中间由 `QuestionPersistenceMapper` 转换。
**不是**在 `Question` 上直接挂 `@Entity`。

### 理由

| 维度 | 分离（我们的选择） | 共用（备选） |
|---|---|---|
| 聚合能否保护不变式 | 能：`Question` 只有工厂方法和业务方法，没有无参构造 + setter | 难：JPA 要求无参构造，反射能绕过校验，等于给不变式开了一个后门 |
| 单元测试 | `new Question(...)` 纯内存，毫秒级 | 需要 Spring 上下文或 H2 |
| 表结构变更 | 只改 `*JpaEntity` 和 Mapper | 领域对象跟着表改，业务语义被数据库牵着走 |
| 标签、枚举这类"存储形态 ≠ 模型形态"的字段 | Mapper 里 `TagSet` ↔ CSV 自由转换 | 需要 `@Convert`、`@Enumerated`，把持久化细节塞进领域 |
| 代码量 | 多一个 Mapper（每个聚合约 40 行） | 少一个类 |
| 学习成本 | 需要理解"同一个概念有两个表示" | 直观 |

对 5 人 2 学时的 Lab 4 来说，多写 40 行 Mapper 换来"不变式不会被反射绕过"，这笔账是划算的；
更关键的是它让 Lab 3 的 `INV-Q-01 ~ INV-Q-05` **在代码里真的存在**，而不是只写在文档里。

### 具体做法（以 Question 为例）

```java
// domain：没有无参构造，没有 setter，只有业务方法
public static Question post(QuestionId id, UserId askerId, Title title, Body body, TagSet tags, Instant now)
public boolean acceptAnswer(AnswerId answerId, UserId operatorId, Instant now)
public void hide(String reason, Instant now)

// infrastructure：只有这里知道表长什么样
@Entity @Table(name = "question")
public class QuestionJpaEntity { ... private String tags; ... }
```

Mapper 还原时走 `Question.rehydrate(...)`：它**不校验、不发事件**，因为库里的数据在写入时已经合法，
重复校验只会在历史数据上炸出假异常。

### 更新时的一个坑（记录下来，避免下次踩）

不能用 `new QuestionJpaEntity(id)` 去 `merge` 更新——实体的 `@Version` 是 0，
Hibernate 会把它当成"过期版本"，直接抛 `OptimisticLockException`。
正确做法是先 `findById` 拿到受管实体，再 `entity.apply(...)`：

```java
QuestionJpaEntity entity = jpa.findById(id).orElseGet(() -> new QuestionJpaEntity(id));
QuestionPersistenceMapper.apply(question, entity);   // 写字段，不新建对象
return QuestionPersistenceMapper.toDomain(jpa.save(entity));
```

---

## 5. 决策四：`UserId` 放共享内核，其余值对象各归各的上下文

`UserId` 被 Question / Answer / Vote / Reputation 同时引用，属于典型的**发布语言（Published Language）**，
所以放 `co-shared-kernel` 的 `shared.identity` 包。

但注意边界：**只共享 ID，不共享 User 对象，也不共享 UserStatus**。
问答上下文要判断"这个人能不能发问题"时，走 `AskerVerifier` 端口，由装配层适配到身份上下文：

```java
// co-qa/domain/port/AskerVerifier.java —— 只有一句话
void ensureCanPublish(UserId askerId);

// co-bootstrap/integration/IdentityAskerVerifier.java —— 真正的翻译发生在边界上
User user = userApplicationService.find(askerId).orElseThrow(...);
user.assertCanPublish();   // INV-U-02
```

这样 `co-qa` 的 pom 里没有 `co-identity`，将来身份上下文拆成独立服务时，
只需要把这个适配器换成 HTTP 客户端，问答上下文一行不用改。

同理，`Tag` 与 `TagVocabulary` 放共享内核（Lab 3 的 SK-01），因为问答上下文写入标签、发现上下文读取标签，
两边必须用同一套词表口径。

---

## 6. 决策五：跨上下文不建数据库外键，同上下文保留

| 关系 | 是否建外键 | 理由 |
|---|---|---|
| `answer.question_id → question.id` | ✅ 建 | 同一上下文、同一聚合簇，删问题必须先处理回答，外键是有用的护栏 |
| `question.asker_id → user.id` | ❌ 不建 | 跨上下文。外键会把"身份"和"问答"焊死在同一套 schema 上，将来单独拆库/拆服务要先删外键；用户注销也不应该连带删问题（内容需要留档） |
| `answer.author_id → user.id` | ❌ 不建 | 同上 |

一致性由应用层保证：发布问题前 `AskerVerifier` 会确认用户存在且 `active`。
代价是**可能出现悬挂引用**（用户被物理删除），所以身份上下文只做"禁用"不删行（`INV-U-04` 的 disabled 就是终态）。

---

## 7. 决策六：Flyway 脚本归 co-bootstrap 统一管理

`db/migration/V1__init_schema.sql` 放在 `co-bootstrap/src/main/resources`，而不是各上下文模块。

理由：

1. **一个数据库只有一个迁移历史**。如果 6 个模块各带一份 `db/migration`，Flyway 的版本号会互相冲突，
   排序依赖 `classpath:` 扫描顺序，行为不可预期。
2. **建表顺序跨上下文**：`question` 与 `answer` 在同一个脚本里，外键才能一起建。
3. **迁移是部署关注点**，和"启动哪个 profile、连哪个库"同属装配层的职责。

代价：改表时要跨模块改文件。因为表本来就不该频繁改（改表 = 改契约），可以接受。

`V1__init_schema.sql` 只建 `user`、`question`、`answer` 三张表——正好对应 Lab 4 Step 3 的要求。
`vote` / `question_tag` / `moderation_case` 等表随各自的用户故事（US-07 / US-10 / US-19）追加 `V2__*.sql`，
不在 Lab 4 提前建空表。

---

## 8. 决策七：乐观锁 `version` 只在持久化层，领域层不感知

`@Version` 挂在 `*JpaEntity` 上，领域对象没有 `version` 字段。

- 好处：领域模型不需要为"数据库并发控制"写任何代码，避免"领域方法里出现 `if (version != ...)`"这种技术泄漏；
- 代价：领域层拿不到版本号，无法在应用层做"读-改-写"的显式版本比对；并发冲突表现为
  `OptimisticLockingFailureException`（应用层可捕获后提示"内容已被他人修改，请刷新"）。

如果将来需要"冲突时自动重试"，再在应用服务外包一层重试切面即可，不需要动聚合。

---

## 9. 决策八（临时）：标签先存逗号分隔字符串

`question.tags` 是 `VARCHAR(512)`，Mapper 里 `TagSet` ↔ `"java,spring-boot"`。

- 为什么现在不做 `question_tag` 关联表：US-10（给问题打标签、按标签筛选）在 Sprint 3，
  Lab 4 的阶段目标是"端到端跑通"，不是"检索建模"；
- 为什么这样做是安全的：`Tag` 值对象的语法**禁止逗号**（`^[a-z0-9\u4e00-\u9fa5-]{2,20}$`），
  所以 CSV 不会产生歧义；
- 什么时候必须改：US-10 需要 `WHERE tag = ?` 走索引时。届时只需改 `V2__question_tag.sql` + Mapper，
  领域模型与接口契约不变。

---

## 10. 决策九：测试用 H2，MySQL 由本地实测覆盖

- `mvn clean install` 必须**不依赖数据库**就能全绿：单元测试只测聚合（纯内存），
  集成测试用 H2（`MODE=MySQL`）+ `ddl-auto=create-drop`；
- Flyway 的 MySQL DDL（`MEDIUMTEXT`、`DATETIME(6)`、`utf8mb4`）在 H2 上不保证兼容，因此测试关闭 Flyway；
- 真实 MySQL 上的迁移与写入，由 `docs/lab4/验证记录.md` 记录一次手工验证（含 `SELECT` 结果）。
  这一步不能省：`ddl-auto=validate` 一旦和 Flyway 脚本对不上，只有真库能发现。

> 取舍说明：没有用 Testcontainers，因为课程环境不要求 Docker，且 Windows 上首次拉镜像容易卡住；
> 用 H2 覆盖"业务链路"，用一次真实 MySQL 验证覆盖"DDL 正确性"，两件事分开做。

---

## 11. 后果

### 正面

- 模块边界在编译期生效：`co-qa` 想用 `User` 会编译失败，边界不靠自觉；
- 领域模型零框架依赖，38 个聚合单元测试 3 秒内跑完，改聚合不用起数据库；
- 新增一个上下文（如 `co-moderation`）只需加模块 + 在 bootstrap 装配，不影响既有上下文；
- 统一语言落到代码：类名/不变式编号/表名都能在 Lab 3 文档里查到出处。

### 负面 / 已知代价

- 每个聚合多一个 Mapper 和一套 JPA 实体，代码量约多 30%；
- 跨上下文查询（如"问题列表带作者昵称"）不能直接 JOIN，需要读模型投影（Sprint 2 末尾的 US-03 处理）；
- 领域对象与实体的字段要人工保持一致，漏改只能在运行期发现（用 `ddl-auto=validate` + 集成测试缓解）。

---

## 12. 复查清单（Review Checklist）

评审 PR 时按这 6 条检查：

1. 新增的类是否落在**正确的层**？`domain` 里出现 `@Entity` 或 `@Service` 直接打回；
2. 新增的跨上下文调用是否走了**端口 + 装配层适配器**？直接 `import` 另一个上下文的类打回；
3. 聚合是否**没有 setter**？状态变更是否只能通过业务方法？
4. 新增的业务规则是否对应**一条不变式编号**（`INV-*`）？没有编号说明还没想清楚；
5. 新表是否同时更新了 `V1__init_schema.sql` 或新增了 `V*__*.sql`？
6. 新用例是否有对应的**聚合单元测试**（不是只有 Controller 测试）？

---

## 13. 后续演进（写在这里，避免下次重新讨论）

| 时机 | 动作 | 触发条件 |
|---|---|---|
| Sprint 2 末（US-03/US-05） | `co-discovery` 落地 `QuestionSummaryRepository` 的 JPA 实现 | 需要问题列表分页 |
| Sprint 3（US-07） | `Vote` 从值对象升为独立聚合 + `vote` 表 | 热点问题投票写放大 |
| Sprint 3（US-10） | 拆 `question_tag` 表 | 需要按标签检索 |
| Sprint 3（US-11/US-12） | `co-reputation` 接领域事件、加持久化 | 声誉功能立项 |
| Sprint 4（US-24） | 抽 `co-moderation` 上下文 + ACL | 治理功能立项 |
| 任意时刻 | `co-identity` 拆为独立服务 | 用户量增长、需要独立扩容 |

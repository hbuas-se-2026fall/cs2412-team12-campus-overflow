# PR #5 评审记录（一）· 声誉模块与事件监听

| 项目 | 内容 |
|---|---|
| PR | <https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/pull/5> |
| 分支 | `feature/lab6-sprint3` → `main` |
| 作者 | 程靖轩（`Cheng-7-28`）：`co-reputation` 持久化、应用服务、监听器 |
| 评审人 | 陶浩峰（`7WenL7`）—— 未参与声誉模块实现（负责问答事件源） |
| 评审时间 | 第 11 周 · Sprint 3 |
| 使用清单 | 团队评审清单 v1.0 §1 正确性 / §5 性能 / §6 风格 |
| 结论 | 🔴 3 条必须改（已修复）→ **LGTM** |

---

## 评审范围

`co-reputation/src/main/java/**`、`co-bootstrap/.../integration/QaReputationListener.java`、
`V2__sprint3_schema.sql` 中声誉两张表。

---

## 评审意见（7 条）

### 🔴 1. 监听器最初放在 `co-reputation` 里，导致它反向依赖 `co-qa`

- **位置**：`co-reputation/.../listener/`（初版）
- **问题**：为了让 `@TransactionalEventListener` 能按 `QuestionPosted` 等类型匹配，作者把监听器写进了 co-reputation，
  于是 co-reputation 的 pom 里出现了 `co-qa` 依赖。这直接违反 ADR-002：**上下文之间编译期零依赖**。
- **建议**：监听器移到 `co-bootstrap/integration`（装配层是唯一同时认识两个上下文的地方），
  co-reputation 只暴露 `ReputationApplicationService`。
- **作者回复**：已迁移为 `QaReputationListener`，co-reputation 的 pom 只剩 shared-kernel + JPA/web。
- **状态**：✅ 已修复

### 🔴 2. 改判采纳时没有给旧作者扣分

- **位置**：`QaReputationListener#onAnswerAccepted`
- **问题**：只处理了新作者 +15；`AnswerAccepted` 事件里只有 `previousAcceptedAnswerId`，
  声誉侧无法反查"上一位作者是谁"，导致改判后旧作者的 +15 永远不扣。
- **建议**：事件补 `previousAnswerAuthorId`（问答侧在应用服务里能拿到），
  监听器据此写一条 `ACCEPT_REVERSED = -15` 的冲正明细。
- **作者回复**：事件与监听器均已调整，集成测试断言了 21 分与冲正路径。
- **状态**：✅ 已修复（ADR-003 §4 记录了这次"事件字段即跨模块 API"的教训）

### 🔴 3. 声誉计分没有独立事务，会拖垮投票

- **位置**：`ReputationApplicationService#apply`
- **问题**：`AFTER_COMMIT` 回调里直接用默认事务传播；一旦声誉写库失败，
  异常会冒泡回请求线程，用户看到 500——但投票其实已经提交了，体验与数据都不一致。
- **建议**：加 `@Transactional(propagation = REQUIRES_NEW)`，声誉失败只记日志、不回滚主流程。
- **状态**：✅ 已修复

### 🟡 4. 每次保存都全量遍历明细并逐条 `existsByEventId`

- **位置**：`ReputationRepositoryAdapter#save`
- **问题**：一个有 200 条明细的用户，每次加 2 分都会产生 200 次查询。
- **建议**：改为"只比对新增 entries"（应用服务记录本次新增的 eventId），或批量 `IN` 查询。
- **状态**：⏳ 已记录在 ADR-003 §10，Sprint 4 优化（当前用户明细量小，不阻塞）

### 🟡 5. `applyDelta` 的负向增量缺少边界测试

- **位置**：`ReputationAccountTest`
- **问题**：`INV-R-01` 只测了单次 `DOWNVOTE_RECEIVED`，没有测"改票 −12"这种一次扣很多的场景。
- **建议**：补 `applyDelta(eventId, rule, -12, now)` 从 5 分扣到 1 分的断言。
- **作者回复**：已在应用服务测试里补 `negativeDeltaKeepsMinimum`。
- **状态**：✅ 已修复

### 🟢 6. 幂等键由业务标识构造

- **评价**：`QP:<questionId>`、`AS:<answerId>`、`AA:<questionId>:<answerId>`、`VC:...:<delta>`，
  比"随机 UUID + 去重表"更容易排查，也满足"至少一次投递"下的幂等要求。

### 🟢 7. `AFTER_COMMIT` 的选择是对的

- **评价**：避免了"投票回滚但声誉已加分"的脏数据；配合 REQUIRES_NEW 后，失败也不会影响主流程。

---

## 结论

三条 🔴 全部是"跨模块协作"问题：**模块依赖、事件字段、事务边界**。
都不是单模块单测能发现的，正是 Lab 6 要求的"排查跨模块 BUG"。修复后 **LGTM**。

> 评审心得：评审事件相关代码时，请把事件当成 **对外 API** 看——字段少一个，下游就少一块信息。

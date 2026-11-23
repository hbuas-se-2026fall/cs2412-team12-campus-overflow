# PR #3 评审记录 · Lab 4 问答领域与接口

| 项目 | 内容 |
|---|---|
| PR | <https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/pull/3> |
| 分支 | `feature/lab4-backend` → `main` |
| 作者 | 陶浩峰（`7WenL7`）：`co-qa` 领域模型、应用服务、REST 接口 |
| 评审人 | 陈玉豪（`215cyh`）—— 未参与 `co-qa` 实现 |
| 评审时间 | 第 7 周 · Sprint 2 |
| 使用清单 | 团队评审清单 v1.0 §1 正确性 / §2 可读性 / §3 可测性 |
| 结论 | 🔴 2 条必须改（已修复）→ **LGTM** |

---

## 评审范围

`co-qa/domain/model/*`、`co-qa/application/QuestionApplicationService`、`co-qa/interfaces/rest/*`，
以及 `QuestionTest` / `AnswerTest` / `VoteTest` / `TagSetTest`。

---

## 评审意见（8 条）

### 🔴 1. `acceptAnswer` 把"操作人"当成了"被采纳回答的作者"

- **位置**：`co-qa/.../domain/model/Question.java#acceptAnswer`
- **问题**：登记 `AnswerAccepted` 事件时，`acceptedAnswerAuthorId` 传的是 `operatorId`（提问者）。
  提问者 ≠ 回答作者，声誉一旦接上就会给错人加分。
- **建议**：把 `acceptedAnswerAuthorId` 作为方法参数显式传入，由应用服务从 `Answer` 聚合取。
- **作者回复**：已改为 `acceptAnswer(answerId, acceptedAnswerAuthorId, previousAnswerAuthorId, operatorId, now)`，
  并补了断言 `assertEquals(AUTHOR, event.acceptedAnswerAuthorId())`。
- **状态**：✅ 已修复（commit `0aeb241`）

### 🔴 2. `Vote` 升为聚合后，`rehydrate` 没有出现在任何测试里

- **位置**：`co-qa/.../domain/model/Vote.java`
- **问题**：`Vote.rehydrate(...)` 只在持久化层用，测试只覆盖 `cast/revise`；
  一旦参数顺序写反，仓储回到领域时会把票投给错误的人且没有测试会红。
- **建议**：在 `VoteTest` 里补一条 `rehydrate` 往返断言（ID、目标、投票人、取值）。
- **作者回复**：已在 `QaValueObjectTest#voteHelpers` 覆盖 `rehydrate` 保留标识与取值。
- **状态**：✅ 已修复

### 🟡 3. `Body` 的长度上限用魔法数字

- **位置**：`co-qa/.../domain/model/Body.java`
- **问题**：`20000` / `10000` 直接写在方法里，`Title` 里却是常量，风格不一致。
- **建议**：提取 `QUESTION_MAX_LENGTH` / `ANSWER_MAX_LENGTH` 常量，并在 `QaValueObjectTest` 引用常量做边界断言。
- **状态**：✅ 已修复（commit `7160e83` 起使用常量）

### 🟡 4. `Question.hide` 是幂等的，但没有记录"谁操作的"

- **位置**：`co-qa/.../domain/model/Question.java#hide`
- **问题**：治理动作需要可追溯，`QuestionHidden` 事件里只有 `reason`，没有操作人。
- **建议**：Sprint 4 的 US-24 上线时给 `hide` 加 `operatorId`，并写审计日志；本期先在 ADR 记录。
- **状态**：⏳ 纳入 Sprint 4（US-24），已记录在 ADR-003 §10

### 🟡 5. 列表分页参数在控制器和聚合之间缺一层"夹紧"

- **位置**：`QuestionApplicationService#list`
- **问题**：`size=100000` 会直接打到数据库。
- **建议**：在应用服务里夹紧 `page >= 0`、`1 <= size <= 100`，并写测试。
- **作者回复**：已加夹紧与 `listClampsPaging` 测试。
- **状态**：✅ 已修复

### 🟢 6. 不变式编号直接作为异常码

- **位置**：`QuestionInvariants` / `InvariantViolationException`
- **评价**：`INV-Q-03` 这类编号贯穿文档、代码与接口响应，排查时可以直接对回 Lab 3 的聚合清单，做得很好，建议其他上下文照做。

### 🟢 7. 领域对象零框架依赖

- **评价**：`co-qa/domain` 没有任何 Spring/JPA import，聚合测试 0.05s 跑完，改领域逻辑不需要起数据库。这是本 PR 最有价值的约束。

### 🟡 8. `AnswerTest` 缺少"被采纳后仍可编辑"的正向路径

- **位置**：`co-qa/src/test/.../AnswerTest.java`
- **建议**：`INV-A-04` 写的是"不可删除，**只能编辑**"，测试只覆盖了删除被拒，没有覆盖编辑成功。
- **作者回复**：已补 `acceptedAnswerCannotBeDeleted` 中的编辑断言。
- **状态**：✅ 已修复

---

## 结论

两条 🔴 都是"事件负载错误"，属于接上声誉模块后必然爆炸的问题，作者已修复并补了断言；
其余建议中 4 条已改，2 条纳入 Sprint 4。**同意合并（LGTM）**。

> 评审心得：这个 PR 让我意识到——**事件字段就是跨模块的 API**，评审时要像看接口契约一样逐字段核对。

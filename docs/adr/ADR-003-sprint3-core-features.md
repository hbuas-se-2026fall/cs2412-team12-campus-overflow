# ADR-003 · Sprint 3 核心特性：投票投影、事件驱动声誉与可扩展标签

| 项目 | 内容 |
|---|---|
| 编号 | ADR-003 |
| 状态 | 已接受（Accepted） |
| 日期 | 第 11 周 · Sprint 3（Lab 6） |
| 决策人 | 第 12 组全员：倪昊宇（PO）、陈玉豪（SM）、刘铭洋（SM）、陶浩峰（Dev）、程靖轩（Dev） |
| 相关文档 | [ADR-002](ADR-002-package-structure.md)、[Lab 3 聚合清单](../../docs/ddd/aggregates.md)、[Sprint 3 回顾](../retro/sprint-3.md) |

---

## 1. 背景

Sprint 3 承诺交付：**投票（US-07）、采纳（US-08）、评论（US-09）、标签（US-10）、声誉（US-11/12）**。

这些功能第一次真正触碰到 Lab 3 设计里的三个"难"点：

1. **票数要不要放在 Question / Answer 行上？** 热点问题会把投票串行化；
2. **声誉怎么算？** 直接同步写在投票事务里，会让问答上下文依赖声誉上下文；
3. **标签词表能不能让用户自己造？** Lab 4 的 `INV-Q-02` 写的是"必须在 SK-01 词表内"，但 US-10 的验收标准是"不存在的新标签自动创建"。

---

## 2. 决策一：Vote 独立成聚合，票数只是目标上的投影

### 决策

- 每次投票是一行 `vote` 记录，唯一键 `(target_type, target_id, voter_id)` 保证一人一票；
- `question.vote_count` / `answer.vote_count` 是**投影值**，由目标聚合的 `applyVoteProjection(delta)` 维护；
- `Vote` 聚合只负责"谁对什么投了什么、改票净变化多少"，不负责统计。

### 为什么

如果按"票数存在问题行上 + 投票时行锁"来做，一个热门问题的所有投票会排队。拆开之后：

- 投票写入之间不互相阻塞（不同 voter 是不同行）；
- 票数允许短暂不一致（投影异步/最终一致），读多写少的场景更划算；
- 排序、展示只需要读 `vote_count`，不必 `COUNT(*)`。

### 代价

- 改票/撤票要同时改两张表（vote + 目标行），必须在同一个事务里；
- 票数可能出现漂移，需要"重算投影"的修复手段（Sprint 4 的运维任务）。

---

## 3. 决策二：声誉由事件驱动，监听器放在装配层

### 决策

```
QuestionApplicationService / VoteApplicationService
   └─ 发布领域事件（QuestionPosted / AnswerSubmitted / AnswerAccepted / VoteCast）
        └─ co-bootstrap 的 QaReputationListener（@TransactionalEventListener(AFTER_COMMIT)）
             └─ ReputationApplicationService（REQUIRES_NEW 事务）
                  └─ reputation_account / reputation_entry
```

- 监听器**不在 co-qa，也不在 co-reputation**，而是在 `co-bootstrap/integration`；
- 声誉计分使用 `REQUIRES_NEW`，与源事务解耦；
- 每条明细带 `event_id` 幂等键，数据库唯一索引兜底。

### 为什么

1. **不让问答依赖声誉**：co-qa 的 pom 里没有 co-reputation，事件是唯一的耦合；
2. **不让脏事件生效**：`AFTER_COMMIT` 保证"投票回滚了，声誉不会加分"；
3. **失败不拖累主流程**：声誉写失败不回滚投票（最终一致），符合 Lab 3 的一致性策略；
4. **可替换**：将来换 MQ，只改监听器与发布器。

### 幂等键设计

| 事件 | eventId | 说明 |
|---|---|---|
| QuestionPosted | `QP:<questionId>` | 一个问题只加一次 |
| AnswerSubmitted | `AS:<answerId>` | 一条回答只加一次 |
| AnswerAccepted | `AA:<questionId>:<answerId>` | 每次改判给新作者加一次 |
| VoteCast | `VC:<type>:<targetId>:<voterId>:<毫秒时间戳>:<delta>` | 改票/撤票各自唯一 |

> 为什么不直接用随机 UUID 做 eventId：随机值无法在"重复投递"时被识别，等于没有幂等。

---

## 4. 决策三：改判冲正用"负向明细"，不删旧明细

原来 `ReputationAccount.reverse(eventId)` 会把旧明细删掉再扣分。Sprint 3 改成：
**保留 +15 那条，再写一条 `ACCEPT_REVERSED = -15`**。

理由：

- 声誉是"账本"，删账会丢掉审计线索；
- 冲正要能被用户看到（"你被撤销了一次采纳"）；
- 幂等键天然不同（`AR:` 前缀），重复投递不会重复冲正。

为此 `AnswerAccepted` 事件新增两个字段：
`acceptedAnswerAuthorId`（给新作者 +15）与 `previousAnswerAuthorId`（给旧作者 −15）。
**这是本 Sprint 修掉的第一个跨模块 bug**：早期版本只带 `previousAcceptedAnswerId`，
声誉上下文无法反查作者，导致改判后旧作者的分永远扣不掉。

---

## 5. 决策四：标签词表从"封闭白名单"改为"可扩展词表"（修订 INV-Q-02）

### 决策

- `tag` 表成为唯一词表来源，`TagVocabulary.registerIfAbsent()` 在发帖时登记未知标签；
- 原 `application.yml` 的 `campus-overflow.tags.vocabulary` 移除，避免两处维护；
- `GET /api/v1/tags` 成为前后端共用的词表接口。

### 为什么改

Lab 2 的 US-10 验收标准明确写着"**不存在的新标签自动创建**"。Lab 4 的 `INV-Q-02`
是 Sprint 2 的保守选择（先保证口径统一）。Sprint 3 要在"统一"和"可扩展"之间取平衡，
我们选择**可扩展 + 归一化**：

- 标签仍然做小写归一化、长度 2–20、字符集限制；
- 新标签自动进入词表，下次就能被筛选与推荐；
- 真正的治理（同义词合并、敏感词）留给 Sprint 4 的管理端。

### 对被否方案的说明

| 方案 | 否决理由 |
|---|---|
| 继续白名单，新标签直接 400 | 与 US-10 验收冲突，用户会被卡住 |
| 完全自由文本，不做词表 | 无法按标签筛选，也无从治理 |
| 新标签进入"待审核队列" | 需要额外的审核界面与角色，Sprint 3 没有预算 |

> 这是一次**有意的契约修订**，已同步更新 `docs/lab4/验证记录.md` 里关于 INV-Q-02 的描述与测试用例。

---

## 6. 决策五：`question_tag` 派生索引 + 递归 CTE 回填

`question.tags` 仍以逗号分隔存储（写入简单、与 Lab 4 兼容），
另建 `question_tag(question_id, tag)` 作为**读模型**，只为"按标签筛选"服务。

- 写入：`QuestionRepositoryAdapter.save()` 中先删后插，保证与聚合一致；
- 回填：V2 迁移用 MySQL 8 的递归 CTE 把历史 CSV 拆成行（见迁移脚本注释）；
- 查询：`SELECT q.* FROM question q JOIN question_tag qt ... WHERE qt.tag = ?`，走 `(tag, question_id)` 索引。

### 踩到的坑（记录在案）

1. 回填语句最初写了 `ON DUPLICATE KEY UPDATE tag = tag`，MySQL 报
   `Column 'tag' in field list is ambiguous`（1052）——目标表与 CTE 都有 `tag` 列。
   目标表此时必然为空，直接去掉该子句即可。
2. `vote.value` 列名与 SQL 保留字冲突：MySQL 能建，**H2 建表失败**
   （`expected "identifier"`）。改名为 `vote_value`，并在实体上同步。
   教训：跨数据库的列名要避开 `value`、`key`、`order` 这类词。

---

## 7. 决策六：投票事件自带"目标作者 + 声誉增量"

`VoteCast(target, voterId, targetAuthorId, value, reputationDelta, occurredAt)`。

- `targetAuthorId`：声誉上下文不需要反查问题/回答的归属；
- `reputationDelta`：改票是 ±12、撤票是 ∓10/±2，不能只用"当前值"推断，必须由源头算好。

这样声誉侧的逻辑退化成一句 `applyDelta(userId, eventId, rule, delta)`，
不需要理解投票的业务规则。

---

## 8. 后果

### 正面

- 投票与声誉都变成"事件 + 幂等 + 可重放"的结构，Sprint 4 的排行榜可以直接复用；
- 跨上下文只通过事件耦合，co-qa 与 co-reputation 仍可独立拆分部署；
- 标签筛选走索引，列表页不再全表 LIKE；
- 50 个测试覆盖到聚合、接口与事件链（含声誉 21 分的端到端断言）。

### 负面 / 已知代价

- 票数与声誉都可能短暂滞后（最终一致），前端需要接受"刷新后一致"；
- `question_tag` 需要同步维护，若绕过仓储直接写库会漂移；
- 标签可自由创建后，词表会出现同义词/错别字，需要 Sprint 4 的治理手段；
- 事件监听在进程内同步执行（after commit），量大时需要换 MQ + 异步消费。

---

## 9. 本 Sprint 修复的跨模块 BUG（Lab 6 要求 1–2 个，实际 3 个）

| # | 现象 | 根因 | 修复 |
|---|---|---|---|
| 1 | 改判采纳后，旧作者的 +15 永远不扣 | `AnswerAccepted` 只带 `previousAcceptedAnswerId`，声誉侧无法反查作者 | 事件增加 `acceptedAnswerAuthorId` / `previousAnswerAuthorId`，监听器写 `AR:` 冲正明细 |
| 2 | 投票改判后声誉只按新值算，少扣/多扣 12 分 | 事件只带"当前值"，丢失旧值 | 事件改为携带 `reputationDelta`（新贡献 − 旧贡献） |
| 3 | 测试环境 `vote` 表建不出来，投票用例无法覆盖 | 列名 `value` 是 H2 保留字 | 列改名 `vote_value`，MySQL 迁移与实体同步 |

三个问题都不是单模块单元测试能发现的——必须跑"聚合 + 事件 + 另一个上下文"的集成链路才会暴露，
这也是 Lab 6 要求"排查跨模块协作 BUG"的意义。

---

## 10. 后续演进（Sprint 4 预留）

| 时机 | 动作 |
|---|---|
| Sprint 4（US-17/18） | 声誉明细改为只追加的事件表 + 定时重算，支持排行榜 |
| Sprint 4（US-19/24） | 治理上下文上线后，标签合并与下架走 ACL 命令 |
| 任意时刻 | 事件投递换成 MQ，`QaReputationListener` 改为消费者 |
| 性能需要时 | `question_tag` 改为投影任务异步重建，读模型独立扩容 |

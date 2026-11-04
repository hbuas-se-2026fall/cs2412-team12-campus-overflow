# 聚合识别清单 v1.0 · CampusOverflow

> **Lab 3 交付物 ③**｜事件风暴第三阶段（Software Design）产出
> 团队：第 12 组（倪昊宇 / 陶浩峰 / 陈玉豪 / 程靖轩 / 刘铭洋）｜第 5 周 · Sprint 1
> 配套图件：[`event-storming-board.png`](event-storming-board.png)（全景板）、[`context-map.png`](context-map.png)（上下文映射）、[`ubiquitous-language.md`](ubiquitous-language.md)（术语表）

## 0. 识别的判据（先说清楚"为什么是这几个"）

我们不按"数据库表"切聚合，而是按四条判据：

| 判据 | 含义 | 反例（被我们否掉的切法） |
|---|---|---|
| **一个事务只改一个聚合** | 聚合 = 一致性边界，跨聚合只能最终一致 | 把 Question 和 Answer 合成一个聚合 → 并发回答时锁冲突 |
| **不变式能在一个聚合内校验** | 需要"立刻同时成立"的规则必须同聚合 | "一人一票"如果放 Question 里，Question 会被投票打爆 |
| **聚合根是唯一的对外入口** | 外部只能拿到 ID 引用，不能持有实体 | Answer 直接改 Question 的 acceptedAnswerId → 绕过 I-4 |
| **小** | 一个聚合的写操作应能在一个事务内完成 | 把 Reputation 塞进 User → 每次投票都锁用户行 |

## 1. 聚合总览（8 个聚合 + 1 个共享内核）

| 编号 | 聚合（中/英） | 所属上下文 | 聚合根 | 与其他聚合的引用方式 |
|---|---|---|---|---|
| AGG-01 | 用户 User | 身份与访问 Identity | `User` | 其他上下文只存 `userId` |
| AGG-02 | 问题 Question | 问答核心 Q&A Core | `Question` | 持有 `acceptedAnswerId`（值，不是对象） |
| AGG-03 | 回答 Answer | 问答核心 Q&A Core | `Answer` | 持有 `questionId` |
| AGG-04 | 投票 Vote | 问答核心 Q&A Core | `Vote`（按目标+用户唯一） | 持有 `targetId` + `targetType` |
| AGG-05 | 评论 Comment | 问答核心 Q&A Core | `Comment` | 持有 `targetId` + `targetType` |
| AGG-06 | 声誉账户 Reputation | 声誉 Reputation | `ReputationAccount` | 以 `userId` 为聚合标识 |
| AGG-07 | 徽章 Badge | 声誉 Reputation | `BadgeAward` | 持有 `userId` + `badgeCode` |
| AGG-08 | 治理案件 ModerationCase | 治理 Moderation | `ModerationCase` | 持有 `targetId` + `targetType` |
| SK-01 | 标签词表 Tag Vocabulary | 共享内核（Q&A ↔ Discovery） | 非聚合（只读词表） | 双方只读引用，变更需双方同意 |

## 2. 逐个聚合

### AGG-01 · 用户 User（身份与访问）

| 项目 | 内容 |
|---|---|
| 聚合根 | `User`（标识 `UserId`） |
| 实体 | `User` |
| 值对象 | `Email`、`Nickname`、`PasswordHash`、`UserStatus`（pending / active / disabled） |
| 生命周期 | 注册 → 待验证 → 已验证 → （可）禁用 |

**关键不变式**

| ID | 不变式 | 违反时 |
|---|---|---|
| INV-U-01 | 邮箱全局唯一，且比较时忽略大小写 | 拒绝注册，提示"该邮箱已注册" |
| INV-U-02 | 未验证邮箱（`pending`）不得发布问题或回答 | 拒绝写操作，提示先验证 |
| INV-U-03 | 密码 ≥ 8 位且同时含字母与数字；只存哈希 | 拒绝注册 |
| INV-U-04 | 状态单向流转：pending→active→disabled，不可回退 | 拒绝非法流转 |

**命令 / 事件**：`RegisterAccount`→`AccountRegistered`、`VerifyEmail`→`EmailVerified`、`SignIn`→`SignedIn`、`DisableUser`→`UserDisabled`

**事务边界**：单条 `user` 记录 + 验证令牌（令牌放独立表，允许最终一致）。

---

### AGG-02 · 问题 Question（问答核心）

| 项目 | 内容 |
|---|---|
| 聚合根 | `Question`（标识 `QuestionId`） |
| 值对象 | `Title`、`Body`（Markdown 原文）、`TagSet`（≤5）、`AcceptedAnswerId`（可空）、`QuestionStatus`（open / solved / hidden） |

**关键不变式**

| ID | 不变式 | 为什么必须在聚合内 |
|---|---|---|
| INV-Q-01 | 标题 5–150 字，正文 ≤ 20000 字 | 单条记录即可校验 |
| INV-Q-02 | 标签 2–20 字，最多 5 个，且必须存在于 SK-01 词表 | 保证检索口径统一 |
| INV-Q-03 | 只有提问者本人可以采纳 | 校验 `askerId == operatorId` |
| INV-Q-04 | **最多一个被采纳答案；改判必须记录 `previousAcceptedAnswerId`** | 声誉冲正需要它，否则无法回溯 |
| INV-Q-05 | `hidden` 状态的问题不可再被回答、不可被投票 | 状态与写操作强一致 |

**命令 / 事件**：`PostQuestion`→`QuestionPosted`、`EditQuestion`→`QuestionEdited`、`AcceptAnswer`→`AnswerAccepted`（携带 `previousAcceptedAnswerId`）、`HideQuestion`→`QuestionHidden`

**边界说明**：`Answer` **不**是 Question 内部的实体集合 —— 一旦放进来，每次发回答都要锁问题行，且回答数量无上限会让聚合无限膨胀。Question 只保存 `acceptedAnswerId` 这一个"指针"，采纳的一致性靠"先校验 ID 属于本问题"的应用服务完成。

---

### AGG-03 · 回答 Answer（问答核心）

| 项目 | 内容 |
|---|---|
| 聚合根 | `Answer`（标识 `AnswerId`） |
| 值对象 | `Body`、`QuestionId`（弱引用）、`AnswerStatus`（normal / hidden） |

**关键不变式**

| ID | 不变式 |
|---|---|
| INV-A-01 | 内容非空，≤ 10000 字 |
| INV-A-02 | 引用的 `questionId` 必须存在且状态为 `open` / `solved`（不能是 `hidden`） |
| INV-A-03 | 同一作者对同一问题 60 秒内不得重复提交（防抖 + 服务端限流兜底） |
| INV-A-04 | 被采纳的回答不可删除，只能编辑 |

**命令 / 事件**：`SubmitAnswer`→`AnswerSubmitted`、`EditAnswer`→`AnswerEdited`、`DeleteAnswer`→`AnswerDeleted`

---

### AGG-04 · 投票 Vote（问答核心）

| 项目 | 内容 |
|---|---|
| 聚合根 | `Vote`，**聚合标识 = (targetType, targetId, voterId)** |
| 值对象 | `VoteValue`（UP / DOWN）、`TargetType`（QUESTION / ANSWER） |

**关键不变式**

| ID | 不变式 |
|---|---|
| INV-V-01 | 一人对同一目标最多一票（唯一键兜底，防并发双击） |
| INV-V-02 | 不能给自己的内容投票 |
| INV-V-03 | 改票 = 先撤销旧票再投新票，票数净变化为 2 |
| INV-V-04 | 目标被下架后不再接受投票 |

**为什么把 Vote 单独成聚合**：如果票数存在 Question 上并由 Question 加锁，热点问题会把所有投票串行化。拆开后 Question 的 `voteCount` 变成**投影值**（由事件异步累加），允许短暂不一致。

---

### AGG-05 · 评论 Comment（问答核心）

| 项目 | 内容 |
|---|---|
| 聚合根 | `Comment`（标识 `CommentId`） |
| 不变式 | INV-C-01 内容 ≤ 500 字；INV-C-02 目标必须存在；INV-C-03 仅作者可删自己的评论 |

**命令 / 事件**：`PostComment`→`CommentPosted`、`DeleteComment`→`CommentDeleted`

> 评论不做盖楼、不做回复，**明确砍掉**：盖楼会让 Comment 退化成树，属于"评论系统"而不是"问答系统"。

---

### AGG-06 · 声誉账户 Reputation（声誉）

| 项目 | 内容 |
|---|---|
| 聚合根 | `ReputationAccount`（标识 `userId`），内部含明细 `ReputationEntry[]` |
| 值对象 | `Score`（≥1）、`RuleCode`（QUESTION_POSTED / ANSWER_ACCEPTED / UPVOTE …）、`EventId`、`Version` |

**关键不变式**

| ID | 不变式 | 落地手段 |
|---|---|---|
| INV-R-01 | 分数永不低于 1 | `Score.max(1)`，扣分时不写负数 |
| INV-R-02 | 同一 `eventId` 只计一次（事件至少一次投递） | 明细表 `(eventId)` 唯一 |
| INV-R-03 | 采纳改判需"冲正"：先扣回上一位作者 +15，再给新作者 +15 | 依赖 INV-Q-04 的 `previousAcceptedAnswerId` |
| INV-R-04 | 重算按 `version` 覆盖而非累加 | 重放时带 version，幂等 |

**命令 / 事件**：`ApplyReputationEvent`→`ReputationChanged`、`RecalculateReputation`→`ReputationChanged`

---

### AGG-07 · 徽章 Badge（声誉）

| 项目 | 内容 |
|---|---|
| 聚合根 | `BadgeAward`（标识 `(userId, badgeCode)`） |
| 不变式 | INV-B-01 同一徽章不重复发放；INV-B-02 发放条件由规则表驱动，不写死在代码里 |

**事件**：`BadgeAwarded`

---

### AGG-08 · 治理案件 ModerationCase（治理）

| 项目 | 内容 |
|---|---|
| 聚合根 | `ModerationCase`（标识 `CaseId`） |
| 值对象 | `Reason`、`CaseStatus`（open / reviewing / removed / dismissed） |

**关键不变式**

| ID | 不变式 |
|---|---|
| INV-M-01 | 同一用户对同一内容只能举报一次（避免刷举报） |
| INV-M-02 | 累计 3 次有效举报自动立案（`open`） |
| INV-M-03 | 下架必须填写理由，且操作写审计日志，不可修改 |
| INV-M-04 | 下架动作通过**防腐层**翻译成核心域命令 `HideQuestion/HideAnswer`，核心域不认识"举报" |

---

## 3. 一致性策略（哪些强一致、哪些最终一致）

| 关系 | 一致性 | 手段 |
|---|---|---|
| Question ↔ Answer（采纳） | 最终一致 | 采纳先改 Question，再发 `AnswerAccepted` |
| Question ↔ Vote（票数） | 最终一致 | 票数走投影，允许秒级延迟 |
| Q&A → Reputation | 最终一致 | 领域事件 + 至少一次 + `eventId` 幂等 |
| Q&A → Discovery（索引） | 最终一致 | 事件投影重建读模型，可随时全量重放 |
| Moderation → Q&A（下架） | 强一致（命令） | 同步调用 + ACL，失败重试 |
| 聚合内部不变式 | 强一致 | 单事务 + 乐观锁（`version`） |

## 4. 与 Lab 4 代码结构的对应

| 聚合 | Maven 模块 | 包 |
|---|---|---|
| User | `co-identity` | `domain.model.User` |
| Question / Answer / Vote / Comment | `co-qa` | `domain.model.{Question,Answer,Vote,Comment}` |
| Reputation / Badge | `co-reputation` | `domain.model.ReputationAccount` |
| ModerationCase | `co-moderation` | `domain.model.ModerationCase` |
| Tag 词表 | `co-shared-kernel` | `shared.TagVocabulary`（只读） |

> Lab 4 只先实现 `Question` + `Answer` 两个聚合（对应 `POST /api/v1/questions`），其余按本清单顺序推进。

## 5. 事件风暴中被否掉的方案（留痕）

| 方案 | 否掉的理由 |
|---|---|
| 把 Answer 作为 Question 内部集合 | 聚合无限膨胀 + 并发回答锁冲突 |
| 票数直接存 `question.vote_count` 并加行锁 | 热点问题写放大，投票串行化 |
| 声誉分直接写在 `user` 表 | 每次投票都锁用户行，且跨上下文职责混乱 |
| 用"分类 Category"代替标签 | 分类是树、标签是网；校园问答需要多标签交叉，单选分类表达不了 |
| 举报直接调 `question.delete()` | 核心域被迫理解治理概念，违反上下文边界 |

## 6. 待办（ADR 收口）

- [ ] `ADR-001`：改采纳时声誉冲正的具体时序（先扣后加 or 一次性重算）
- [ ] `ADR-002`：票数投影的补偿机制（事件丢失如何自愈）
- [ ] `ADR-003`：标签词表的变更流程（谁审批、如何合并同义词）

# PR #5 评审记录（二）· Sprint 3 数据库迁移

| 项目 | 内容 |
|---|---|
| PR | <https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/pull/5> |
| 分支 | `feature/lab6-sprint3` → `main` |
| 作者 | 陈玉豪（`215cyh`）：`V2__sprint3_schema.sql`、标签索引与回填 |
| 评审人 | 刘铭洋（`215lmy`）—— 未参与迁移脚本编写（负责前端与持久化评审视角） |
| 评审时间 | 第 11 周 · Sprint 3 |
| 使用清单 | 团队评审清单 v1.0 §1 正确性 / §5 性能 / §6 风格 |
| 结论 | 🔴 2 条必须改（已修复）→ **LGTM** |

---

## 评审范围

`co-bootstrap/src/main/resources/db/migration/V2__sprint3_schema.sql` 全文（建表、ALTER、种子数据、历史回填）。

---

## 评审意见（6 条）

### 🔴 1. 回填语句 `ON DUPLICATE KEY UPDATE tag = tag` 列名歧义，迁移直接失败

- **位置**：`V2__sprint3_schema.sql` 末尾的历史标签回填
- **问题**：目标表 `question_tag` 与递归 CTE 都有 `tag` 列，MySQL 报
  `Column 'tag' in field list is ambiguous (1052)`。更麻烦的是 DDL 不是事务性的，
  迁移失败后库里留下了半成品（表建了一半、`flyway_schema_history` 有一条 success=0）。
- **建议**：目标表此时必然为空（V2 刚建），直接删除该子句；同时在验证记录里写清"失败后如何清理再重跑"。
- **作者回复**：已删除子句；验证记录补了清理步骤（drop 半成品表 + 删 version=2 的失败记录）。
- **状态**：✅ 已修复

### 🔴 2. `vote.value` 是 H2 保留字，测试环境建表失败

- **位置**：`V2` 的 `vote` 表
- **问题**：MySQL 能建，但 H2 2.x 在 `create table vote (... value varchar(8) ...)` 处报
  `expected "identifier"`。由于当时还没有投票用例，测试没变红，隐患一直埋到 Sprint 3 才炸。
- **建议**：列名改为 `vote_value`，MySQL 迁移与 JPA 实体同步；并在评审清单 §5 加一条"列名避开 SQL 保留字"。
- **状态**：✅ 已修复（列名 `vote_value`，实体 `VoteJpaEntity` 同步）

### 🟡 3. 没有回滚脚本

- **位置**：整份 V2
- **问题**：Flyway 社区版不做 undo；迁移一旦失败只能手工清理。
- **建议**：团队约定"**只追加、向前修复**"：不改历史脚本，出问题用 V3 修正；失败清理步骤写进验证记录。
- **状态**：✅ 已写入 ADR-003 §6 与 Lab 7 评审清单 §6

### 🟡 4. 跨上下文外键的边界要在 SQL 里说清楚

- **位置**：`question_tag` 有外键到 `question`，但 `reputation_entry.user_id` 没有外键
- **问题**：新人容易以为"漏了"，其实是有意为之（跨上下文不建外键，见 ADR-002 §6）。
- **建议**：在脚本注释里显式写明"为什么这里没有外键"，减少误改。
- **状态**：✅ 已补注释（V2 文件头"约定"段）

### 🟡 5. 回填依赖 MySQL 8 递归 CTE

- **位置**：`WITH RECURSIVE seq ...`
- **问题**：MySQL 5.7 不支持，若老师环境是 5.7 会直接失败。
- **建议**：在文件头注明"最低 MySQL 8.0"，并在 README 的数据库要求里同步。
- **状态**：✅ 已注明（README 要求 MySQL 8.x）

### 🟢 6. 标签种子数据是幂等的

- **评价**：`INSERT ... ON DUPLICATE KEY UPDATE label = VALUES(label)`，重复执行不会报错也不会产生重复词条，
  比"先查再插"的脚本更抗并发。

---

## 结论

两条 🔴 都来自"只在一种数据库上试过"：一个 MySQL 语法坑、一个 H2 保留字坑。
修复后迁移在**空库**与**已有数据**两种状态都能一次成功（Lab 6 验证记录 §2）。
**同意合并（LGTM）**，并把"迁移双环境验证"写成 Sprint 4 的强制动作。

> 评审心得：迁移脚本的评审不能只看"SQL 像不像"，要在**空库**和**有数据**两种库上各跑一次。

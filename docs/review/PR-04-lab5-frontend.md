# PR #4 评审记录 · Lab 5 前端工程与 Sprint 3 交互

| 项目 | 内容 |
|---|---|
| PR | <https://github.com/hbuas-se-2026fall/cs2412-team12-campus-overflow/pull/4> |
| 分支 | `feature/lab5-frontend` → `main` |
| 作者 | 刘铭洋（`215lmy`）：前端工程、页面、TanStack Query hooks |
| 评审人 | 程靖轩（`Cheng-7-28`）—— 未参与前端页面实现（负责后端 identity 与 API 层评审视角） |
| 评审时间 | 第 9 周 · Sprint 2 末 |
| 使用清单 | 团队评审清单 v1.0 §3 可测性 / §5 性能 / §6 风格 |
| 结论 | 🔴 2 条必须改（已修复）→ **LGTM** |

---

## 评审范围

`frontend/src/**`（页面、hooks、API 封装、生成客户端）、`frontend/e2e/**`、`frontend/package.json`。

---

## 评审意见（7 条）

### 🔴 1. 生成的 axios 客户端 `import 'form-data'`，浏览器工程装不了

- **位置**：`frontend/src/api/generated/core/request.ts`
- **问题**：`openapi-typescript-codegen` 的模板写死了 Node 专用包；`pnpm typecheck` 直接报
  `Cannot find module 'form-data'`，且 Vite 构建也无法解析。
- **建议**：不要手改生成文件——加一个生成后补丁脚本（`scripts/patch-generated.mjs`），
  把 `form-data` 换成 `globalThis.FormData`，并挂到 `api:generate` 后面。
- **作者回复**：已按建议实现补丁脚本，`pnpm api:generate` 会自动执行。
- **状态**：✅ 已修复

### 🔴 2. 投票 mutation 的变量类型用了裸字符串，和生成的枚举不兼容

- **位置**：`frontend/src/features/questions/hooks.ts#useVote`
- **问题**：`{ targetType: 'question' | 'answer' }` 无法赋给生成模型里的枚举类型，
  TypeScript 严格模式下编译失败。
- **建议**：直接用 `CastVoteRequest.targetType.QUESTION / ANSWER` 与 `CastVoteRequest.value.UP / DOWN`。
- **状态**：✅ 已修复

### 🟡 3. 详情页为每个作者单独发一次声誉请求

- **位置**：`frontend/src/features/questions/components/ReputationBadge.tsx`
- **问题**：一个问题 + N 条回答会触发 N+1 次 `GET /users/{id}/reputation`。当前数据量小看不出来，
  列表页一旦也用这个组件就会放大。
- **建议**：短期用 `staleTime` 抑制重复请求（已做）；中期让后端在问题详情里返回作者声誉快照，
  或提供批量接口（`?ids=`）。已记录到 Sprint 4 backlog。
- **状态**：⏳ 纳入 Sprint 4

### 🟡 4. 评论按钮的可访问名是「评 论」，E2E 选择器容易碎

- **位置**：`frontend/src/features/questions/components/CommentsPanel.tsx`
- **问题**：antd 会在两个中文字之间插空格，`getByRole('button', { name: '评论' })` 匹配不到；
  我们已经被这个坑卡过一次。
- **建议**：给按钮加 `aria-label="发表评论"`，E2E 用稳定的可访问名，而不是正则绕。
- **状态**：⏳ 本期用正则 `/评\s*论/` 规避，下个 PR 加 aria-label

### 🟡 5. `NewQuestionPage` 的标签选项硬编码在常量里

- **位置**：`frontend/src/shared/constants/tags.ts`
- **问题**：后端 Sprint 3 已经把词表改成可扩展（US-10），前端却还是写死 20 个，会出现"后端能收、前端选不到"。
- **建议**：改为 `GET /api/v1/tags`，常量只作为接口失败时的兜底。
- **作者回复**：已改为优先用 `useTags()`，常量仅兜底。
- **状态**：✅ 已修复（Lab 6 期间）

### 🟢 6. 查询键工厂 + 失效策略

- **位置**：`frontend/src/features/questions/hooks.ts`
- **评价**：`questionKeys` / `answerKeys` / `commentKeys` 统一生成，投票、回答、评论后分别失效对应前缀，
  没有出现"改了数据但列表不刷新"的经典问题。建议其他页面沿用这个模式。

### 🟢 7. 端到端用例是可执行的验收标准

- **位置**：`frontend/e2e/lab5.spec.ts`
- **评价**：直接按用户旅程写（发问 → 详情 → 列表），比测试报告里的"点击了按钮"截图更有说服力。

---

## 结论

两条 🔴 都属于"生成产物与浏览器环境不兼容"，会直接卡住 CI；作者已修复并加了补丁脚本。
性能与可访问性的两条建议已登记到 Sprint 4，**同意合并（LGTM）**。

> 评审心得：**生成代码不是免评审区**——恰恰因为不手改，才更要在流水线里保证它是对的。

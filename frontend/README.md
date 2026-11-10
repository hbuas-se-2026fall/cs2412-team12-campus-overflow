# CampusOverflow 前端（Lab 5）

> 《软件工程》课程项目 · 第 12 组 · Sprint 2 · 第 9 周
>
> **React 18 + TypeScript 5 + Vite 6** 的校园问答前端，已与 Lab 4 的 Spring Boot 后端端到端贯通：
> 提问 → 列表显示 → 详情查看。

| Lab 5 提交物                             | 实现                                                           | 状态 |
| ---------------------------------------- | -------------------------------------------------------------- | ---- |
| ① 前端工程 `pnpm dev` 可启动，首页可访问 | Vite 6 + React 18，`/questions` 为首页                         | ✅   |
| ② `pnpm lint` 与 `pnpm typecheck` 全绿   | ESLint 9 + tsc `--noEmit`（strict + noUncheckedIndexedAccess） | ✅   |
| ③ pre-commit 钩子可拦截不合规提交        | husky + lint-staged + typecheck                                | ✅   |
| ④ 提问 → 列表 → 详情 端到端跑通          | Playwright 驱动本机 Edge，含录像与截图                         | ✅   |

证据见 [`docs/lab5/验证记录.md`](../docs/lab5/验证记录.md)。

---

## 1. 技术栈

| 项         | 版本                                        | 说明                                                  |
| ---------- | ------------------------------------------- | ----------------------------------------------------- |
| React      | 18.3                                        | 函数组件 + Hooks，无 class 组件（ErrorBoundary 除外） |
| TypeScript | 5.6                                         | `strict` + `noUncheckedIndexedAccess`                 |
| Vite       | 6.4                                         | 开发服务器 + 构建，`@/` 别名                          |
| 路由       | react-router-dom 6                          | `/questions`、`/questions/new`、`/questions/:id`      |
| 服务器状态 | @tanstack/react-query 5                     | 列表/详情的缓存、失效与重取                           |
| 客户端状态 | zustand 5                                   | 会话（当前用户）与全局 Loading                        |
| 请求       | axios + openapi-typescript-codegen          | 由 `docs/api/openapi.yaml` 生成类型与服务             |
| UI         | Ant Design 5                                | Layout / Menu / Form / Select / List / Card           |
| 质量       | ESLint 9 + Prettier 3 + husky + lint-staged | pre-commit 拦截                                       |
| E2E        | @playwright/test + 本机 Edge                | 录像、截图、trace                                     |

---

## 2. 目录结构

```
frontend/
├── src/
│   ├── api/
│   │   ├── generated/          # openapi-typescript-codegen 产物（不手改）
│   │   └── http.ts             # axios 拦截器、全局 Loading、统一错误 AppApiError
│   ├── features/questions/
│   │   └── hooks.ts            # useQuestions / useQuestion / useCreateQuestion
│   ├── pages/
│   │   ├── NotFoundPage.tsx
│   │   └── questions/
│   │       ├── QuestionListPage.tsx
│   │       ├── NewQuestionPage.tsx
│   │       └── QuestionDetailPage.tsx
│   ├── shared/
│   │   ├── components/ErrorBoundary.tsx
│   │   ├── constants/tags.ts   # 与后端 SK-01 词表对齐
│   │   ├── layout/AppShell.tsx # Header + Sidebar + Outlet
│   │   ├── lib/queryClient.ts  # QueryClient 全局配置
│   │   ├── lib/format.ts
│   │   └── store/              # uiStore（Loading）、sessionStore（当前用户）
│   ├── App.tsx                 # 路由表
│   ├── main.tsx                # Provider 装配
│   └── index.css
├── e2e/lab5.spec.ts            # 端到端用例
├── scripts/
│   ├── patch-generated.mjs     # 生成后补丁：去掉 Node 专用 form-data
│   └── install-git-hooks.mjs   # 把 core.hooksPath 指向仓库根 .husky
├── playwright.config.ts
├── eslint.config.js
├── tsconfig.json
└── vite.config.ts
```

---

## 3. 快速开始

前置：**先启动 Lab 4 后端**（默认 `http://localhost:8080`，MySQL 已建库）。

```bash
# 1) 安装依赖（会自动启用 pre-commit 钩子）
cd frontend
pnpm install

# 2) 启动开发服务器
pnpm dev
# 打开 http://localhost:5173 —— 会自动跳转到 /questions
```

> 首次进入页面时，前端会自动注册并验证一个"演示同学"账号（Sprint 2 还没有 US-02 登录），
> 这样提问表单的 `askerId` 才有值。账号会存在 localStorage，刷新不会重复注册。

### 常用脚本

| 命令                | 作用                                                     |
| ------------------- | -------------------------------------------------------- |
| `pnpm dev`          | 启动开发服务器（5173，`/api` 代理到 8080）               |
| `pnpm build`        | 类型检查 + 生产构建到 `dist/`                            |
| `pnpm preview`      | 预览生产构建                                             |
| `pnpm lint`         | ESLint（0 warning 才通过）                               |
| `pnpm typecheck`    | `tsc --noEmit`                                           |
| `pnpm format`       | Prettier 格式化                                          |
| `pnpm api:generate` | 由 `../docs/api/openapi.yaml` 重新生成客户端（含补丁）   |
| `pnpm e2e`          | Playwright 端到端（自动拉起 dev server，需要后端已启动） |

---

## 4. TypeScript 严格模式

`tsconfig.json` 的关键项：

```jsonc
"strict": true,
"noUncheckedIndexedAccess": true,   // 数组下标访问返回 T | undefined
"noUnusedLocals": true,
"noUnusedParameters": true
```

- 业务代码 **零 `any`**：ESLint 规则 `@typescript-eslint/no-explicit-any: error`；
- 唯一例外是 `src/api/generated/**`（生成产物），已在 ESLint 中 ignore；
- `noUncheckedIndexedAccess` 会逼着你在取数组/对象下标时先判空，例如列表分页参数、URL 参数。

---

## 5. 两种状态严格分开

| 状态类型   | 归属           | 例子                                 |
| ---------- | -------------- | ------------------------------------ |
| 服务器状态 | TanStack Query | 问题列表、问题详情、创建后的缓存失效 |
| 客户端状态 | Zustand        | 当前登录用户、全局 Loading 计数      |

页面里没有用 `useState` 保存"从后端取来的数据"，所以不存在把 props 一层层往下传的问题。

---

## 6. API 客户端

1. 后端契约写在 [`docs/api/openapi.yaml`](../docs/api/openapi.yaml)；
2. `pnpm api:generate` 用 `openapi-typescript-codegen` 生成 `src/api/generated/`（TS 类型 + 请求函数）；
3. 生成的 axios 模板会 `import FormData from 'form-data'`（Node 专用包），浏览器里装不了，
   所以 `scripts/patch-generated.mjs` 会在生成后自动替换成原生 `FormData`——**不要手改 generated**，改脚本；
4. `src/api/http.ts` 统一封装：
   - 请求拦截器：全局 Loading +1、注入 `Authorization: Bearer <token>`（token 存在 localStorage，Sprint 3 接 US-02 后启用）；
   - 响应拦截器：Loading −1，网络层错误弹一次全局提示；
   - `call()` 把生成的 `ApiError` 翻译成 `AppApiError`（`code / message / traceId / details`），页面按 `code` 分支。

---

## 7. 路由与页面

| 路由             | 页面                                     | 数据                       |
| ---------------- | ---------------------------------------- | -------------------------- |
| `/`              | 重定向到 `/questions`                    | —                          |
| `/questions`     | 问题列表（分页、空状态、错误重试）       | `useQuestions(page, size)` |
| `/questions/new` | 发布问题（标题/正文/标签，成功后跳详情） | `useCreateQuestion()`      |
| `/questions/:id` | 问题详情（标题、标签、正文、状态、时间） | `useQuestion(id)`          |
| `*`              | 404                                      | —                          |

---

## 8. pre-commit 钩子

前端在仓库的 `frontend/` 子目录，但 git 钩子只能挂在仓库根，所以：

- 钩子文件：仓库根 `.husky/pre-commit`
- `pnpm install` 时 `scripts/install-git-hooks.mjs` 会把 `core.hooksPath` 指向 `.husky`
- 钩子内容：`cd frontend` → `lint-staged --relative`（只查暂存区）→ `pnpm typecheck`（全量）

验证方式：把一个含 `any` 的文件加入暂存区并 commit，应当被 ESLint 拦下（提交失败）。实测记录见验证文档。

---

## 9. 端到端测试

```bash
# 1) 后端（另开终端）
java -jar co-bootstrap/target/campus-overflow.jar

# 2) 前端 E2E（Playwright 会自动拉起 dev server）
cd frontend
pnpm e2e
```

- 浏览器用本机 **Edge**（`channel: 'msedge'`），不下载 Playwright 自带 Chromium；
- 用例覆盖：表单就绪 → 填写 → 选标签 → 发布 → 跳详情 → 回列表可见；
- 产物：`test-results/` 下的 `video.webm`、`test-finished-*.png`、失败时的 `trace.zip`。

---

## 10. 已知边界（Lab 5 范围内刻意不做）

| 项                         | 原因                                                        | 何时做            |
| -------------------------- | ----------------------------------------------------------- | ----------------- |
| 登录 / 注册页面            | US-02 在 Sprint 2 末尾；当前用自动演示账号代替              | Sprint 2 末       |
| 回答、投票、采纳的前端交互 | 后端对应接口尚未全部开放（US-06/07/08）                     | Sprint 3          |
| 标签从后端拉取             | 词表暂时硬编码在 `shared/constants/tags.ts`，与后端配置对齐 | Sprint 3（US-10） |
| Markdown 渲染              | 当前按纯文本展示（保留换行），编辑器/预览属 US-04 增强      | Sprint 3          |
| 国际化                     | 只做中文                                                    | 不做              |

---

## 11. 常见问题

| 现象                                         | 原因                       | 处理                                                                            |
| -------------------------------------------- | -------------------------- | ------------------------------------------------------------------------------- |
| 表单一直是禁用状态                           | 演示账号还没创建成功       | 看浏览器 Network：`POST /api/v1/users` 是否 201；确认后端已启动                 |
| 请求 404 / 连接失败                          | 后端没起或端口不对         | `VITE_PROXY_TARGET=http://localhost:8080` 后重启 `pnpm dev`                     |
| `form-data` 类型报错                         | 重新生成客户端后补丁没跑   | `pnpm api:generate`（会链式执行 `patch-generated.mjs`）                         |
| `pnpm install` 报 `ERR_PNPM_IGNORED_BUILDS`  | pnpm 12 默认不允许构建脚本 | 已在 `pnpm-workspace.yaml` 中允许 `esbuild`，必要时 `pnpm approve-builds --all` |
| E2E 报 `Executable doesn't exist ... ffmpeg` | 录像需要 ffmpeg            | `pnpm exec playwright install ffmpeg`                                           |

import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * 安装 git 钩子（Lab 5 Step 2）。
 *
 * 背景：前端工程在团队仓库的 `frontend/` 子目录里，而 git 钩子只能挂在仓库根。
 * 所以这里把 `core.hooksPath` 指向仓库根的 `.husky`，钩子内部再 `cd frontend` 执行检查。
 * 由 package.json 的 `prepare` 自动调用，`pnpm install` 后即生效。
 */
const here = path.dirname(fileURLToPath(import.meta.url));
const frontendDir = path.resolve(here, '..');

const git = (args, cwd) =>
  execFileSync('git', args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();

try {
  const root = git(['rev-parse', '--show-toplevel'], frontendDir);
  const hookFile = path.join(root, '.husky', 'pre-commit');
  if (!existsSync(hookFile)) {
    console.log('[install-git-hooks] 仓库根没有 .husky/pre-commit，跳过');
    process.exit(0);
  }
  git(['config', 'core.hooksPath', '.husky'], root);
  console.log('[install-git-hooks] 已启用 core.hooksPath=.husky（pre-commit 生效）');
} catch {
  console.log('[install-git-hooks] 当前目录不在 git 仓库内，跳过钩子安装');
}

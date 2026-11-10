import { defineConfig, devices } from '@playwright/test';

/**
 * Lab 5 提交物④的自动化部分：用真实浏览器（本机 Edge）跑「提问 → 详情 → 列表」。
 *
 * 前置：
 *   1. 后端已启动（默认 http://localhost:8080，MySQL 已建库）
 *   2. 前端 dev server 已启动（默认 http://localhost:5173）
 *
 * 用 channel: 'msedge' 避免下载 Playwright 自带 Chromium，课堂环境更快。
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    channel: 'msedge',
    headless: true,
    screenshot: 'on',
    video: 'on',
    trace: 'retain-on-failure',
    locale: 'zh-CN',
  },
  projects: [
    {
      name: 'msedge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' },
    },
  ],
  outputDir: 'test-results',
  // 测试自己拉起 dev server（后端仍需先启动在 8080）
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 120_000,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});

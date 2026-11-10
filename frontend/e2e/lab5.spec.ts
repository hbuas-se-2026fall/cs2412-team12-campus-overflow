import { expect, test } from '@playwright/test';

/**
 * Lab 5 提交物④：提问 → 列表显示 → 详情查看，端到端跑通。
 *
 * 断言链：
 *   1. 打开 /questions/new，演示账号就绪后表单可用
 *   2. 填写并发布，跳转到 /questions/:id
 *   3. 详情页显示标题与正文
 *   4. 回到 /questions，列表里能看到刚发布的问题
 */
test('提问 → 详情 → 列表 端到端贯通', async ({ page }) => {
  const stamp = Date.now();
  const title = `E2E 自动化问题 ${stamp}`;
  const body = `这是 Playwright 在 ${new Date(stamp).toISOString()} 发布的问题，用于验证前后端链路。`;

  await page.goto('/questions/new');

  // 演示账号是异步注册的：表单可用 = 会话已就绪
  const titleInput = page.locator('#title');
  await expect(titleInput).toBeEnabled({ timeout: 30_000 });

  await titleInput.fill(title);
  await page.locator('#body').fill(body);

  // 选一个共享词表里的标签（INV-Q-02 要求标签必须在 SK-01 内）。
  // 注意：antd Select 的选项是虚拟列表，直接点 title 会找不到未渲染的项，
  // 所以用"输入过滤 + 回车"的方式选择。
  await page.locator('#tags').click();
  await expect(page.locator('.ant-select-dropdown')).toBeVisible();
  await page.keyboard.type('react', { delay: 30 });
  await page.keyboard.press('Enter');
  await expect(
    page.locator('.ant-select-selection-item').filter({ hasText: 'react' }),
  ).toBeVisible();

  // antd 会在两个中文字之间插空格，按钮可访问名是「发 布」
  await page.getByRole('button', { name: /发\s*布/ }).click();

  // 2) 跳转详情
  await expect(page).toHaveURL(/\/questions\/[0-9a-f-]{36}$/, { timeout: 20_000 });
  const detailUrl = page.url();

  // 3) 详情内容
  await expect(page.getByRole('heading', { name: title })).toBeVisible();
  await expect(page.getByText(body)).toBeVisible();

  // 4) 列表里能看到
  await page.goto('/questions');
  await expect(page.getByText(title)).toBeVisible();
  await page.screenshot({ path: 'test-results/e2e-list.png', fullPage: true });

  // 详情页地址可回访
  await page.goto(detailUrl);
  await expect(page.getByRole('heading', { name: title })).toBeVisible();
});

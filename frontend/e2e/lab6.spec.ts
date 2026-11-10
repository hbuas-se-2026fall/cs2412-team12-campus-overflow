import { expect, test } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';

/**
 * Lab 6 端到端：回答 → 采纳 → 评论 → 投票 → 标签筛选 → 声誉。
 *
 * 投票不能投自己的内容（INV-V-02），所以通过 API 造一个"别人的问题"再投；
 * 声誉用接口断言，避免依赖页面缓存刷新时机。
 */
test('Sprint 3：回答、采纳、评论、投票、标签筛选与声誉', async ({ page, request }) => {
  const stamp = Date.now();
  const title = `Sprint3 演示 ${stamp}`;
  const answerBody = `这是回答 ${stamp}，稍后会被采纳。`;
  const commentBody = `这是评论 ${stamp}。`;

  // 1) 当前演示账号发问
  await page.goto('/questions/new');
  await expect(page.locator('#title')).toBeEnabled({ timeout: 30_000 });
  await page.locator('#title').fill(title);
  await page.locator('#body').fill('Sprint 3 演示正文：验证投票、采纳、评论与声誉。');
  await page.locator('#tags').click();
  await page.keyboard.type('react', { delay: 30 });
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: /发\s*布/ }).click();

  await expect(page).toHaveURL(/\/questions\/[0-9a-f-]{36}$/, { timeout: 20_000 });

  // 2) 回答自己的问题（业务上允许；限流只针对同一问题的重复提交）
  await page.getByPlaceholder('写下你的回答（Markdown）').fill(answerBody);
  await page.getByRole('button', { name: '提交回答' }).click();
  await expect(page.getByText(answerBody)).toBeVisible();

  // 3) 采纳
  await page.getByRole('button', { name: '采纳这条回答' }).click();
  await expect(page.getByText('已采纳', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '采纳这条回答' })).toHaveCount(0);

  // 4) 评论问题（详情页第一个评论框属于问题本身）
  await page.getByPlaceholder('补充说明（≤500 字）').first().fill(commentBody);
  await page
    .getByRole('button', { name: /评\s*论/ })
    .first()
    .click();
  await expect(page.getByText(commentBody)).toBeVisible();
  await page.screenshot({ path: 'test-results/sprint3-detail.png', fullPage: true });

  // 5) 投票：先造一条别人的问题，再投赞成
  const other = await createOtherQuestion(request);
  await page.goto(`/questions/${other.questionId}`);
  await expect(page.getByTestId('vote-count').first()).toHaveText('0');
  await page.getByRole('button', { name: '赞成' }).first().click();
  await expect(page.getByTestId('vote-count').first()).toHaveText('1');

  // 6) 标签筛选：列表页选 react，应能看到刚发的问题
  await page.goto('/questions');
  await page.locator('.ant-select').first().click();
  await page.keyboard.type('react', { delay: 30 });
  await page.keyboard.press('Enter');
  await expect(page.getByText(title)).toBeVisible();
  await page.screenshot({ path: 'test-results/sprint3-list-filtered.png', fullPage: true });

  // 7) 声誉：当前用户 = 提问 2 + 回答 5 + 被采纳 15 = 22 起（初始 1 → 23）
  const userId = await page.evaluate(() => {
    const raw = window.localStorage.getItem('campus-overflow-session');
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as { state?: { user?: { id?: string } } };
    return parsed.state?.user?.id ?? null;
  });
  expect(userId).toBeTruthy();

  const reputationResponse = await request.get(`/api/v1/users/${userId}/reputation`);
  const reputation = (await reputationResponse.json()) as { score: number };
  expect(reputation.score).toBeGreaterThanOrEqual(23);
});

async function createOtherQuestion(request: APIRequestContext): Promise<{ questionId: string }> {
  const suffix = Math.random().toString(36).slice(2, 8);
  const userResponse = await request.post('/api/v1/users', {
    data: {
      email: `other-${suffix}@hbuas.edu.cn`,
      nickname: `别人-${suffix}`,
      password: 'abc12345',
    },
  });
  const user = (await userResponse.json()) as { id: string };
  await request.post(`/api/v1/users/${user.id}/email-verification`);

  const questionResponse = await request.post('/api/v1/questions', {
    data: {
      askerId: user.id,
      title: `别人的问题 ${suffix}`,
      body: '这条问题用于验证"不能给自己投票"之外的投票路径。',
      tags: ['java'],
    },
  });
  const question = (await questionResponse.json()) as { id: string };
  return { questionId: question.id };
}

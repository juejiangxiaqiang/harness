// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：知识库模块 E2E。覆盖列表展示、状态筛选、上传流程（含非法文件拦截）
// 与上传成功后列表刷新（对照计划书 P2 验收流程）。

import { expect, test } from '@playwright/test'
import { kbFailed, kbReady, mockKnowledgeBases, seedAdminSession } from './mock'

test.beforeEach(async ({ page }) => {
  await seedAdminSession(page)
})

test('知识库列表展示与状态筛选', async ({ page }) => {
  await mockKnowledgeBases(page, [kbReady, kbFailed])
  await page.goto('/knowledge-bases')
  await expect(page.getByRole('heading', { name: '知识库' })).toBeVisible()
  await expect(page.getByText('产品手册 v2')).toBeVisible()
  // 状态标签断言限定在 .status-tag 内，避免与筛选标签组重名冲突
  await expect(page.locator('.status-tag', { hasText: '已就绪' })).toBeVisible()
  await expect(page.locator('.status-tag', { hasText: '失败' })).toBeVisible()

  // 筛选失败状态：仅剩失败条目
  await page.getByRole('tab', { name: '失败' }).click()
  await expect(page.getByText('合规制度库')).toBeVisible()
  await expect(page.getByText('产品手册 v2')).toHaveCount(0)
})

test('上传知识库成功并刷新列表', async ({ page }) => {
  await mockKnowledgeBases(page, [])
  await page.goto('/knowledge-bases')
  await expect(page.getByText('暂无知识库')).toBeVisible()

  await page.getByRole('button', { name: '上传知识库' }).click()
  await page.locator('input[type="file"]').setInputFiles({
    name: 'demo.md',
    mimeType: 'text/markdown',
    buffer: Buffer.from('# 演示内容')
  })
  await expect(page.getByText('demo.md')).toBeVisible()
  await page.getByRole('button', { name: '开始上传' }).click()
  // 上传完成关闭弹窗，列表刷新展示新条目
  await expect(page.getByText('演示上传')).toBeVisible()
})

test('非法文件类型被前端预校验拦截', async ({ page }) => {
  await mockKnowledgeBases(page, [])
  await page.goto('/knowledge-bases')
  await page.getByRole('button', { name: '上传知识库' }).click()
  await page.locator('input[type="file"]').setInputFiles({
    name: 'virus.exe',
    mimeType: 'application/octet-stream',
    buffer: Buffer.from('x')
  })
  await expect(page.getByText(/不支持 exe 格式/)).toBeVisible()
  await expect(page.getByRole('button', { name: '开始上传' })).toBeDisabled()
})

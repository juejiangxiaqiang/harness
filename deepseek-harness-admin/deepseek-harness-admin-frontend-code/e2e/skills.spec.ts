// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：Skill 创建链路 E2E。覆盖表单填写、仅 READY 知识库可勾选（FAILED 禁用）、
// 提交创建并回列表（对照计划书 P2 验收流程与 API_SPEC.md 第 4 章）。

import { expect, test } from '@playwright/test'
import { kbFailed, kbReady, mockKnowledgeBases, mockSkills, seedAdminSession } from './mock'

test.beforeEach(async ({ page }) => {
  await seedAdminSession(page)
  await mockSkills(page, [])
  await mockKnowledgeBases(page, [kbReady, kbFailed])
})

test('创建 Skill：READY 知识库可选、FAILED 禁用，提交后回列表', async ({ page }) => {
  await page.goto('/skills/new')

  // FAILED 知识库为禁用态，READY 可勾选
  const failedRow = page.locator('.panel-item', { hasText: '合规制度库' })
  await expect(failedRow).toHaveClass(/disabled/)
  const readyRow = page.locator('.panel-item', { hasText: '产品手册 v2' })
  await readyRow.click()

  // 填写必填字段
  await page.getByPlaceholder('Skill 名称').fill('制度问答')
  await page.getByPlaceholder('提示词模板 / 内容配置').fill('你是制度专家，基于知识库回答。')

  await page.getByRole('button', { name: '保存' }).click()
  // 创建成功后跳转 Skills 列表
  await expect(page).toHaveURL(/skills$/)
  await expect(page.getByRole('heading', { name: 'Skills' })).toBeVisible()
})

test('创建 Skill：未选知识库时内联提示', async ({ page }) => {
  await page.goto('/skills/new')
  await page.getByPlaceholder('Skill 名称').fill('制度问答')
  await page.getByPlaceholder('提示词模板 / 内容配置').fill('内容')
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByRole('alert')).toHaveText('请至少选择一个 READY 状态的知识库')
})

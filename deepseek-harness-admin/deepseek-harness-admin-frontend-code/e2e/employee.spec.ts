// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：数字员工创建—发布—预览链路 E2E。覆盖必填校验拦截、Skill 勾选、
// 发布成功跳转预览与版本展示（对照计划书 P3 验收流程）。

import { expect, test } from '@playwright/test'
import { mockDigitalEmployee, mockSkills, seedAdminSession } from './mock'

const skillDemo = {
  id: 201,
  name: '制度问答',
  description: '制度检索与问答',
  content: '内容',
  status: 'ACTIVE',
  version: 'v1',
  knowledgeBaseIds: [1],
  createdAt: '2026-10-07T11:00:00'
}

const employeeDetail = {
  id: 1,
  name: '售后助手',
  description: '处理售后咨询',
  systemPrompt: '你是售后服务助手，基于知识库回答用户问题。',
  modelConfig: { model: 'deepseek-chat', temperature: 0.7, maxTokens: 2048 },
  status: 'PUBLISHED',
  publishedVersion: 'v1',
  skillIds: [201],
  createdAt: '2026-10-07T12:00:00'
}

test.beforeEach(async ({ page }) => {
  await seedAdminSession(page)
  await mockSkills(page, [skillDemo])
  await mockDigitalEmployee(page, employeeDetail)
})

test('未选 Skill 时发布按钮禁用并展示校验项失败', async ({ page }) => {
  await page.goto('/digital-employees/new')
  await page.getByPlaceholder('数字员工名称').fill('售后助手')
  await page.getByPlaceholder('作为数字员工行为底座，发布后不可修改').fill('你是售后服务助手')
  // 本地校验项中 Skill 选择失败 → 发布按钮禁用
  await expect(page.getByText('✗ 已选择至少一个 Skill')).toBeVisible()
  await expect(page.getByRole('button', { name: '发布数字员工' })).toBeDisabled()
})

test('创建—发布—预览全链路', async ({ page }) => {
  await page.goto('/digital-employees/new')
  await page.getByPlaceholder('数字员工名称').fill('售后助手')
  await page.getByPlaceholder('作为数字员工行为底座，发布后不可修改').fill('你是售后服务助手')

  // 勾选 Skill 卡片
  await page.locator('.panel-item', { hasText: '制度问答' }).click()

  await page.getByRole('button', { name: '发布数字员工' }).click()
  // 发布成功跳转预览页并展示已发布版本
  await expect(page).toHaveURL(/digital-employees\/1\/preview/)
  await expect(page.getByRole('heading', { name: '数字员工预览' })).toBeVisible()
  await expect(page.locator('.info-status')).toContainText('已发布')
  await expect(page.locator('.info-status')).toContainText('v1')
})

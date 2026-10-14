// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：登录链路 E2E。覆盖登录成功跳转、错误凭据内联提示、路由守卫拦截
// （对照计划书 P1 验收流程）。

import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  // 默认登录接口返回成功
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      json: {
        code: 'SUCCESS',
        message: 'success',
        data: { token: 'e2e-token', user: { id: 1, username: 'admin', role: 'ADMIN' } },
        traceId: 'e2e'
      }
    })
  )
})

test('登录成功跳转知识库页', async ({ page }) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('admin123')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/knowledge-bases/)
  await expect(page.getByRole('heading', { name: '知识库' })).toBeVisible()
})

test('错误凭据显示内联错误且不跳转', async ({ page }) => {
  // 覆盖 beforeEach 注册的处理器：本次用例返回业务错误
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      json: { code: 'AUTH_INVALID_CREDENTIALS', message: 'bad credential', data: null, traceId: 'e2e' }
    })
  )
  await page.goto('/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('wrong')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('alert')).toHaveText('账号或密码错误')
  await expect(page).toHaveURL(/login/)
})

test('空表单提交给出内联提示', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('alert')).toHaveText('请输入账号')
})

test('未登录访问受保护页自动跳转登录', async ({ page }) => {
  await page.goto('/knowledge-bases')
  await expect(page).toHaveURL(/login/)
})

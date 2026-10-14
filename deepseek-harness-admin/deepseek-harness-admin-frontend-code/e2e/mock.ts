// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：E2E 后端接口模拟工具。以 Playwright 路由拦截返回统一响应结构
// （对应 docs/API_SPEC.md 第 1 章），使验收用例无需本地后端即可独立运行。

import type { Page } from '@playwright/test'

/** 构造统一成功响应体 */
export function ok<T>(data: T) {
  return { code: 'SUCCESS', message: 'success', data, traceId: 'e2e-trace' }
}

/** 预置管理员会话（直接写 localStorage，避免每个用例重复走登录） */
export async function seedAdminSession(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('harness-admin-token', 'e2e-token')
    localStorage.setItem(
      'harness-admin-user',
      JSON.stringify({ id: 1, username: 'admin', role: 'ADMIN' })
    )
  })
}

/** 常用演示数据：READY 与 FAILED 知识库 */
export const kbReady = {
  id: 1,
  name: '产品手册 v2',
  status: 'READY',
  fileName: 'product.md',
  chunkCount: 8,
  vectorStore: 'redis',
  embeddingModel: 'qwen',
  failureReason: null,
  createdAt: '2026-10-07T10:00:00'
}

export const kbFailed = {
  id: 2,
  name: '合规制度库',
  status: 'FAILED',
  fileName: 'policy.pdf',
  chunkCount: 0,
  vectorStore: 'redis',
  embeddingModel: 'qwen',
  failureReason: '解析失败：暂不支持该文件类型',
  createdAt: '2026-10-07T09:00:00'
}

/** 模拟知识库列表与上传接口（multipart 上传返回 READY 条目）；
 * 维护内存状态：上传成功后 GET 列表能返回新条目，贴近真实行为 */
export async function mockKnowledgeBases(page: Page, initial: unknown[]) {
  const state: unknown[] = [...initial]
  await page.route('**/api/knowledge-bases**', async (route) => {
    const req = route.request()
    if (req.method() === 'GET') {
      await route.fulfill({ json: ok({ page: 1, size: 100, total: state.length, items: state }) })
    } else {
      const created = { ...kbReady, id: 101, name: '演示上传', fileName: 'demo.md' }
      state.push(created)
      await route.fulfill({ json: ok(created) })
    }
  })
}

/** 模拟 Skill 列表与创建接口 */
export async function mockSkills(page: Page, items: unknown[]) {
  await page.route('**/api/skills**', async (route) => {
    const req = route.request()
    if (req.method() === 'GET') {
      await route.fulfill({ json: ok({ page: 1, size: 100, total: items.length, items }) })
    } else {
      const body = req.postDataJSON()
      await route.fulfill({
        json: ok({
          id: 201,
          name: body.name,
          description: body.description,
          content: body.content,
          status: 'ACTIVE',
          version: 'v1',
          knowledgeBaseIds: body.knowledgeBaseIds,
          createdAt: '2026-10-07T11:00:00'
        })
      })
    }
  })
}

/** 模拟数字员工详情、创建与发布接口 */
export async function mockDigitalEmployee(page: Page, detail: unknown) {
  await page.route('**/api/digital-employees**', async (route) => {
    const req = route.request()
    const url = req.url()
    if (req.method() === 'GET' && !url.includes('/publish') && !url.includes('/rollback')) {
      await route.fulfill({ json: ok(detail) })
    } else if (req.method() === 'POST' && url.endsWith('/publish')) {
      await route.fulfill({
        json: ok({
          id: 1,
          digitalEmployeeId: 1,
          version: 'v1',
          status: 'SUCCESS',
          failureReason: null,
          releasedAt: '2026-10-07T12:00:00'
        })
      })
    } else {
      await route.fulfill({ json: ok(detail) })
    }
  })
}

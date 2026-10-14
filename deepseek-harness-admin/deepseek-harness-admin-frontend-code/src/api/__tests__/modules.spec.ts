// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：四个业务 API 模块封装测试。隔离 request 后校验各函数拼装出的
// url/method/params/data，覆盖列表、详情、创建、上传、发布与回滚路径。

import { beforeEach, describe, expect, it, vi } from 'vitest'

// 隔离 ApiClient 的 request，验证各模块的请求参数拼装
vi.mock('../apiClient', () => ({
  request: vi.fn()
}))

import { request } from '../apiClient'
import { login, logout } from '../auth'
import { fetchKnowledgeBases, fetchKnowledgeBase, uploadKnowledgeBase, reprocessKnowledgeBase } from '../knowledgeBase'
import { fetchSkills, fetchSkill, createSkill } from '../skill'
import {
  fetchDigitalEmployees,
  fetchDigitalEmployee,
  createDigitalEmployee,
  publishDigitalEmployee,
  rollbackDigitalEmployee
} from '../digitalEmployee'

const mockedRequest = vi.mocked(request)

describe('auth 模块', () => {
  beforeEach(() => vi.clearAllMocks())

  it('登录以 POST 提交账号密码', async () => {
    mockedRequest.mockResolvedValue({ token: 't', user: { id: 1, username: 'a', role: 'ADMIN' } })
    await login({ username: 'admin', password: 'secret' })
    expect(mockedRequest).toHaveBeenCalledWith({
      url: '/auth/login',
      method: 'POST',
      data: { username: 'admin', password: 'secret' }
    })
  })

  it('登出为 POST 无请求体', async () => {
    mockedRequest.mockResolvedValue(undefined)
    await logout()
    expect(mockedRequest).toHaveBeenCalledWith({ url: '/auth/logout', method: 'POST' })
  })
})

describe('knowledgeBase 模块', () => {
  beforeEach(() => vi.clearAllMocks())

  it('列表携带 page/size 与可选名称', async () => {
    mockedRequest.mockResolvedValue({ page: 1, size: 10, total: 0, items: [] })
    await fetchKnowledgeBases({ page: 1, size: 10, name: '手册' })
    expect(mockedRequest).toHaveBeenCalledWith({
      url: '/knowledge-bases',
      method: 'GET',
      params: { page: 1, size: 10, name: '手册' }
    })
  })

  it('详情以 GET 携带 ID', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    await fetchKnowledgeBase(1)
    expect(mockedRequest).toHaveBeenCalledWith({ url: '/knowledge-bases/1', method: 'GET' })
  })

  it('上传构造 multipart 表单并回传进度', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    const file = new File(['# demo'], 'demo.md', { type: 'text/markdown' })
    const progress = vi.fn()
    await uploadKnowledgeBase(file, '演示', progress)
    const call = mockedRequest.mock.calls[0][0]
    expect(call.url).toBe('/knowledge-bases')
    expect(call.method).toBe('POST')
    expect(call.data).toBeInstanceOf(FormData)
    expect(call.data.get('file')).toBe(file)
    expect(call.data.get('name')).toBe('演示')
    expect(call.headers).toEqual({ 'Content-Type': 'multipart/form-data' })
    expect(call.onUploadProgress).toBeTypeOf('function')
  })

  it('重新向量化以 POST 提交 ID', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    await reprocessKnowledgeBase(1)
    expect(mockedRequest).toHaveBeenCalledWith({ url: '/knowledge-bases/1/reprocess', method: 'POST' })
  })
})

describe('skill 模块', () => {
  beforeEach(() => vi.clearAllMocks())

  it('创建 Skill 携带 knowledgeBaseIds', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    await createSkill({ name: '制度问答', content: '内容', knowledgeBaseIds: [1, 2] })
    expect(mockedRequest).toHaveBeenCalledWith({
      url: '/skills',
      method: 'POST',
      data: { name: '制度问答', content: '内容', knowledgeBaseIds: [1, 2] }
    })
  })

  it('列表与详情路径正确', async () => {
    mockedRequest.mockResolvedValue({ page: 1, size: 10, total: 0, items: [] })
    await fetchSkills({ page: 1, size: 10 })
    expect(mockedRequest.mock.calls[0][0]).toMatchObject({ url: '/skills', method: 'GET' })
    await fetchSkill(5)
    expect(mockedRequest.mock.calls[1][0]).toMatchObject({ url: '/skills/5', method: 'GET' })
  })
})

describe('digitalEmployee 模块', () => {
  beforeEach(() => vi.clearAllMocks())

  it('创建请求体不含 knowledgeBaseIds', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    await createDigitalEmployee({
      name: '售后助手',
      systemPrompt: 'p',
      skillIds: [201],
      modelConfig: { model: 'deepseek-chat' }
    })
    const call = mockedRequest.mock.calls[0][0]
    expect(call).toMatchObject({ url: '/digital-employees', method: 'POST' })
    expect(call.data).not.toHaveProperty('knowledgeBaseIds')
    expect(call.data.skillIds).toEqual([201])
  })

  it('发布以 POST 调用无请求体', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    await publishDigitalEmployee(1)
    expect(mockedRequest).toHaveBeenCalledWith({ url: '/digital-employees/1/publish', method: 'POST' })
  })

  it('回滚请求体携带 releaseId', async () => {
    mockedRequest.mockResolvedValue({ id: 1 } as never)
    await rollbackDigitalEmployee(1, 99)
    expect(mockedRequest).toHaveBeenCalledWith({
      url: '/digital-employees/1/rollback',
      method: 'POST',
      data: { releaseId: 99 }
    })
  })

  it('列表与详情路径正确', async () => {
    mockedRequest.mockResolvedValue({ page: 1, size: 10, total: 0, items: [] })
    await fetchDigitalEmployees({ page: 1, size: 10 })
    expect(mockedRequest.mock.calls[0][0]).toMatchObject({ url: '/digital-employees', method: 'GET' })
    await fetchDigitalEmployee(9)
    expect(mockedRequest.mock.calls[1][0]).toMatchObject({ url: '/digital-employees/9', method: 'GET' })
  })
})

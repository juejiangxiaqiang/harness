// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：ApiClient 单元测试。通过自定义 axios adapter 模拟后端响应，
// 覆盖成功解码、业务错误抛 ApiError、401 触发全局回调与网络错误兜底。

import { describe, expect, it, vi } from 'vitest'
import type { InternalAxiosRequestConfig } from 'axios'
import { ApiError, request, setUnauthorizedHandler } from '../apiClient'
import type { ApiResponse } from '@/types'

/** 构造一个返回指定响应体的自定义 adapter */
function buildAdapter(body: ApiResponse<unknown>, status = 200) {
  return async (config: InternalAxiosRequestConfig) => ({
    data: body,
    status,
    statusText: status === 200 ? 'OK' : 'ERR',
    headers: {},
    config
  })
}

/** 网络错误 adapter：模拟超时/断网 */
function buildNetworkErrorAdapter() {
  return async () => {
    throw new Error('timeout of 30000ms exceeded')
  }
}

describe('request 成功路径', () => {
  it('SUCCESS 时返回 data 字段', async () => {
    const data = await request({
      url: '/health',
      method: 'GET',
      adapter: buildAdapter({ code: 'SUCCESS', message: 'success', data: { ok: true }, traceId: 't1' })
    })
    expect(data).toEqual({ ok: true })
  })
})

describe('request 业务错误路径', () => {
  it('业务 code 非 SUCCESS 时抛 ApiError 并带映射文案与 traceId', async () => {
    await expect(
      request({
        url: '/auth/login',
        method: 'POST',
        data: {},
        adapter: buildAdapter({
          code: 'AUTH_INVALID_CREDENTIALS',
          message: 'bad credential',
          data: null,
          traceId: 't2'
        })
      })
    ).rejects.toMatchObject({ code: 'AUTH_INVALID_CREDENTIALS', message: '账号或密码错误', traceId: 't2' })
  })

  it('AUTH_UNAUTHORIZED 时触发全局 401 回调', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    await expect(
      request({
        url: '/knowledge-bases',
        method: 'GET',
        adapter: buildAdapter({ code: 'AUTH_UNAUTHORIZED', message: 'unauthorized', data: null, traceId: 't3' })
      })
    ).rejects.toBeInstanceOf(ApiError)
    expect(handler).toHaveBeenCalledTimes(1)
    setUnauthorizedHandler(() => {})
  })

  it('HTTP 非 200 且响应体为统一结构时同样抛 ApiError', async () => {
    await expect(
      request({
        url: '/skills',
        method: 'GET',
        adapter: buildAdapter(
          { code: 'RESOURCE_NOT_FOUND', message: 'not found', data: null, traceId: 't4' },
          404
        )
      })
    ).rejects.toMatchObject({ code: 'RESOURCE_NOT_FOUND', message: '资源不存在或已删除' })
  })

  it('网络错误（超时/断网）时抛出 NETWORK_ERROR 且不暴露堆栈', async () => {
    await expect(
      request({
        url: '/knowledge-bases',
        method: 'GET',
        adapter: buildNetworkErrorAdapter()
      })
    ).rejects.toMatchObject({ code: 'NETWORK_ERROR' })
  })
})

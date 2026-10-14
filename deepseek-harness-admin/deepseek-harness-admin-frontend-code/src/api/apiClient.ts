// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：统一 Axios 封装 ApiClient。所有 HTTP 必须经过本文件：响应解码、
// Bearer 注入、错误码映射、traceId 透传与 401 登出处理。业务代码不得自行创建
// Axios 实例（对照 docs/API_SPEC.md 第 6 章与 AGENTS.md 4.2）。

import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'
import { resolveErrorMessage } from '@/utils/errorCode'

/**
 * 业务异常：携带后端错误码与 traceId，供页面内联展示与调试追溯
 */
export class ApiError extends Error {
  /** 后端错误码（如 AUTH_INVALID_CREDENTIALS）或 NETWORK_ERROR */
  code: string
  /** 本次请求的 traceId，便于前后端联查 */
  traceId: string | undefined

  constructor(code: string, message: string, traceId?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
  }
}

/** 未授权事件回调：由 main.ts 注册，触发清会话与跳转登录页 */
type UnauthorizedHandler = () => void
let onUnauthorized: UnauthorizedHandler | null = null

/** 注册全局 401 回调；应用入口处调用一次即可 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  onUnauthorized = handler
}

/** 共享 Axios 实例，baseURL 由环境变量控制，默认 /api（Nginx 反代） */
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30_000
})

// 请求拦截：已登录时注入 Bearer 凭证，token 统一从本地存储读取。
// token 存储键与 user store 写入键保持一致（对照 stores/user.ts 的 TOKEN_KEY）
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('harness-admin-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：后端约定业务成功/失败均 HTTP 200，实际结果由响应体 code 区分
instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    // 响应结构异常时按网络错误兜底，不向页面抛未解码的数据
    if (!body || typeof body.code !== 'string') {
      throw new ApiError('NETWORK_ERROR', '响应格式异常，请稍后重试')
    }
    if (body.code !== 'SUCCESS') {
      // 凭证失效统一走登出，避免各页面各自处理
      if (body.code === 'AUTH_UNAUTHORIZED') onUnauthorized?.()
      throw new ApiError(body.code, resolveErrorMessage(body.code, body.message), body.traceId)
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const body = error.response?.data
    // 后端返回的业务错误（HTTP 非 200 场景兜底解码）
    if (body) {
      if (body.code === 'AUTH_UNAUTHORIZED') onUnauthorized?.()
      throw new ApiError(body.code, resolveErrorMessage(body.code, body.message), body.traceId)
    }
    // 网络层错误（超时/断网/网关）：统一提示，不暴露堆栈
    throw new ApiError('NETWORK_ERROR', resolveErrorMessage(undefined, error.message))
  }
)

/**
 * 发起请求并返回响应体中的 data 字段
 * @param config Axios 请求配置（url 不含 /api 前缀，baseURL 已统一注入）
 * @returns 后端 ApiResponse<T>.data；业务失败时抛出 ApiError
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await instance.request<ApiResponse<T>>(config)
  return response.data.data as T
}

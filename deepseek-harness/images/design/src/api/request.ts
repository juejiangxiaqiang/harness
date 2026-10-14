// 文件路径: design/src/api/request.ts
// 作用: Axios 请求实例 —— 对接 deepseek-harness-admin 后端（docs/2.0/后端接口文档.md §2）
// 约定: 业务成败以响应体 code == "SUCCESS" 判断，HTTP 恒为 200；
//       受保护接口自动携带 Authorization: Bearer <token>；
//       AUTH_UNAUTHORIZED 统一清理本地登录态并跳转登录页。
import axios from 'axios'
import { clearAuth, getToken } from '@/utils/storage'
import type { ApiResponse } from '@/types'

// 业务错误（带后端错误码，供页面按 message 提示）
export class ApiError extends Error {
  code: string

  constructor(code: string, message: string) {
    super(message)
    this.code = code
  }
}

// 创建 axios 实例
const request = axios.create({
  // 接口基础地址：开发环境经 Vite 代理转发到 8080，部署时由环境变量覆盖
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  // 请求超时时间
  timeout: 15000
})

// 请求拦截器：附加登录态信息
request.interceptors.request.use(
  (config) => {
    // 已登录时携带凭证
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    // 请求错误直接抛出
    return Promise.reject(error)
  }
)

// 响应拦截器：按业务 code 解包/抛错，未授权统一处理
request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    // 业务成功：直接解包 data 供调用方使用
    if (body?.code === 'SUCCESS') {
      return body.data as never
    }
    // 未授权：清理登录态并跳转登录页（保证不放行受保护页面）
    if (body?.code === 'AUTH_UNAUTHORIZED') {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new ApiError(body.code, body.message || '登录已失效，请重新登录'))
    }
    // 其余业务错误：按 message 提示
    return Promise.reject(new ApiError(body?.code ?? 'UNKNOWN', body?.message ?? '请求失败，请稍后重试'))
  },
  (error) => {
    // 网络/服务端错误
    const message = error instanceof Error && error.message ? error.message : '网络异常，请稍后重试'
    return Promise.reject(new ApiError('NETWORK_ERROR', message))
  }
)

// 导出请求实例
export default request

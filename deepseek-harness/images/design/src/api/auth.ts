// 文件路径: design/src/api/auth.ts
// 作用: 认证接口（docs/2.0/后端接口文档.md §4）
// 路径: POST /auth/login、POST /auth/register、POST /auth/logout
import request from './request'
import type { LoginResult, UserResponse } from '@/types'

// 登录：返回 token + 用户信息
export function login(username: string, password: string): Promise<LoginResult> {
  return request.post('/auth/login', { username, password }) as Promise<LoginResult>
}

// 注册：只返回用户信息（无 token），服务端固定 USER 角色
export function register(username: string, password: string): Promise<UserResponse> {
  return request.post('/auth/register', { username, password }) as Promise<UserResponse>
}

// 退出登录：吊销当前凭证（失败也由调用方清理本地登录态）
export function logout(): Promise<null> {
  return request.post('/auth/logout') as Promise<null>
}

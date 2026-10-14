// 文件路径: design/src/utils/storage.ts
// 作用: localStorage 封装 —— 登录态（token/user）、收藏、会话历史、对话设置
import type { AuthUser, ChatSession, ChatSettings } from '@/types'

// 存储键名统一前缀
const KEY_TOKEN = 'ssms:token' // 登录凭证（后端签发）
const KEY_USER = 'ssms:user' // 当前登录用户信息
const KEY_FAVORITES = 'ssms:favorites' // 数字员工收藏 id 列表
const KEY_SETTINGS = 'ssms:settings' // 对话设置

// 泛型读取：解析失败时返回兜底值
function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

// 泛型写入
function write<T>(key: string, value: T): void {
  localStorage.setItem(key, JSON.stringify(value))
}

// ===== 登录态 =====

// 读取当前登录凭证（未登录返回空串）
export function getToken(): string {
  return localStorage.getItem(KEY_TOKEN) ?? ''
}

// 保存登录凭证
export function setToken(token: string): void {
  localStorage.setItem(KEY_TOKEN, token)
}

// 清除登录凭证
export function clearToken(): void {
  localStorage.removeItem(KEY_TOKEN)
}

// 读取当前登录用户（未登录返回 null）
export function getUser(): AuthUser | null {
  return read<AuthUser | null>(KEY_USER, null)
}

// 保存登录用户
export function setUser(user: AuthUser): void {
  write(KEY_USER, user)
}

// 清除登录态（token + 用户信息，退出登录使用）
export function clearAuth(): void {
  clearToken()
  localStorage.removeItem(KEY_USER)
}

// ===== 数字员工收藏（仅前端本地态，不影响后端） =====

// 读取收藏 id 列表
export function loadFavorites(): string[] {
  return read<string[]>(KEY_FAVORITES, [])
}

// 写入收藏 id 列表
export function saveFavorites(ids: string[]): void {
  write(KEY_FAVORITES, ids)
}

// 切换收藏状态，返回最新收藏列表
export function toggleFavorite(id: string): string[] {
  const ids = loadFavorites()
  const index = ids.indexOf(id)
  if (index >= 0) {
    ids.splice(index, 1)
  } else {
    ids.push(id)
  }
  saveFavorites(ids)
  return ids
}

// ===== 会话历史 =====

// 读取指定作用域下的会话列表（按更新时间倒序）
export function loadSessions(scope: string): ChatSession[] {
  const sessions = read<ChatSession[]>(`ssms:sessions:${scope}`, [])
  return sessions.sort((a, b) => b.updatedAt - a.updatedAt)
}

// 保存指定作用域下的会话列表
export function saveSessions(scope: string, sessions: ChatSession[]): void {
  write(`ssms:sessions:${scope}`, sessions)
}

// ===== 对话设置 =====

// 读取对话设置（带默认值）
export function loadSettings(): ChatSettings {
  return read<ChatSettings>(KEY_SETTINGS, {
    model: 'deepseek-v4',
    temperature: 0.7,
    systemPrompt: '',
    enterToSend: true
  })
}

// 保存对话设置
export function saveSettings(settings: ChatSettings): void {
  write(KEY_SETTINGS, settings)
}

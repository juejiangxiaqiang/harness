// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：错误码映射工具单元测试，覆盖已知错误码、未知错误码与兜底分支。

import { describe, expect, it } from 'vitest'
import { resolveErrorMessage, DEFAULT_ERROR_MESSAGE } from './errorCode'

describe('resolveErrorMessage', () => {
  it('已知错误码返回映射中文提示', () => {
    expect(resolveErrorMessage('AUTH_INVALID_CREDENTIALS', 'raw')).toBe('账号或密码错误')
    expect(resolveErrorMessage('KNOWLEDGE_BASE_NOT_READY', 'raw')).toBe('所选知识库尚未准备完成')
    expect(resolveErrorMessage('DIGITAL_EMPLOYEE_NOT_PUBLISHABLE', 'raw')).toBe('数字员工不满足发布/回滚条件')
  })

  it('未知错误码优先使用后端原始 message', () => {
    expect(resolveErrorMessage('UNKNOWN_CODE', '后端说明')).toBe('后端说明')
  })

  it('未知错误码且无 message 时返回兜底文案', () => {
    expect(resolveErrorMessage('UNKNOWN_CODE', undefined)).toBe(DEFAULT_ERROR_MESSAGE)
  })

  it('无错误码（网络层）时返回兜底文案', () => {
    expect(resolveErrorMessage(undefined, undefined)).toBe(DEFAULT_ERROR_MESSAGE)
  })
})

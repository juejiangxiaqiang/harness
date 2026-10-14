// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：后端错误码到中文提示的统一映射，避免各页面散落错误文案；
// 错误码字典对齐 docs/API_SPEC.md 第 7 章。

/** 错误码与中文提示映射表 */
const ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: '账号或密码错误',
  AUTH_UNAUTHORIZED: '登录已失效，请重新登录',
  USERNAME_EXISTS: '账号已存在',
  ACCOUNT_DISABLED: '账号已禁用',
  RESOURCE_NOT_FOUND: '资源不存在或已删除',
  RESOURCE_FORBIDDEN: '无权限执行该操作',
  VALIDATION_FAILED: '参数校验失败，请检查输入',
  KNOWLEDGE_BASE_PROCESSING: '知识库正在向量化处理中',
  KNOWLEDGE_BASE_NOT_READY: '所选知识库尚未准备完成',
  VECTOR_INDEX_FAILED: '向量化处理失败',
  DIGITAL_EMPLOYEE_NOT_PUBLISHABLE: '数字员工不满足发布/回滚条件',
  INTERNAL_ERROR: '系统内部错误，请稍后重试'
}

/** 未识别错误码时的兜底文案 */
export const DEFAULT_ERROR_MESSAGE = '请求失败，请稍后重试'

/**
 * 将后端错误码解析为中文提示
 * @param code 后端错误码，可能为空（网络层错误）
 * @param message 后端原始 message，仅作为未收录错误码时的回退
 * @returns 面向用户的中文提示，不暴露堆栈与内部细节
 */
export function resolveErrorMessage(code: string | undefined, message: string | undefined): string {
  if (code && ERROR_MESSAGES[code]) {
    return ERROR_MESSAGES[code]
  }
  return message || DEFAULT_ERROR_MESSAGE
}

// 文件路径: design/src/mock/chat.ts
// 作用: 模拟对话引擎 —— 根据输入关键词返回预设回复，供本地演示
// 注意: 后续接入真实大模型接口时，替换 mockReply 的实现即可

// 数字员工人设（对话页展示名称/简介，回复风格）
export interface Persona {
  name: string
  intro: string
  greetings: string[]
  replies: string[]
  fallback: string
}

// deepseek harness 通用助手人设
export const harnessPersona: Persona = {
  name: 'DeepSeek Harness',
  intro: '面向任务的智能体开发与执行环境，支持规划、编码、工具调用与多轮协作',
  greetings: ['你好，我是 DeepSeek Harness 智能助手，可以帮你写代码、查资料、规划任务。请问有什么需要？'],
  replies: [
    '好的，我来处理。已把该任务拆分为可执行步骤，稍后给出结果。',
    '理解你的需求。我会先检索相关信息，再给出完整方案。',
    '收到。已开始执行，完成后会汇报进展与产出文件。',
    '这个需求可以落地。我先生成实现草案，再逐步验证。',
    '已记录该任务，纳入当前会话的执行队列，随时可发起执行。'
  ],
  fallback: '我已收到你的消息。当前为本地演示环境，接入真实模型后可针对该问题给出完整回答。'
}

// 根据输入关键词命中回复（演示用关键词匹配）
export function mockReply(input: string, persona: Persona): string {
  const text = input.trim()
  // 命中问候语时返回专属问候
  if (/你好|您好|hi|hello|嗨/i.test(text) && persona.greetings.length) {
    return persona.greetings[0]
  }
  // 命中关键词库中的主题词时返回对应回复
  const index = Math.abs(hashCode(text)) % persona.replies.length
  return persona.replies[index]
}

// 简单字符串哈希（用于回复轮换，保证同一问题稳定命中）
function hashCode(str: string): number {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i)
    hash |= 0
  }
  return hash
}

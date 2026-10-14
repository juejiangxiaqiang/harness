// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：格式化工具单元测试，覆盖文件大小、时间与扩展名提取各分支。

import { describe, expect, it } from 'vitest'
import { extractExtension, formatBytes, formatDateTime } from './format'

describe('formatBytes', () => {
  it('非正数返回 0 B', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(-1)).toBe('0 B')
  })

  it('按 1024 进制换算并保留一位小数', () => {
    expect(formatBytes(1024)).toBe('1.0 KB')
    expect(formatBytes(8.2 * 1024 * 1024)).toBe('8.2 MB')
    expect(formatBytes(2 * 1024 * 1024 * 1024)).toBe('2.0 GB')
  })
})

describe('formatDateTime', () => {
  it('ISO 时间格式化为 yyyy-MM-dd HH:mm', () => {
    expect(formatDateTime('2026-10-07T10:15:30')).toBe('2026-10-07 10:15')
  })

  it('非法输入原样返回', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date')
  })
})

describe('extractExtension', () => {
  it('提取小写扩展名', () => {
    expect(extractExtension('手册.MD')).toBe('md')
    expect(extractExtension('readme.txt')).toBe('txt')
  })

  it('无扩展名返回空串', () => {
    expect(extractExtension('README')).toBe('')
  })
})

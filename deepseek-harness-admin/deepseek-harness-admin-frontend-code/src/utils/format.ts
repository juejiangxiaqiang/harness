// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：通用格式化工具（文件大小、时间、扩展名），供列表与详情页复用，
// 保证展示口径一致。

/**
 * 字节数格式化为可读大小
 * @param bytes 字节数，非正数时返回 "0 B"
 * @returns 如 "8.2 MB"、"0.4 KB"
 */
export function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const idx = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  const value = bytes / 1024 ** idx
  return `${value.toFixed(1)} ${units[idx]}`
}

/**
 * ISO-8601 时间格式化为 yyyy-MM-dd HH:mm
 * @param iso 后端返回的时间字符串（如 2026-10-07T10:15:30）
 * @returns 非法输入原样返回，避免展示 "Invalid Date"
 */
export function formatDateTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 提取文件扩展名（不含点、小写）
 * @param fileName 文件名，如 "手册.MD"
 * @returns "md"；无扩展名返回空串
 */
export function extractExtension(fileName: string): string {
  const dot = fileName.lastIndexOf('.')
  return dot >= 0 ? fileName.slice(dot + 1).toLowerCase() : ''
}

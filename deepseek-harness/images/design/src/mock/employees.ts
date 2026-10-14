// 文件路径: design/src/mock/employees.ts
// 作用: 数字员工列表页静态配置 —— 排序选项与左侧导航菜单
// 注意: 数字员工数据已切换为真实后端接口（见 src/api/employees.ts），本文件不再承载卡片 mock 数据

// 排序选项（列表页排序栏）
export const sortOptions = ['综合', '最热', '最新'] as const

// 左侧导航菜单（仅保留新建任务 / 数字员工）
export const navMenus = [
  { key: 'new-task', label: '新建任务', icon: 'clock' },
  { key: 'employees', label: '数字员工', icon: 'avatar' }
] as const

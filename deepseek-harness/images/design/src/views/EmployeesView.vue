<!-- 文件路径: design/src/views/EmployeesView.vue -->
<!-- 作用: 数字员工列表页 —— 对齐参考图：左侧导航/面包屑/标题+搜索/类型与排序切换/分类栏/4 列卡片网格 -->
<template>
  <div class="employees-page">
    <!-- 左侧导航栏 -->
    <AppSidebar />

    <!-- 主内容区 -->
    <main class="emp-main">
      <!-- 面包屑导航 -->
      <nav class="breadcrumb">
        <span>随身秘书</span>
        <el-icon :size="12"><ArrowRight /></el-icon>
        <span class="breadcrumb-current">能力中心</span>
      </nav>

      <!-- 页面标题区：大标题 + 描述 + 右侧搜索框 -->
      <div class="page-head">
        <div class="head-text">
          <h1 class="page-title">数字员工</h1>
          <p class="page-desc">选择一位数字员工，释放团队生产力，让专业能力触手可及。</p>
        </div>
        <!-- 搜索框 -->
        <el-input
          v-model="keyword"
          class="head-search"
          placeholder="搜索数字员工"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 筛选区：类型切换 + 排序 -->
      <div class="filter-row">
        <!-- 类型分段控件（专家/专家团） -->
        <div class="segmented">
          <button
            v-for="t in typeOptions"
            :key="t"
            class="segmented-item"
            :class="{ 'is-active': type === t }"
            @click="type = t"
          >
            {{ t }}
          </button>
        </div>
        <!-- 排序选项（综合/最热/最新） -->
        <div class="sort-group">
          <button
            v-for="s in sortOptions"
            :key="s"
            class="filter-tab"
            :class="{ 'is-active': sort === s }"
            @click="sort = s"
          >
            {{ s }}
          </button>
        </div>
      </div>

      <!-- 卡片网格（加载态骨架屏） -->
      <div v-if="loading" class="card-grid">
        <el-skeleton v-for="i in 8" :key="i" animated class="card-skeleton" />
      </div>

      <!-- 卡片网格 -->
      <div v-else-if="filtered.length" class="card-grid">
        <EmployeeCard
          v-for="emp in filtered"
          :key="emp.id"
          :employee="emp"
          @click="goChat(emp.id)"
          @favorite="onFavorite(emp, $event)"
        />
      </div>

      <!-- 加载更多指示 -->
      <div v-if="loadingMore" class="load-more">加载更多…</div>

      <!-- 空态 -->
      <div v-else-if="!loading && employees.length === 0" class="empty-state">
        <p>暂无数字员工</p>
        <button class="btn-secondary" @click="resetFilters">刷新列表</button>
      </div>

      <!-- 筛选无结果 -->
      <div v-else-if="!loading && employees.length > 0 && filtered.length === 0" class="empty-state">
        <p>未找到匹配的数字员工</p>
        <button class="btn-secondary" @click="resetFilters">重置筛选条件</button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
// 数字员工列表页逻辑 —— 对接 GET /api/digital-employees 分页（docs/2.0/前端改造计划文档.md §3.3）
// 字段映射/派生规则见 api/employees.ts；搜索/排序/类型切换为前端本地过滤
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import AppSidebar from '@/components/AppSidebar.vue'
import EmployeeCard from '@/components/EmployeeCard.vue'
import { fetchEmployees, toEmployeeCard } from '@/api/employees'
import { sortOptions } from '@/mock/employees'
import { toggleFavorite } from '@/utils/storage'
import type { Employee } from '@/types'

// 路由实例（跳转数字员工对话页）
const router = useRouter()

// 员工数据（接口分页拉取，滚动追加）
const employees = ref<Employee[]>([])
// 首屏加载状态（骨架屏）
const loading = ref(true)
// 加载更多状态
const loadingMore = ref(false)
// 分页状态
const page = ref(1)
const total = ref(0)
// 每页条数
const PAGE_SIZE = 12
// 加载完成标记（避免滚动到底重复请求）
const finished = ref(false)
// 搜索关键词
const keyword = ref('')
// 类型筛选（专家/专家团）
const type = ref<'专家' | '专家团'>('专家')
// 排序方式
const sort = ref<(typeof sortOptions)[number]>('综合')

// 类型切换选项
const typeOptions: Array<'专家' | '专家团'> = ['专家', '专家团']

// 拉取一页数据并追加（页码超过 total 后停止）
async function loadPage(): Promise<void> {
  if (loadingMore.value || finished.value) return
  loadingMore.value = true
  try {
    const data = await fetchEmployees(page.value, PAGE_SIZE)
    employees.value.push(...data.items.map(toEmployeeCard))
    total.value = data.total
    page.value += 1
    // 已加载满全部数据
    if (employees.value.length >= total.value) {
      finished.value = true
    }
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载数字员工列表失败')
  } finally {
    loadingMore.value = false
    loading.value = false
  }
}

// 滚动到底部时加载下一页
function onScroll(): void {
  const nearBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 200
  if (nearBottom) {
    void loadPage()
  }
}

// 页面加载时：拉取首屏并监听滚动
onMounted(() => {
  void loadPage()
  window.addEventListener('scroll', onScroll)
})

// 卸载时移除滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})

// 综合过滤后的员工列表（类型/搜索/排序均为前端本地过滤）
const filtered = computed(() => {
  let list = [...employees.value]

  // 类型筛选：专家 = 全部；专家团 = 已发布（status == PUBLISHED）
  if (type.value === '专家团') {
    list = list.filter((e) => e.status === 'PUBLISHED')
  }

  // 关键词搜索（名称/描述）
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      (e) => e.name.toLowerCase().includes(kw) || e.desc.toLowerCase().includes(kw)
    )
  }

  // 排序：综合 = 接口原序；最热 = 已发布在前；最新 = id 倒序
  if (sort.value === '最热') {
    list.sort((a, b) => Number(b.inUse) - Number(a.inUse))
  } else if (sort.value === '最新') {
    list.sort((a, b) => Number(b.id) - Number(a.id))
  }

  return list
})

// 收藏状态同步到本地列表与 localStorage（仅前端本地态）
function onFavorite(emp: Employee, favorite: boolean): void {
  emp.favorite = favorite
  toggleFavorite(emp.id)
}

// 进入数字员工对话页
function goChat(id: string): void {
  router.push(`/employee/${id}`)
}

// 重置全部筛选条件
function resetFilters(): void {
  keyword.value = ''
  type.value = '专家'
  sort.value = '综合'
}
</script>

<style scoped>
/* 页面容器：侧栏 + 主区 */
.employees-page {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

/* 主内容区 */
.emp-main {
  flex: 1;
  min-width: 0;
  padding: 20px 28px 40px;
}

/* 面包屑导航 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
  margin-bottom: 20px;
}
/* 当前层级高亮 */
.breadcrumb-current {
  color: var(--color-text-secondary);
}

/* 页面标题区 */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

/* 标题文字列 */
.head-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 大标题：24px 粗体 */
.page-title {
  font-size: var(--font-size-h1);
  line-height: var(--line-height-h1);
  font-weight: var(--font-weight-bold);
  color: var(--color-text);
}

/* 描述文案 */
.page-desc {
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}

/* 搜索框 */
.head-search {
  width: 260px;
}

/* 筛选行：类型 + 排序 */
.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 12px;
}

/* 排序组 */
.sort-group {
  display: flex;
  gap: 4px;
}

/* 卡片网格：自动填充列（≥1440 4 列，≥1024 3 列，≥768 2 列）；min(240px,100%) 保证窄容器不横向溢出 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(240px, 100%), 1fr));
  gap: 16px;
}

/* 骨架卡片圆角 */
.card-skeleton {
  border-radius: var(--radius-lg);
}

/* 空态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 0;
  color: var(--color-text-secondary);
}

/* 加载更多指示 */
.load-more {
  padding: 16px 0 8px;
  text-align: center;
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 响应式：小屏侧栏压缩为图标模式 */
@media (max-width: 960px) {
  .head-search {
    width: 100%;
  }
}
</style>

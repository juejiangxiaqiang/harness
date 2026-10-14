<!-- 文件路径: design/src/components/EmployeeCard.vue -->
<!-- 作用: 数字员工能力卡片 —— 图标/标题/收藏星/使用角标/描述/标签，对齐参考图卡片结构 -->
<template>
  <div class="employee-card" @click="emit('click')">
    <!-- 使用角标：黑色圆角标签，右上角定位 -->
    <span v-if="employee.inUse" class="use-badge">使用</span>

    <!-- 顶部行：圆形单字图标 + 标题 + 收藏星 -->
    <div class="card-top">
      <EmployeeAvatar :char="employee.iconChar" :color="employee.color" :size="40" />
      <div class="title-row">
        <h3 class="card-title">{{ employee.name }}</h3>
        <!-- 收藏星标：点击切换填充，带轻微弹跳动效 -->
        <button
          class="star-btn"
          :class="{ 'is-active': employee.favorite, 'is-bounce': bouncing }"
          :aria-label="employee.favorite ? '取消收藏' : '收藏'"
          @click.stop="toggleFavorite"
        >
          <el-icon :size="18" :color="employee.favorite ? '#f59e0b' : '#d1d5db'">
            <StarFilled v-if="employee.favorite" />
            <Star v-else />
          </el-icon>
        </button>
      </div>
    </div>

    <!-- 描述文案：最多两行截断 -->
    <p class="card-desc">{{ employee.desc }}</p>

    <!-- 底部标签组：浅灰圆角标签（无数据时隐藏） -->
    <div v-if="employee.tags.length" class="card-tags">
      <span v-for="tag in employee.tags" :key="tag" class="card-tag">{{ tag }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// 数字员工实体数据
import { ref } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import EmployeeAvatar from '@/components/EmployeeAvatar.vue'
import type { Employee } from '@/types'

// 卡片展示数据
const props = defineProps<{ employee: Employee }>()

// 向父组件广播收藏变更
const emit = defineEmits<{
  (e: 'click'): void
  (e: 'favorite', favorite: boolean): void
}>()

// 星标弹跳动效状态
const bouncing = ref(false)

// 切换收藏并播放弹跳动效
function toggleFavorite(): void {
  props.employee.favorite = !props.employee.favorite
  emit('favorite', props.employee.favorite)
  bouncing.value = true
  // 动效结束后移除类名
  setTimeout(() => (bouncing.value = false), 300)
}
</script>

<style scoped>
/* 卡片容器：圆角 12px、内边距 20px、极浅阴影 */
.employee-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition:
    transform var(--duration-base) var(--ease-out),
    box-shadow var(--duration-base) var(--ease-out),
    border-color var(--duration-base) var(--ease-out);
}
/* 悬停：轻微上移 + 阴影增强 + 蓝色描边 */
.employee-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--color-primary-light);
}

/* 使用角标：黑色圆角，右上角 */
.use-badge {
  position: absolute;
  top: 0;
  right: 16px;
  padding: 3px 10px;
  background: var(--color-black);
  color: var(--color-text-invert);
  font-size: var(--font-size-caption);
  line-height: 18px;
  border-radius: 0 0 6px 6px;
  box-shadow: var(--shadow-card);
  user-select: none;
}

/* 顶部行布局 */
.card-top {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 标题 + 星标行 */
.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

/* 卡片标题：16px 中等字重 */
.card-title {
  font-size: var(--font-size-h2);
  line-height: var(--line-height-h2);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
  padding-right: 4px;
}

/* 收藏按钮 */
.star-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: transform var(--duration-base) var(--ease-out);
}
.star-btn:hover {
  transform: scale(1.15);
}
/* 点击弹跳动效 */
.star-btn.is-bounce {
  animation: star-bounce 0.3s var(--ease-out);
}
@keyframes star-bounce {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.35);
  }
  100% {
    transform: scale(1);
  }
}

/* 描述文案：两行截断 */
.card-desc {
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部标签组 */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* 浅灰圆角标签：圆角 4px */
.card-tag {
  padding: 2px 8px;
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
  line-height: var(--line-height-caption);
  border-radius: var(--radius-sm);
}
</style>

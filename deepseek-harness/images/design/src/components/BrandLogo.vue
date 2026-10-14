<!-- 文件路径: design/src/components/BrandLogo.vue -->
<!-- 作用: 品牌标识 —— 蓝色三角形 logo + 主标题“随身秘书” + 可选副标题（参考图顶部品牌区） -->
<template>
  <div class="brand" :class="{ 'has-subtitle': showSubtitle }">
    <!-- 蓝色三角形 logo（SVG） -->
    <svg class="brand-logo" viewBox="0 0 24 24" width="28" height="28" aria-hidden="true">
      <!-- 外圈蓝色三角 -->
      <path d="M12 2 L22 21 L2 21 Z" fill="#2563eb" />
      <!-- 内圈白色三角，形成层次感 -->
      <path d="M12 9 L17.5 18 L6.5 18 Z" fill="#ffffff" />
    </svg>
    <div class="brand-text">
      <!-- 主标题 -->
      <span class="brand-title">随身秘书</span>
      <!-- 副标题（数字员工页显示） -->
      <span v-if="showSubtitle" class="brand-subtitle">数字员工</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// 是否显示副标题（数字员工页为“数字员工”）
withDefaults(
  defineProps<{ showSubtitle?: boolean }>(),
  { showSubtitle: false }
)
</script>

<style scoped>
/* 品牌区布局：logo + 文字纵排 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 12px;
}
/* logo 轻微投影增强层级 */
.brand-logo {
  flex-shrink: 0;
  filter: drop-shadow(0 2px 4px rgb(37 99 235 / 0.3));
}
/* 文字列 */
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
/* 主标题 */
.brand-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}
/* 副标题 */
.brand-subtitle {
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>

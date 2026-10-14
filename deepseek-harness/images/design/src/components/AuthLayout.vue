<!-- 文件路径: design/src/components/AuthLayout.vue -->
<!-- 作用: 登录/注册页共用外壳 —— 居中卡片 + 品牌区 + 标题插槽，保持两页视觉统一 -->
<template>
  <div class="auth-page">
    <!-- 背景装饰圆（浅蓝色调，与品牌呼应） -->
    <div class="deco deco-1" />
    <div class="deco deco-2" />

    <!-- 居中卡片 -->
    <div class="auth-card">
      <!-- 品牌区 -->
      <div class="auth-brand">
        <svg class="auth-logo" viewBox="0 0 24 24" width="40" height="40" aria-hidden="true">
          <path d="M12 2 L22 21 L2 21 Z" fill="#2563eb" />
          <path d="M12 9 L17.5 18 L6.5 18 Z" fill="#ffffff" />
        </svg>
        <h1 class="auth-title">{{ title }}</h1>
        <p class="auth-subtitle">{{ subtitle }}</p>
      </div>

      <!-- 表单插槽 -->
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
// 页面标题与副标题
defineProps<{
  title: string
  subtitle: string
}>()
</script>

<style scoped>
/* 全屏浅灰背景，内容居中 */
.auth-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg);
  overflow: hidden;
  padding: 24px;
}

/* 背景装饰圆 */
.deco {
  position: absolute;
  border-radius: 50%;
  background: var(--color-primary-lighter);
  z-index: 0;
}
.deco-1 {
  width: 320px;
  height: 320px;
  top: -120px;
  left: -120px;
}
.deco-2 {
  width: 220px;
  height: 220px;
  bottom: -80px;
  right: -60px;
  background: rgb(219 234 254 / 0.6);
}

/* 居中卡片：白底 16px 圆角 + 阴影 */
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 40px 36px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card-hover);
}

/* 品牌区 */
.auth-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 28px;
}

/* 品牌 logo */
.auth-logo {
  filter: drop-shadow(0 4px 8px rgb(37 99 235 / 0.35));
}

/* 页面标题 */
.auth-title {
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
  color: var(--color-text);
}

/* 副标题 */
.auth-subtitle {
  font-size: var(--font-size-body);
  color: var(--color-text-muted);
}
</style>

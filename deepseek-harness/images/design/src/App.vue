<!-- 文件路径: design/src/App.vue -->
<!-- 作用: 根组件 —— 仅承载路由出口与全局过渡动效 -->
<template>
  <!-- 路由出口，切换页面时使用淡入动效 -->
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style scoped>
/* 页面切换淡入淡出动效 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease;
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>

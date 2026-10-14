<!-- 文件路径: design/src/components/EmployeeAvatar.vue -->
<!-- 作用: 圆形单字头像 —— 彩色圆底 + 白色单字，参考图 40×40 全圆图标 -->
<template>
  <div
    class="emp-avatar"
    :style="{
      width: size + 'px',
      height: size + 'px',
      background: color,
      fontSize: Math.round(size * 0.45) + 'px'
    }"
  >
    {{ char }}
  </div>
</template>

<script setup lang="ts">
// 头像展示内容
withDefaults(
  defineProps<{
    char: string // 圆内单字
    color: string // 底色（业务色相）
    size?: number // 直径
  }>(),
  { size: 40 }
)
</script>

<style scoped>
/* 全圆头像：居中白色单字 */
.emp-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 50%;
  color: #ffffff;
  font-weight: 500;
  user-select: none;
  box-shadow: 0 2px 6px rgb(0 0 0 / 0.08);
}
</style>

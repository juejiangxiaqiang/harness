<!-- 文件路径: design/src/views/LoginView.vue -->
<!-- 作用: 登录页 —— 账号密码校验、记住账号、登录后跳转目标页或对话页 -->
<template>
  <AuthLayout title="欢迎回来" subtitle="登录随身秘书 · 数字员工平台">
    <!-- 登录表单 -->
    <form class="auth-form" @submit.prevent="onSubmit">
      <!-- 账号输入 -->
      <div class="field">
        <label class="field-label">账号</label>
        <el-input
          v-model="form.username"
          size="large"
          placeholder="请输入用户名"
          autocomplete="username"
          clearable
          @keyup.enter="onSubmit"
        />
      </div>

      <!-- 密码输入 -->
      <div class="field">
        <label class="field-label">密码</label>
        <el-input
          v-model="form.password"
          type="password"
          size="large"
          placeholder="请输入密码"
          autocomplete="current-password"
          show-password
          @keyup.enter="onSubmit"
        />
      </div>

      <!-- 记住账号 + 快捷演示提示 -->
      <div class="field-row">
        <el-checkbox v-model="form.remember">记住账号</el-checkbox>
        <span class="demo-hint">演示：可先去注册一个新账号</span>
      </div>

      <!-- 登录按钮 -->
      <button type="submit" class="btn-primary auth-submit" :disabled="submitting">
        {{ submitting ? '登录中…' : '登 录' }}
      </button>
      <p class="auth-switch">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
    </form>
  </AuthLayout>
</template>

<script setup lang="ts">
// 登录页逻辑 —— 对接 POST /api/auth/login（docs/2.0/前端改造计划文档.md §3.1）
// 成功存 token + user 后按 redirect 跳转；失败按后端 message 提示
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import { ApiError } from '@/api/request'
import { login } from '@/api/auth'
import { setToken, setUser } from '@/utils/storage'

// 路由（用于跳转目标页）
const route = useRoute()
// 路由实例
const router = useRouter()

// 表单数据（account → username，与接口字段对齐）
const form = reactive({
  username: (route.query.username as string) ?? localStorage.getItem('ssms:last-account') ?? '',
  password: '',
  remember: true
})

// 提交中状态（防止重复提交）
const submitting = ref(false)

// 提交登录
async function onSubmit(): Promise<void> {
  // 基础校验（非空兜底，最终以服务端 VALIDATION_FAILED 为准）
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }

  submitting.value = true
  try {
    const data = await login(form.username.trim(), form.password)
    // 记住账号
    if (form.remember) {
      localStorage.setItem('ssms:last-account', form.username.trim())
    } else {
      localStorage.removeItem('ssms:last-account')
    }
    // 写入登录态并跳转
    setToken(data.token)
    setUser(data.user)
    ElMessage.success(`欢迎回来，${data.user.username}`)
    // 优先回到登录前想访问的页面
    const redirect = (route.query.redirect as string) || '/chat'
    router.replace(redirect)
  } catch (err) {
    // 账号不存在/密码错误/账号禁用/校验失败等：展示后端 message
    ElMessage.error(err instanceof ApiError ? err.message : '登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* 登录表单纵向排列 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 表单项 */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 标签 */
.field-label {
  font-size: var(--font-size-body);
  font-weight: var(--font-weight-medium);
  color: var(--color-text);
}

/* 记住账号行 */
.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 演示提示 */
.demo-hint {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* 登录按钮通栏 */
.auth-submit {
  width: 100%;
  height: 40px;
  font-size: var(--font-size-h2);
  letter-spacing: 4px;
}

/* 切换注册 */
.auth-switch {
  text-align: center;
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}
</style>

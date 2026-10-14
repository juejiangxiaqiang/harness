<!-- 文件路径: design/src/views/RegisterView.vue -->
<!-- 作用: 注册页 —— 账号/密码校验对齐接口，注册成功跳登录页并预填用户名 -->
<template>
  <AuthLayout title="创建账号" subtitle="注册随身秘书 · 数字员工平台">
    <!-- 注册表单 -->
    <form class="auth-form" @submit.prevent="onSubmit">
      <!-- 账号 -->
      <div class="field">
        <label class="field-label">账号</label>
        <el-input
          v-model="form.username"
          size="large"
          placeholder="字母/数字/下划线，3-32 位"
          autocomplete="username"
          clearable
        />
      </div>

      <!-- 密码 -->
      <div class="field">
        <label class="field-label">密码</label>
        <el-input
          v-model="form.password"
          type="password"
          size="large"
          placeholder="至少 8 位"
          autocomplete="new-password"
          show-password
        />
      </div>

      <!-- 确认密码 -->
      <div class="field">
        <label class="field-label">确认密码</label>
        <el-input
          v-model="form.confirm"
          type="password"
          size="large"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          show-password
        />
      </div>

      <!-- 服务协议 -->
      <el-checkbox v-model="form.agreed">我已阅读并同意《服务协议》与《隐私政策》</el-checkbox>

      <!-- 注册按钮 -->
      <button type="submit" class="btn-primary auth-submit" :disabled="submitting">
        {{ submitting ? '注册中…' : '注 册' }}
      </button>

      <!-- 前往登录 -->
      <p class="auth-switch">
        已有账号？
        <router-link to="/login">直接登录</router-link>
      </p>
    </form>
  </AuthLayout>
</template>

<script setup lang="ts">
// 注册页逻辑 —— 对接 POST /api/auth/register（docs/2.0/前端改造计划文档.md §3.2）
// 后端只返 user（无 token），注册成功后跳登录页并预填用户名，由用户完成登录拿 token
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import { ApiError } from '@/api/request'
import { register } from '@/api/auth'

// 路由实例
const router = useRouter()

// 表单数据（后端不接收昵称/角色字段，仅 username + password）
const form = reactive({
  username: '',
  password: '',
  confirm: '',
  agreed: false
})

// 提交中状态
const submitting = ref(false)

// 提交注册
async function onSubmit(): Promise<void> {
  // 账号格式校验：仅字母/数字/下划线，长度 3-32（与服务端一致）
  if (!/^[A-Za-z0-9_]{3,32}$/.test(form.username.trim())) {
    ElMessage.warning('账号仅支持字母、数字、下划线，长度 3-32')
    return
  }
  // 密码长度校验：8-64（与服务端一致）
  if (form.password.length < 8 || form.password.length > 64) {
    ElMessage.warning('密码长度需在 8-64 位之间')
    return
  }
  // 确认密码一致性校验（仅前端，不送后端）
  if (form.password !== form.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  // 协议勾选校验
  if (!form.agreed) {
    ElMessage.warning('请先阅读并同意服务协议')
    return
  }

  submitting.value = true
  try {
    await register(form.username.trim(), form.password)
    localStorage.setItem('ssms:last-account', form.username.trim())
    ElMessage.success('注册成功，请登录')
    // 跳登录页并预填用户名
    router.replace({ path: '/login', query: { username: form.username.trim() } })
  } catch (err) {
    // 账号已存在/校验失败等：展示后端 message
    ElMessage.error(err instanceof ApiError ? err.message : '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* 注册表单纵向排列 */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

/* 注册按钮通栏 */
.auth-submit {
  width: 100%;
  height: 40px;
  font-size: var(--font-size-h2);
  letter-spacing: 4px;
}

/* 切换登录 */
.auth-switch {
  text-align: center;
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}
</style>

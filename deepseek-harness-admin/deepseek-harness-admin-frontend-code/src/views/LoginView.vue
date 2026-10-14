<!--
---
last_updated: 2026-10-08
status: active          # active | deprecated | draft
owner: @qiang
---
文件职责：登录页。左右分栏：品牌展示区 + 登录表单区；账号密码登录、记住账号、
内联错误、防重复提交；成功后跳转原目标页或首页（对照 VISUAL_DESIGN.md 4.1）。
-->
<script setup lang="ts">
// @ts-ignore - 解决 vue 模块声明文件未找到的类型检查问题
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { ApiError } from '@/api/apiClient'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 登录表单；remember 仅记住账号不存密码 */
const form = reactive({ username: '', password: '', remember: true })
const loading = ref(false)
const inlineError = ref('')
/** 密码明文切换（仅视觉反馈，不改变登录流程） */
const showPassword = ref(false)

/** 登录后回跳地址：优先携带的 redirect，其次首页 */
const redirectTarget = computed(() => {
  const redirect = route.query.redirect as string | undefined
  return redirect && redirect.startsWith('/') ? redirect : '/knowledge-bases'
})

/** 恢复记住的账号 */
const savedUsername = localStorage.getItem('harness-admin-remember-username')
if (savedUsername) {
  form.username = savedUsername
}

/** 表单基础校验：账号密码必填，缺失时内联提示 */
function validateForm(): boolean {
  if (!form.username.trim()) {
    inlineError.value = '请输入账号'
    return false
  }
  if (!form.password) {
    inlineError.value = '请输入密码'
    return false
  }
  return true
}

/** 提交登录：校验 → 调登录接口 → 处理记住账号 → 跳转 */
async function handleLogin() {
  if (loading.value) return
  inlineError.value = ''
  if (!validateForm()) return
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    // 记住账号仅写用户名，密码不入本地存储
    if (form.remember) {
      localStorage.setItem('harness-admin-remember-username', form.username.trim())
    } else {
      localStorage.removeItem('harness-admin-remember-username')
    }
    ElMessage.success('登录成功')
    router.push(redirectTarget.value)
  } catch (e) {
    // 内联展示后端错误码映射后的中文提示，不泄漏堆栈
    inlineError.value = e instanceof ApiError ? e.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-brand" aria-label="品牌展示">
      <div class="brand-logo">
        <svg viewBox="0 0 24 24" width="40" height="40" fill="none">
          <path d="M12 2 L20 20 L12 15 L4 20 Z" fill="#fff" />
        </svg>
      </div>
      <h1 class="brand-name">Harness 管理后台</h1>
      <p class="brand-slogan">统一管理知识库 / Skills / 数字员工</p>
      <div class="brand-decor" aria-hidden="true">
        <div class="decor-card">
          <div class="decor-bar" />
          <div class="decor-bar short" />
          <div class="decor-dot" />
        </div>
      </div>
    </section>

    <section class="login-form-wrap">
      <div class="login-card card">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">请登录管理员账号</p>

        <form novalidate @submit.prevent="handleLogin">
          <label class="field">
            <span class="field-label">账号</span>
            <input
              v-model="form.username"
              class="field-input"
              type="text"
              autocomplete="username"
              placeholder="请输入账号"
              :disabled="loading"
            />
          </label>

          <label class="field">
            <span class="field-label">密码</span>
            <div class="password-wrap">
              <input
                v-model="form.password"
                class="field-input"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入密码"
                :disabled="loading"
              />
              <button
                type="button"
                class="eye-btn"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                :disabled="loading"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '🙈' : '👁' }}
              </button>
            </div>
          </label>

          <div class="form-row">
            <label class="remember">
              <input v-model="form.remember" type="checkbox" :disabled="loading" />
              <span>记住账号</span>
            </label>
            <span class="forgot-link" aria-hidden="true">忘记密码</span>
          </div>

          <p v-if="inlineError" class="inline-error" role="alert">{{ inlineError }}</p>

          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>
        </form>

        <p class="copyright">© 2026 Harness 管理后台</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  height: 100%;
  min-height: 100vh;
}

/* 左区：主色向浅蓝的柔和渐变 */
.login-brand {
  width: 60%;
  background: linear-gradient(135deg, #5b6bf5 0%, #4a90e2 70%, #ebf2fc 120%);
  color: #fff;
  padding: var(--space-7);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.brand-logo {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-3);
}

.brand-name {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: var(--font-weight-title);
}

.brand-slogan {
  margin: 0;
  font-size: var(--font-size-body);
  opacity: 0.85;
}

.brand-decor {
  margin-top: var(--space-7);
}

.decor-card {
  width: 240px;
  height: 150px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.16);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.decor-bar {
  height: 8px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.9);
  width: 70%;
}

.decor-bar.short {
  width: 45%;
  opacity: 0.7;
}

.decor-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  opacity: 0.8;
}

/* 右区：登录卡片 */
.login-form-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-page);
}

.login-card {
  width: 380px;
  max-width: calc(100vw - var(--space-6));
  padding: var(--space-6);
  box-shadow: var(--shadow-card);
}

.form-title {
  margin: 0 0 4px;
  font-size: 28px;
  font-weight: var(--font-weight-title);
  color: var(--color-text-main);
}

.form-subtitle {
  margin: 0 0 var(--space-5);
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
}

.field {
  display: block;
  margin-bottom: var(--space-3);
}

.field-label {
  display: block;
  font-size: var(--font-size-body);
  color: var(--color-text-main);
  margin-bottom: 6px;
}

.field-input {
  width: 100%;
  height: 40px;
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-sm);
  padding: 0 12px;
  font-size: var(--font-size-body);
  font-family: inherit;
  outline: none;
  transition: var(--transition-base);
}

.field-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-focus-ring);
}

/* 密码框：右侧眼睛切换按钮 */
.password-wrap {
  position: relative;
}

.password-wrap .field-input {
  padding-right: 40px;
}

.eye-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 2px;
  color: var(--color-text-placeholder);
}

.eye-btn:hover {
  color: var(--color-primary);
}

.eye-btn:disabled {
  cursor: not-allowed;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-1) 0 var(--space-3);
}

.remember {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-body);
  color: var(--color-text-secondary);
  cursor: pointer;
}

/* 忘记密码：主色文字链接（当前无找回流程，仅视觉占位） */
.forgot-link {
  font-size: var(--font-size-small);
  color: var(--color-primary);
  cursor: default;
}

.forgot-link:hover {
  text-decoration: underline;
}

.inline-error {
  margin: 0 0 var(--space-2);
  font-size: var(--font-size-body);
  color: var(--color-danger);
}

.login-btn {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-body);
  cursor: pointer;
  transition: var(--transition-base);
}

.login-btn:hover {
  background: var(--color-primary-hover);
}

.login-btn:disabled {
  background: #f5f5f5;
  color: var(--color-text-placeholder);
  cursor: not-allowed;
}

.copyright {
  margin: var(--space-4) 0 0;
  text-align: center;
  font-size: var(--font-size-small);
  color: var(--color-text-placeholder);
}

/* 窄屏：品牌区隐藏，登录卡片全宽 */
@media (max-width: 767px) {
  .login-brand {
    display: none;
  }
}
</style>

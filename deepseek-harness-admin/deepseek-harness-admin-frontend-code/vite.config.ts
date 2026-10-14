// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：Vite 构建与 Vitest 测试统一配置；含 @ 路径别名、开发代理与产物分包，
// 使前端按 docs/前端开发计划书.md 的工程基线与性能要求落地。

/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'

import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    // 统一 @ 指向 src，避免深层相对路径
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5174,
    // 开发环境将 /api 反代到本地后端，避免跨域并贴近生产 Nginx 代理形态
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 组件库独立分包体积较大（gzip 约 304KB），属管理后台按需加载的预期成本，上调阈值避免误报
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus'],
          vendor: ['vue', 'vue-router', 'pinia', 'axios']
        }
      }
    }
  },
  test: {
    // 单测运行环境与全局 API，便于使用 expect/vi 免重复导入；
    // include 限定 src，避免误收集 e2e 下的 Playwright 用例
    environment: 'jsdom',
    globals: true,
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.spec.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: ['src/main.ts', 'src/router/**', 'src/types/**', 'src/test/**', 'src/assets/**']
    }
  }
})

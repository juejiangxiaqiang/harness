// 文件路径: design/vite.config.ts
// 作用: Vite 构建配置 —— 注册 Vue 插件与开发服务器
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 导出 Vite 配置
export default defineConfig({
  // 注册 Vue 单文件组件插件
  plugins: [vue()],
  // 路径别名：@ 指向 src 目录
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 开发服务器配置
  server: {
    // 监听所有网卡，方便本地与局域网访问
    host: '127.0.0.1',
    // 固定端口，便于浏览器冒烟验证
    port: 5173,
    // 开发代理：/api 转发到 deepseek-harness-admin 后端（默认 8080）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

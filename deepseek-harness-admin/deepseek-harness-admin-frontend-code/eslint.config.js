// ---
// last_updated: 2026-10-07
// status: active          # active | deprecated | draft
// owner: @qiang
// ---
// 文件职责：ESLint 平面配置。接入 Vue 与 TypeScript 推荐规则，并用 Prettier
// 关闭格式冲突规则，保证 `npm run lint` 无 Error。

import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  // 构建产物与报告目录不参与检查
  { ignores: ['dist/**', 'coverage/**', 'node_modules/**', 'playwright-report/**', 'test-results/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  // .vue 文件的 script 部分交由 typescript-eslint 解析
  { files: ['**/*.vue'], languageOptions: { parserOptions: { parser: tseslint.parser } } },
  {
    rules: {
      // 组件名不强要求多词，设计稿按业务命名（如 StatusTag 本身即多词）
      'vue/multi-word-component-names': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
      // 未使用变量仅告警，便于保留解构占位参数
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      // 允许带描述的 @ts-ignore：仅用于 AGENTS.md 8.1 登记的 IDE 误报场景，
      // 要求必须附原因描述，避免无说明地绕过类型检查
      '@typescript-eslint/ban-ts-comment': ['error', { 'ts-ignore': 'allow-with-description' }]
    }
  },
  prettier
)

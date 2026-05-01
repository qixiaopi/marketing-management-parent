// Vitest 测试框架配置文件
// Vitest 是一个基于 Vite 的单元测试框架，速度快，功能强大
// 更多信息：https://vitest.dev/

import { fileURLToPath } from 'node:url'
import { defineConfig, configDefaults } from 'vitest/config'

export default defineConfig({
  // ==================== 插件配置 ====================
  plugins: [],  // 可以添加 Vue 等插件

  // ==================== 路径解析配置 ====================
  resolve: {
    alias: {
      // 路径别名：@ 代表 src 目录
      // 与 tsconfig.app.json 中的 paths 配置保持一致
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  // ==================== 测试配置 ====================
  test: {
    // 启用全局 API
    // 可以直接使用 describe、it、expect 等，不需要导入
    globals: true,

    // 测试环境
    // jsdom：模拟浏览器 DOM 环境
    // node：纯 Node.js 环境
    environment: 'jsdom',

    // 排除哪些文件
    // 使用默认排除项，并额外排除 e2e 目录
    exclude: [...configDefaults.exclude, 'e2e/**'],

    // 测试根目录
    root: fileURLToPath(new URL('./', import.meta.url)),

    // ==================== 覆盖率配置 ====================
    coverage: {
      // 覆盖率报告格式
      // text：终端输出
      // json：JSON 格式
      // html：HTML 报告（可在浏览器查看）
      reporter: ['text', 'json', 'html'],

      // 排除哪些文件不统计覆盖率
      exclude: [
        'node_modules/**',       // 依赖包
        'src/**/*.test.ts',      // 测试文件本身
        'src/**/*.spec.ts',      // 测试文件本身
        'src/types/**',          // 类型声明
        'src/env.d.ts'           // 环境变量类型
      ]
    },

    // ==================== 测试报告配置 ====================
    reporters: ['default'],  // 默认报告格式
  },
})

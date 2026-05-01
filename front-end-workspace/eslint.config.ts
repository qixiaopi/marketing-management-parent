// ESLint 配置文件（Flat Config 格式）
// ESLint 9.0+ 推荐的新格式，比旧格式更清晰、更强大
// 更多信息：https://eslint.org/docs/latest/use/configure/configuration-files-new

import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginVitest from '@vitest/eslint-plugin'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from 'eslint-config-prettier/flat'

// 如需在 .vue 文件中支持更多语言（如 tsx），可取消以下注释：
// import { configureVueProject } from '@vue/eslint-config-typescript'
// configureVueProject({ scriptLangs: ['ts', 'tsx'] })
// 更多信息：https://github.com/vuejs/eslint-config-typescript/#advanced-setup

// 使用 Vue + TypeScript 配置包装器导出
export default defineConfigWithVueTs(
  // ==================== 检查哪些文件 ====================
  {
    name: 'app/files-to-lint',
    // 只检查 Vue 和 TypeScript 文件
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  // ==================== 忽略哪些文件 ====================
  globalIgnores([
    '**/dist/**',        // 构建产物
    '**/dist-ssr/**',    // SSR 构建产物
    '**/coverage/**',    // 测试覆盖率报告
    'node_modules',      // 依赖包
    '*.log',             // 日志文件
    '*.md',              // Markdown 文件
    '.vscode',           // VS Code 配置
    'package-lock.json', // npm 锁文件
    'yarn.lock'          // Yarn 锁文件
  ]),

  // ==================== Vue 插件配置 ====================
  // 使用 Vue 插件的基础规则
  ...pluginVue.configs['flat/essential'],
  // 使用 Vue + TypeScript 推荐配置
  vueTsConfigs.recommended,

  // ==================== Vitest 测试配置 ====================
  {
    // 使用 Vitest 推荐的测试规则
    ...pluginVitest.configs.recommended,
    // 只对测试文件生效
    files: ['src/**/__tests__/*'],
  },

  // ==================== Oxlint 配置 ====================
  // 从 .oxlintrc.json 读取 Oxlint 配置
  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  // ==================== Prettier 集成 ====================
  // 禁用 ESLint 中与 Prettier 冲突的格式化规则
  // 让 Prettier 专门负责格式化，ESLint 负责代码质量检查
  skipFormatting,

  // ==================== 自定义规则 ====================
  {
    name: 'app/rules',
    rules: {
      // 关闭"Vue 组件名必须多词"规则
      // 允许用单个词作为组件名（如 App.vue）
      'vue/multi-word-component-names': 'off',
      
      // 未使用变量警告
      // 但允许以下划线开头的变量（通常是故意不使用的）
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }]
    }
  }
)

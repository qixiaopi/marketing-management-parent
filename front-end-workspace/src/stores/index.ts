/**
 * Pinia 状态管理库配置文件
 * Pinia 是 Vue 3 官方推荐的状态管理工具，替代 Vuex
 * 
 * Pinia 特点：
 * - 更轻量（小于 1KB）
 * - 完整的 TypeScript 支持
 * - 去除 mutations，只有 state 和 actions
 * - 支持组合式 API 风格的 Store
 */

// 从 pinia 库导入 createPinia 函数
import { createPinia } from 'pinia'

/**
 * 创建 Pinia 实例
 * Pinia 实例是 Vue 应用状态管理的核心
 * 需要在 Vue 应用创建后调用 app.use(pinia) 注册
 */
const pinia = createPinia()

/**
 * 导出 Pinia 实例
 * 在 main.ts 中引入并注册到 Vue 应用
 * 
 * 使用方式：
 * import { createApp } from 'vue'
 * import App from './App.vue'
 * import pinia from './stores'
 * 
 * const app = createApp(App)
 * app.use(pinia)  // 注册 Pinia
 * app.mount('#app')
 */
export default pinia

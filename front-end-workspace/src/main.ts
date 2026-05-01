// 引入 Vue 的 createApp 函数，用于创建应用实例
import { createApp } from 'vue'

// 引入 Element Plus UI 组件库
import ElementPlus from 'element-plus'
// 引入 Element Plus 的全局 CSS 样式
import 'element-plus/dist/index.css'

// 引入根组件 App.vue
import App from './App.vue'
// 引入 Vue Router 路由实例
import router from './router'
// 引入 Pinia 状态管理实例
import pinia from './stores'

// 创建 Vue 应用实例，参数为根组件
const app = createApp(App)

// 注册 Pinia 状态管理插件，使整个应用可以访问 store
app.use(pinia)

// 注册 Vue Router 插件，使应用支持路由功能
app.use(router)

// 注册 Element Plus 插件，使应用可以使用 UI 组件
app.use(ElementPlus)

// 将应用挂载到 #app DOM 元素上
app.mount('#app')
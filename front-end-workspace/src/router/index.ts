/**
 * Vue Router 路由配置文件
 * 定义应用的所有路由规则，管理页面导航
 * 
 * 路由模式：History 模式（使用 HTML5 History API）
 * 特点：URL 更美观，无 # 号，需要服务器配置支持
 */

// 从 vue-router 导入路由创建函数和 History 模式
import { createRouter, createWebHistory } from 'vue-router'

/**
 * 创建路由实例
 * 配置路由模式和路由规则
 */
const router = createRouter({
  /**
   * 路由历史模式配置
   * createWebHistory：使用 HTML5 History API，URL 格式为 /path（无 #）
   * import.meta.env.BASE_URL：从环境变量获取基础 URL，默认为 /
   */
  history: createWebHistory(import.meta.env.BASE_URL),

  /**
   * 路由规则数组
   * 每个路由对象定义一个页面的访问路径和对应的组件
   */
  routes: [
    {
      /**
       * 根路径重定向
       * 访问 / 时自动跳转到 /product
       */
      path: '/',
      redirect: '/product'
    },
    {
      /**
       * 商品列表页路由
       * path：访问路径
       * name：路由名称，用于编程式导航
       * component：对应的页面组件（懒加载方式）
       */
      path: '/product',
      name: 'product',
      // 使用动态导入实现路由懒加载，优化首屏加载速度
      component: () => import('../views/product/index.vue')
    },
    {
      /**
       * 商品详情页路由
       * path：包含动态参数 :id，用于标识具体商品
       * 例如：/product/detail/123 表示查看 ID 为 123 的商品详情
       */
      path: '/product/detail/:id',
      name: 'productDetail',
      // 懒加载商品详情页组件
      component: () => import('../views/product/detail.vue')
    }
  ]
})

/**
 * 导出路由实例
 * 在 main.ts 中引入并注册到 Vue 应用
 */
export default router

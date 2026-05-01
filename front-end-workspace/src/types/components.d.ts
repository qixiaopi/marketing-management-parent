/**
 * 全局组件类型声明文件
 * 
 * 此文件由 unplugin-vue-components 插件自动生成
 * 用于声明项目中全局注册的组件，使 TypeScript 能够识别这些组件
 * 
 * 功能说明：
 * - 声明 Element Plus UI 组件（ElAlert, ElButton, ElInput 等）
 * - 声明自定义业务组件（LazyImage 等）
 * - 声明 Vue Router 内置组件（RouterLink, RouterView）
 * 
 * 使用方式：
 * 在模板中可以直接使用这些组件，无需手动 import
 * 
 * 示例：
 * <el-button>点我</el-button>  <!-- 无需 import ElButton -->
 * <LazyImage src="..." />       <!-- 无需 import LazyImage -->
 * <router-link to="...">...</router-link>  <!-- 无需 import -->
 * 
 * 注意：
 * - 此文件由工具自动生成，不应手动修改
 * - 如需修改组件自动导入配置，修改 vite.config.ts 中的 unplugin-vue-components 配置
 */

/* eslint-disable */
/* prettier-ignore */
// @ts-nocheck：跳过 TypeScript 类型检查（因为是自动生成的文件）
// biome-ignore lint: disable：禁用 biome linter 检查
// oxlint-disable：禁用 oxlint 检查

// ------
// 标记：此文件由 unplugin-vue-components 生成
// 相关讨论：https://github.com/vuejs/core/pull/3399

/**
 * 空导出声明
 * 确保此文件作为模块而非脚本处理
 */
export {}

/**
 * Vue 模块扩展
 * 在 vue 包上扩展 GlobalComponents 接口
 */
declare module 'vue' {
  /**
   * 全局组件接口
   * 声明所有全局可用的组件
   * 在模板中使用这些组件时，TypeScript 能够识别并提供类型检查和智能提示
   */
  export interface GlobalComponents {
    // ==================== Element Plus UI 组件 ====================
    
    /** ElAlert：警告提示组件 */
    ElAlert: typeof import('element-plus/es')['ElAlert']
    
    /** ElBreadcrumb：面包屑导航容器 */
    ElBreadcrumb: typeof import('element-plus/es')['ElBreadcrumb']
    
    /** ElBreadcrumbItem：面包屑导航项 */
    ElBreadcrumbItem: typeof import('element-plus/es')['ElBreadcrumbItem']
    
    /** ElButton：按钮组件 */
    ElButton: typeof import('element-plus/es')['ElButton']
    
    /** ElInput：输入框组件 */
    ElInput: typeof import('element-plus/es')['ElInput']

    // ==================== 自定义业务组件 ====================
    
    /**
     * LazyImage：图片懒加载组件
     * 组件路径：./../components/LazyImage.vue
     * 功能：当图片进入可视区域时才加载，优化页面性能
     */
    LazyImage: typeof import('./../components/LazyImage.vue')['default']

    // ==================== Vue Router 内置组件 ====================
    
    /**
     * RouterLink：路由链接组件
     * 用于创建导航链接
     * 相当于 <a> 标签，但支持单页应用路由
     */
    RouterLink: typeof import('vue-router')['RouterLink']
    
    /**
     * RouterView：路由视图组件
     * 用于渲染当前路由对应的组件
     * 相当于一个"占位符"，显示当前路由的内容
     */
    RouterView: typeof import('vue-router')['RouterView']
  }
}

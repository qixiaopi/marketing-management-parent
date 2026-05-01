/**
 * Vite 配置文件
 *
 * 本配置文件用于配置 Vite 构建工具的各种行为，包括：
 * - 插件系统：启用 Vue 支持、自动导入 API 和组件
 * - 路径别名：简化模块导入路径
 * - 开发服务器：配置本地开发服务器、代理、 热模块替换
 * - 生产构建：配置代码分割、压缩、优化等
 * - 依赖预构建：优化第三方依赖的加载性能
 */

import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

/**
 * Vite 配置定义
 *
 * 使用 defineConfig 函数包装配置，提供更好的类型提示和 IDE 支持
 */
export default defineConfig({

  /**
   * 插件配置
   *
   * 插件可以扩展 Vite 的功能，包括：
   * - 支持特定的文件类型（如 .vue 单文件组件）
   * - 自动导入常用 API，减少样板代码
   * - 自动注册组件，无需手动 import
   */
  plugins: [
    // Vue 3 官方插件
    // - 启用对 .vue 单文件组件的支持
    // - 处理模板编译、脚本解析、样式提取
    // - 支持 <script setup> 语法糖
    vue(),

    // Vue DevTools 开发者工具插件
    // - 在浏览器开发者工具中启用 Vue 专属面板
    // - 可以查看组件树、props、状态、事件等
    // - 生产环境自动禁用，不影响性能
    vueDevTools(),

    // 自动导入 API 插件
    // - 自动导入 Vue Composition API（如 ref, reactive, computed, watch 等）
    // - 自动导入 Vue Router API（如 useRoute, useRouter 等）
    // - 自动导入 Pinia API（如 defineStore, storeToRefs 等）
    // - 大幅减少手动 import 语句，提高开发效率
    AutoImport({
      // 指定要自动导入的库
      imports: ['vue', 'vue-router', 'pinia'],

      // 组件库解析器配置
      resolvers: [
        // Element Plus 组件自动导入解析器
        // - 自动导入 Element Plus 组件（如 ElButton, ElInput 等）
        // - 自动导入组件的 CSS 样式
        ElementPlusResolver({
          // 使用 CSS 方式导入组件样式
          // 可选值：'css'（CSS 文件）或 'sass'（SCSS 文件）
          importStyle: 'css'
        })
      ],

      // 生成 TypeScript 类型声明文件
      // 自动生成的类型定义，方便 IDE 进行类型检查和自动补全
      dts: 'src/types/auto-imports.d.ts'
    }),

    // 自动注册组件插件
    // - 自动扫描 src/components 目录下的所有组件
    // - 自动注册组件为局部组件，无需手动 import 和 components 注册
    Components({
      // 组件库解析器配置
      resolvers: [
        // Element Plus 组件自动导入解析器
        ElementPlusResolver({
          importStyle: 'css'
        })
      ],

      // 生成 TypeScript 类型声明文件
      dts: 'src/types/components.d.ts'
    })
  ],

  /**
   * 路径解析配置
   *
   * 配置模块导入的路径解析规则，支持：
   * - 路径别名：简化长路径的书写
   * - 扩展名自动解析：自动补全文件扩展名
   */
  resolve: {
    alias: {
      // '@' 路径别名，指向 src 目录
      // 使用方式：import Foo from '@/components/Foo.vue'
      // 优点：避免使用相对路径（如 ../../../components/Foo.vue）导致的混乱
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  /**
   * 开发服务器配置
   *
   * 配置本地开发服务器的行为，包括：
   * - 端口号和主机名
   * - 代理配置（解决跨域问题）
   * - 热模块替换（HMR）设置
   */
  server: {
    // 开发服务器端口号
    // 访问地址：http://localhost:3000
    port: 3000,

    // 代理配置
    // 用于解决开发环境中的跨域问题
    // 当前端请求后端 API 时，可以将请求代理到后端服务器
    // 暂时禁用代理，使用模拟数据
    proxy: {
      // 秒杀领取接口 - 端口 8087
      // '/api/seckill': {
      //   target: 'http://localhost:8087',
      //   changeOrigin: true,
      //   rewrite: (path) => path.replace(/^\/api/, '')
      // },
      // 商品相关接口 - 端口 8086
      // '/product': {
      //   target: 'http://localhost:8086',
      //   changeOrigin: true
      // },
      // 其他 API 接口 - 端口 8080（默认）
      // '/api': {
      //   target: 'http://localhost:8080',
      //   changeOrigin: true,
      //   rewrite: (path) => path.replace(/^\/api/, '')
      // }
    },

    // 热模块替换（HMR）配置
    // HMR 允许在不刷新页面的情况下更新模块
    // 大幅提升开发体验，保留页面状态
    hmr: {
      // 启用 HMR 错误遮罩
      // 当代码出错时，在浏览器中显示红色遮罩
      // 显示具体的错误信息和堆栈，方便调试
      overlay: true
    }
  },

  /**
   * 生产构建配置
   *
   * 配置生产环境的构建行为，包括：
   * - 代码压缩和混淆
   * - 代码分割策略
   * - 输出目录和文件命名
   */
  build: {
    // 代码分割警告阈值（单位：KB）
    // 当单个 chunk 超过此大小时，会在构建时发出警告
    // 帮助开发者发现过大的依赖包，及时优化
    chunkSizeWarningLimit: 1000,

    // 启用 CSS 代码分割
    // 将每个 CSS 文件单独打包
    // 优点：更好的缓存策略，只有修改的 CSS 才会重新下载
    cssCodeSplit: true,

    // 不生成 sourcemap
    // sourcemap 是源码和编译后代码的映射文件，用于调试
    // 生产环境不需要，且会增大包体积
    sourcemap: false,

    // 代码压缩器配置
    // 使用 terser 进行 JavaScript 代码压缩和混淆
    minify: 'terser',

    // terser 压缩选项
    terserOptions: {
      compress: {
        // 移除 console.log 语句
        // 生产环境不需要调试日志
        drop_console: true,

        // 移除 debugger 语句
        // 防止生产代码执行 debugger 阻塞
        drop_debugger: true
      }
    },

    // 输出目录
    // 构建后的文件将输出到 dist 目录
    outDir: 'dist',

    // 构建前清空输出目录
    // 确保 dist 目录中只有最新构建的文件
    // 避免旧文件残留导致的问题
    emptyOutDir: true,

    // Rollup 构建选项
    // Rollup 是 Vite 使用的底层打包工具
    rollupOptions: {
      output: {
        // 手动代码分割策略
        // 将大的第三方库拆分成独立文件
        // 优点：更好地利用浏览器缓存，只有修改的库才重新下载
        manualChunks(id) {
          // 只处理 node_modules 中的第三方库
          if (id.includes('node_modules')) {
            // 按库分割，每个主要库单独一个文件
            if (id.includes('element-plus')) return 'element-plus'
            if (id.includes('vue')) return 'vue'
            if (id.includes('axios')) return 'axios'
            if (id.includes('pinia')) return 'pinia'
            if (id.includes('vue-router')) return 'vue-router'

            // 其他所有第三方库打包到 vendor 文件
            return 'vendor'
          }
        },

        // chunk 文件命名模板
        // [name]：原始文件名
        // [hash]：基于内容生成的哈希值，用于缓存控制
        chunkFileNames: 'assets/js/[name]-[hash].js',

        // 入口文件命名模板
        // 入口文件是应用的主文件，负责加载其他模块
        entryFileNames: 'assets/js/[name]-[hash].js',

        // 静态资源命名模板
        // 包括 CSS、图片、字体等
        // [ext]：文件扩展名
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]'
      }
    }
  },

  /**
   * 依赖预构建配置
   *
   * 预构建可以将 CommonJS 格式的依赖转换为 ESM 格式
   * 提高浏览器的加载速度和兼容性
   */
  optimizeDeps: {
    // 指定需要预构建的依赖
    // 这些依赖会在首次启动时进行转换和优化
    include: [
      'vue',           // Vue 核心库
      'vue-router',    // Vue Router 路由管理
      'pinia',         // Pinia 状态管理
      'axios',         // Axios HTTP 客户端
      'element-plus'    // Element Plus UI 组件库
    ],

    // 排除不需要预构建的依赖
    exclude: [],

    // 是否强制重新构建依赖缓存
    // false：使用缓存，加速启动
    // true：每次都重新构建，用于解决依赖问题
    force: false
  }
})

# front-end-workspace

商品展示和领取系统的前端项目，基于 Vue 3 + TypeScript + Pinia 技术栈构建。

## 项目介绍

本项目是一个商品展示和领取系统的前端部分，主要功能包括：
- 商品列表展示
- 商品搜索功能
- 商品详情查看
- 商品领取功能（秒杀接口）

## 技术栈

- **前端框架**：Vue 3 (Composition API)
- **类型系统**：TypeScript
- **状态管理**：Pinia
- **路由管理**：Vue Router
- **UI 组件库**：Element Plus
- **HTTP 客户端**：Axios
- **构建工具**：Vite
- **代码质量**：ESLint + Prettier
- **测试框架**：Vitest

## 目录结构

```
front-end-workspace/
├── public/              # 静态资源目录
├── src/                 # 源代码目录
│   ├── api/             # API 相关代码
│   ├── assets/          # 静态资源
│   ├── components/      # 组件
│   ├── router/          # 路由配置
│   ├── stores/          # 状态管理
│   ├── types/           # TypeScript 类型定义
│   ├── utils/           # 工具函数
│   ├── views/           # 页面组件
│   └── main.ts          # 应用入口
├── .vscode/             # VS Code 配置
├── package.json         # 项目配置和依赖
├── vite.config.ts       # Vite 配置
└── tsconfig.json        # TypeScript 配置
```

## 核心功能模块

### 商品模块
- **商品列表**：展示商品列表，支持搜索功能
- **商品详情**：展示商品详细信息
- **商品领取**：实现商品领取功能（秒杀接口）

### API 模拟
- **模拟数据**：在 `request.ts` 中定义了模拟商品数据
- **模拟接口**：重写了 Axios 的 `post` 和 `get` 方法，拦截请求并返回模拟数据
- **支持的接口**：
  - `POST /product/search`：搜索商品
  - `GET /product/{skuId}`：获取商品详情
  - `POST /api/seckill/do/{productId}?userId={userId}`：领取商品

## 性能优化

- **请求缓存**：实现了请求缓存机制，避免重复请求
- **请求防抖**：实现了请求防抖，避免频繁请求
- **错误重试**：实现了请求失败自动重试机制
- **图片懒加载**：使用 `LazyImage` 组件实现图片懒加载

## 视觉设计

- **响应式布局**：适配不同屏幕尺寸
- **渐变背景**：使用 CSS 渐变背景
- **动画效果**：添加了按钮悬停、价格脉动等动画效果
- **卡片设计**：使用卡片式布局展示商品

## 推荐 IDE 配置

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (并禁用 Vetur)。

## 推荐浏览器配置

- **基于 Chromium 的浏览器** (Chrome, Edge, Brave 等)：
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [在 Chrome DevTools 中启用自定义对象格式化器](http://bit.ly/object-formatters)
- **Firefox**：
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [在 Firefox DevTools 中启用自定义对象格式化器](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## TypeScript 支持

TypeScript 默认无法处理 `.vue` 导入的类型信息，因此我们使用 `vue-tsc` 替代 `tsc` CLI 进行类型检查。在编辑器中，我们需要 [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) 来让 TypeScript 语言服务识别 `.vue` 类型。

## 配置自定义

请参阅 [Vite 配置参考](https://vite.dev/config/)。

## 项目设置

```sh
npm install
```

### 开发环境编译和热重载

```sh
npm run dev
```

### 类型检查、编译和生产环境压缩

```sh
npm run build
```

### 使用 [Vitest](https://vitest.dev/) 运行单元测试

```sh
npm run test:unit
```

### 使用 [ESLint](https://eslint.org/) 进行代码检查

```sh
npm run lint
```

## API 文档

### 商品相关接口

#### 搜索商品
- **接口**：`POST /product/search`
- **参数**：
  - `keyword` (可选)：搜索关键词
- **返回**：
  ```json
  {
    "code": 200,
    "data": [
      {
        "skuId": 1,
        "productName": "iPhone 15 Pro",
        "description": "Apple iPhone 15 Pro 256GB 钛金属色",
        "price": 99900,
        "stock": 50,
        "image": "/src/assets/iphone15pro.svg"
      }
    ],
    "message": "success"
  }
  ```

#### 获取商品详情
- **接口**：`GET /product/{skuId}`
- **参数**：
  - `skuId` (必需)：商品 ID
- **返回**：
  ```json
  {
    "code": 200,
    "data": {
      "skuId": 1,
      "productName": "iPhone 15 Pro",
      "description": "Apple iPhone 15 Pro 256GB 钛金属色",
      "price": 99900,
      "stock": 50,
      "image": "/src/assets/iphone15pro.svg"
    },
    "message": "success"
  }
  ```

#### 领取商品
- **接口**：`POST /api/seckill/do/{productId}?userId={userId}`
- **参数**：
  - `productId` (必需)：商品 ID
  - `userId` (必需)：用户 ID
- **返回**：
  ```json
  {
    "code": 200,
    "data": null,
    "message": "商品领取成功"
  }
  ```

## 项目部署

1. 执行 `npm run build` 命令构建生产版本
2. 将 `dist` 目录中的文件部署到服务器
3. 配置服务器以支持 Vue Router 的历史模式（如果使用）

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件


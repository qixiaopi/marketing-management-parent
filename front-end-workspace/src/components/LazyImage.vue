<template>
  <div class="lazy-image" :style="{ width: width, height: height }">
    <img 
      ref="imageRef"
      :src="placeholder"
      :data-src="src"
      :alt="alt"
      class="lazy-image__img"
      @load="handleLoad"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * LazyImage 组件 - 图片懒加载组件
 * 使用 IntersectionObserver API 实现图片进入可视区域时才加载
 */

import { onMounted, ref } from 'vue'

/**
 * 定义组件接收的 props
 * @property {string} src - 图片的真实 URL 地址
 * @property {string} alt - 图片的替代文本
 * @property {string} width - 容器宽度
 * @property {string} height - 容器高度
 */
defineProps({
  /** 图片的真实 URL 地址，懒加载的核心 */
  src: {
    type: String,
    required: true
  },
  /** 图片加载失败时显示的替代文本 */
  alt: {
    type: String,
    default: ''
  },
  /** 容器宽度，支持 CSS 单位如 '100%'、'200px' */
  width: {
    type: String,
    default: '100%'
  },
  /** 容器高度，支持 CSS 单位如 'auto'、'300px' */
  height: {
    type: String,
    default: 'auto'
  }
})

/**
 * 图片元素的模板引用
 * 用于操作 DOM 元素，如设置真实 src 和添加加载完成样式
 */
const imageRef = ref<HTMLImageElement | null>(null)

/**
 * 占位图（SVG 格式的灰色图片）
 * 在图片真实加载完成前显示，提供视觉反馈
 * 使用 base64 编码的 SVG，避免额外网络请求
 */
const placeholder = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2YzZjNmMyIvPjx0ZXh0IHg9IjEwMCIgeT0iMTAwIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYsIGFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjNjY2IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj7liIflj5HmiJDlu7rliLDluLg8L3RleHQ+PC9zdmc+'

/**
 * 图片加载完成时的回调函数
 * 添加 loaded 样式类，实现淡入显示效果
 */
const handleLoad = () => {
  if (imageRef.value) {
    // 添加 loaded 类，触发 CSS 过渡动画，从透明变为不透明
    imageRef.value.classList.add('lazy-image__img--loaded')
  }
}

/**
 * 组件挂载后的生命周期钩子
 * 使用 IntersectionObserver API 实现懒加载
 * 当图片进入可视区域时，才开始加载真实图片
 */
onMounted(() => {
  /**
   * IntersectionObserver 实例
   * 用于检测元素是否进入可视区域
   * @param {IntersectionObserverCallback} entries - 回调函数
   * @param {IntersectionObserverInit} options - 配置选项
   */
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      /**
       * entry.isIntersecting 表示元素是否进入可视区域
       * 当为 true 时，表示元素可见，开始加载图片
       */
      if (entry.isIntersecting && imageRef.value) {
        // 将 data-src 的值设置为图片的真实 src，开始加载
        imageRef.value.src = imageRef.value.dataset.src || ''
        // 加载完成后停止观察该元素，避免重复处理
        observer.unobserve(entry.target)
      }
    })
  })
  
  // 开始观察图片元素
  if (imageRef.value) {
    observer.observe(imageRef.value)
  }
})
</script>

<style scoped>
.lazy-image {
  position: relative;
  overflow: hidden;
}

.lazy-image__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.3s ease;
  opacity: 0;
}

.lazy-image__img--loaded {
  opacity: 1;
}
</style>
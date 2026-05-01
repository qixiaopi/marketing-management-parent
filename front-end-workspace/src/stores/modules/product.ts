/**
 * 商品状态管理模块
 * 使用 Pinia 进行状态管理，处理商品搜索、详情获取、领取等功能
 */

// 导入 Pinia 的 defineStore 函数，用于创建状态管理仓库
import { defineStore } from 'pinia'
// 导入商品相关的 API 接口
import { productApi } from '@/api/modules/product'
// 导入 API 相关的类型定义
import type { DetailResponse, SearchParams, SearchResponse } from '@/api/modules/product'
import type { ApiError } from '@/api/request'

/**
 * 商品接口定义
 * @property {number} skuId - 商品唯一标识
 * @property {string} productName - 商品名称
 * @property {string} description - 商品描述
 * @property {number} price - 商品价格
 * @property {number} stock - 商品库存
 * @property {string} image - 商品图片 URL
 */
export interface Product {
  skuId: number
  productName: string
  description: string
  price: number
  stock: number
  image: string
}

/**
 * 领取记录接口定义
 * @property {number} productId - 商品 ID
 * @property {string} productName - 商品名称
 * @property {string} claimedAt - 领取时间（ISO 格式）
 * @property {('success' | 'failed')} status - 领取状态
 * @property {string} message - 领取结果消息
 */
export interface ClaimRecord {
  productId: number
  productName: string
  claimedAt: string
  status: 'success' | 'failed'
  message: string
}

/**
 * 存储值类型定义
 * 支持商品列表、单个商品、空值或领取记录列表
 */
type StorageValue = Product[] | Product | null | ClaimRecord[]

// 使用 Map 存储定时器，避免类型冲突和内存泄漏
const timeoutMap = new Map<string, ReturnType<typeof setTimeout>>()

/**
 * 商品状态管理仓库
 * 命名空间: 'product'
 */
export const useProductStore = defineStore('product', {
  /**
   * 状态定义
   * 包含商品列表、搜索状态、详情状态、领取状态等
   */
  state: () => ({
    /** 商品列表，从 localStorage 初始化 */
    products: JSON.parse(localStorage.getItem('products') || '[]') as Product[],
    /** 搜索加载状态 */
    searchLoading: false,
    /** 搜索错误信息 */
    searchError: null as string | null,
    /** 搜索参数 */
    searchParams: {} as SearchParams,

    /** 当前选中的商品详情，从 localStorage 初始化 */
    currentProduct: JSON.parse(localStorage.getItem('currentProduct') || 'null') as Product | null,
    /** 详情加载状态 */
    detailLoading: false,
    /** 详情错误信息 */
    detailError: null as string | null,

    /** 领取加载状态 */
    claimLoading: false,
    /** 领取错误信息 */
    claimError: null as string | null,
    /** 领取记录列表，从 localStorage 初始化 */
    claimRecords: JSON.parse(localStorage.getItem('claimRecords') || '[]') as ClaimRecord[],

    /** 用户 ID，从 localStorage 初始化，默认值为 1 */
    userId: parseInt(localStorage.getItem('userId') || '1')
  }),

  /**
   * 计算属性
   * 提供派生状态，如是否有商品、是否正在搜索等
   */
  getters: {
    /**
     * 是否有商品
     * @param {Object} state - 状态对象
     * @returns {boolean} 是否存在商品
     */
    hasProducts: state => state.products.length > 0,

    /**
     * 是否正在搜索
     * @param {Object} state - 状态对象
     * @returns {boolean} 搜索加载状态
     */
    isSearching: state => state.searchLoading,

    /**
     * 是否正在加载详情
     * @param {Object} state - 状态对象
     * @returns {boolean} 详情加载状态
     */
    isLoadingDetail: state => state.detailLoading,

    /**
     * 是否正在领取商品
     * @param {Object} state - 状态对象
     * @returns {boolean} 领取加载状态
     */
    isClaiming: state => state.claimLoading,

    /**
     * 获取最近的 5 条领取记录
     * @param {Object} state - 状态对象
     * @returns {ClaimRecord[]} 最近的领取记录
     */
    recentClaims: state => state.claimRecords.slice(0, 5)
  },

  /**
   * 动作方法
   * 处理异步操作和状态更新
   */
  actions: {
    /**
     * 搜索商品
     * @param {SearchParams} params - 搜索参数
     * @returns {Promise<void>}
     */
    async searchProducts(params: SearchParams) {
      try {
        // 设置搜索加载状态
        this.searchLoading = true
        // 清除之前的错误
        this.searchError = null
        // 保存搜索参数
        this.searchParams = params

        // 调用 API 搜索商品
        const response = await productApi.search(params)

        // 处理响应结果
        if ('code' in response && response.code === 200) {
          // 更新商品列表
          this.products = (response as SearchResponse).data || []
          // 防抖存储到 localStorage
          this.debouncedStorage('products', this.products)
        } else {
          // 处理错误响应
          const error = response as ApiError
          this.searchError = error.message || '搜索商品失败'
          console.error('搜索商品失败:', error)
        }
      } catch (error) {
        // 处理网络或其他错误
        this.searchError = error instanceof Error ? error.message : '搜索商品失败'
        console.error('搜索商品失败:', error)
      } finally {
        // 无论成功失败，都关闭加载状态
        this.searchLoading = false
      }
    },

    /**
     * 获取商品详情
     * @param {number} skuId - 商品 SKU ID
     * @returns {Promise<void>}
     */
    async getProductDetail(skuId: number) {
      try {
        // 设置详情加载状态
        this.detailLoading = true
        // 清除之前的错误
        this.detailError = null

        // 调用 API 获取商品详情
        const response = await productApi.getDetail(skuId)

        // 处理响应结果
        if ('code' in response && response.code === 200) {
          // 更新当前商品
          this.currentProduct = (response as DetailResponse).data
          // 防抖存储到 localStorage
          this.debouncedStorage('currentProduct', this.currentProduct)
        } else {
          // 处理错误响应
          const error = response as ApiError
          this.detailError = error.message || '获取商品详情失败'
          console.error('获取商品详情失败:', error)
        }
      } catch (error) {
        // 处理网络或其他错误
        this.detailError = error instanceof Error ? error.message : '获取商品详情失败'
        console.error('获取商品详情失败:', error)
      } finally {
        // 无论成功失败，都关闭加载状态
        this.detailLoading = false
      }
    },

    /**
     * 领取商品
     * @param {number} productId - 商品 ID
     * @returns {Promise<any>} 领取结果
     */
    async claimProduct(productId: number) {
      try {
        // 设置领取加载状态
        this.claimLoading = true
        // 清除之前的错误
        this.claimError = null

        // 调用 API 领取商品
        const result = await productApi.claim(productId, this.userId)

        // 查找商品信息
        const product = this.products.find(p => p.skuId === productId) || this.currentProduct
        // 创建领取记录
        const claimRecord: ClaimRecord = {
          productId,
          productName: product?.productName || '未知商品',
          claimedAt: new Date().toISOString(),
          status: 'code' in result && result.code === 200 ? 'success' : 'failed',
          message: 'message' in result ? result.message : '领取商品失败'
        }

        // 添加领取记录到列表开头
        this.claimRecords.unshift(claimRecord)
        // 限制领取记录数量为 50 条
        if (this.claimRecords.length > 50) {
          this.claimRecords = this.claimRecords.slice(0, 50)
        }

        // 防抖存储到 localStorage
        this.debouncedStorage('claimRecords', this.claimRecords)

        // 返回领取结果
        return result
      } catch (error) {
        // 处理网络或其他错误
        this.claimError = error instanceof Error ? error.message : '领取商品失败'
        console.error('领取商品失败:', error)
        // 重新抛出错误，让调用方知道领取失败
        throw error
      } finally {
        // 无论成功失败，都关闭加载状态
        this.claimLoading = false
      }
    },

    /**
     * 清除所有错误信息
     */
    clearErrors() {
      this.searchError = null
      this.detailError = null
      this.claimError = null
    },

    /**
     * 设置用户 ID
     * @param {number} userId - 用户 ID
     */
    setUserId(userId: number) {
      this.userId = userId
      // 直接存储到 localStorage，不需要防抖
      localStorage.setItem('userId', userId.toString())
    },

    /**
     * 防抖存储数据到 localStorage
     * 避免频繁操作 localStorage 影响性能
     * @param {string} key - 存储键名
     * @param {StorageValue} value - 存储值
     */
    debouncedStorage(key: string, value: StorageValue) {
      const storageKey = `product_${key}`
      const timeoutKey = `storageTimeout_${key}`

      // 清除之前的定时器
      if (timeoutMap.has(timeoutKey)) {
        clearTimeout(timeoutMap.get(timeoutKey))
        timeoutMap.delete(timeoutKey)
      }

      // 设置新的定时器，300ms 后执行存储
      const timeoutId = setTimeout(() => {
        localStorage.setItem(storageKey, JSON.stringify(value))
        // 存储完成后删除定时器
        timeoutMap.delete(timeoutKey)
      }, 300)

      // 保存定时器 ID
      timeoutMap.set(timeoutKey, timeoutId)
    },

    /**
     * 清除所有状态和本地存储
     */
    clearState() {
      // 重置状态
      this.products = []
      this.currentProduct = null
      this.claimRecords = []
      this.searchLoading = false
      this.searchError = null
      this.detailLoading = false
      this.detailError = null
      this.claimLoading = false
      this.claimError = null

      // 清除本地存储
      localStorage.removeItem('products')
      localStorage.removeItem('currentProduct')
      localStorage.removeItem('claimRecords')
    }
  }
})

/**
 * 监听 localStorage 变化，实现多标签页状态同步
 * 当其他标签页修改了商品相关的 localStorage 数据时，自动更新当前标签页的状态
 */
window.addEventListener('storage', event => {
  // 只处理以 'product_' 开头的键
  if (event.key?.startsWith('product_')) {
    // 提取实际的键名
    const key = event.key.replace('product_', '')
    // 获取状态管理实例
    const store = useProductStore()

    // 根据键名更新对应状态
    switch (key) {
      case 'products':
        store.products = JSON.parse(event.newValue || '[]')
        break
      case 'currentProduct':
        store.currentProduct = JSON.parse(event.newValue || 'null')
        break
      case 'claimRecords':
        store.claimRecords = JSON.parse(event.newValue || '[]')
        break
      case 'userId':
        store.userId = parseInt(event.newValue || '1')
        break
    }
  }
})

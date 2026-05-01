/**
 * HTTP 请求封装模块
 * 基于 Axios 实现，提供统一的请求拦截、响应处理、错误处理和 Mock 数据支持
 * 
 * 主要功能：
 * - 请求拦截：自动添加 token、CSRF 令牌
 * - 响应拦截：缓存响应、自动重试、提取响应数据
 * - 错误处理：统一的错误格式和日志记录
 * - Mock 支持：开发阶段使用模拟数据
 */

// 导入 Axios 核心库
import axios from 'axios'
// 导入 Axios 类型定义
import type { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
// 导入 Mock 数据函数和类型
import { mockClaim, mockGetDetail, mockSearch, type SearchParams } from '@/mock/product'

/**
 * API 错误类型定义
 * 统一的错误响应格式
 */
export interface ApiError {
  /** 错误状态码 */
  code: number
  /** 错误消息 */
  message: string
  /** 附加数据（可选） */
  data?: unknown
  /** 错误发生时间戳 */
  timestamp: number
}

/**
 * 全局错误处理函数
 * 将各种错误类型统一转换为 ApiError 格式
 * @param {unknown} error - 原始错误对象
 * @returns {ApiError} 统一的错误对象
 */
const handleApiError = (error: unknown): ApiError => {
  let apiError: ApiError

  // 判断是否为 Axios 错误
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<{ message?: string, data?: unknown }>

    // 服务器响应错误
    if (axiosError.response) {
      const { status, data } = axiosError.response
      apiError = {
        code: status || 0,
        message: data?.message || `服务器错误 (${status})`,
        data: data?.data,
        timestamp: Date.now()
      }
    } else if (axiosError.request) {
      // 请求已发送但无响应（网络错误）
      apiError = {
        code: 0,
        message: '网络错误，请检查网络连接',
        timestamp: Date.now()
      }
    } else {
      // 请求配置错误
      apiError = {
        code: 0,
        message: axiosError.message || '请求失败',
        timestamp: Date.now()
      }
    }
  } else if (error instanceof Error) {
    // 原生 JavaScript 错误
    apiError = {
      code: 0,
      message: error.message || '请求失败',
      timestamp: Date.now()
    }
  } else {
    // 其他未知错误
    apiError = {
      code: 0,
      message: '请求失败',
      timestamp: Date.now()
    }
  }

  // 输出错误日志
  console.error('API Error:', apiError)

  return apiError
}

/**
 * 错误日志记录函数
 * 记录详细的错误信息，便于调试和追踪
 * @param {ApiError} error - 错误对象
 * @param {string} [context] - 错误发生的上下文信息
 */
const logError = (error: ApiError, context?: string) => {
  const errorLog = {
    ...error,
    context,
    url: window.location.href,
    userAgent: navigator.userAgent
  }

  console.error('Error Log:', errorLog)
}

/**
 * 创建 Axios 实例
 * 配置基础 URL、超时时间和默认请求头
 */
const request = axios.create({
  /** 基础 URL，从环境变量获取或默认为 /api */
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  /** 请求超时时间（毫秒） */
  timeout: 10000,
  /** 默认请求头 */
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求缓存 Map
 * key: 请求标识（method:url:params:data）
 * value: 响应数据
 */
const requestCache = new Map<string, unknown>()

/**
 * 请求防抖定时器 Map
 * key: 请求标识
 * value: 定时器 ID
 */
const debouncedRequests = new Map<string, ReturnType<typeof setTimeout>>()

/**
 * 待处理请求 Map
 * 用于追踪正在进行的请求
 */
const pendingRequests = new Map<string, unknown>()

/**
 * 请求拦截器
 * 在发送请求前执行的逻辑
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig<unknown>): InternalAxiosRequestConfig<unknown> | Promise<InternalAxiosRequestConfig<unknown>> => {
    // 添加 Authorization 令牌
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加 CSRF 令牌
    const csrfToken = localStorage.getItem('csrfToken')
    if (csrfToken) {
      config.headers['X-CSRF-Token'] = csrfToken
    }

    // 生成请求缓存 key
    const cacheKey = `${config.method}:${config.url}:${JSON.stringify(config.params || {})}:${JSON.stringify(config.data || {})}`

    // 检查缓存
    if (requestCache.has(cacheKey)) {
      const cached = requestCache.get(cacheKey)
      if (cached && typeof cached === 'object' && 'headers' in cached) {
        return cached as InternalAxiosRequestConfig<unknown>
      }
    }

    // 防抖处理：清除之前的定时器
    if (debouncedRequests.has(cacheKey)) {
      clearTimeout(debouncedRequests.get(cacheKey))
    }

    // 设置 300ms 防抖延迟
    return new Promise<InternalAxiosRequestConfig<unknown>>(resolve => {
      const timeoutId = setTimeout(() => {
        debouncedRequests.delete(cacheKey)
        resolve(config)
      }, 300)
      debouncedRequests.set(cacheKey, timeoutId)
    })
  },
  // 请求错误处理
  error => {
    const apiError = handleApiError(error)
    logError(apiError, 'Request Interceptor')
    return Promise.reject(apiError)
  }
)

/**
 * 响应拦截器
 * 在处理响应后执行的逻辑
 */
request.interceptors.response.use(
  // 成功响应处理
  response => {
    // 生成缓存 key
    const params = response.config.params || {}
    const data = response.config.data || {}
    const method = response.config.method || 'get'
    const url = response.config.url || ''
    const cacheKey = `${method}:${url}:${JSON.stringify(params)}:${JSON.stringify(data)}`

    // 缓存响应数据
    requestCache.set(cacheKey, response)

    // 5 分钟后清除缓存
    setTimeout(() => {
      requestCache.delete(cacheKey)
    }, 5 * 60 * 1000)

    // 提取并保存 CSRF 令牌
    const csrfToken = response.headers['x-csrf-token'] || response.headers['X-CSRF-Token']
    if (csrfToken) {
      localStorage.setItem('csrfToken', csrfToken as string)
    }

    // 从待处理列表中移除
    pendingRequests.delete(cacheKey)

    // 只返回响应数据部分
    return response.data
  },
  // 错误响应处理
  error => {
    const apiError = handleApiError(error)
    logError(apiError, 'Response Interceptor')

    const config = error.config

    // 重试逻辑：最多重试 3 次
    if (config && !config.retryCount) {
      config.retryCount = 0
    }
    if (config && config.retryCount < 3) {
      config.retryCount++
      console.log(`Retrying request (${config.retryCount}/3)...`)
      return new Promise(resolve => {
        setTimeout(() => {
          resolve(request(config))
        }, 1000)
      })
    }

    // 清理待处理请求
    if (config) {
      const params = config.params || {}
      const data = config.data || {}
      const method = config.method || 'get'
      const url = config.url || ''
      const cacheKey = `${method}:${url}:${JSON.stringify(params)}:${JSON.stringify(data)}`
      pendingRequests.delete(cacheKey)
    }

    return Promise.reject(apiError)
  }
)

/**
 * Mock 参数类型定义
 */
type MockParams = Record<string, string | number>

/**
 * 保存原始的 post 和 get 方法
 */
const originalPost = request.post
const originalGet = request.get

/**
 * 重写 post 方法，添加 Mock 数据支持
 */
request.post = (<T = unknown, R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>) => {
  // 商品搜索接口使用 Mock
  if (url === '/product/search') {
    return mockSearch(data as SearchParams) as unknown as Promise<R>
  }
  // 商品领取（秒杀）接口使用 Mock
  else if (url.includes('/api/seckill/do/')) {
    const productId = parseInt(url.split('/').pop() || '0')
    const params = config?.params as MockParams | undefined
    const userId = params?.userId ? String(params.userId) : '1'
    return mockClaim(productId, parseInt(userId)) as unknown as Promise<R>
  }
  // 其他接口使用真实请求
  return originalPost<T, R, D>(url, data, config)
}) as typeof originalPost

/**
 * 重写 get 方法，添加 Mock 数据支持
 */
request.get = (<T = unknown, R = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>) => {
  // 商品详情接口使用 Mock（匹配 /product/{id} 格式）
  if (url.match(/\/product\/\d+/)) {
    const skuId = parseInt(url.split('/').pop() || '0')
    return mockGetDetail(skuId) as unknown as Promise<R>
  }
  // 其他接口使用真实请求
  return originalGet<T, R, D>(url, config)
}) as typeof originalGet

/**
 * 导出配置好的 Axios 实例
 */
export default request
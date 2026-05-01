/**
 * 请求节流工具模块
 * 提供请求限流、防抖、节流等功能，用于控制请求频率，避免请求过多导致服务器压力过大或被限流
 */

/**
 * 节流配置选项接口
 * 用于配置请求节流器的参数
 */
export interface ThrottleOptions {
  /**
   * 时间间隔内允许的最大请求数量
   * @default 10
   */
  limit?: number
  
  /**
   * 时间间隔（毫秒）
   * 在此时间间隔内计算请求数量
   * @default 1000（1秒）
   */
  interval?: number
}

/**
 * 请求节流器类
 * 用于控制请求频率，防止请求过于频繁
 * 
 * 使用场景：
 * - 用户快速点击按钮导致多次请求
 * - 搜索框输入时控制搜索请求频率
 * - 防止 API 接口被频繁调用导致限流
 */
export class RequestThrottle {
  /**
   * 待处理的请求队列
   * key: 请求标识（如 API 路径）
   * value: 同一请求的待处理数量
   */
  private pendingRequests: Map<string, number>
  
  /**
   * 请求计数器
   * key: 请求标识
   * value: 在当前时间间隔内的请求次数
   */
  private requestCounts: Map<string, number>
  
  /**
   * 定时器存储
   * 用于存储 setTimeout 的 ID，便于后续清除
   */
  private timers: Map<string, ReturnType<typeof setTimeout>>
  
  /**
   * 时间间隔内允许的最大请求数
   */
  private limit: number
  
  /**
   * 时间间隔（毫秒）
   */
  private interval: number

  /**
   * 构造函数
   * @param {ThrottleOptions} options - 配置选项
   * @param {number} options.limit - 最大请求数，默认为 10
   * @param {number} options.interval - 时间间隔，默认为 1000ms
   */
  constructor(options: ThrottleOptions = {}) {
    // 设置最大请求数，默认为 10
    this.limit = options.limit || 10
    // 设置时间间隔，默认为 1000ms（1秒）
    this.interval = options.interval || 1000
    // 初始化待处理请求 Map
    this.pendingRequests = new Map()
    // 初始化请求计数 Map
    this.requestCounts = new Map()
    // 初始化定时器 Map
    this.timers = new Map()
  }

  /**
   * 清理请求标识中的特殊字符
   * 将 < 和 > 替换为空，避免 XSS 攻击或标识错误
   * @param {string} key - 请求标识
   * @returns {string} 清理后的标识
   */
  private cleanKey(key: string): string {
    return key.replace(/[<>]/g, '')
  }

  /**
   * 执行请求（带节流控制）
   * @template T - 请求返回数据类型
   * @param {string} key - 请求标识（如 API 路径）
   * @param {() => Promise<T>} request - 请求函数
   * @returns {Promise<T>} 请求结果
   * 
   * @description
   * - 如果当前请求数超过限制，等待一段时间后重试
   * - 记录每个请求的执行次数
   * - 在指定时间间隔后重置计数器
   */
  async execute<T>(key: string, request: () => Promise<T>): Promise<T> {
    // 清理标识中的特殊字符
    const cleanKeyName = this.cleanKey(key)
    
    // 检查是否有待处理的相同请求
    if (this.pendingRequests.has(cleanKeyName)) {
      const count = this.pendingRequests.get(cleanKeyName)!
      
      // 如果待处理请求数超过限制，等待后重试
      if (count >= this.limit) {
        return new Promise((resolve, reject) => {
          const timer = setTimeout(() => {
            // 递归重试
            this.execute(cleanKeyName, request).then(resolve).catch(reject)
          }, this.interval)
          // 存储重试定时器
          this.timers.set(cleanKeyName + '_retry', timer)
        })
      }
    }

    // 添加到待处理请求
    this.pendingRequests.set(cleanKeyName, (this.pendingRequests.get(cleanKeyName) || 0) + 1)

    // 如果没有该请求的计数器，初始化
    if (!this.requestCounts.has(cleanKeyName)) {
      this.requestCounts.set(cleanKeyName, 0)
      // 设置定时器，在 interval 后重置计数器
      const timer = setTimeout(() => {
        this.requestCounts.set(cleanKeyName, 0)
      }, this.interval)
      this.timers.set(cleanKeyName, timer)
    }

    // 增加请求计数
    this.requestCounts.set(cleanKeyName, this.requestCounts.get(cleanKeyName)! + 1)

    try {
      // 执行实际请求
      const result = await request()
      return result
    } finally {
      // 请求完成后，减少待处理数量
      const count = this.pendingRequests.get(cleanKeyName) || 1
      if (count <= 1) {
        // 如果没有待处理了，删除记录
        this.pendingRequests.delete(cleanKeyName)
      } else {
        // 否则减 1
        this.pendingRequests.set(cleanKeyName, count - 1)
      }
    }
  }

  /**
   * 清除所有状态
   * 清空所有待处理请求、计数器和定时器
   * 通常用于组件卸载或页面刷新时清理
   */
  clear() {
    // 清空待处理请求
    this.pendingRequests.clear()
    // 清空请求计数
    this.requestCounts.clear()
    // 清除所有定时器
    this.timers.forEach(timer => clearTimeout(timer))
    // 清空定时器存储
    this.timers.clear()
  }

  /**
   * 获取待处理的请求数量
   * @param {string} [key] - 可选的请求标识
   * @returns {number} 待处理的请求数量
   * 
   * @description
   * - 如果提供了 key，返回该请求的待处理数量
   * - 如果没有提供 key，返回所有待处理请求的总数
   */
  getPendingCount(key?: string): number {
    if (key) {
      return this.pendingRequests.get(this.cleanKey(key)) || 0
    }
    // 计算所有待处理请求的总数
    let total = 0
    this.pendingRequests.forEach(count => {
      total += count
    })
    return total
  }
}

/**
 * 全局请求节流器实例
 * 预配置的请求节流器，限制为：
 * - 最多 10 个并发请求
 * - 1 秒时间间隔
 */
export const requestThrottle = new RequestThrottle({
  limit: 10,
  interval: 1000
})

/**
 * 防抖函数
 * 在事件触发 n 秒后才执行，如果 n 秒内再次触发，则重新计时
 * 
 * @template T - 函数类型
 * @param {T} func - 要执行的函数
 * @param {number} wait - 等待时间（毫秒）
 * @returns {(...args: Parameters<T>) => void} 防抖后的函数
 * 
 * @example
 * const debouncedSearch = debounce(search, 300)
 * // 用户输入时，每 300ms 只执行一次搜索
 * input.addEventListener('input', () => debouncedSearch(query))
 */
export function debounce<T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  // 定时器引用
  let timeout: ReturnType<typeof setTimeout> | null = null
  
  // 返回防抖处理后的函数
  return function(this: unknown, ...args: Parameters<T>) {
    // 如果有定时器，清除它（重新计时）
    if (timeout) {
      clearTimeout(timeout)
    }
    
    // 设置新的定时器
    timeout = setTimeout(() => {
      // 执行函数
      func.apply(this, args)
    }, wait)
  }
}

/**
 * 节流函数
 * 规定时间内只执行一次，如果再次触发则忽略
 * 
 * @template T - 函数类型
 * @param {T} func - 要执行的函数
 * @param {number} wait - 时间间隔（毫秒）
 * @returns {(...args: Parameters<T>) => void} 节流后的函数
 * 
 * @example
 * const throttledScroll = throttle(handleScroll, 100)
 * // 滚动时，每 100ms 最多执行一次
 * window.addEventListener('scroll', () => throttledScroll())
 */
export function throttle<T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  // 上次执行时间
  let lastTime = 0
  
  // 返回节流处理后的函数
  return function(this: unknown, ...args: Parameters<T>) {
    // 当前时间
    const now = Date.now()
    
    // 如果距离上次执行已经超过 wait 时间，执行函数
    if (now - lastTime >= wait) {
      lastTime = now
      func.apply(this, args)
    }
  }
}

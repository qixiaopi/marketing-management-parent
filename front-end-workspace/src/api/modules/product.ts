/**
 * 商品 API 模块
 * 提供商品搜索、详情获取、领取等功能的接口封装
 * 
 * 注意：由于项目使用了 Mock 数据，实际请求会被拦截并返回模拟数据
 * @see {@link @/api/request.ts} 了解 Mock 拦截逻辑
 */

// 导入 HTTP 请求工具
import request from '@/api/request'
// 导入错误类型定义
import type { ApiError } from '@/api/request'

/**
 * 商品接口定义
 * 表示一个商品的基本信息
 */
export interface Product {
  /** 商品唯一标识 SKU ID */
  skuId: number
  /** 商品名称 */
  productName: string
  /** 商品描述 */
  description: string
  /** 商品价格（单位：元） */
  price: number
  /** 商品库存数量 */
  stock: number
  /** 商品图片 URL */
  image: string
}

/**
 * 搜索参数接口
 * 用于商品搜索的查询条件
 */
export interface SearchParams {
  /** 搜索关键词（可选） */
  keyword?: string
}

/**
 * 搜索响应接口
 * 商品列表搜索的返回结果
 */
export interface SearchResponse {
  /** 响应状态码，200 表示成功 */
  code: number
  /** 商品列表数据 */
  data: Product[]
  /** 响应消息 */
  message: string
}

/**
 * 详情响应接口
 * 单个商品详情的返回结果
 */
export interface DetailResponse {
  /** 响应状态码，200 表示成功 */
  code: number
  /** 商品详情数据 */
  data: Product
  /** 响应消息 */
  message: string
}

/**
 * 领取响应接口
 * 商品领取操作的返回结果
 */
export interface ClaimResponse {
  /** 响应状态码，200 表示成功 */
  code: number
  /** 领取成功时数据为 null */
  data: null
  /** 响应消息（如"领取成功"或"库存不足"） */
  message: string
}

/**
 * 商品 API 类型定义
 * 定义了所有商品相关 API 方法的签名
 */
export type ProductApi = {
  /**
   * 搜索商品
   * @param {SearchParams} params - 搜索参数
   * @returns {Promise<SearchResponse | ApiError>} 搜索结果或错误信息
   */
  search: (params: SearchParams) => Promise<SearchResponse | ApiError>
  
  /**
   * 获取商品详情
   * @param {number} skuId - 商品 SKU ID
   * @returns {Promise<DetailResponse | ApiError>} 商品详情或错误信息
   */
  getDetail: (skuId: number) => Promise<DetailResponse | ApiError>
  
  /**
   * 领取商品（秒杀功能）
   * @param {number} productId - 商品 ID
   * @param {number} userId - 用户 ID
   * @returns {Promise<ClaimResponse | ApiError>} 领取结果或错误信息
   */
  claim: (productId: number, userId: number) => Promise<ClaimResponse | ApiError>
}

/**
 * 商品 API 实现
 * 封装了所有商品相关的 HTTP 请求
 */
export const productApi: ProductApi = {
  /**
   * 搜索商品
   * @param {SearchParams} params - 搜索参数
   * @returns {Promise<SearchResponse | ApiError>}
   */
  search: async (params: SearchParams) => {
    try {
      // 发送 POST 请求到搜索接口
      return await request.post('/product/search', params)
    } catch (error) {
      // 捕获异常并转换为 ApiError 类型返回
      return error as ApiError
    }
  },
  
  /**
   * 获取商品详情
   * @param {number} skuId - 商品 SKU ID
   * @returns {Promise<DetailResponse | ApiError>}
   */
  getDetail: async (skuId: number) => {
    try {
      // 发送 GET 请求到详情接口
      return await request.get(`/product/${skuId}`)
    } catch (error) {
      // 捕获异常并转换为 ApiError 类型返回
      return error as ApiError
    }
  },
  
  /**
   * 领取商品（秒杀功能）
   * @param {number} productId - 商品 ID
   * @param {number} userId - 用户 ID
   * @returns {Promise<ClaimResponse | ApiError>}
   */
  claim: async (productId: number, userId: number) => {
    try {
      // 发送 POST 请求到秒杀接口
      return await request.post(`/api/seckill/do/${productId}?userId=${userId}`)
    } catch (error) {
      // 捕获异常并转换为 ApiError 类型返回
      return error as ApiError
    }
  },
}

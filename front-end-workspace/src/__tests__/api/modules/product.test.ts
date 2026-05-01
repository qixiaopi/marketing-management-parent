import { describe, it, expect, vi, beforeEach } from 'vitest'
import { productApi } from '@/api/modules/product'
import type { Mock } from 'vitest'

// 模拟 request 模块
vi.mock('@/api/request', () => {
  const mockPost = vi.fn()
  const mockGet = vi.fn()
  return {
    default: {
      post: mockPost,
      get: mockGet,
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    }
  }
})

// 导入模拟的 request
import request from '@/api/request'

describe('productApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('search', () => {
    it('should call post with correct parameters', async () => {
      const mockResponse = {
        code: 200,
        data: [
          {
            skuId: 1,
            productName: 'iPhone 15 Pro',
            description: 'Apple iPhone 15 Pro 256GB',
            price: 99900,
            stock: 50,
            image: '/src/assets/iphone15pro.svg'
          }
        ],
        message: 'success'
      }

      ;(request.post as Mock).mockResolvedValue(mockResponse)

      const params = { keyword: 'iPhone' }
      const result = await productApi.search(params)

      expect(request.post).toHaveBeenCalledWith('/product/search', params)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('getDetail', () => {
    it('should call get with correct parameters', async () => {
      const mockResponse = {
        code: 200,
        data: {
          skuId: 1,
          productName: 'iPhone 15 Pro',
          description: 'Apple iPhone 15 Pro 256GB',
          price: 99900,
          stock: 50,
          image: '/src/assets/iphone15pro.svg'
        },
        message: 'success'
      }

      ;(request.get as Mock).mockResolvedValue(mockResponse)

      const skuId = 1
      const result = await productApi.getDetail(skuId)

      expect(request.get).toHaveBeenCalledWith(`/product/${skuId}`)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('claim', () => {
    it('should call post with correct parameters', async () => {
      const mockResponse = {
        code: 200,
        data: null,
        message: '商品领取成功'
      }

      ;(request.post as Mock).mockResolvedValue(mockResponse)

      const productId = 1
      const userId = 123
      const result = await productApi.claim(productId, userId)

      expect(request.post).toHaveBeenCalledWith(`/api/seckill/do/${productId}?userId=${userId}`)
      expect(result).toEqual(mockResponse)
    })
  })
})
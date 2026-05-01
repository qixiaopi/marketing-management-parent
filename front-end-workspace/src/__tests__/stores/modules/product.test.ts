import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useProductStore } from '@/stores/modules/product'
import { productApi } from '@/api/modules/product'
import { createPinia, setActivePinia } from 'pinia'
import type { Mock } from 'vitest'

// 模拟 productApi 模块
vi.mock('@/api/modules/product', () => ({
  productApi: {
    search: vi.fn(),
    getDetail: vi.fn(),
    claim: vi.fn()
  }
}))

// 模拟 localStorage
const mockLocalStorage = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value.toString()
    },
    clear: () => {
      store = {}
    }
  }
})()

Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage
})

describe('useProductStore', () => {
  let store: ReturnType<typeof useProductStore>
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    vi.clearAllMocks()
    mockLocalStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
    store = useProductStore()
  })

  afterEach(() => {
    // 清理
  })

  describe('state', () => {
    it('should initialize with empty products array', () => {
      expect(store.products).toEqual([])
    })

    it('should initialize with null currentProduct', () => {
      expect(store.currentProduct).toBeNull()
    })

    it('should initialize with userId 1', () => {
      expect(store.userId).toBe(1)
    })
  })

  describe('actions', () => {
    describe('searchProducts', () => {
      it('should call productApi.search and update products', async () => {
        const mockProducts = [
          {
            skuId: 1,
            productName: 'iPhone 15 Pro',
            description: 'Apple iPhone 15 Pro 256GB',
            price: 99900,
            stock: 50,
            image: '/src/assets/iphone15pro.svg'
          }
        ]

        const mockResponse = {
          code: 200,
          data: mockProducts,
          message: 'success'
        }

        ;(productApi.search as Mock).mockResolvedValue(mockResponse)

        const params = { keyword: 'iPhone' }
        await store.searchProducts(params)

        expect(productApi.search).toHaveBeenCalledWith(params)
        expect(store.products).toEqual(mockProducts)
      })
    })

    describe('claimProduct', () => {
      it('should call productApi.claim with correct parameters', async () => {
        const mockResponse = {
          code: 200,
          data: null,
          message: '商品领取成功'
        }

        ;(productApi.claim as Mock).mockResolvedValue(mockResponse)

        const productId = 1
        const result = await store.claimProduct(productId)

        expect(productApi.claim).toHaveBeenCalledWith(productId, store.userId)
        expect(result).toEqual(mockResponse)
      })
    })
  })
})
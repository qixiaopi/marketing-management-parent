// 商品类型定义
export interface Product {
  skuId: number
  productName: string
  description: string
  price: number
  stock: number
  image: string
}

// 搜索参数类型定义
export interface SearchParams {
  keyword?: string
}

// 模拟商品数据
export const mockProducts: Product[] = [
  {
    skuId: 1,
    productName: 'iPhone 15 Pro',
    description: 'Apple iPhone 15 Pro 256GB 钛金属色',
    price: 99900,
    stock: 50,
    image: '/src/assets/iphone15pro.svg'
  },
  {
    skuId: 2,
    productName: 'AirPods Pro 2',
    description: 'Apple AirPods Pro 2 主动降噪耳机',
    price: 189900,
    stock: 100,
    image: '/src/assets/airpods-pro2.svg'
  },
  {
    skuId: 3,
    productName: 'MacBook Air M3',
    description: 'Apple MacBook Air M3 13英寸 8GB+256GB',
    price: 899900,
    stock: 30,
    image: '/src/assets/macbook-air-m3.svg'
  },
  {
    skuId: 4,
    productName: 'iPad Pro 12.9',
    description: 'Apple iPad Pro 12.9英寸 M2芯片',
    price: 899900,
    stock: 20,
    image: '/src/assets/ipad-pro-129.svg'
  }
]

// 模拟搜索功能
export const mockSearch = (params: SearchParams) => {
  return new Promise<{ code: number; data: Product[]; message: string }>(resolve => {
    setTimeout(() => {
      let result = [...mockProducts]
      const keyword = params.keyword || ''
      if (keyword) {
        result = result.filter(product =>
          product.productName.includes(keyword) ||
          product.description.includes(keyword)
        )
      }
      resolve({
        code: 200,
        data: result,
        message: 'success'
      })
    }, 300)
  })
}

// 模拟获取商品详情
export const mockGetDetail = (skuId: number) => {
  return new Promise<{ code: number; data: Product | null; message: string }>(resolve => {
    setTimeout(() => {
      const product = mockProducts.find(p => p.skuId === skuId)
      if (product) {
        resolve({
          code: 200,
          data: product,
          message: 'success'
        })
      } else {
        resolve({
          code: 404,
          data: null,
          message: '商品不存在'
        })
      }
    }, 300)
  })
}

// 模拟商品领取
export const mockClaim = (productId: number, _userId: number) => {
  return new Promise<{ code: number; data: null; message: string }>(resolve => {
    setTimeout(() => {
      const product = mockProducts.find(p => p.skuId === productId)
      if (product) {
        resolve({
          code: 200,
          data: null,
          message: '商品领取成功'
        })
      } else {
        resolve({
          code: 404,
          data: null,
          message: '商品不存在'
        })
      }
    }, 500)
  })
}
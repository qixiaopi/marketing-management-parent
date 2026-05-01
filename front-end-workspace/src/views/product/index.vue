<template>
  <div class="product-list">
    <div class="container">
      <h1 class="page-title">商品中心</h1>
      <div class="search-bar">
        <div class="search-container">
          <el-input
            v-model="searchQuery"
            placeholder="搜索商品名称"
            class="search-input"
            :disabled="productStore.isSearching"
          />
          <el-button type="primary" @click="handleSearch" class="search-btn" :loading="productStore.isSearching">
            搜索
          </el-button>
        </div>
      </div>
      <el-alert
        v-if="productStore.searchError"
        :title="productStore.searchError"
        type="error"
        show-icon
        :closable="true"
        @close="productStore.clearErrors()"
        style="margin-bottom: 20px"
      />
      <div v-if="productStore.isSearching" class="loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else class="product-grid">
        <div v-for="product in productStore.products" :key="product.skuId" class="product-card">
          <div class="product-image">
            <LazyImage :src="product.image" :alt="product.productName" width="100%" height="220px" />
            <div class="image-overlay">
              <div class="overlay-content">
                <el-button link size="small" @click="handleView(product.skuId)" class="overlay-btn">
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.productName }}</h3>
            <p class="product-description">{{ product.description }}</p>
            <div class="product-price">¥{{ (product.price / 100).toFixed(2) }}</div>
            <div class="product-actions">
              <el-button type="primary" size="small" @click="handleView(product.skuId)" class="view-btn">
                查看详情
              </el-button>
              <el-button
                type="success"
                size="small"
                @click="handleClaim(product.skuId)"
                class="claim-btn"
                :loading="productStore.isClaiming"
                :disabled="productStore.isClaiming"
              >
                立即领取
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!productStore.isSearching && !productStore.hasProducts" class="empty">
        <div class="empty-icon">📦</div>
        <p>暂无商品</p>
        <el-button type="primary" @click="handleSearch" class="refresh-btn"> 刷新 </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入商品状态管理
import { useProductStore } from '@/stores/modules/product'

// 路由实例
const router = useRouter()
// 商品状态管理实例
const productStore = useProductStore()
// 搜索关键词（响应式变量）
const searchQuery = ref('')
// 搜索防抖定时器
let searchTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 处理搜索功能（带防抖）
 */
const handleSearch = async () => {
  // 清除之前的定时器
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  // 300ms 防抖，避免频繁搜索
  searchTimer = setTimeout(async () => {
    // 验证搜索关键词长度
    if (searchQuery.value && searchQuery.value.length > 50) {
      ElMessage.warning('搜索关键词不能超过50个字符')
      return
    }

    try {
      // 调用 store 中的搜索方法
      await productStore.searchProducts({ keyword: searchQuery.value })
    } catch {
      // 错误已在 store 中处理
    }
  }, 300)
}

/**
 * 跳转到商品详情页
 * @param skuId 商品ID
 */
const handleView = (skuId: number) => {
  router.push(`/product/detail/${skuId}`)
}

/**
 * 处理商品领取
 * @param skuId 商品ID
 */
const handleClaim = async (skuId: number) => {
  try {
    // 调用 store 中的领取方法
    const result = await productStore.claimProduct(skuId)

    // 根据领取结果显示不同提示
    if (result.code === 200) {
      // 领取成功
      await ElMessageBox.alert('恭喜您！商品领取成功！', '领取成功', {
        confirmButtonText: '确定',
        type: 'success'
      })
      // 领取成功后刷新商品列表
      handleSearch()
    } else {
      // 领取失败
      await ElMessageBox.alert(result.message, '领取失败', {
        confirmButtonText: '确定',
        type: 'error'
      })
    }
  } catch {
    // 网络错误等异常
    await ElMessageBox.alert('抱歉，商品领取失败，请稍后再试！', '领取失败', {
      confirmButtonText: '确定',
      type: 'error'
    })
  }
}

/**
 * 组件挂载时自动执行搜索
 */
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
/* 商品列表页面容器 */
.product-list {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 0;
  position: relative;
  overflow: hidden;
}

/* 网格背景 */
.product-list::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse"><path d="M 20 0 L 0 0 0 20" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>')
    repeat;
  opacity: 0.4;
  z-index: 0;
}

/* 浮动效果 */
.product-list::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: float 20s linear infinite;
  z-index: 0;
}

/* 浮动动画 */
@keyframes float {
  0% {
    transform: rotate(0deg) translate(0, 0);
  }
  100% {
    transform: rotate(360deg) translate(0, 0);
  }
}

/* 容器 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

/* 页面标题 */
.page-title {
  font-size: 48px;
  font-weight: 800;
  margin-bottom: 40px;
  text-align: center;
  color: #ffffff;
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  animation: titleFloat 3s ease-in-out infinite alternate;
}

/* 标题浮动动画 */
@keyframes titleFloat {
  from {
    transform: translateY(0px);
  }
  to {
    transform: translateY(-10px);
  }
}

/* 搜索栏 */
.search-bar {
  margin-bottom: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

/* 搜索容器 */
.search-container {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
}

/* 搜索输入框 */
.search-input {
  width: 100% !important;
  height: 65px !important;
  font-size: 18px !important;
  border-radius: 32px !important;
  border: 2px solid rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(15px) !important;
  transition: all 0.3s ease !important;
  padding: 0 60px 0 30px !important;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1) !important;
  outline: none !important;
}

/* 搜索输入框聚焦效果 */
.search-input:focus {
  border-color: #667eea !important;
  box-shadow: 0 0 30px rgba(102, 126, 234, 0.6) !important;
  transform: translateY(-3px) !important;
  background: rgba(255, 255, 255, 0.98) !important;
}

/* 搜索按钮 */
.search-btn {
  position: absolute !important;
  right: 5px !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
  height: 55px !important;
  padding: 0 30px !important;
  border-radius: 28px !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  color: #ffffff !important;
  font-weight: 600 !important;
  font-size: 16px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4) !important;
  cursor: pointer !important;
  z-index: 1 !important;
}

/* 搜索按钮悬停效果 */
.search-btn:hover {
  transform: translateY(-50%) scale(1.05) !important;
  box-shadow: 0 8px 28px rgba(102, 126, 234, 0.6) !important;
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%) !important;
}

/* 搜索按钮点击效果 */
.search-btn:active {
  transform: translateY(-50%) scale(0.98) !important;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.5) !important;
}

/* 商品网格布局 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 30px;
}

/* 商品卡片 */
.product-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 24px;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 卡片悬停效果 - 渐变覆盖层 */
.product-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.3), transparent);
  transition: left 0.6s ease;
}

/* 卡片悬停效果 */
.product-card:hover::before {
  left: 100%;
}

/* 卡片悬停效果 */
.product-card:hover {
  transform: translateY(-15px) scale(1.02);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  border-color: rgba(102, 126, 234, 0.5);
}

/* 卡片边框动画 */
.product-card::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(45deg, #667eea, #764ba2, #667eea, #764ba2);
  border-radius: 20px;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.4s ease;
  background-size: 400%;
  animation: borderAnimation 8s linear infinite;
}

/* 卡片悬停时显示边框动画 */
.product-card:hover::after {
  opacity: 1;
}

/* 边框动画 */
@keyframes borderAnimation {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 400% 0;
  }
}

/* 商品图片容器 */
.product-image {
  text-align: center;
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 16px;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

/* 商品图片悬停效果 */
.product-image:hover {
  transform: scale(1.05);
}

/* 图片覆盖层 */
.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(102, 126, 234, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  border-radius: 16px;
}

/* 图片悬停时显示覆盖层 */
.product-image:hover .image-overlay {
  opacity: 1;
}

/* 覆盖层内容 */
.overlay-content {
  transform: translateY(20px);
  transition: all 0.3s ease;
}

/* 覆盖层内容动画 */
.product-image:hover .overlay-content {
  transform: translateY(0);
}

/* 覆盖层按钮 */
.overlay-btn {
  color: white !important;
  font-weight: 600 !important;
  font-size: 14px !important;
}

/* 商品图片 */
.product-image img {
  max-width: 100%;
  height: 220px;
  object-fit: contain;
  border-radius: 12px;
  transition: all 0.3s ease;
}

/* 商品名称 */
.product-name {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 14px;
  color: #333;
  height: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  transition: color 0.3s ease;
}

/* 商品名称悬停效果 */
.product-card:hover .product-name {
  color: #667eea;
}

/* 商品描述 */
.product-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 18px;
  height: 52px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 商品价格 */
.product-price {
  font-size: 28px;
  font-weight: 700;
  color: #ff4d4f;
  margin-bottom: 14px;
  text-shadow: 0 2px 4px rgba(255, 77, 79, 0.3);
  transition: all 0.3s ease;
  animation: pricePulse 2s ease-in-out infinite alternate;
}

/* 价格脉冲动画 */
@keyframes pricePulse {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.05);
  }
}

/* 操作按钮容器 */
.product-actions {
  display: flex;
  gap: 12px;
}

/* 查看详情按钮 */
.view-btn {
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important;
  flex: 1;
  font-weight: 600 !important;
  border-radius: 12px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3) !important;
}

/* 查看详情按钮悬停效果 */
.view-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.4) !important;
}

/* 领取按钮 */
.claim-btn {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%) !important;
  border: none !important;
  flex: 1;
  font-weight: 600 !important;
  border-radius: 12px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 16px rgba(82, 196, 26, 0.3) !important;
}

/* 领取按钮悬停效果 */
.claim-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(82, 196, 26, 0.4) !important;
}

/* 加载中状态 */
.loading {
  text-align: center;
  padding: 120px 0;
  color: white;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

/* 加载动画 */
.loading-spinner {
  width: 80px;
  height: 80px;
  border: 8px solid rgba(255, 255, 255, 0.3);
  border-top: 8px solid #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 24px;
}

/* 旋转动画 */
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.empty {
  text-align: center;
  padding: 120px 0;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  margin-top: 40px;
}

/* 空状态图标 */
.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
  animation: bounce 2s ease-in-out infinite;
}

/* 弹跳动画 */
@keyframes bounce {
  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-30px);
  }
  60% {
    transform: translateY(-15px);
  }
}

/* 空状态文本 */
.empty p {
  font-size: 20px;
  color: #666;
  margin-bottom: 30px;
}

/* 刷新按钮 */
.refresh-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  border-radius: 25px !important;
  padding: 10px 30px !important;
  font-weight: 600 !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3) !important;
}

/* 刷新按钮悬停效果 */
.refresh-btn:hover {
  transform: translateY(-3px) !important;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4) !important;
}

/* 响应式设计 - 平板 */
@media (max-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
  }
}

/* 响应式设计 - 手机 */
@media (max-width: 768px) {
  .product-list {
    padding: 20px 0;
  }

  .page-title {
    font-size: 36px;
    margin-bottom: 30px;
  }

  .search-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .search-input {
    width: 100% !important;
    height: 55px !important;
    font-size: 16px !important;
    padding: 0 50px 0 20px !important;
  }

  .search-btn {
    height: 45px !important;
    padding: 0 20px !important;
    font-size: 14px !important;
  }

  .product-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .product-card {
    padding: 20px;
  }

  .product-image img {
    height: 180px;
  }

  .product-name {
    font-size: 18px;
  }

  .product-price {
    font-size: 24px;
  }

  .product-actions {
    flex-direction: column;
  }

  .view-btn,
  .claim-btn {
    padding: 12px 0 !important;
  }
}

/* 响应式设计 - 小屏幕手机 */
@media (max-width: 480px) {
  .product-list {
    padding: 10px 0;
  }

  .page-title {
    font-size: 28px;
    margin-bottom: 20px;
  }

  .container {
    padding: 0 15px;
  }

  .search-input {
    height: 48px !important;
    font-size: 14px !important;
  }

  .search-btn {
    height: 38px !important;
    font-size: 12px !important;
  }

  .product-card {
    padding: 16px;
  }

  .product-image {
    padding: 16px;
  }

  .product-image img {
    height: 150px;
  }

  .product-name {
    font-size: 16px;
  }

  .product-description {
    font-size: 12px;
  }

  .product-price {
    font-size: 20px;
  }

  .loading {
    padding: 80px 0;
  }

  .loading-spinner {
    width: 60px;
    height: 60px;
  }

  .empty {
    padding: 80px 0;
    margin-top: 20px;
  }

  .empty-icon {
    font-size: 60px;
  }

  .empty p {
    font-size: 16px;
  }

  .refresh-btn {
    padding: 8px 20px !important;
  }
}
</style>

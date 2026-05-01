<template>
  <div class="product-detail">
    <div class="container">
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item><router-link to="/product">商品列表</router-link></el-breadcrumb-item>
          <el-breadcrumb-item>商品详情</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <el-alert
        v-if="productStore.detailError"
        :title="productStore.detailError"
        type="error"
        show-icon
        :closable="true"
        @close="productStore.clearErrors()"
        style="margin-bottom: 20px"
      />

      <div v-if="productStore.isLoadingDetail" class="loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="productStore.currentProduct" class="product-card">
        <div class="product-content">
          <div class="product-image">
            <LazyImage
              :src="productStore.currentProduct.image"
              :alt="productStore.currentProduct.productName"
              width="100%"
              height="450px"
            />
            <div class="image-overlay">
              <div class="overlay-content">
                <el-button link size="small" @click="handleBack" class="overlay-btn"> 返回列表 </el-button>
              </div>
            </div>
          </div>
          <div class="product-info">
            <h1 class="product-name">{{ productStore.currentProduct.productName }}</h1>
            <div class="product-price">¥{{ (productStore.currentProduct.price / 100).toFixed(2) }}</div>
            <div class="product-description">
              <h3>商品描述</h3>
              <p>{{ productStore.currentProduct.description }}</p>
            </div>
            <div class="product-actions">
              <el-button
                type="primary"
                size="large"
                @click="handleClaim"
                class="claim-btn"
                :loading="productStore.isClaiming"
                :disabled="productStore.isClaiming"
              >
                立即领取
              </el-button>
              <el-button size="large" @click="handleBack" class="back-btn">返回</el-button>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="error">
        <div class="error-icon">❌</div>
        <p>商品不存在</p>
        <el-button type="primary" @click="handleBack" class="back-btn">返回列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 商品详情页组件
 * 展示商品详细信息，提供领取商品功能
 */

// 导入 Pinia 商品状态管理
import { useProductStore } from '@/stores/modules/product'

/**
 * Vue Router 相关
 * router: 路由实例，用于导航
 * route: 当前路由信息，包含路由参数
 */
const router = useRouter()
const route = useRoute()

/**
 * Pinia Store 实例
 * 用于管理商品状态和执行业务逻辑
 */
const productStore = useProductStore()

/**
 * 上次领取时间（响应式变量）
 * 用于限制领取操作频率，防止重复提交
 */
const lastClaimTime = ref(0)

/**
 * 处理商品领取
 * @description 包含频率限制、领取逻辑、结果提示等完整流程
 */
const handleClaim = async () => {
  // 验证商品信息
  if (!productStore.currentProduct) return
  if (!productStore.currentProduct.skuId) {
    ElMessage.error('商品信息错误')
    return
  }

  // 频率限制：2秒内只能领取一次
  const now = Date.now()
  if (now - lastClaimTime.value < 2000) {
    ElMessage.warning('操作太频繁，请稍后再试')
    return
  }

  // 更新上次领取时间
  lastClaimTime.value = now

  try {
    // 调用 store 中的领取方法
    const result = await productStore.claimProduct(productStore.currentProduct.skuId)

    // 根据领取结果显示不同提示
    if (result.code === 200) {
      // 领取成功
      await ElMessageBox.alert('恭喜您！商品领取成功！', '领取成功', {
        confirmButtonText: '确定',
        type: 'success'
      })
      // 领取成功后刷新商品信息
      loadProduct()
    } else {
      // 领取失败（业务错误）
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
 * 返回商品列表页
 * @description 导航到商品列表页面
 */
const handleBack = () => {
  router.push('/product')
}

/**
 * 加载商品详情
 * @description 根据路由参数获取商品详情数据
 */
const loadProduct = async () => {
  // 从路由参数获取商品 SKU ID
  const skuId = route.params.id
  if (!skuId) return

  try {
    // 调用 store 中的获取详情方法
    await productStore.getProductDetail(Number(skuId))
  } catch {
    // 错误已在 store 中处理，此处无需额外处理
  }
}

/**
 * 组件挂载时执行
 * @description 页面加载完成后立即加载商品详情
 */
onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.product-detail {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 0;
  position: relative;
  overflow: hidden;
}

.product-detail::before {
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

.product-detail::after {
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

@keyframes float {
  0% {
    transform: rotate(0deg) translate(0, 0);
  }
  100% {
    transform: rotate(360deg) translate(0, 0);
  }
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

.breadcrumb {
  margin-bottom: 40px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  backdrop-filter: blur(15px);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.breadcrumb:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
  border-color: rgba(102, 126, 234, 0.5);
}

.breadcrumb .el-breadcrumb__item {
  font-size: 18px;
  color: #666;
  transition: all 0.3s ease;
}

.breadcrumb .el-breadcrumb__item a {
  color: #667eea !important;
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 600;
  position: relative;
}

.breadcrumb .el-breadcrumb__item a::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s ease;
}

.breadcrumb .el-breadcrumb__item a:hover {
  color: #764ba2 !important;
  text-shadow: 0 0 10px rgba(118, 75, 162, 0.3);
}

.breadcrumb .el-breadcrumb__item a:hover::after {
  width: 100%;
}

.loading {
  text-align: center;
  padding: 150px 0;
  color: white;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.loading-spinner {
  width: 100px;
  height: 100px;
  border: 10px solid rgba(255, 255, 255, 0.3);
  border-top: 10px solid #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 30px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.product-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 0 16px 64px rgba(0, 0, 0, 0.2);
  padding: 60px;
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.product-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.3), transparent);
  transition: left 0.8s ease;
}

.product-card:hover::before {
  left: 100%;
}

.product-card::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(45deg, #667eea, #764ba2, #667eea, #764ba2);
  border-radius: 24px;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.4s ease;
  background-size: 400%;
  animation: borderAnimation 8s linear infinite;
}

.product-card:hover::after {
  opacity: 1;
}

@keyframes borderAnimation {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 400% 0;
  }
}

.product-content {
  display: flex;
  gap: 80px;
  position: relative;
  z-index: 1;
}

.product-image {
  flex: 0 0 500px;
  padding: 32px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 20px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

@media (max-width: 1024px) {
  .product-card {
    padding: 40px;
  }

  .product-content {
    gap: 40px;
  }

  .product-image {
    flex: 0 0 400px;
  }
}

@media (max-width: 768px) {
  .product-detail {
    padding: 20px 0;
  }

  .breadcrumb {
    margin-bottom: 30px;
    padding: 16px;
  }

  .breadcrumb .el-breadcrumb__item {
    font-size: 14px;
  }

  .product-card {
    padding: 30px;
  }

  .product-content {
    flex-direction: column;
    gap: 30px;
  }

  .product-image {
    flex: none;
    width: 100%;
    max-width: 400px;
    margin: 0 auto;
  }

  .product-name {
    font-size: 24px;
  }

  .product-price {
    font-size: 36px;
  }

  .product-actions {
    flex-direction: column;
    gap: 12px;
  }

  .claim-btn,
  .back-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .product-detail {
    padding: 10px 0;
  }

  .container {
    padding: 0 15px;
  }

  .breadcrumb {
    margin-bottom: 20px;
    padding: 12px;
  }

  .product-card {
    padding: 20px;
  }

  .product-image {
    padding: 20px;
  }

  .product-image img {
    height: 200px;
  }

  .product-name {
    font-size: 20px;
  }

  .product-price {
    font-size: 28px;
  }

  .product-description h3 {
    font-size: 18px;
  }

  .product-description p {
    font-size: 14px;
  }

  .loading {
    padding: 100px 0;
  }

  .loading-spinner {
    width: 80px;
    height: 80px;
  }

  .error {
    padding: 100px 0;
  }

  .error-icon {
    font-size: 60px;
  }

  .error p {
    font-size: 16px;
  }
}

.product-image:hover {
  transform: scale(1.05);
  box-shadow: 0 20px 64px rgba(0, 0, 0, 0.2);
}

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
  border-radius: 20px;
}

.product-image:hover .image-overlay {
  opacity: 1;
}

.overlay-content {
  transform: translateY(20px);
  transition: all 0.3s ease;
}

.product-image:hover .overlay-content {
  transform: translateY(0);
}

.overlay-btn {
  color: white !important;
  font-weight: 600 !important;
  font-size: 16px !important;
}

.product-image img {
  width: 100%;
  height: 450px;
  object-fit: contain;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.product-info {
  flex: 1;
  padding: 30px 0;
}

.product-name {
  font-size: 42px;
  font-weight: 800;
  margin-bottom: 40px;
  color: #333;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  -webkit-background-clip: text;
  animation: nameGlow 3s ease-in-out infinite alternate;
  line-height: 1.2;
}

@keyframes nameGlow {
  from {
    text-shadow: 0 0 10px rgba(102, 126, 234, 0.5);
  }
  to {
    text-shadow:
      0 0 20px rgba(102, 126, 234, 0.8),
      0 0 30px rgba(118, 75, 162, 0.5);
  }
}

.product-price {
  font-size: 56px;
  font-weight: 800;
  color: #ff4d4f;
  margin-bottom: 30px;
  text-shadow: 0 4px 8px rgba(255, 77, 79, 0.3);
  transition: all 0.3s ease;
  animation: pricePulse 2s ease-in-out infinite alternate;
}

@keyframes pricePulse {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.05);
  }
}

.product-description {
  margin-bottom: 60px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.product-description h3 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #333;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  -webkit-background-clip: text;
}

.product-description p {
  font-size: 18px;
  line-height: 1.8;
  color: #666;
}

.product-actions {
  display: flex;
  gap: 30px;
}

.claim-btn {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%) !important;
  border: none !important;
  flex: 2;
  font-size: 20px !important;
  font-weight: 700 !important;
  padding: 20px 0 !important;
  border-radius: 16px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 8px 32px rgba(82, 196, 26, 0.4) !important;
  position: relative;
  overflow: hidden;
}

.claim-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}

.claim-btn:hover::before {
  left: 100%;
}

.claim-btn:hover {
  transform: translateY(-5px) !important;
  box-shadow: 0 16px 48px rgba(82, 196, 26, 0.6) !important;
}

.back-btn {
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%) !important;
  border: none !important;
  flex: 1;
  font-size: 20px !important;
  font-weight: 700 !important;
  padding: 20px 0 !important;
  border-radius: 16px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 8px 32px rgba(24, 144, 255, 0.4) !important;
  position: relative;
  overflow: hidden;
}

.back-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}

.back-btn:hover::before {
  left: 100%;
}

.back-btn:hover {
  transform: translateY(-5px) !important;
  box-shadow: 0 16px 48px rgba(24, 144, 255, 0.6) !important;
}

.error {
  text-align: center;
  padding: 150px 0;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  backdrop-filter: blur(10px);
  box-shadow: 0 16px 64px rgba(0, 0, 0, 0.15);
  margin-top: 40px;
}

.error-icon {
  font-size: 120px;
  margin-bottom: 30px;
  animation: errorBounce 2s ease-in-out infinite;
}

@keyframes errorBounce {
  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-40px);
  }
  60% {
    transform: translateY(-20px);
  }
}

.error p {
  font-size: 28px;
  color: #666;
  margin-bottom: 40px;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .product-content {
    flex-direction: column;
    align-items: center;
    gap: 40px;
  }

  .product-image {
    flex: none;
    width: 100%;
    max-width: 500px;
  }

  .product-info {
    width: 100%;
    text-align: center;
  }

  .product-actions {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .product-detail {
    padding: 20px 0;
  }

  .product-card {
    padding: 30px;
  }

  .product-name {
    font-size: 32px;
    margin-bottom: 30px;
  }

  .product-price {
    font-size: 42px;
  }

  .product-actions {
    flex-direction: column;
  }

  .claim-btn,
  .back-btn {
    width: 100% !important;
  }

  .product-image img {
    height: 300px;
  }

  .product-description {
    padding: 20px;
  }
}
</style>

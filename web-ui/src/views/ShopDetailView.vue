<template>
  <div class="shop-detail-container" v-loading="loadingShop">
    <template v-if="shop.id">
      <div class="shop-header">
        <div class="shop-info">
          <img
            :src="shop.logoUrl || shop.logo || '/vite.svg'"
            :alt="shop.shopName"
            class="shop-logo"
          />
          <div class="shop-text">
            <h1>{{ shop.shopName }}</h1>
            <p class="shop-desc">{{ shop.description || '暂无店铺介绍' }}</p>
            <p class="shop-phone" v-if="shop.phone">
              联系电话: {{ shop.phone }}
            </p>
            <el-tag v-if="shop.status" size="small">
              {{ shop.status === 'ACTIVE' ? '营业中' : shop.status }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="shop-products">
        <h3>店铺商品</h3>

        <el-empty v-if="!loadingProducts && products.length === 0" description="暂无商品" />

        <div v-loading="loadingProducts" class="product-grid">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-card"
            @click="$router.push(`/product/${product.id}`)"
          >
            <el-card :body-style="{ padding: '0' }" shadow="hover">
              <div class="product-image-wrap">
                <img
                  :src="product.images?.[0] || product.mainImage || '/vite.svg'"
                  :alt="product.productName"
                  class="product-image"
                />
              </div>
              <div class="product-info">
                <h4 class="product-name">{{ product.productName }}</h4>
                <div class="product-bottom">
                  <span class="product-price">
                    <span class="price-symbol">&yen;</span>
                    <span class="price-value">{{ formatPrice(product.minPrice) }}</span>
                    <span
                      v-if="product.maxPrice && product.maxPrice > product.minPrice"
                      class="price-range"
                    >
                      - {{ formatPrice(product.maxPrice) }}
                    </span>
                  </span>
                  <el-tag v-if="product.status === 'ON_SHELF'" type="success" size="small">
                    在售
                  </el-tag>
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <div class="pagination-wrap" v-if="productTotal > 0">
          <el-pagination
            v-model:current-page="productPage"
            :total="productTotal"
            :page-size="productPageSize"
            layout="total, prev, pager, next"
            background
            @current-change="fetchProducts"
          />
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loadingShop" description="店铺不存在" />

    <!-- Buyer Chat -->
    <el-card v-if="userStore.token && shop.id" class="chat-card">
      <template #header><span>联系客服</span></template>
      <div class="chat-messages" ref="chatMsgs" v-loading="chatLoading">
        <el-empty v-if="chatMessages.length === 0 && !chatLoading" description="暂无消息，发送第一条消息吧" :image-size="40" />
        <div
          v-for="msg in chatMessages"
          :key="msg.id"
          :class="['chat-msg', msg.senderType === 'BUYER' ? 'msg-right' : 'msg-left']"
        >
          <el-avatar v-if="msg.senderType !== 'BUYER'" :size="32" :src="shop.logoUrl || shop.logo" class="msg-avatar" />
          <div class="msg-content-wrap">
            <div class="msg-bubble">{{ msg.content }}</div>
            <div class="msg-time">{{ msg.createTime }}</div>
          </div>
          <el-avatar v-if="msg.senderType === 'BUYER'" :size="32" :src="userStore.userInfo?.avatar" class="msg-avatar">{{ (userStore.userInfo?.username || 'U')[0] }}</el-avatar>
        </div>
      </div>
      <div class="chat-input-row">
        <el-input v-model="chatInput" placeholder="输入消息..." @keyup.enter="sendChat" :disabled="chatSending" />
        <el-button type="primary" @click="sendChat" :loading="chatSending">发送</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getShop } from '@/api/shop'
import { search } from '@/api/product'
import { getMessages, sendMessage, markAllRead } from '@/api/shop'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const shop = ref<any>({})
const products = ref<any[]>([])
const loadingShop = ref(false)
const loadingProducts = ref(false)
const productPage = ref(1)
const productPageSize = ref(12)
const productTotal = ref(0)

// chat
const chatMessages = ref<any[]>([])
const chatInput = ref('')
const chatSending = ref(false)
const chatLoading = ref(false)
const chatMsgs = ref<HTMLElement>()

function formatPrice(price: number) {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}

async function fetchShop() {
  loadingShop.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await getShop(id)
    shop.value = res.data || {}
  } catch {
    shop.value = {}
  } finally {
    loadingShop.value = false
  }
}

async function fetchProducts() {
  loadingProducts.value = true
  try {
    const shopId = Number(route.params.id)
    const res: any = await search({
      shopId,
      page: productPage.value,
      pageSize: productPageSize.value
    })
    products.value = res.data?.records || res.data?.list || res.data || []
    productTotal.value = res.data?.total || 0
  } catch {
    products.value = []
    productTotal.value = 0
  } finally {
    loadingProducts.value = false
  }
}

async function fetchChat() {
  if (!shop.value.id || !userStore.token || !userStore.userInfo?.id) return
  chatLoading.value = true
  try {
    const shopId = Number(route.params.id)
    const res: any = await getMessages({ shopId, userId: userStore.userInfo.id, page: 1, pageSize: 100 })
    chatMessages.value = (res.data?.records || res.data?.list || res.data || []).reverse()
    if (userStore.userInfo?.id) markAllRead(shopId, userStore.userInfo.id)
    await nextTick()
    scrollChatBottom()
  } catch {
    chatMessages.value = []
  } finally {
    chatLoading.value = false
  }
}

function scrollChatBottom() {
  if (chatMsgs.value) {
    chatMsgs.value.scrollTop = chatMsgs.value.scrollHeight
  }
}

async function sendChat() {
  const content = chatInput.value.trim()
  if (!content || chatSending.value || !shop.value.id) return
  chatSending.value = true
  try {
    await sendMessage({
      shopId: Number(route.params.id),
      content,
      senderType: 'BUYER'
    })
    chatMessages.value.push({
      id: Date.now(),
      content,
      senderType: 'BUYER',
      createTime: new Date().toLocaleString()
    })
    chatInput.value = ''
    await nextTick()
    scrollChatBottom()
    fetchChat()
  } catch {
    // handled by interceptor
  } finally {
    chatSending.value = false
  }
}

watch(() => userStore.userInfo, (val) => {
  if (val?.id && shop.value.id && chatMessages.value.length === 0) {
    fetchChat()
  }
}, { immediate: false })

onMounted(async () => {
  await fetchShop()
  if (shop.value.id) {
    await fetchProducts()
    await fetchChat()
  }
})
</script>

<style scoped>
.shop-detail-container {
  max-width: 1000px;
  margin: 0 auto;
}

.shop-header {
  margin-bottom: 8px;
}

.shop-info {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.shop-logo {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 12px;
  border: 2px solid #e4e7ed;
}

.shop-text h1 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.shop-desc {
  margin: 0 0 8px;
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
}

.shop-phone {
  margin: 0 0 8px;
  color: #909399;
  font-size: 14px;
}

.shop-products h3 {
  margin: 0 0 16px;
  font-size: 18px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image-wrap {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  padding: 10px 12px;
}

.product-name {
  margin: 0 0 8px;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  color: #f56c6c;
}

.price-symbol {
  font-size: 12px;
}

.price-value {
  font-size: 17px;
  font-weight: 700;
}

.price-range {
  font-size: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-bottom: 30px;
}

.chat-card {
  margin-top: 24px;
}

.chat-messages {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.chat-msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.msg-left {
  justify-content: flex-start;
}

.msg-right {
  justify-content: flex-end;
}

.msg-content-wrap {
  max-width: 70%;
}

.msg-bubble {
  display: inline-block;
  width: 100%;
  padding: 8px 14px;
  border-radius: 12px;
  font-size: 14px;
  word-break: break-word;
  box-sizing: border-box;
}

.msg-left .msg-bubble {
  background: #f0f0f0;
  color: #303133;
}

.msg-right .msg-bubble {
  background: #409eff;
  color: #fff;
}

.msg-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}

.chat-input-row {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

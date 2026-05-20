<template>
  <div class="home-container">
    <div class="home-banner">
      <h1>欢迎来到 TradeStation</h1>
      <p>发现优质好物，支持优质店铺</p>
    </div>

    <div class="search-section">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称、店铺"
        size="large"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="'Search'" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <div class="product-grid" v-loading="loading">
      <div v-if="products.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无商品" />
      </div>

      <div
        v-for="product in products"
        :key="product.id"
        class="product-card"
        @click="$router.push(`/product/${product.id}`)"
      >
        <el-card :body-style="{ padding: '0' }" shadow="hover">
          <div class="product-image-wrap">
            <img
              :src="product.images?.[0] || '/vite.svg'"
              :alt="product.productName"
              class="product-image"
            />
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.productName }}</h3>
            <p class="product-shop">{{ product.shopName || product.shop?.name || '' }}</p>
            <div class="product-bottom">
              <span class="product-price">
                <span class="price-symbol">&yen;</span>
                <span class="price-value">{{ formatPrice(product.minPrice) }}</span>
                <span v-if="product.maxPrice && product.maxPrice > product.minPrice" class="price-range">
                  - {{ formatPrice(product.maxPrice) }}
                </span>
              </span>
              <span class="product-sales" v-if="product.sales">已售 {{ product.sales }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="fetchProducts"
        @size-change="fetchProducts"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { search as searchProducts } from '@/api/product'

const route = useRoute()

const keyword = ref((route.query.keyword as string) || '')
const products = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

watch(() => route.query.keyword, (val) => {
  keyword.value = (val as string) || ''
  page.value = 1
  fetchProducts()
})

function formatPrice(price: number) {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}

function handleSearch() {
  page.value = 1
  fetchProducts()
}

async function fetchProducts() {
  loading.value = true
  try {
    const res: any = await searchProducts({
      keyword: keyword.value || undefined,
      page: page.value,
      size: pageSize.value
    })
    products.value = res.data.records || res.data.list || res.data || []
    total.value = res.data.total || 0
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.home-container {
  min-height: 70vh;
}

.home-banner {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: #fff;
  text-align: center;
  padding: 40px 20px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.home-banner h1 {
  margin: 0 0 10px;
  font-size: 28px;
}

.home-banner p {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
}

.search-section {
  max-width: 600px;
  margin: 0 auto 30px;
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
  height: 200px;
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
  padding: 12px;
}

.product-name {
  margin: 0 0 6px;
  font-size: 15px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-shop {
  margin: 0 0 10px;
  font-size: 12px;
  color: #909399;
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
  font-size: 13px;
}

.price-value {
  font-size: 18px;
  font-weight: 700;
}

.price-range {
  font-size: 13px;
}

.product-sales {
  font-size: 12px;
  color: #c0c4cc;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding-bottom: 30px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

@media (max-width: 992px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

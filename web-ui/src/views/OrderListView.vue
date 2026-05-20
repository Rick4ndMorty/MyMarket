<template>
  <div class="order-list-container">
    <h2>我的订单</h2>

    <el-tabs v-model="activeStatus" @tab-change="handleStatusChange" class="order-tabs">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="PENDING_PAYMENT" />
      <el-tab-pane label="待发货" name="PENDING_SHIP" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="退款中" name="REFUNDING" />
      <el-tab-pane label="已取消" name="CANCELLED" />
    </el-tabs>

    <div v-loading="loading">
      <div v-if="orders.length === 0 && !loading">
        <el-empty description="暂无订单" />
      </div>

      <div v-else>
        <el-card
          v-for="order in orders"
          :key="order.id"
          class="order-card"
          shadow="hover"
          @click="$router.push(`/orders/${order.id}`)"
        >
          <div class="order-header">
            <div class="order-meta">
              <span class="order-no">订单号: {{ order.orderNo || order.id }}</span>
              <span class="order-time">{{ order.createTime || order.createdAt }}</span>
            </div>
            <el-tag :type="statusTagType(order.status)" size="small">
              {{ statusLabel(order.status) }}
            </el-tag>
          </div>

          <div class="order-items">
            <div v-for="item in (order.items || []).slice(0, 3)" :key="item.id" class="order-item-row">
              <img
                :src="item.skuSnapshot?.image || item.image || '/vite.svg'"
                class="order-item-img"
              />
              <div class="order-item-info">
                <div class="order-item-name">{{ item.skuSnapshot?.productName || item.productName }} - {{ item.skuSnapshot?.skuName || item.skuName }}</div>
                <div class="order-item-price">&yen;{{ (item.unitPrice || item.price || 0).toFixed(2) }} x {{ item.quantity }}</div>
              </div>
            </div>
            <div v-if="order.items?.length > 3" class="more-items">
              还有 {{ order.items.length - 3 }} 件商品...
            </div>
          </div>

          <div class="order-footer">
            <span class="order-total">
              共 {{ order.items?.length || 0 }} 件商品，合计：
              <span class="total-amount">&yen;{{ (order.totalAmount || 0).toFixed(2) }}</span>
            </span>
          </div>
        </el-card>

        <div class="pagination-wrap" v-if="total > 0">
          <el-pagination
            v-model:current-page="page"
            :total="total"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            background
            @current-change="fetchOrders"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBuyerOrders } from '@/api/order'

const orders = ref<any[]>([])
const loading = ref(false)
const activeStatus = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIP: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  REFUNDING: '退款中',
  CANCELLED: '已取消'
}

const statusTagMap: Record<string, string> = {
  PENDING_PAYMENT: 'warning',
  PENDING_SHIP: 'primary',
  SHIPPED: 'info',
  COMPLETED: 'success',
  REFUNDING: 'danger',
  CANCELLED: 'info'
}

function statusLabel(status: string) {
  return statusMap[status] || status
}

function statusTagType(status: string) {
  return statusTagMap[status] || 'info'
}

function handleStatusChange() {
  page.value = 1
  fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    const res: any = await getBuyerOrders({
      status: activeStatus.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    orders.value = res.data?.records || res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-list-container h2 {
  margin: 0 0 16px;
  font-size: 20px;
}

.order-tabs {
  margin-bottom: 16px;
}

.order-card {
  margin-bottom: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.order-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #606266;
}

.order-no {
  font-weight: 500;
}

.order-time {
  color: #909399;
}

.order-items {
  padding: 4px 0;
}

.order-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}

.order-item-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}

.order-item-info {
  flex: 1;
}

.order-item-name {
  font-size: 14px;
}

.order-item-price {
  color: #909399;
  font-size: 13px;
}

.more-items {
  color: #909399;
  font-size: 13px;
  padding: 4px 0 4px 60px;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
  font-size: 14px;
}

.total-amount {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 700;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

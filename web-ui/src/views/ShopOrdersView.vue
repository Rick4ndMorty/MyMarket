<template>
  <div class="shop-orders-container">
    <h2>店铺订单</h2>

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

      <el-table
        v-else
        :data="orders"
        border
        stripe
        style="width: 100%"
        row-key="id"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <h4>订单商品</h4>
              <div v-for="item in row.items" :key="item.id" class="order-item-row">
                <img
                  :src="item.skuSnapshot?.image || '/vite.svg'"
                  class="item-image"
                />
                <div class="item-info">
                  <div class="item-name">{{ item.skuSnapshot?.productName }} - {{ item.skuSnapshot?.skuName }}</div>
                  <div class="item-meta">
                    &yen;{{ (item.unitPrice || 0).toFixed(2) }} x {{ item.quantity }}
                  </div>
                </div>
                <div class="item-subtotal">
                  &yen;{{ ((item.unitPrice || 0) * item.quantity).toFixed(2) }}
                </div>
              </div>
              <div class="order-address" v-if="row.address">
                <strong>收货地址：</strong>
                {{ row.address.receiverName }} {{ row.address.phone }}
                {{ row.address.province }}{{ row.address.city }}{{ row.address.district }}
                {{ row.address.detail }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="订单号" width="100" />
        <el-table-column prop="buyerName" label="买家" width="120" />
        <el-table-column label="订单金额" width="130">
          <template #default="{ row }">
            <span class="order-total">&yen;{{ row.totalAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">
            {{ row.createTime || row.createdAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING_SHIP'"
              size="small"
              type="primary"
              @click="handleDeliver(row)"
            >
              发货
            </el-button>
            <el-button
              v-if="row.status === 'REFUNDING'"
              size="small"
              type="success"
              @click="handleRefund(row, true)"
            >
              同意退款
            </el-button>
            <el-button
              v-if="row.status === 'REFUNDING'"
              size="small"
              type="danger"
              @click="handleRefund(row, false)"
            >
              拒绝退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getShopOrders, shipOrder, processRefund } from '@/api/order'
import { getMyShop } from '@/api/shop'
import { ElMessage, ElMessageBox } from 'element-plus'

const shop = ref<any>({})
const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeStatus = ref('')

const statusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIP: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  REFUNDING: '退款中',
  CANCELLED: '已取消'
}

const statusTagType: Record<string, string> = {
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

function statusTag(status: string) {
  return statusTagType[status] || 'info'
}

function handleStatusChange() {
  page.value = 1
  fetchOrders()
}

async function handleDeliver(order: any) {
  try {
    await ElMessageBox.confirm(`确认发货？订单号: ${order.id}`, '确认发货', { type: 'warning' })
  } catch {
    return
  }
  try {
    await shipOrder(order.id, shop.value.id)
    ElMessage.success('已发货')
    await fetchOrders()
  } catch {
    // handled by interceptor
  }
}

async function handleRefund(order: any, approve: boolean) {
  const action = approve ? '同意退款' : '拒绝退款'
  try {
    await ElMessageBox.confirm(`确定${action}？订单号: ${order.id}`, action, { type: 'warning' })
  } catch {
    return
  }
  try {
    await processRefund(order.id, shop.value.id, approve)
    ElMessage.success(approve ? '已同意退款，库存已恢复' : '已拒绝退款')
    await fetchOrders()
  } catch {
    // handled by interceptor
  }
}

async function fetchShop() {
  try {
    const res: any = await getMyShop()
    shop.value = res.data || {}
  } catch {
    shop.value = {}
  }
}

async function fetchOrders() {
  if (!shop.value.id) return
  loading.value = true
  try {
    const res: any = await getShopOrders({
      shopId: shop.value.id,
      status: activeStatus.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    orders.value = res.data.records || res.data.list || res.data || []
    total.value = res.data.total || 0
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchShop()
  fetchOrders()
})
</script>

<style scoped>
.shop-orders-container h2 {
  margin: 0 0 16px;
  font-size: 20px;
}

.order-tabs {
  margin-bottom: 16px;
}

.expand-content {
  padding: 12px 20px;
}

.expand-content h4 {
  margin: 0 0 10px;
  color: #606266;
}

.order-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.item-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 14px;
}

.item-meta {
  color: #909399;
  font-size: 13px;
}

.item-subtotal {
  font-weight: 500;
}

.order-address {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
}

.order-total {
  color: #f56c6c;
  font-weight: 700;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

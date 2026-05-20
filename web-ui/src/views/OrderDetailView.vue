<template>
  <div class="order-detail-container" v-loading="loading">
    <template v-if="order.id">
      <div class="order-status-bar">
        <div class="status-left">
          <h2>订单详情</h2>
          <span class="order-no">订单号: {{ order.orderNo || order.id }}</span>
        </div>
        <el-tag :type="statusTagType(order.status)" size="large">
          {{ statusLabel(order.status) }}
        </el-tag>
      </div>

      <el-card class="section-card" v-if="order.address || order.addressSnapshot">
        <template #header><strong>收货信息</strong></template>
        <div class="address-info">
          <p>
            <strong>{{ (order.address || order.addressSnapshot).receiverName }}</strong>
            {{ (order.address || order.addressSnapshot).phone || (order.address || order.addressSnapshot).receiverPhone }}
          </p>
          <p>
            {{ (order.address || order.addressSnapshot).province }}
            {{ (order.address || order.addressSnapshot).city }}
            {{ (order.address || order.addressSnapshot).district }}
            {{ (order.address || order.addressSnapshot).detail }}
          </p>
        </div>
      </el-card>

      <el-card class="section-card">
        <template #header><strong>商品信息</strong></template>
        <el-table :data="order.items || []" border stripe style="width: 100%">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="product-cell">
                <img :src="row.skuSnapshot?.image || '/vite.svg'" class="product-thumb" />
                <div>
                  <div class="product-name">{{ row.skuSnapshot?.productName }}</div>
                  <div class="product-sku">{{ row.skuSnapshot?.skuName }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              &yen;{{ (row.price || row.unitPrice || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="subtotal">
                &yen;{{ ((row.price || row.unitPrice || 0) * row.quantity).toFixed(2) }}
              </span>
            </template>
          </el-table-column>
        </el-table>

        <div class="order-total-row">
          <span>订单总金额：</span>
          <span class="total-amount">&yen;{{ (order.payAmount || order.totalAmount || 0).toFixed(2) }}</span>
        </div>
      </el-card>

      <el-card class="section-card" v-if="order.remark || order.cancelReason">
        <template #header><strong>备注</strong></template>
        <p v-if="order.remark">订单备注: {{ order.remark }}</p>
        <p v-if="order.cancelReason" style="color: #f56c6c;">取消原因: {{ order.cancelReason }}</p>
      </el-card>

      <div class="order-actions" v-if="showActions">
        <el-button
          v-if="order.status === 'PENDING_PAYMENT'"
          type="success"
          size="large"
          @click="handlePay"
        >
          模拟支付
        </el-button>
        <el-button
          v-if="order.status === 'PENDING_PAYMENT'"
          type="danger"
          size="large"
          @click="handleCancel"
        >
          取消订单
        </el-button>
        <el-button
          v-if="order.status === 'PENDING_SHIP'"
          type="warning"
          size="large"
          @click="handleCancel"
        >
          申请退款
        </el-button>
        <el-button
          v-if="order.status === 'SHIPPED'"
          type="primary"
          size="large"
          @click="handleConfirmReceipt"
        >
          确认收货
        </el-button>
      </div>

      <el-card class="section-card" v-if="order.statusHistory?.length">
        <template #header><strong>订单状态</strong></template>
        <el-timeline>
          <el-timeline-item
            v-for="item in order.statusHistory"
            :key="item.id"
            :timestamp="item.createTime"
          >
            {{ statusLabel(item.status) }}
            <span v-if="item.remark" class="timeline-remark"> - {{ item.remark }}</span>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>

    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, cancelOrder, confirmReceipt } from '@/api/order'
import { createPayment, mockPayCallback } from '@/api/payment'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()

const order = ref<any>({})
const loading = ref(false)

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

const showActions = computed(() => {
  return ['PENDING_PAYMENT', 'PENDING_SHIP', 'SHIPPED'].includes(order.value.status)
})

async function fetchOrder() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await getOrderDetail(id)
    order.value = res.data || {}
  } catch {
    order.value = {}
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  try {
    const paymentRes: any = await createPayment({
      orderId: order.value.id,
      paymentMethod: 'MOCK'
    })
    const paymentNo = paymentRes.data?.paymentNo || paymentRes.data?.id
    if (paymentNo) {
      await mockPayCallback(String(paymentNo))
      ElMessage.success('支付成功')
    }
    await fetchOrder()
  } catch {
    // handled by interceptor
  }
}

async function handleCancel() {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputType: 'textarea'
    })
  } catch {
    return
  }
  try {
    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await fetchOrder()
  } catch {
    // handled by interceptor
  }
}

async function handleConfirmReceipt() {
  try {
    await ElMessageBox.confirm('确认已收到货物？', '确认收货', { type: 'success' })
  } catch {
    return
  }
  try {
    await confirmReceipt(order.value.id)
    ElMessage.success('已确认收货，订单完成')
    await fetchOrder()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail-container {
  max-width: 900px;
  margin: 0 auto;
}

.order-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-left h2 {
  margin: 0 0 6px;
  font-size: 22px;
}

.order-no {
  color: #909399;
  font-size: 14px;
}

.section-card {
  margin-bottom: 16px;
}

.address-info p {
  margin: 4px 0;
  color: #606266;
  line-height: 1.6;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
}

.product-name {
  font-size: 14px;
}

.product-sku {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}

.subtotal {
  color: #f56c6c;
  font-weight: 500;
}

.order-total-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-top: 16px;
  font-size: 15px;
}

.total-amount {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 700;
  margin-left: 8px;
}

.order-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 20px;
}

.timeline-remark {
  color: #909399;
  font-size: 13px;
}
</style>

<template>
  <div class="payment-container">
    <h2>订单支付</h2>

    <el-card class="payment-card" v-loading="loading">
      <div v-if="order.id">
        <div class="order-info">
          <div class="info-row">
            <span>订单编号</span>
            <span>{{ order.orderNo || order.id }}</span>
          </div>
          <div class="info-row">
            <span>订单金额</span>
            <span class="amount">&yen;{{ (order.payAmount || order.totalAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="info-row">
            <span>订单状态</span>
            <el-tag :type="statusTag">{{ statusText }}</el-tag>
          </div>
        </div>

        <el-divider />

        <div v-if="order.status === 'PENDING_PAYMENT'">
          <el-radio-group v-model="paymentMethod" class="payment-methods">
            <el-radio label="ALIPAY">支付宝</el-radio>
            <el-radio label="MOCK">模拟支付（开发用）</el-radio>
          </el-radio-group>

          <div v-if="payUrl" class="alipay-redirect">
            <p>支付宝支付页面已生成，请在打开的页面中完成支付。</p>
            <el-button type="primary" size="large" @click="openPayPage">
              打开支付宝支付页面
            </el-button>
            <el-button size="large" @click="checkPaymentStatus" :loading="checking">
              支付已完成
            </el-button>
          </div>
          <el-button
            v-else
            type="primary"
            size="large"
            class="pay-button"
            :loading="paying"
            @click="handlePay"
          >
            &yen;{{ (order.payAmount || order.totalAmount || 0).toFixed(2) }} 立即支付
          </el-button>
        </div>

        <div v-else>
          <el-result
            :icon="isSuccess ? 'success' : 'warning'"
            :title="statusText"
          >
            <template #extra>
              <el-button type="primary" @click="$router.push(`/orders/${order.id}`)">查看订单</el-button>
              <el-button @click="$router.push('/')">返回首页</el-button>
            </template>
          </el-result>
        </div>
      </div>

      <el-empty v-else-if="!loading" description="订单不存在">
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import { createPayment, mockPayCallback, getPayment } from '@/api/payment'
import { ElMessage } from 'element-plus'

const route = useRoute()

const order = ref<any>({})
const loading = ref(false)
const paying = ref(false)
const checking = ref(false)
const paymentMethod = ref('ALIPAY')
const payUrl = ref('')
const paymentNo = ref('')

const statusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_SHIP: '待发货',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const statusText = computed(() => statusMap[order.value.status] || order.value.status || '未知')
const isSuccess = computed(() => ['PENDING_SHIP', 'SHIPPED', 'COMPLETED'].includes(order.value.status))

const statusTag = computed(() => {
  const map: Record<string, string> = {
    PENDING_PAYMENT: 'warning',
    PENDING_SHIP: 'primary',
    SHIPPED: 'success',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[order.value.status] || 'info'
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
  paying.value = true
  try {
    const paymentRes: any = await createPayment({
      orderId: order.value.id,
      paymentMethod: paymentMethod.value
    })
    const data = paymentRes.data || {}

    if (data.payUrl) {
      // Alipay 支付：保存 payUrl，让用户手动跳转
      payUrl.value = data.payUrl
      paymentNo.value = data.paymentNo
      window.open(data.payUrl, '_blank')
    } else {
      // MOCK 支付：自动完成
      if (data.paymentNo) {
        await mockPayCallback(data.paymentNo)
      }
      ElMessage.success('支付成功')
      order.value.status = 'PENDING_SHIP'
    }
  } catch {
    // handled by interceptor
  } finally {
    paying.value = false
  }
}

function openPayPage() {
  if (payUrl.value) {
    window.open(payUrl.value, '_blank')
  }
}

async function checkPaymentStatus() {
  checking.value = true
  try {
    const orderId = Number(route.params.id)
    const res: any = await getOrderDetail(orderId)
    if (res.data?.status === 'PENDING_SHIP' || res.data?.status === 'SHIPPED' || res.data?.status === 'COMPLETED') {
      order.value.status = res.data.status
      ElMessage.success('支付成功！')
    } else {
      ElMessage.warning('支付尚未完成，请完成支付后重试')
    }
  } catch {
    // handled by interceptor
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.payment-container {
  max-width: 600px;
  margin: 0 auto;
}

.payment-container h2 {
  margin: 0 0 20px;
  font-size: 22px;
}

.payment-card {
  padding: 10px;
}

.order-info {
  margin-bottom: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 15px;
}

.amount {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 700;
}

.payment-methods {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
}

.pay-button {
  width: 100%;
  font-size: 16px;
}

.alipay-redirect {
  text-align: center;
  color: #606266;
}

.alipay-redirect p {
  margin-bottom: 16px;
}

.alipay-redirect .el-button {
  margin: 0 8px;
}
</style>

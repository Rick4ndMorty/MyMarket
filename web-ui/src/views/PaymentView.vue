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

        <!-- 待支付状态 -->
        <div v-if="order.status === 'PENDING_PAYMENT'">
          <el-radio-group v-model="paymentMethod" class="payment-methods" :disabled="paying || paymentInitiated">
            <el-radio label="ALIPAY">支付宝沙箱支付</el-radio>
            <el-radio label="MOCK">模拟支付（开发用）</el-radio>
          </el-radio-group>

          <!-- 支付宝支付 -->
          <div v-if="paymentMethod === 'ALIPAY' && !paymentInitiated" class="alipay-info">
            <el-alert type="info" :closable="false" show-icon>
              <template #title>
                支付宝沙箱支付说明
              </template>
              <template #default>
                <p>点击支付后将跳转至<strong>支付宝沙箱支付页面</strong>。</p>
                <p>请在支付宝页面使用<strong>沙箱买家账号</strong>登录并输入<strong>支付密码</strong>完成支付。</p>
                <p style="margin-top: 8px; color: #909399;">
                  沙箱账号请前往支付宝开放平台沙箱控制台 → 沙箱账号 查看
                </p>
              </template>
            </el-alert>
          </div>

          <!-- 支付已发起，等待用户在支付宝页面完成 -->
          <div v-if="paymentInitiated" class="alipay-processing">
            <el-alert type="warning" :closable="false" show-icon title="支付处理中">
              <template #default>
                <p>已为您打开支付宝沙箱支付页面，请在新窗口完成支付。</p>
                <p v-if="paymentNo">支付单号：{{ paymentNo }}</p>
              </template>
            </el-alert>

            <div class="processing-actions">
              <el-button type="primary" size="large" @click="openPayPage" style="margin-right: 12px;">
                重新打开支付页面
              </el-button>
              <el-button size="large" @click="checkPaymentStatus" :loading="checking">
                支付已完成，查询结果
              </el-button>
            </div>

            <el-divider />

            <div class="polling-tip">
              <el-text type="info" size="small">
                支付完成后系统会自动跳转，您也可以手动点击上方按钮查询支付结果。
                如遇页面未自动跳转，请勿重复支付，刷新本页即可。
              </el-text>
            </div>
          </div>

          <!-- 未发起支付时显示支付按钮 -->
          <el-button
            v-if="!paymentInitiated"
            type="primary"
            size="large"
            class="pay-button"
            :loading="paying"
            @click="handlePay"
          >
            &yen;{{ (order.payAmount || order.totalAmount || 0).toFixed(2) }} 立即支付
          </el-button>
        </div>

        <!-- 非待支付状态 -->
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
import { createPayment, mockPayCallback, queryAlipayStatus } from '@/api/payment'
import { ElMessage } from 'element-plus'

const route = useRoute()

const order = ref<any>({})
const loading = ref(false)
const paying = ref(false)
const checking = ref(false)
const paymentMethod = ref('ALIPAY')
const payUrl = ref('')
const paymentNo = ref('')
// 支付是否已发起（已创建支付记录并打开支付宝页面）
const paymentInitiated = ref(false)

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
    // 如果订单已支付，不需要再发起支付
    if (order.value.status !== 'PENDING_PAYMENT') {
      paymentInitiated.value = false
    }
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

    if (paymentMethod.value === 'ALIPAY' && data.payUrl) {
      // 支付宝 pageExecute 返回的是 HTML 表单，需要转为 Blob URL 再打开
      payUrl.value = data.payUrl
      paymentNo.value = data.paymentNo
      paymentInitiated.value = true
      // 将 HTML 表单字符串转为 Blob URL，在新窗口中打开 → 自动提交到支付宝
      const blob = new Blob([data.payUrl], { type: 'text/html;charset=utf-8' })
      const blobUrl = URL.createObjectURL(blob)
      window.open(blobUrl, '_blank')
      ElMessage.info('已打开支付宝支付页面，请在新窗口中完成支付')
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
    const blob = new Blob([payUrl.value], { type: 'text/html;charset=utf-8' })
    const blobUrl = URL.createObjectURL(blob)
    window.open(blobUrl, '_blank')
    ElMessage.info('已重新打开支付宝支付页面')
  } else {
    ElMessage.warning('支付信息已过期，请刷新页面重试')
  }
}

/**
 * 查询支付状态（安全关键：向支付宝主动查询，而非仅查本地订单状态）
 */
async function checkPaymentStatus() {
  checking.value = true
  try {
    // 优先使用支付宝主动查询接口，确保用户支付成功但异步通知未到达时也能正确识别
    if (paymentNo.value) {
      const res: any = await queryAlipayStatus(paymentNo.value)
      if (res.data?.status === 'SUCCESS') {
        order.value.status = 'PENDING_SHIP'
        ElMessage.success('支付成功！支付宝已确认收款')
        return
      }
    }
    // 备用：查询本地订单状态
    const orderId = Number(route.params.id)
    const orderRes: any = await getOrderDetail(orderId)
    if (orderRes.data?.status === 'PENDING_SHIP' || orderRes.data?.status === 'SHIPPED' || orderRes.data?.status === 'COMPLETED') {
      order.value.status = orderRes.data.status
      ElMessage.success('支付成功！')
    } else {
      ElMessage.warning('支付尚未完成，请在支付宝页面完成支付后重试')
    }
  } catch {
    ElMessage.warning('状态查询失败，请稍后重试')
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
  max-width: 650px;
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

.alipay-info {
  margin-bottom: 20px;
}

.alipay-info p {
  margin: 4px 0;
  line-height: 1.6;
}

.alipay-processing {
  margin-bottom: 16px;
}

.alipay-processing p {
  margin: 4px 0;
  line-height: 1.6;
}

.processing-actions {
  text-align: center;
  margin: 20px 0 8px;
}

.polling-tip {
  text-align: center;
  padding: 8px 0;
}
</style>
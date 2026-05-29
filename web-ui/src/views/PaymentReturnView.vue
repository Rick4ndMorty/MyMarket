<template>
  <div class="return-container">
    <el-card v-loading="loading">
      <el-result
        v-if="result === 'success'"
        icon="success"
        title="支付成功"
        sub-title="您的订单已支付成功，等待商家发货"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push(`/orders/${payment?.orderId}`)">查看订单</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>

      <el-result
        v-else-if="result === 'pending'"
        icon="warning"
        title="支付处理中"
        sub-title="支付正在处理中，请稍后查看订单状态"
      >
        <template #extra>
          <el-button type="primary" @click="checkAgain" :loading="loading">刷新状态</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>

      <el-result
        v-else-if="result === 'error'"
        icon="error"
        title="支付遇到问题"
        :sub-title="errorMsg"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/orders')">我的订单</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPaymentByNo, queryAlipayStatus } from '@/api/payment'

const route = useRoute()

const loading = ref(true)
const result = ref('pending')
const errorMsg = ref('')
const payment = ref<any>({})

async function checkAgain() {
  loading.value = true
  await doCheck()
  loading.value = false
}

/**
 * 同步回调后确认支付状态
 * 关键安全流程：
 * 1. 先通过 queryAlipayStatus 向支付宝主动查询（二次确认，防止异步通知延迟/丢失）
 * 2. 再通过 getPaymentByNo 查询本地支付记录作为兜底
 */
async function doCheck() {
  const paymentNo = route.query.out_trade_no as string
  if (!paymentNo) {
    // URL 中无支付单号，尝试从 query 中获取其他参数
    result.value = 'error'
    errorMsg.value = '缺少支付单号参数，请返回订单页面查看'
    return
  }

  try {
    // 第一步：主动向支付宝查询支付状态（最可靠的确认方式）
    const alipayRes: any = await queryAlipayStatus(paymentNo)
    if (alipayRes.data?.status === 'SUCCESS') {
      payment.value = alipayRes.data
      result.value = 'success'
      return
    }
  } catch {
    // 支付宝查询失败，降级使用本地查询
  }

  try {
    // 第二步：查询本地支付记录
    const res: any = await getPaymentByNo(paymentNo)
    payment.value = res.data || {}
    if (payment.value.status === 'SUCCESS') {
      result.value = 'success'
    } else {
      result.value = 'pending'
    }
  } catch {
    result.value = 'error'
    errorMsg.value = '支付状态查询失败，请前往「我的订单」查看'
  }
}

onMounted(async () => {
  await doCheck()
  loading.value = false
})
</script>

<style scoped>
.return-container {
  max-width: 600px;
  margin: 40px auto;
}
</style>
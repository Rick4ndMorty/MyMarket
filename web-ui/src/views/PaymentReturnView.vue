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
          <el-button type="primary" @click="checkAgain">刷新状态</el-button>
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
import { getPaymentByNo } from '@/api/payment'

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

async function doCheck() {
  const paymentNo = route.query.out_trade_no as string
  if (!paymentNo) {
    result.value = 'error'
    errorMsg.value = '缺少支付单号参数'
    return
  }

  try {
    const res: any = await getPaymentByNo(paymentNo)
    payment.value = res.data || {}
    if (payment.value.status === 'SUCCESS') {
      result.value = 'success'
    } else {
      result.value = 'pending'
    }
  } catch {
    result.value = 'error'
    errorMsg.value = '支付状态查询失败'
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

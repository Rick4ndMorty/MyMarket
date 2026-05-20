<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="4" v-for="card in cards" :key="card.label">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-sub" v-if="card.sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserStats, getShopStats, getProductStats, getOrderStats, getPaymentStats } from '@/api/admin'

const loading = ref(false)
const cards = ref<any[]>([])

async function fetchStats() {
  loading.value = true
  try {
    const [user, shop, product, order, payment] = await Promise.all([
      getUserStats(), getShopStats(), getProductStats(), getOrderStats(), getPaymentStats()
    ])
    const u = (user as any).data || {}
    const s = (shop as any).data || {}
    const p = (product as any).data || {}
    const o = (order as any).data || {}
    const pay = (payment as any).data || {}

    cards.value = [
      { label: '用户总数', value: u.totalUsers || 0, sub: `活跃: ${u.activeUsers || 0}` },
      { label: '店铺总数', value: s.totalShops || 0, sub: `待审核: ${s.byStatus?.PENDING || 0}` },
      { label: '商品总数', value: p.totalProducts || 0, sub: `在售: ${p.byStatus?.ON_SHELF || 0}` },
      { label: '订单总数', value: o.totalOrders || 0, sub: `待发货: ${o.byStatus?.PENDING_SHIP || 0}` },
      { label: '成交额', value: '¥' + ((o.totalRevenue || 0) as number).toFixed(2), sub: `成功支付: ${pay.totalSuccessAmount ? '¥' + Number(pay.totalSuccessAmount).toFixed(2) : '¥0.00'}` }
    ]
  } catch { /* ignore */ }
  finally { loading.value = false }
}

onMounted(() => fetchStats())
</script>

<style scoped>
.dashboard h2 {
  margin: 0 0 20px;
  font-size: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.el-col {
  margin-bottom: 16px;
  min-width: 200px;
}
</style>

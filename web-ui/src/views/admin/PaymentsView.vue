<template>
  <div class="admin-page">
    <h2>支付管理</h2>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索支付单号" style="width: 200px" clearable @clear="fetchPayments" @keyup.enter="fetchPayments" />
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 140px" clearable @change="fetchPayments">
          <el-option label="待支付" value="PENDING" />
          <el-option label="支付成功" value="SUCCESS" />
          <el-option label="支付失败" value="FAILED" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" @click="fetchPayments">搜索</el-button>
      </div>

      <el-table :data="payments" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="paymentNo" label="支付单号" width="170" />
        <el-table-column prop="orderId" label="订单ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">&yen;{{ (row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" width="170">
          <template #default="{ row }">{{ row.paymentTime ? row.paymentTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm v-if="row.status === 'SUCCESS'" title="确定退款？" confirm-button-text="确定" @confirm="doRefund(row.id)">
              <template #reference>
                <el-button size="small" type="danger">退款</el-button>
              </template>
            </el-popconfirm>
            <span v-else style="color: #c0c4cc; font-size: 12px;">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchPayments" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminPayments, adminRefundPayment } from '@/api/admin'
import { ElMessage } from 'element-plus'

const statusMap: Record<string, string> = {
  PENDING: '待支付', SUCCESS: '支付成功', FAILED: '支付失败', REFUNDED: '已退款', CLOSED: '已关闭'
}
function statusType(s: string) {
  return { PENDING: 'warning', SUCCESS: 'success', FAILED: 'danger', REFUNDED: 'danger', CLOSED: 'info' }[s] || ''
}

const payments = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')

async function fetchPayments() {
  loading.value = true
  try {
    const res: any = await getAdminPayments({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined
    })
    const data = res.data || {}
    payments.value = data.records || []
    total.value = data.total || 0
  } catch { payments.value = [] } finally { loading.value = false }
}

async function doRefund(id: number) {
  try {
    await adminRefundPayment(id)
    ElMessage.success('退款成功')
    fetchPayments()
  } catch { /* handled */ }
}

onMounted(() => fetchPayments())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
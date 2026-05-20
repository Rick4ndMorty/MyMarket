<template>
  <div class="admin-page">
    <h2>订单管理</h2>

    <el-card>
      <div class="search-bar">
        <el-input v-model="orderNo" placeholder="搜索订单号" style="width: 200px" clearable @clear="fetchOrders" @keyup.enter="fetchOrders" />
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 150px" clearable @change="fetchOrders">
          <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button type="primary" @click="fetchOrders">搜索</el-button>
      </div>

      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="userId" label="买家ID" width="80" />
        <el-table-column prop="shopId" label="店铺ID" width="80" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            &yen;{{ (row.totalAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.substring(0, 16) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-dropdown @command="(cmd: string) => handleCommand(row, cmd)">
              <el-button size="small">更新状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="PENDING_SHIP">待发货</el-dropdown-item>
                  <el-dropdown-item command="SHIPPED">已发货</el-dropdown-item>
                  <el-dropdown-item command="COMPLETED">已完成</el-dropdown-item>
                  <el-dropdown-item command="CANCELLED">已取消</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchOrders" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminOrders, adminUpdateOrderStatus } from '@/api/admin'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

const statusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付', PENDING_SHIP: '待发货',
  SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消'
}

function statusType(s: string) {
  return { PENDING_PAYMENT: 'warning', PENDING_SHIP: 'primary', SHIPPED: 'success', COMPLETED: 'success', CANCELLED: 'danger' }[s] || 'info'
}

const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const orderNo = ref('')
const filterStatus = ref('')

async function fetchOrders() {
  loading.value = true
  try {
    const res: any = await getAdminOrders({
      page: page.value, size: size.value,
      orderNo: orderNo.value || undefined,
      status: filterStatus.value || undefined
    })
    const data = res.data || {}
    orders.value = data.records || []
    total.value = data.total || 0
  } catch { orders.value = [] } finally { loading.value = false }
}

async function handleCommand(row: any, cmd: string) {
  try {
    await adminUpdateOrderStatus(row.id, cmd)
    row.status = cmd
    ElMessage.success('状态已更新')
  } catch { /* handled */ }
}

onMounted(() => fetchOrders())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>

<template>
  <div class="admin-page">
    <h2>订单管理</h2>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索订单号" style="width: 200px" clearable @clear="fetchOrders" @keyup.enter="fetchOrders" />
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 140px" clearable @change="fetchOrders">
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="待发货" value="PENDING_SHIP" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
        <el-button type="primary" @click="fetchOrders">搜索</el-button>
      </div>

      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">&yen;{{ (row.totalAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ row.updateTime ? row.updateTime.substring(0, 16) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row.id)">详情</el-button>

            <!-- 待发货 → 发货 -->
            <el-button size="small" type="primary" v-if="row.status === 'PENDING_SHIP'" @click="updateStatus(row.id, 'SHIPPED')">发货</el-button>

            <!-- 已发货 → 完成 -->
            <el-button size="small" type="success" v-if="row.status === 'SHIPPED'" @click="updateStatus(row.id, 'COMPLETED')">完成</el-button>

            <!-- 退款按钮 -->
            <el-popconfirm v-if="canRefund(row.status)" title="确定退款？" confirm-button-text="确定" @confirm="refundOrder(row.id)">
              <template #reference>
                <el-button size="small" type="danger">退款</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchOrders" />
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialog" title="订单详情" width="650px">
      <template v-if="detailOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单ID">{{ detailOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="总金额">&yen;{{ (detailOrder.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailOrder.status)" size="small">{{ statusMap[detailOrder.status] || detailOrder.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="取消原因">{{ detailOrder.cancelReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detailOrder.receiverAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailOrder.createTime?.substring(0, 16) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailOrder.updateTime?.substring(0, 16) || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px; margin-bottom: 10px;">订单商品</h4>
        <el-table :data="detailItems" size="small" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="productName" label="商品名" min-width="140" />
          <el-table-column prop="skuName" label="规格" width="100" />
          <el-table-column prop="quantity" label="数量" width="60" />
          <el-table-column label="单价" width="90">
            <template #default="{ row }">&yen;{{ (row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="90">
            <template #default="{ row }">&yen;{{ ((row.price || 0) * (row.quantity || 0)).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminOrders, getAdminOrderDetail, adminUpdateOrderStatus, adminRefundOrder } from '@/api/admin'
import { ElMessage } from 'element-plus'

const statusMap: Record<string, string> = {
  PENDING_PAYMENT: '待支付', PENDING_SHIP: '待发货',
  SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款'
}
function statusType(s: string) {
  return { PENDING_PAYMENT: 'warning', PENDING_SHIP: 'primary', SHIPPED: 'success',
    COMPLETED: 'success', CANCELLED: 'danger', REFUNDED: 'danger' }[s] || 'info'
}
function canRefund(s: string) { return s !== 'CANCELLED' && s !== 'REFUNDED' }

const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')

const detailDialog = ref(false)
const detailOrder = ref<any>(null)
const detailItems = ref<any[]>([])

async function fetchOrders() {
  loading.value = true
  try {
    const res: any = await getAdminOrders({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined
    })
    const data = res.data || {}
    orders.value = data.records || []
    total.value = data.total || 0
  } catch { orders.value = [] } finally { loading.value = false }
}

async function updateStatus(id: number, status: string) {
  try {
    await adminUpdateOrderStatus(id, status)
    ElMessage.success('状态更新成功')
    fetchOrders()
  } catch { /* handled */ }
}

async function refundOrder(id: number) {
  try {
    await adminRefundOrder(id)
    ElMessage.success('退款成功')
    fetchOrders()
  } catch { /* handled */ }
}

async function openDetail(id: number) {
  try {
    const res: any = await getAdminOrderDetail(id)
    const d = res.data || {}
    detailOrder.value = d.order || null
    detailItems.value = d.items || []
    detailDialog.value = true
  } catch { /* handled */ }
}

onMounted(() => fetchOrders())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
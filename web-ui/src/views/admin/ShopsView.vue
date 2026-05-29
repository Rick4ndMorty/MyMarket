<template>
  <div class="admin-page">
    <h2>店铺审核</h2>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索店铺名" style="width: 200px" clearable @clear="fetchShops" @keyup.enter="fetchShops" />
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 140px" clearable @change="fetchShops">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="ACTIVE" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" @click="fetchShops">搜索</el-button>
      </div>

      <el-table :data="shops" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="shopName" label="店铺名" width="160" />
        <el-table-column prop="userId" label="店主ID" width="80" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="140" />
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" @click="audit(row.id, 'ACTIVE')">通过</el-button>
              <el-button size="small" type="danger" @click="openReject(row.id)">拒绝</el-button>
            </template>
            <template v-if="row.status === 'ACTIVE'">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
              <el-button size="small" type="danger" @click="openClose(row.id)">关闭</el-button>
            </template>
            <template v-if="row.status !== 'PENDING' && row.status !== 'ACTIVE'">
              <el-button size="small" @click="openDetail(row.id)">详情</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchShops" />
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialog" title="拒绝原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="doReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 关闭确认 -->
    <el-dialog v-model="closeDialog" title="关闭店铺" width="400px">
      <p>确定要关闭此店铺吗？关闭后店铺将无法展示商品。</p>
      <el-input v-model="closeReason" type="textarea" :rows="2" placeholder="关闭原因（选填）" style="margin-top: 10px" />
      <template #footer>
        <el-button @click="closeDialog = false">取消</el-button>
        <el-button type="danger" :loading="auditing" @click="doClose">确认关闭</el-button>
      </template>
    </el-dialog>

    <!-- 店铺详情 -->
    <el-dialog v-model="detailDialog" title="店铺详情" width="500px">
      <el-descriptions :column="1" border size="small" v-if="detailShop">
        <el-descriptions-item label="ID">{{ detailShop.id }}</el-descriptions-item>
        <el-descriptions-item label="店铺名">{{ detailShop.shopName }}</el-descriptions-item>
        <el-descriptions-item label="店主ID">{{ detailShop.userId }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailShop.phone }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailShop.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detailShop.status)" size="small">{{ statusMap[detailShop.status] || detailShop.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailShop.createTime?.substring(0, 16) || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminShops, getAdminShopDetail, auditShop, closeShop } from '@/api/admin'
import { ElMessage } from 'element-plus'

const statusMap: Record<string, string> = { PENDING: '待审核', ACTIVE: '已通过', REJECTED: '已拒绝', CLOSED: '已关闭' }
function statusType(s: string) {
  return { PENDING: 'warning', ACTIVE: 'success', REJECTED: 'danger', CLOSED: 'info' }[s] || ''
}

const shops = ref<any[]>([])
const loading = ref(false)
const auditing = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')

const rejectDialog = ref(false)
const rejectId = ref<number | null>(null)
const rejectReason = ref('')

const closeDialog = ref(false)
const closeId = ref<number | null>(null)
const closeReason = ref('')

const detailDialog = ref(false)
const detailShop = ref<any>(null)

async function fetchShops() {
  loading.value = true
  try {
    const res: any = await getAdminShops({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined
    })
    const data = res.data || {}
    shops.value = data.records || []
    total.value = data.total || 0
  } catch { shops.value = [] } finally { loading.value = false }
}

async function audit(id: number, status: string) {
  auditing.value = true
  try {
    await auditShop(id, { status })
    ElMessage.success(status === 'ACTIVE' ? '已通过' : '已拒绝')
    fetchShops()
  } catch { /* handled */ } finally { auditing.value = false }
}

function openReject(id: number) {
  rejectId.value = id
  rejectReason.value = ''
  rejectDialog.value = true
}

async function doReject() {
  if (!rejectId.value) return
  auditing.value = true
  try {
    await auditShop(rejectId.value, { status: 'REJECTED', rejectReason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectDialog.value = false
    fetchShops()
  } catch { /* handled */ } finally { auditing.value = false }
}

function openClose(id: number) {
  closeId.value = id
  closeReason.value = ''
  closeDialog.value = true
}

async function doClose() {
  if (!closeId.value) return
  auditing.value = true
  try {
    await closeShop(closeId.value, closeReason.value ? { reason: closeReason.value } : undefined)
    ElMessage.success('店铺已关闭')
    closeDialog.value = false
    fetchShops()
  } catch { /* handled */ } finally { auditing.value = false }
}

async function openDetail(id: number) {
  try {
    const res: any = await getAdminShopDetail(id)
    detailShop.value = res.data || null
    detailDialog.value = true
  } catch { /* handled */ }
}

onMounted(() => fetchShops())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
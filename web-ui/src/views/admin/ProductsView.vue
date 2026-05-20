<template>
  <div class="admin-page">
    <h2>商品管理</h2>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索商品名" style="width: 200px" clearable @clear="fetchProducts" @keyup.enter="fetchProducts" />
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 140px" clearable @change="fetchProducts">
          <el-option label="在售" value="ON_SHELF" />
          <el-option label="下架" value="OFF_SHELF" />
        </el-select>
        <el-button type="primary" @click="fetchProducts">搜索</el-button>
      </div>

      <el-table :data="products" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="productName" label="商品名" min-width="160" />
        <el-table-column prop="shopId" label="店铺ID" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SHELF' ? 'success' : 'info'" size="small">
              {{ row.status === 'ON_SHELF' ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ row.createTime ? row.createTime.substring(0, 16) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :type="row.status === 'ON_SHELF' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchProducts" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminProducts, adminUpdateProductStatus } from '@/api/admin'
import { ElMessage } from 'element-plus'

const products = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')

async function fetchProducts() {
  loading.value = true
  try {
    const res: any = await getAdminProducts({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined
    })
    const data = res.data || {}
    products.value = data.records || []
    total.value = data.total || 0
  } catch { products.value = [] } finally { loading.value = false }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  try {
    await adminUpdateProductStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 'ON_SHELF' ? '已上架' : '已下架')
  } catch { /* handled */ }
}

onMounted(() => fetchProducts())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>

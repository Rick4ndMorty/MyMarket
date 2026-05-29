<template>
  <div class="admin-page">
    <h2>商品管理</h2>

    <el-card>
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索商品名" style="width: 200px" clearable @clear="fetchProducts" @keyup.enter="fetchProducts" />
        <el-select v-model="filterStatus" placeholder="状态筛选" style="width: 130px" clearable @change="fetchProducts">
          <el-option label="在售" value="ON_SHELF" />
          <el-option label="下架" value="OFF_SHELF" />
          <el-option label="已删除" value="DELETED" />
        </el-select>
        <el-button type="primary" @click="fetchProducts">搜索</el-button>
      </div>

      <div class="batch-bar" v-if="selectedIds.length">
        <span>已选 {{ selectedIds.length }} 项</span>
        <el-button size="small" type="success" @click="batchUpdateStatus('ON_SHELF')">批量上架</el-button>
        <el-button size="small" type="warning" @click="batchUpdateStatus('OFF_SHELF')">批量下架</el-button>
        <el-button size="small" @click="selectedIds = []">取消选择</el-button>
      </div>

      <el-table :data="products" v-loading="loading" stripe @selection-change="(rows: any[]) => selectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="主图" width="80">
          <template #default="{ row }">
            <el-image :src="row.mainImage" style="width: 50px; height: 50px; border-radius: 4px;" fit="cover" v-if="row.mainImage" />
            <span v-else>无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名" min-width="160" />
        <el-table-column prop="shopId" label="店铺ID" width="80" />
        <el-table-column label="最低价" width="90">
          <template #default="{ row }">&yen;{{ (row.minPrice || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row.id)">编辑</el-button>
            <el-button size="small" type="success" v-if="row.status !== 'ON_SHELF'" @click="adminUpdateProductStatus(row.id, 'ON_SHELF')">上架</el-button>
            <el-button size="small" type="warning" v-if="row.status === 'ON_SHELF'" @click="adminUpdateProductStatus(row.id, 'OFF_SHELF')">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchProducts" />
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialog" title="编辑商品" width="600px" @close="editSkuList = []">
      <el-form :model="editForm" label-width="80px" v-if="editForm.productName !== undefined">
        <el-form-item label="商品名"><el-input v-model="editForm.productName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="分类ID"><el-input-number v-model="editForm.categoryId" :min="1" /></el-form-item>
        <el-form-item label="主图URL"><el-input v-model="editForm.mainImage" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 160px">
            <el-option label="在售" value="ON_SHELF" />
            <el-option label="下架" value="OFF_SHELF" />
          </el-select>
        </el-form-item>

        <!-- SKU 价格编辑 -->
        <el-divider content-position="left">SKU 价格</el-divider>
        <div class="sku-list">
          <div class="sku-row" v-for="(sku, i) in editSkuList" :key="i">
            <span class="sku-name">{{ sku.skuName }}</span>
            <span class="sku-attr" v-if="sku.skuAttrs">ID: {{ sku.id }} - {{ sku.skuAttrs }}</span>
            <el-input-number v-model="sku.price" :precision="2" :min="0" size="small" style="width: 140px" />
          </div>
          <div v-if="!editSkuList.length" class="empty-tip">该商品无 SKU</div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminProducts, getAdminProductDetail, adminUpdateProduct, adminUpdateProductStatus } from '@/api/admin'
import { ElMessage } from 'element-plus'

const statusMap: Record<string, string> = { ON_SHELF: '在售', OFF_SHELF: '下架', DELETED: '已删除' }
function statusType(s: string) {
  return { ON_SHELF: 'success', OFF_SHELF: 'warning', DELETED: 'danger' }[s] || ''
}

const products = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')
const selectedIds = ref<number[]>([])

const editDialog = ref(false)
const editingId = ref<number | null>(null)
const editForm = ref<any>({})
const editSkuList = ref<any[]>([])

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

async function openEdit(id: number) {
  try {
    const res: any = await getAdminProductDetail(id)
    const d = res.data || {}
    editingId.value = id
    editForm.value = { ...d.product } as any
    editSkuList.value = (d.skus || []).map((s: any) => ({ ...s, price: s.price || 0 }))
    editDialog.value = true
  } catch { /* handled */ }
}

async function saveProduct() {
  if (!editingId.value) return
  saving.value = true
  try {
    const payload: any = {
      productName: editForm.value.productName,
      description: editForm.value.description,
      categoryId: editForm.value.categoryId,
      mainImage: editForm.value.mainImage,
      status: editForm.value.status
    }
    if (editSkuList.value.length) {
      payload.skus = editSkuList.value.map(s => ({ id: s.id, price: s.price, skuName: s.skuName }))
    }
    await adminUpdateProduct(editingId.value, payload)
    ElMessage.success('保存成功')
    editDialog.value = false
    fetchProducts()
  } catch { /* handled */ } finally { saving.value = false }
}

async function batchUpdateStatus(status: string) {
  if (!selectedIds.value.length) return
  loading.value = true
  try {
    await Promise.all(selectedIds.value.map(id => adminUpdateProductStatus(id, status)))
    ElMessage.success('批量操作成功')
    selectedIds.value = []
    fetchProducts()
  } catch { /* handled */ } finally { loading.value = false }
}

onMounted(() => fetchProducts())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 8px 12px; margin-bottom: 12px; background: #ecf5ff; border-radius: 4px; font-size: 13px; color: #606266; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.sku-list { max-height: 220px; overflow-y: auto; }
.sku-row { display: flex; align-items: center; gap: 12px; padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
.sku-name { font-weight: 500; width: 100px; }
.sku-attr { color: #909399; font-size: 12px; flex: 1; }
.empty-tip { text-align: center; color: #c0c4cc; padding: 16px 0; }
:deep(.el-divider) { margin: 12px 0; }
</style>
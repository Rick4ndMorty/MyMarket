<template>
  <div class="shop-manage-container" v-loading="loadingShop">
    <!-- ACTIVE 店铺：正常管理 -->
    <template v-if="shop.id && shop.status === 'ACTIVE'">
      <div class="shop-header">
        <div class="shop-info">
          <img :src="shop.logo || '/vite.svg'" class="shop-logo" />
          <div>
            <h2>{{ shop.shopName }}</h2>
            <p class="shop-desc">{{ shop.description }}</p>
          </div>
        </div>
        <el-button type="primary" @click="openProductDialog(null)">添加商品</el-button>
      </div>
      <el-table :data="products" border stripe v-loading="loadingProducts" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <img
              :src="row.images?.[0] || '/vite.svg'"
              style="width: 60px; height: 60px; object-fit: cover; border-radius: 4px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="价格区间" width="160">
          <template #default="{ row }">
            <span v-if="row.minPrice != null">
              &yen;{{ row.minPrice?.toFixed(2) }}
              <template v-if="row.maxPrice && row.maxPrice !== row.minPrice">
                - &yen;{{ row.maxPrice?.toFixed(2) }}
              </template>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="80">
          <template #default="{ row }">
            {{ row.totalStock ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SHELF' ? 'success' : 'info'" size="small">
              {{ row.status === 'ON_SHELF' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="openProductDialog(row)">
              编辑
            </el-button>
            <el-button
              text
              size="small"
              :type="row.status === 'ON_SHELF' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
            </el-button>
            <el-button
              text
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="productTotal > 0">
        <el-pagination
          v-model:current-page="productPage"
          v-model:page-size="productPageSize"
          :total="productTotal"
          layout="total, prev, pager, next"
          background
          @current-change="fetchProducts"
        />
      </div>

      <el-dialog
        v-model="dialogVisible"
        :title="editingProduct ? '编辑商品' : '添加商品'"
        width="700px"
        destroy-on-close
      >
        <el-form ref="formRef" :model="form" :rules="productRules" label-width="100px">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称" />
          </el-form-item>
          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="请输入商品描述"
            />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-input v-model="form.category" placeholder="请输入商品分类" />
          </el-form-item>
          <el-form-item label="图片URL" prop="imageInput">
            <el-input v-model="form.imageInput" placeholder="请输入图片URL，多个用逗号分隔" />
          </el-form-item>

          <el-divider>SKU 列表</el-divider>
          <div v-for="(sku, idx) in form.skus" :key="idx" class="sku-row">
            <el-input v-model="sku.skuName" placeholder="规格名" style="width: 130px;" />
            <el-input-number v-model="sku.price" :min="0.01" :precision="2" placeholder="价格" style="width: 120px;" />
            <el-input-number v-model="sku.stock" :min="0" placeholder="库存" style="width: 100px;" />
            <el-input v-model="sku.image" placeholder="图片URL" style="width: 150px;" />
            <el-button type="danger" text @click="form.skus.splice(idx, 1)" :disabled="form.skus.length <= 1">
              删除
            </el-button>
          </div>
          <el-button type="primary" text @click="addSku" style="margin-top: 8px;">
            + 添加 SKU
          </el-button>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleProductSubmit">
            确定
          </el-button>
        </template>
      </el-dialog>
    </template>

    <!-- PENDING 审核中 -->
    <template v-else-if="shop.id && shop.status === 'PENDING'">
      <div class="shop-header">
        <div class="shop-info">
          <img :src="shop.logo || '/vite.svg'" class="shop-logo" />
          <div>
            <h2>{{ shop.shopName }}</h2>
            <p class="shop-desc">{{ shop.description }}</p>
          </div>
        </div>
      </div>
      <el-result icon="warning" title="店铺审核中" description="您的店铺申请正在审核中，审核通过后即可管理商品">
        <template #extra>
          <el-tag type="warning">审核中</el-tag>
        </template>
      </el-result>
    </template>

    <!-- REJECTED 审核未通过 -->
    <template v-else-if="shop.id && shop.status === 'REJECTED'">
      <div class="shop-header">
        <div class="shop-info">
          <img :src="shop.logo || '/vite.svg'" class="shop-logo" />
          <div>
            <h2>{{ shop.shopName }}</h2>
            <p class="shop-desc">{{ shop.description }}</p>
          </div>
        </div>
      </div>
      <el-result icon="error" title="审核未通过" :description="shop.rejectReason || '您的店铺申请未通过审核'">
        <template #extra>
          <el-button type="primary" @click="router.push('/shop/apply')">重新申请</el-button>
        </template>
      </el-result>
    </template>

    <!-- CLOSED 店铺已关闭 -->
    <template v-else-if="shop.id && shop.status === 'CLOSED'">
      <div class="shop-header">
        <div class="shop-info">
          <img :src="shop.logo || '/vite.svg'" class="shop-logo" />
          <div>
            <h2>{{ shop.shopName }}</h2>
            <p class="shop-desc">{{ shop.description }}</p>
          </div>
        </div>
      </div>
      <el-result icon="info" title="店铺已关闭" description="您的店铺已关闭，如需重新开店请重新申请">
        <template #extra>
          <el-button type="primary" @click="router.push('/shop/apply')">重新申请</el-button>
        </template>
      </el-result>
    </template>

    <!-- 无店铺 -->
    <el-empty v-else description="尚未开通店铺">
      <el-button type="primary" @click="router.push('/shop/apply')">申请开店</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMyShopSafe } from '@/api/shop'
import { publish, search, updateProduct, updateStatus, deleteProduct } from '@/api/product'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const shop = ref<any>({})
const products = ref<any[]>([])
const loadingShop = ref(false)
const loadingProducts = ref(false)
const productPage = ref(1)
const productPageSize = ref(10)
const productTotal = ref(0)

const router = useRouter()
const userStore = useUserStore()

const dialogVisible = ref(false)
const submitting = ref(false)
const editingProduct = ref<any>(null)
const formRef = ref<FormInstance>()

interface SkuForm {
  skuName: string
  price: number
  stock: number
  image: string
}

const form = reactive({
  name: '',
  description: '',
  category: '',
  imageInput: '',
  skus: [{ skuName: '默认', price: 0.01, stock: 0, image: '' }] as SkuForm[]
})

const productRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  skus: [{ required: true, message: '至少需要一个SKU', trigger: 'blur' }]
}

function addSku() {
  form.skus.push({ skuName: '', price: 0.01, stock: 0, image: '' })
}

function openProductDialog(product: any) {
  editingProduct.value = product
  if (product) {
    form.name = product.productName
    form.description = product.description || ''
    form.category = product.category || ''
    form.imageInput = product.images?.join(',') || ''
    form.skus = product.skus?.length
      ? product.skus.map((s: any) => ({
          skuName: s.skuName || '默认',
          price: s.price,
          stock: s.stock,
          image: s.image || ''
        }))
      : [{ skuName: '默认', price: 0.01, stock: 0, image: '' }]
  } else {
    form.name = ''
    form.description = ''
    form.category = ''
    form.imageInput = ''
    form.skus = [{ skuName: '默认', price: 0.01, stock: 0, image: '' }]
  }
  dialogVisible.value = true
}

async function handleProductSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const images = form.imageInput
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)

  const skus = form.skus.map(s => ({
    skuName: s.skuName || '默认',
    price: s.price,
    stock: s.stock,
    image: s.image || undefined
  }))

  submitting.value = true
  try {
    if (editingProduct.value) {
      await updateProduct(editingProduct.value.id, {
        productName: form.name,
        description: form.description,
        category: form.category,
        images: images.length ? images : undefined,
        skus
      }, shop.value.id)
      ElMessage.success('商品已更新')
    } else {
      await publish({
        productName: form.name,
        description: form.description,
        category: form.category,
        images: images.length ? images : undefined,
        skus
      }, shop.value.id)
      ElMessage.success('商品已发布')
    }
    dialogVisible.value = false
    await fetchProducts()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(product: any) {
  const newStatus = product.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  const actionText = newStatus === 'ON_SHELF' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${actionText}该商品吗？`, `确认${actionText}`, { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateStatus(product.id, newStatus, shop.value.id)
    ElMessage.success(`已${actionText}`)
    await fetchProducts()
  } catch {
    // handled by interceptor
  }
}

async function handleDelete(product: any) {
  try {
    await ElMessageBox.confirm(`确定要删除商品"${product.productName}"吗？删除后无法恢复。`, '确认删除', { type: 'error' })
  } catch {
    return
  }
  try {
    await deleteProduct(product.id, shop.value.id)
    ElMessage.success('商品已删除')
    await fetchProducts()
  } catch {
    // handled by interceptor
  }
}

async function fetchShop() {
  loadingShop.value = true
  const data = await getMyShopSafe()
  shop.value = data || {}
  loadingShop.value = false
}

async function fetchProducts() {
  loadingProducts.value = true
  try {
    const res: any = await search({
      shopId: shop.value.id,
      page: productPage.value,
      size: productPageSize.value,
      status: 'ALL'
    })
    products.value = res.data.records || res.data.list || res.data || []
    productTotal.value = res.data.total || 0
  } catch {
    products.value = []
  } finally {
    loadingProducts.value = false
  }
}

onMounted(async () => {
  await userStore.fetchProfile()
  await fetchShop()
  if (shop.value.id) {
    await fetchProducts()
  }
})
</script>

<style scoped>
.shop-manage-container {
  min-height: 70vh;
}

.shop-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.shop-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shop-logo {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.shop-info h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.shop-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.sku-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

<template>
  <div class="checkout-container">
    <h2>确认订单</h2>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="section-card">
          <template #header><strong>选择收货地址</strong></template>
          <div v-if="addresses.length === 0">
            <el-empty description="暂无收货地址">
              <el-button type="primary" @click="$router.push('/address')">去添加地址</el-button>
            </el-empty>
          </div>
          <el-radio-group v-model="selectedAddressId" v-else class="address-group">
            <div
              v-for="addr in addresses"
              :key="addr.id"
              :class="['address-card', { selected: selectedAddressId === addr.id }]"
              @click="selectedAddressId = addr.id"
            >
              <div class="addr-info">
                <div class="addr-contact">
                  <span class="addr-name">{{ addr.receiverName }}</span>
                  <span class="addr-phone">{{ addr.phone }}</span>
                  <el-tag v-if="addr.isDefault" type="danger" size="small">默认</el-tag>
                </div>
                <div class="addr-full">
                  {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}
                </div>
              </div>
            </div>
          </el-radio-group>
        </el-card>

        <el-card class="section-card">
          <template #header><strong>商品清单</strong></template>
          <div v-if="cartStore.items.length === 0">
            <el-empty description="购物车为空">
              <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
            </el-empty>
          </div>
          <div v-else>
            <div
              v-for="item in cartStore.items"
              :key="item.skuId"
              class="checkout-item"
            >
              <img :src="item.image || '/vite.svg'" class="checkout-item-img" />
              <div class="checkout-item-info">
                <div class="checkout-item-name">{{ item.productName }}</div>
                <div class="checkout-item-sku">{{ item.skuName }}</div>
              </div>
              <span class="checkout-item-price">&yen;{{ item.price.toFixed(2) }}</span>
              <div class="checkout-item-qty-wrap">
                <el-button size="small" text @click="cartStore.updateQuantity(item.skuId, item.quantity - 1)">-</el-button>
                <span>{{ item.quantity }}</span>
                <el-button size="small" text @click="cartStore.updateQuantity(item.skuId, item.quantity + 1)">+</el-button>
              </div>
              <span class="checkout-item-subtotal">
                &yen;{{ (item.price * item.quantity).toFixed(2) }}
              </span>
              <el-button type="danger" size="small" text @click="cartStore.removeItem(item.skuId)">删除</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="summary-card">
          <template #header><strong>订单摘要</strong></template>
          <div class="summary-row">
            <span>商品总计</span>
            <span>&yen;{{ cartStore.totalPrice.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>运费</span>
            <span class="free-shipping">免运费</span>
          </div>
          <el-divider />
          <div class="summary-total">
            <span>应付金额</span>
            <span class="total-price">&yen;{{ cartStore.totalPrice.toFixed(2) }}</span>
          </div>
          <div class="remark-wrap">
            <el-input
              v-model="remark"
              placeholder="订单备注（可选）"
              size="small"
            />
          </div>
          <el-button
            type="primary"
            size="large"
            style="width: 100%; margin-top: 16px;"
            :loading="submitting"
            :disabled="!selectedAddressId || cartStore.items.length === 0"
            @click="placeOrder"
          >
            提交订单
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { getAddresses } from '@/api/user'
import { createOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()

const addresses = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)
const remark = ref('')
const submitting = ref(false)

async function loadAddresses() {
  try {
    const res: any = await getAddresses()
    addresses.value = res.data || []
    const def = addresses.value.find((a: any) => a.isDefault)
    if (def) {
      selectedAddressId.value = def.id
    } else if (addresses.value.length > 0) {
      selectedAddressId.value = addresses.value[0].id
    }
  } catch {
    addresses.value = []
  }
}

async function placeOrder() {
  if (!selectedAddressId.value || cartStore.items.length === 0) {
    ElMessage.warning('请选择收货地址')
    return
  }

  // 检测跨店商品
  const shopIds = new Set(cartStore.items.map(i => i.shopId).filter(Boolean))
  if (shopIds.size > 1) {
    ElMessage.warning('购物车中包含不同店铺的商品，请分别下单。可在购物车下拉框中清空后重新添加。')
    return
  }

  submitting.value = true
  try {
    const orderItems = cartStore.items.map(i => ({
      skuId: i.skuId,
      quantity: i.quantity
    }))

    const orderRes: any = await createOrder({
      addressId: selectedAddressId.value,
      items: orderItems,
      remark: remark.value || undefined
    })

    const orderId = orderRes.data?.id || orderRes.data?.orderId
    if (!orderId) {
      ElMessage.error('订单创建失败')
      return
    }

    cartStore.clear()
    ElMessage.success('下单成功，请完成支付')
    router.push(`/payment/${orderId}`)
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAddresses()
})
</script>

<style scoped>
.checkout-container {
  max-width: 1000px;
  margin: 0 auto;
}

.checkout-container h2 {
  margin: 0 0 20px;
  font-size: 22px;
}

.section-card {
  margin-bottom: 16px;
}

.address-group {
  display: block;
}

.address-card {
  padding: 12px 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.address-card:hover {
  border-color: #b3d8ff;
}

.address-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.addr-contact {
  margin-bottom: 4px;
}

.addr-name {
  font-weight: 600;
  margin-right: 10px;
}

.addr-phone {
  color: #909399;
  margin-right: 8px;
}

.addr-full {
  color: #606266;
  font-size: 14px;
}

.checkout-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.checkout-item-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
}

.checkout-item-info {
  flex: 1;
}

.checkout-item-name {
  font-size: 14px;
  color: #303133;
}

.checkout-item-sku {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.checkout-item-price {
  color: #606266;
  font-size: 14px;
}

.checkout-item-qty-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}

.checkout-item-qty-wrap span {
  min-width: 24px;
  text-align: center;
  font-size: 14px;
}

.checkout-item-subtotal {
  color: #f56c6c;
  font-weight: 500;
}

.summary-card {
  position: sticky;
  top: 80px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}

.free-shipping {
  color: #67c23a;
}

.summary-total {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  margin-top: 4px;
}

.total-price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
}

.remark-wrap {
  margin-top: 12px;
}
</style>

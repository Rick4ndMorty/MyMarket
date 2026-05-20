<template>
  <div class="address-container">
    <div class="address-header">
      <h2>收货地址</h2>
      <el-button type="primary" @click="openDialog(null)">新增地址</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="addresses.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无收货地址" />
      </div>

      <div v-for="addr in addresses" :key="addr.id" class="address-card">
        <div class="addr-main">
          <div class="addr-top">
            <span class="addr-name">{{ addr.receiverName }}</span>
            <span class="addr-phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault" type="danger" size="small">默认</el-tag>
          </div>
          <div class="addr-detail">
            {{ addr.province }} {{ addr.city }} {{ addr.district }} {{ addr.detail }}
          </div>
        </div>
        <div class="addr-actions">
          <el-button text type="primary" size="small" @click="openDialog(addr)">编辑</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(addr)">删除</el-button>
          <el-button
            v-if="!addr.isDefault"
            text
            type="success"
            size="small"
            @click="setDefault(addr)"
          >
            设为默认
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑地址' : '新增地址'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="请输入省份" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="form.district" placeholder="请输入区/县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  getAddresses,
  addAddress,
  updateAddress,
  deleteAddress
} from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const addresses = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editing = ref<any>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  receiverName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

const rules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

function openDialog(addr: any) {
  editing.value = addr
  if (addr) {
    form.receiverName = addr.receiverName
    form.phone = addr.phone
    form.province = addr.province
    form.city = addr.city
    form.district = addr.district
    form.detail = addr.detail
    form.isDefault = addr.isDefault || false
  } else {
    form.receiverName = ''
    form.phone = ''
    form.province = ''
    form.city = ''
    form.district = ''
    form.detail = ''
    form.isDefault = false
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = { ...form }
    if (editing.value) {
      await updateAddress(editing.value.id, data)
      ElMessage.success('地址已更新')
    } else {
      await addAddress(data)
      ElMessage.success('地址已添加')
    }
    dialogVisible.value = false
    await fetchAddresses()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(addr: any) {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '确认删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAddress(addr.id)
    ElMessage.success('已删除')
    await fetchAddresses()
  } catch {
    // handled by interceptor
  }
}

async function setDefault(addr: any) {
  try {
    await updateAddress(addr.id, {
      receiverName: addr.receiverName,
      phone: addr.phone,
      province: addr.province,
      city: addr.city,
      district: addr.district,
      detail: addr.detail,
      isDefault: true
    })
    ElMessage.success('已设为默认地址')
    await fetchAddresses()
  } catch {
    // handled by interceptor
  }
}

async function fetchAddresses() {
  loading.value = true
  try {
    const res: any = await getAddresses()
    addresses.value = res.data || []
  } catch {
    addresses.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAddresses()
})
</script>

<style scoped>
.address-container {
  max-width: 800px;
  margin: 0 auto;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.address-header h2 {
  margin: 0;
  font-size: 20px;
}

.address-card {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e4e7ed;
}

.addr-top {
  margin-bottom: 6px;
}

.addr-name {
  font-weight: 600;
  font-size: 15px;
  margin-right: 10px;
}

.addr-phone {
  color: #909399;
  margin-right: 10px;
}

.addr-detail {
  color: #606266;
  font-size: 14px;
}

.addr-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 20px;
}

.empty-state {
  padding: 60px 0;
}
</style>

<template>
  <div class="shop-apply-container" v-loading="pageStatus === 'loading'">
    <el-card v-if="pageStatus === 'PENDING'" header="店铺申请">
      <el-result
        icon="warning"
        title="您的店铺申请正在审核中"
        description="请耐心等待管理员审核，审核通过后即可管理您的店铺。"
      >
        <template #extra>
          <el-button type="primary" @click="$router.push('/shop/manage')">查看店铺状态</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </el-card>

    <el-card v-else-if="pageStatus === 'denied'" header="店铺申请">
      <el-result
        :icon="shopInfo.status === 'REJECTED' ? 'error' : 'info'"
        :title="shopInfo.status === 'REJECTED' ? '审核未通过' : '店铺已关闭'"
        :description="shopInfo.rejectReason || (shopInfo.status === 'REJECTED' ? '您的店铺申请未通过审核' : '您的店铺已关闭')"
      >
        <template #extra>
          <el-button v-if="shopInfo.status === 'REJECTED'" type="danger" @click="pageStatus = 'form'">重新申请</el-button>
          <el-button v-if="shopInfo.status === 'CLOSED'" type="primary" @click="pageStatus = 'form'">重新申请</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </el-card>

    <el-card v-else-if="pageStatus === 'form'" header="申请开店">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        size="large"
        style="max-width: 600px;"
      >
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="Logo URL" prop="logoUrl">
          <el-input v-model="form.logoUrl" placeholder="请输入店铺Logo地址" />
          <div class="logo-preview" v-if="form.logoUrl">
            <img :src="form.logoUrl" alt="logo预览" />
          </div>
        </el-form-item>
        <el-form-item label="店铺描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入店铺描述"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交申请
          </el-button>
          <el-button @click="$router.push('/')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apply, getMyShopSafe } from '@/api/shop'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const pageStatus = ref<'loading' | 'form' | 'PENDING' | 'denied'>('loading')
const shopInfo = ref<any>({})

const form = reactive({
  shopName: '',
  logoUrl: '',
  description: '',
  phone: ''
})

const rules: FormRules = {
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 2, max: 30, message: '店铺名称长度 2-30 位', trigger: 'blur' }
  ],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

onMounted(async () => {
  const shop = await getMyShopSafe()
  if (shop) {
    if (shop.status === 'PENDING') {
      pageStatus.value = 'PENDING'
      return
    }
    if (shop.status === 'ACTIVE') {
      router.push('/shop/manage')
      return
    }
    shopInfo.value = shop
    pageStatus.value = 'denied'
    return
  }
  pageStatus.value = 'form'
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await apply({
      shopName: form.shopName,
      logoUrl: form.logoUrl || undefined,
      description: form.description || undefined,
      phone: form.phone
    })
    ElMessage.success('店铺申请已提交')
    pageStatus.value = 'PENDING'
    await userStore.fetchProfile()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.shop-apply-container {
  max-width: 700px;
  margin: 0 auto;
  min-height: 50vh;
}

.logo-preview {
  margin-top: 8px;
}

.logo-preview img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}
</style>

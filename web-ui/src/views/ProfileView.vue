<template>
  <div class="profile-container">
    <el-card header="个人中心" v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        label-width="100px"
        size="large"
        style="max-width: 500px;"
      >
        <el-form-item label="用户名">
          <el-input :model-value="userStore.userInfo?.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="userStore.isSeller ? 'warning' : 'success'">
            {{ userStore.isSeller ? '卖家' : '买家' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="form.avatar" placeholder="请输入头像URL" />
          <div class="avatar-preview" v-if="form.avatar">
            <img :src="form.avatar" alt="头像预览" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSave">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  email: '',
  phone: '',
  avatar: ''
})

async function loadProfile() {
  loading.value = true
  try {
    await userStore.fetchProfile()
    form.email = userStore.userInfo?.email || ''
    form.phone = userStore.userInfo?.phone || ''
    form.avatar = userStore.userInfo?.avatar || ''
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  submitting.value = true
  try {
    await userStore.updateProfile({
      email: form.email,
      phone: form.phone,
      avatar: form.avatar
    })
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-container {
  max-width: 700px;
  margin: 0 auto;
}

.avatar-preview {
  margin-top: 10px;
}

.avatar-preview img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e4e7ed;
}
</style>

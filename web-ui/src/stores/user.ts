import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getProfile, updateProfile as updateProfileApi } from '@/api/user'
import { ElMessage } from 'element-plus'

interface UserInfo {
  id: number
  username: string
  email: string
  phone: string
  role: string
  avatar: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isSeller = computed(() => userInfo.value?.role === 'SELLER')
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isBuyer = computed(() => !isSeller.value)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  async function login(username: string, password: string) {
    const res: any = await loginApi(username, password)
    setToken(res.data.token)
    setUserInfo(res.data.userInfo)
    ElMessage.success('登录成功')
  }

  async function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  async function fetchProfile() {
    const res: any = await getProfile()
    setUserInfo(res.data)
  }

  async function updateProfile(data: { email?: string; phone?: string; avatar?: string }) {
    const res: any = await updateProfileApi(data)
    setUserInfo({ ...userInfo.value!, ...data })
    ElMessage.success('个人信息已更新')
    return res.data
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isSeller,
    isAdmin,
    isBuyer,
    login,
    logout,
    fetchProfile,
    setToken,
    setUserInfo,
    updateProfile
  }
})

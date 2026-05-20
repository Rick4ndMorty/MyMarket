import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const instance = response.headers['x-instance']
    if (instance) {
      console.log(`[负载均衡] 本次请求由 ${instance} 处理`)
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || 'request failed')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    }
    ElMessage.error(error.message || 'network error')
    return Promise.reject(error)
  }
)

export default request

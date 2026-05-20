import request from '@/api/request'
import axios from 'axios'

export function apply(data: {
  shopName: string
  logoUrl?: string
  description?: string
  phone?: string
}) {
  return request.post('/shop', data)
}

export function getShop(id: number) {
  return request.get(`/shop/${id}`)
}

export function getMyShop() {
  return request.get('/shop/my')
}

/** 静默查询当前用户店铺，不存在时返回 null，不弹错误提示 */
export async function getMyShopSafe(): Promise<any | null> {
  const token = localStorage.getItem('token')
  if (!token) return null
  try {
    const res = await axios.get('/api/shop/my', {
      headers: { Authorization: `Bearer ${token}` },
      timeout: 5000,
    })
    if (res.data?.code === 200 && res.data?.data) return res.data.data
  } catch {}
  return null
}

export function updateShop(id: number, data: {
  shopName?: string
  logoUrl?: string
  description?: string
  phone?: string
}) {
  return request.put(`/shop/${id}`, data)
}

export function sendMessage(data: {
  shopId: number
  userId?: number
  content: string
  senderType?: string
}) {
  return request.post('/shop/customer/message', data)
}

export function getMessages(params: {
  shopId: number
  userId?: number
  page?: number
  pageSize?: number
}) {
  return request.get('/shop/customer/message/list', { params })
}

export function markRead(id: number) {
  return request.put(`/shop/customer/message/${id}/read`)
}

export function markAllRead(shopId: number, userId: number) {
  return request.put(`/shop/customer/message/read/${shopId}/${userId}`)
}

export function getConversations() {
  return request.get('/shop/customer/conversations')
}

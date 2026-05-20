import request from '@/api/request'

export function login(username: string, password: string) {
  return request.post('/user/login', { username, password })
}

export function register(data: { username: string; password: string; email: string; phone?: string; role?: string }) {
  return request.post('/user/register', data)
}

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data: { email?: string; phone?: string; avatar?: string }) {
  return request.put('/user/profile', data)
}

export function getAddresses() {
  return request.get('/user/address')
}

export function addAddress(data: {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: boolean
}) {
  return request.post('/user/address', data)
}

export function updateAddress(id: number, data: {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: boolean
}) {
  return request.put(`/user/address/${id}`, data)
}

export function deleteAddress(id: number) {
  return request.delete(`/user/address/${id}`)
}

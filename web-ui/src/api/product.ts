import request from '@/api/request'

export function publish(data: {
  productName: string
  description?: string
  images?: string[]
  category?: string
  skus: Array<{ skuName: string; price: number; stock: number; image?: string }>
}, shopId: number) {
  return request.post('/product', data, { params: { shopId } })
}

export function search(params: {
  keyword?: string
  page?: number
  size?: number
  category?: string
  shopId?: number
  status?: string
}) {
  return request.get('/product', { params })
}

export function getDetail(id: number) {
  return request.get(`/product/${id}`)
}

export function updateProduct(id: number, data: {
  productName?: string
  description?: string
  images?: string[]
  category?: string
  skus?: Array<{ skuName: string; price: number; stock: number; image?: string }>
}, shopId: number) {
  return request.put(`/product/${id}`, data, { params: { shopId } })
}

export function updateStatus(id: number, status: string, shopId: number) {
  return request.put(`/product/${id}/status`, null, { params: { shopId, status } })
}

export function deleteProduct(id: number, shopId: number) {
  return request.delete(`/product/${id}`, { params: { shopId } })
}

export function createReview(data: {
  productId: number
  orderId?: number
  type: string
  rating?: number
  content: string
  parentId?: number
}) {
  return request.post('/product/review', data)
}

export function getReviews(productId: number, type: string = 'REVIEW', page: number = 1, size: number = 10) {
  return request.get(`/product/${productId}/reviews`, { params: { type, page, size } })
}

export function getQuestions(productId: number, page: number = 1, size: number = 10) {
  return request.get(`/product/${productId}/questions`, { params: { page, size } })
}

import request from '@/api/request'

export function createOrder(data: {
  addressId: number
  items: Array<{ skuId: number; quantity: number }>
  remark?: string
}) {
  return request.post('/order', data)
}

export function getBuyerOrders(params: {
  status?: string
  page?: number
  pageSize?: number
}) {
  return request.get('/order/my', { params: { status: params.status, page: params.page, page_size: params.pageSize } })
}

export function getShopOrders(params: {
  shopId: number
  status?: string
  page?: number
  pageSize?: number
}) {
  return request.get('/order/shop', { params: { shop_id: params.shopId, status: params.status, page: params.page, page_size: params.pageSize } })
}

export function getOrderDetail(id: number) {
  return request.get(`/order/${id}`)
}

export function cancelOrder(id: number, reason?: string) {
  return request.put(`/order/${id}/cancel`, { reason })
}

export function shipOrder(id: number, shopId: number) {
  return request.put(`/order/${id}/ship`, null, { params: { shopId } })
}

export function processRefund(id: number, shopId: number, approve: boolean) {
  return request.put(`/order/${id}/refund`, null, { params: { shopId, approve } })
}

export function confirmReceipt(id: number) {
  return request.put(`/order/${id}/complete`)
}

import request from '@/api/request'

export function createPayment(data: {
  orderId: number
  paymentMethod?: string
}) {
  return request.post('/payment', data)
}

export function getPayment(id: number) {
  return request.get(`/payment/${id}`)
}

export function getPaymentByNo(paymentNo: string) {
  return request.get(`/payment/no/${paymentNo}`)
}

export function mockPayCallback(paymentNo: string) {
  return request.post(`/payment/callback/mock/${paymentNo}`)
}

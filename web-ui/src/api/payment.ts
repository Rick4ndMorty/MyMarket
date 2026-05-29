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

/**
 * 主动查询支付宝支付状态（二次确认，防止异步通知丢失）
 */
export function queryAlipayStatus(paymentNo: string) {
  return request.post(`/payment/query-alipay/${paymentNo}`)
}

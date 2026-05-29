import request from '@/api/request'

// ==================== Dashboard ====================
export function getUserStats() { return request.get('/user/admin/stats') }
export function getShopStats() { return request.get('/shop/admin/stats') }
export function getProductStats() { return request.get('/product/admin/stats') }
export function getOrderStats() { return request.get('/order/admin/stats') }
export function getPaymentStats() { return request.get('/payment/admin/stats') }

// ==================== User management ====================
export function getAdminUsers(params: any) { return request.get('/user/admin/users', { params }) }
export function adminUpdateUser(id: number, data: any) { return request.put(`/user/admin/users/${id}`, data) }
export function adminDeleteUser(id: number) { return request.delete(`/user/admin/users/${id}`) }
export function adminBatchUpdateUserStatus(data: any) { return request.put('/user/admin/users/batch/status', data) }

// ==================== Shop management ====================
export function getAdminShops(params: any) { return request.get('/shop/admin/shops', { params }) }
export function getAdminShopDetail(id: number) { return request.get(`/shop/admin/shops/${id}`) }
export function auditShop(id: number, data: any) { return request.put(`/shop/admin/shops/${id}/audit`, data) }
export function closeShop(id: number, data?: any) { return request.put(`/shop/admin/shops/${id}/close`, data || {}) }

// ==================== Product management ====================
export function getAdminProducts(params: any) { return request.get('/product/admin/products', { params }) }
export function getAdminProductDetail(id: number) { return request.get(`/product/admin/products/${id}`) }
export function adminUpdateProduct(id: number, data: any) { return request.put(`/product/admin/products/${id}`, data) }
export function adminUpdateProductStatus(id: number, status: string) { return request.put(`/product/admin/products/${id}/status`, { status }) }

// ==================== Order management ====================
export function getAdminOrders(params: any) { return request.get('/order/admin/orders', { params }) }
export function getAdminOrderDetail(id: number) { return request.get(`/order/admin/orders/${id}`) }
export function adminUpdateOrderStatus(id: number, status: string) { return request.put(`/order/admin/orders/${id}/status`, { status }) }
export function adminRefundOrder(id: number, reason?: string) { return request.put(`/order/admin/orders/${id}/refund`, reason ? { reason } : {}) }

// ==================== Payment management ====================
export function getAdminPayments(params: any) { return request.get('/payment/admin/payments', { params }) }
export function adminRefundPayment(id: number) { return request.put(`/payment/admin/payments/${id}/refund`) }
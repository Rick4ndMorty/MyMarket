import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetailView.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/shop/:id',
    name: 'ShopDetail',
    component: () => import('@/views/ShopDetailView.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue')
  },
  {
    path: '/address',
    name: 'Address',
    component: () => import('@/views/AddressView.vue')
  },
  {
    path: '/shop/apply',
    name: 'ShopApply',
    component: () => import('@/views/ShopApplyView.vue')
  },
  {
    path: '/shop/manage',
    name: 'ShopManage',
    component: () => import('@/views/ShopManageView.vue')
  },
  {
    path: '/shop/orders',
    name: 'ShopOrders',
    component: () => import('@/views/ShopOrdersView.vue'),
    meta: { sellerOnly: true }
  },
  {
    path: '/shop/chat',
    name: 'ShopChat',
    component: () => import('@/views/ShopChatView.vue'),
    meta: { sellerOnly: true }
  },
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('@/views/OrderListView.vue')
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetailView.vue')
  },
  {
    path: '/messages',
    name: 'MessageCenter',
    component: () => import('@/views/MessageCenterView.vue')
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('@/views/CheckoutView.vue')
  },
  {
    path: '/payment/:id',
    name: 'Payment',
    component: () => import('@/views/PaymentView.vue')
  },
  {
    path: '/payment/return',
    name: 'PaymentReturn',
    component: () => import('@/views/PaymentReturnView.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { adminOnly: true },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UsersView.vue') },
      { path: 'shops', name: 'AdminShops', component: () => import('@/views/admin/ShopsView.vue') },
      { path: 'products', name: 'AdminProducts', component: () => import('@/views/admin/ProductsView.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrdersView.vue') },
      { path: 'payments', name: 'AdminPayments', component: () => import('@/views/admin/PaymentsView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.noAuth) {
    next()
    return
  }

  if (!userStore.token) {
    next('/login')
    return
  }

  if (to.meta.sellerOnly && !userStore.isSeller) {
    next('/')
    return
  }

  if (to.meta.adminOnly && !userStore.isAdmin) {
    next('/')
    return
  }

  next()
})

export default router

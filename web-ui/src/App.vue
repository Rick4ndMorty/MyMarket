<template>
  <template v-if="isAdminRoute">
    <router-view />
  </template>

  <el-container v-else class="app-container">
    <el-header class="app-header">
      <div class="header-inner">
        <router-link to="/" class="logo">TradeStation</router-link>

        <div class="header-search" v-if="showSearch">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索商品"
            size="default"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>

        <div class="header-nav">
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            :ellipsis="false"
            class="nav-menu"
            router
          >
            <el-menu-item index="/">首页</el-menu-item>

            <template v-if="userStore.isLoggedIn">
              <el-menu-item index="/orders">我的订单</el-menu-item>

              <el-menu-item index="/messages">
                <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" :max="99">消息</el-badge>
              </el-menu-item>

              <template v-if="userStore.isSeller">
                <el-menu-item index="/shop/manage">店铺管理</el-menu-item>
                <el-menu-item index="/shop/orders">店铺订单</el-menu-item>
                <el-menu-item index="/shop/chat">客户消息</el-menu-item>
              </template>

              <el-sub-menu index="cart-sub">
                <template #title>
                  <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
                    <span>购物车</span>
                  </el-badge>
                </template>
                <div class="cart-dropdown">
                  <div v-if="cartStore.items.length === 0" class="cart-empty">购物车为空</div>
                  <div v-else>
                    <div
                      v-for="item in cartStore.items.slice(0, 5)"
                      :key="item.skuId"
                      class="cart-item"
                    >
                      <span class="cart-item-name">{{ item.productName }} - {{ item.skuName }}</span>
                      <div class="cart-item-qty-wrap">
                        <el-button size="small" text @click.stop="cartStore.updateQuantity(item.skuId, item.quantity - 1)">-</el-button>
                        <span>{{ item.quantity }}</span>
                        <el-button size="small" text @click.stop="cartStore.updateQuantity(item.skuId, item.quantity + 1)">+</el-button>
                      </div>
                      <span class="cart-item-price">&yen;{{ (item.price * item.quantity).toFixed(2) }}</span>
                      <el-button
                        type="danger"
                        size="small"
                        text
                        @click.stop="cartStore.removeItem(item.skuId)"
                      >
                        删除
                      </el-button>
                    </div>
                    <div class="cart-footer">
                      <span class="cart-total">合计: &yen;{{ cartStore.totalPrice.toFixed(2) }}</span>
                      <div>
                        <el-button size="small" text type="danger" @click="cartStore.clear()">清空</el-button>
                        <el-button size="small" type="primary" @click="$router.push('/checkout')">
                          去结算
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </el-sub-menu>

              <el-sub-menu index="user-sub">
                <template #title>
                  <span>{{ userStore.userInfo?.username || '用户' }}</span>
                </template>
                <el-menu-item index="/profile">个人中心</el-menu-item>
                <el-menu-item index="/address">收货地址</el-menu-item>
                <template v-if="userStore.isAdmin">
                  <el-menu-item index="/admin">管理后台</el-menu-item>
                </template>
                <template v-if="!userStore.isSeller">
                  <el-menu-item index="/shop/apply">申请开店</el-menu-item>
                </template>
                <el-menu-item index="logout" @click="handleLogout">退出登录</el-menu-item>
              </el-sub-menu>
            </template>

            <template v-if="!userStore.isLoggedIn">
              <el-menu-item index="/login">登录</el-menu-item>
              <el-menu-item index="/register">注册</el-menu-item>
            </template>
          </el-menu>
        </div>
      </div>
    </el-header>

    <el-main class="app-main">
      <div class="main-content">
        <router-view />
      </div>
    </el-main>

    <el-footer class="app-footer">
      <span>&copy; 2026 TradeStation. All rights reserved.</span>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { getConversations } from '@/api/shop'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchKeyword = ref('')
const unreadTotal = ref(0)
let unreadTimer: ReturnType<typeof setInterval> | null = null

async function fetchUnreadTotal() {
  if (!userStore.token || !userStore.userInfo?.id) return
  try {
    const res: any = await getConversations()
    const list: any[] = res.data || []
    unreadTotal.value = list.reduce((sum: number, c: any) => sum + (c.unreadCount || 0), 0)
  } catch {
    // silent
  }
}

const activeMenu = computed(() => {
  return route.path
})

const isAdminRoute = computed(() => {
  return (route.path as string).startsWith('/admin')
})

const showSearch = computed(() => {
  return ['Home', 'ProductDetail', 'ShopDetail', 'Login', 'Register'].includes(
    route.name as string
  )
})

onMounted(() => {
  if (userStore.token) {
    userStore.fetchProfile()
    fetchUnreadTotal()
    unreadTimer = setInterval(fetchUnreadTotal, 15000)
  }
})

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/', query: { keyword: searchKeyword.value.trim() } })
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  background: #f5f7fa;
}

.el-menu--horizontal {
  border-bottom: none !important;
}
</style>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  height: auto !important;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 60px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  text-decoration: none;
  white-space: nowrap;
  margin-right: 30px;
}

.header-search {
  width: 280px;
  margin-right: 20px;
}

.header-nav {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.nav-menu {
  background: transparent !important;
}

.app-main {
  flex: 1;
  padding: 0;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.app-footer {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

.cart-dropdown {
  min-width: 300px;
  max-height: 320px;
  padding: 10px;
}

.cart-empty {
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.cart-item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item-qty-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.cart-item-qty-wrap span {
  min-width: 20px;
  text-align: center;
}

.cart-item-price {
  color: #f56c6c;
  font-weight: 500;
  min-width: 70px;
  text-align: right;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  margin-top: 4px;
}

.cart-total {
  font-weight: 600;
  color: #f56c6c;
}
</style>

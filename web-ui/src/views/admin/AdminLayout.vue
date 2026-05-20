<template>
  <el-container class="admin-container">
    <el-aside class="admin-sidebar" width="220px">
      <div class="sidebar-header">
        <router-link to="/" class="admin-logo">TradeStation</router-link>
        <span class="admin-badge">管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        router
      >
        <el-menu-item index="/admin">
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/shops">
          <span>店铺审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/payments">
          <span>支付记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="admin-header-right">
          <span class="admin-user">{{ userStore.userInfo?.username }}</span>
          <el-button text @click="$router.push('/')">返回前台</el-button>
          <el-button text type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-container {
  min-height: 100vh;
}

.admin-sidebar {
  background: #304156;
  overflow-y: auto;
}

.sidebar-header {
  padding: 16px;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.admin-logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  text-decoration: none;
  display: block;
}

.admin-badge {
  color: #909399;
  font-size: 12px;
}

.sidebar-menu {
  border-right: none !important;
  background: #304156 !important;
}

.sidebar-menu .el-menu-item {
  color: #bfcbd9;
}

.sidebar-menu .el-menu-item:hover {
  background: #263445;
  color: #fff;
}

.sidebar-menu .el-menu-item.is-active {
  color: #409eff;
  background: #263445;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  height: 56px;
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-user {
  color: #303133;
  font-weight: 500;
}

.admin-main {
  background: #f0f2f5;
  padding: 20px;
}
</style>

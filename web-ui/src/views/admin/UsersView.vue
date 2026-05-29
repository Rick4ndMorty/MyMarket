<template>
  <div class="admin-page">
    <h2>用户管理</h2>

    <el-card>
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索用户名/邮箱/手机" style="width: 240px" clearable @clear="fetchUsers" @keyup.enter="fetchUsers" />
        <el-select v-model="filterRole" placeholder="角色筛选" style="width: 140px" clearable @change="fetchUsers">
          <el-option label="买家" value="BUYER" />
          <el-option label="卖家" value="SELLER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-button type="primary" @click="fetchUsers">搜索</el-button>
      </div>

      <!-- 批量操作栏 -->
      <div class="batch-bar" v-if="selectedIds.length">
        <span>已选 {{ selectedIds.length }} 项</span>
        <el-button size="small" type="success" @click="batchUpdateStatus(1)">批量启用</el-button>
        <el-button size="small" type="danger" @click="batchUpdateStatus(0)">批量禁用</el-button>
        <el-button size="small" @click="selectedIds = []">取消选择</el-button>
      </div>

      <!-- 用户表格 -->
      <el-table :data="users" v-loading="loading" stripe @selection-change="(rows: any[]) => selectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="45" :selectable="(row: any) => row.role !== 'ADMIN'" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'SELLER' ? 'warning' : ''" size="small">
              {{ roleMap[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除此用户？" confirm-button-text="确定" cancel-button-text="取消" @confirm="deleteUser(row)" :disabled="row.role === 'ADMIN'">
              <template #reference>
                <el-button size="small" type="danger" :disabled="row.role === 'ADMIN'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" layout="prev, pager, next, total" :total="total" :page-size="size" v-model:current-page="page" @current-change="fetchUsers" />
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" title="编辑用户" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="买家" value="BUYER" />
            <el-option label="卖家" value="SELLER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminUsers, adminUpdateUser, adminDeleteUser, adminBatchUpdateUserStatus } from '@/api/admin'
import { ElMessage } from 'element-plus'

const roleMap: Record<string, string> = { BUYER: '买家', SELLER: '卖家', ADMIN: '管理员' }

const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const filterRole = ref('')
const selectedIds = ref<number[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<any>({})

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await getAdminUsers({
      page: page.value, size: size.value,
      keyword: keyword.value || undefined,
      filterRole: filterRole.value || undefined
    })
    const data = res.data || {}
    users.value = data.records || []
    total.value = data.total || 0
  } catch { users.value = [] } finally { loading.value = false }
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function saveUser() {
  if (!editingId.value) return
  saving.value = true
  try {
    await adminUpdateUser(editingId.value, {
      username: form.value.username,
      email: form.value.email,
      phone: form.value.phone,
      role: form.value.role,
      status: form.value.status
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchUsers()
  } catch { /* handled */ } finally { saving.value = false }
}

async function deleteUser(row: any) {
  try {
    await adminDeleteUser(row.id)
    ElMessage.success('已删除')
    fetchUsers()
  } catch { /* handled */ }
}

async function batchUpdateStatus(status: number) {
  if (!selectedIds.value.length) return
  try {
    await adminBatchUpdateUserStatus({ ids: selectedIds.value, status })
    ElMessage.success(status === 1 ? '批量启用成功' : '批量禁用成功')
    selectedIds.value = []
    fetchUsers()
  } catch { /* handled */ }
}

onMounted(() => fetchUsers())
</script>

<style scoped>
.admin-page h2 { margin: 0 0 16px; font-size: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 8px 12px; margin-bottom: 12px; background: #ecf5ff; border-radius: 4px; font-size: 13px; color: #606266; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
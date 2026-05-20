<template>
  <div class="msg-center-container">
    <div class="msg-layout">
      <div class="conv-list-panel">
        <h3>我的消息</h3>
        <div class="conv-list" v-loading="loading">
          <div v-if="conversations.length === 0 && !loading" class="empty-tip">
            暂无消息
          </div>
          <div
            v-for="conv in conversations"
            :key="conv.shopId"
            class="conv-item"
            @click="$router.push(`/shop/${conv.shopId}`)"
          >
            <div class="conv-avatar">
              <el-avatar :size="44" :src="conv.shopLogo">
                {{ (conv.shopName || 'S')[0] }}
              </el-avatar>
            </div>
            <div class="conv-info">
              <div class="conv-name">{{ conv.shopName || '店铺' }}</div>
              <div class="conv-last-msg">{{ conv.lastMessage || '' }}</div>
            </div>
            <div class="conv-meta">
              <div class="conv-time">{{ conv.lastMessageTime ? formatTime(conv.lastMessageTime) : '' }}</div>
              <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" type="danger" />
            </div>
          </div>
        </div>
      </div>
      <div class="conv-panel">
        <el-empty description="请选择一个会话" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getConversations } from '@/api/shop'

const conversations = ref<any[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 86400000) return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  if (diff < 172800000) return '昨天'
  return d.toLocaleDateString([], { month: '2-digit', day: '2-digit' })
}

async function loadConversations() {
  loading.value = true
  try {
    const res: any = await getConversations()
    conversations.value = res.data || []
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

function startPolling() {
  loadConversations()
  pollTimer = setInterval(loadConversations, 10000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => startPolling())
onUnmounted(() => stopPolling())
</script>

<style scoped>
.msg-center-container {
  height: calc(100vh - 140px);
}

.msg-layout {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.conv-list-panel {
  width: 360px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.conv-list-panel h3 {
  margin: 0;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 16px;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}

.conv-item:hover {
  background: #f5f7fa;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.conv-last-msg {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.conv-time {
  font-size: 11px;
  color: #c0c4cc;
  white-space: nowrap;
}

.conv-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 30px 0;
  font-size: 14px;
}
</style>

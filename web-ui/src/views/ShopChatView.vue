<template>
  <div class="shop-chat-container">
    <div class="chat-layout">
      <div class="user-list-panel">
        <h3>客户列表</h3>
        <div class="user-list" v-loading="loadingUsers">
          <div v-if="users.length === 0 && !loadingUsers" class="empty-tip">
            暂无客户消息
          </div>
          <div
            v-for="user in users"
            :key="user.userId || user.id"
            :class="['user-item', { active: activeUserId === (user.userId || user.id) }]"
            @click="selectUser(user)"
          >
            <div class="user-avatar">
              <el-avatar :size="36" :src="user.avatar">{{ (user.username || 'U')[0] }}</el-avatar>
            </div>
            <div class="user-info">
              <div class="user-name">{{ user.username || '用户' }}</div>
              <div class="user-last-msg">{{ user.lastMessage || '' }}</div>
            </div>
            <el-badge
              v-if="user.unreadCount > 0"
              :value="user.unreadCount"
              type="danger"
            />
          </div>
        </div>
      </div>

      <div class="chat-panel" v-if="activeUserId">
        <div class="chat-header">
          <span>{{ activeUserName }}</span>
        </div>

        <div class="chat-messages" ref="msgContainerRef">
          <div v-if="messages.length === 0" class="empty-tip">
            暂无消息，发送第一条消息开始对话
          </div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="[
              'message-row',
              (msg.senderType || msg.senderRole) === 'SELLER' ? 'message-right' : 'message-left'
            ]"
          >
            <div
              :class="[
                'message-bubble',
                msg.type === 'AI' ? 'bubble-ai' : 'bubble-human'
              ]"
            >
              <div class="msg-content">{{ msg.content }}</div>
              <div class="msg-time">{{ msg.createTime || '' }}</div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputMsg"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            @keyup.enter.exact="handleSend"
          />
          <el-button
            type="primary"
            :loading="sending"
            :disabled="!inputMsg.trim()"
            @click="handleSend"
            style="margin-top: 8px;"
          >
            发送
          </el-button>
        </div>
      </div>

      <div class="chat-panel" v-else>
        <el-empty description="请选择一个客户开始对话" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getMessages, sendMessage, getMyShop, markAllRead } from '@/api/shop'

const shop = ref<any>({})
const users = ref<any[]>([])
const messages = ref<any[]>([])
const activeUserId = ref<string | number>('')
const activeUserName = ref('')
const inputMsg = ref('')
const sending = ref(false)
const loadingUsers = ref(false)
const msgContainerRef = ref<HTMLElement>()
let pollTimer: ReturnType<typeof setInterval> | null = null

function selectUser(user: any) {
  activeUserId.value = user.userId || user.id
  activeUserName.value = user.username || '用户'
  inputMsg.value = ''
  messages.value = []
  fetchMessages()
  markAllRead(shop.value.id, activeUserId.value)
  user.unreadCount = 0
}

async function fetchMessages() {
  if (!activeUserId.value || !shop.value.id) return
  try {
    const res: any = await getMessages({
      shopId: shop.value.id,
      userId: Number(activeUserId.value),
      page: 1,
      pageSize: 100
    })
    const all: any[] = res.data?.records || res.data?.list || res.data || []
    messages.value = all.reverse()
    await nextTick()
    scrollToBottom()
  } catch {
    // silent poll
  }
}

async function handleSend() {
  const content = inputMsg.value.trim()
  if (!content) return

  sending.value = true
  try {
    await sendMessage({
      shopId: shop.value.id,
      userId: Number(activeUserId.value),
      content,
      senderType: 'SELLER'
    })
    messages.value.push({
      id: Date.now(),
      content,
      senderType: 'SELLER',
      type: 'HUMAN',
      createTime: new Date().toLocaleString()
    })
    inputMsg.value = ''
    await nextTick()
    scrollToBottom()
    fetchMessages()
  } catch {
    // handled by interceptor
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  if (msgContainerRef.value) {
    msgContainerRef.value.scrollTop = msgContainerRef.value.scrollHeight
  }
}

async function loadUsers() {
  if (!shop.value.id) return
  loadingUsers.value = true
  try {
    const res: any = await getMessages({ shopId: shop.value.id, page: 1, pageSize: 200 })
    const all: any[] = res.data?.records || res.data?.list || res.data || []
    const userMap = new Map<string, any>()
    for (const msg of all) {
      const uid = msg.userId
      if (!uid) continue
      if (!userMap.has(uid)) {
        userMap.set(uid, {
          userId: uid,
          username: msg.username || '用户',
          avatar: msg.avatar || '',
          lastMessage: msg.content,
          unreadCount: (msg.senderType === 'BUYER' && msg.isRead === false) ? 1 : 0
        })
      } else {
        const u = userMap.get(uid)!
        if (msg.senderType === 'BUYER' && !msg.isRead) u.unreadCount++
      }
    }
    users.value = Array.from(userMap.values())
  } catch {
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

function startPolling() {
  pollTimer = setInterval(() => {
    if (activeUserId.value) {
      fetchMessages()
    }
    loadUsers()
  }, 10000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  try {
    const res: any = await getMyShop()
    shop.value = res.data || {}
  } catch {
    shop.value = {}
  }
  loadUsers()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.shop-chat-container {
  height: calc(100vh - 140px);
}

.chat-layout {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.user-list-panel {
  width: 280px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.user-list-panel h3 {
  margin: 0;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 16px;
}

.user-list {
  flex: 1;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}

.user-item:hover {
  background: #f5f7fa;
}

.user-item.active {
  background: #ecf5ff;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.user-last-msg {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 16px;
  font-weight: 500;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 30px 0;
  font-size: 14px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-left {
  justify-content: flex-start;
}

.message-right {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.bubble-human {
  background: #fff;
  border: 1px solid #e4e7ed;
}

.message-left .bubble-human,
.message-left .bubble-ai {
  border-radius: 8px 8px 8px 2px;
}

.message-left .bubble-ai {
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  color: #337ecc;
}

.message-right .bubble-human,
.message-right .bubble-ai {
  background: #409eff;
  color: #fff;
  border: 1px solid #409eff;
  border-radius: 8px 8px 2px 8px;
}

.msg-content {
  word-break: break-word;
}

.msg-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}
</style>

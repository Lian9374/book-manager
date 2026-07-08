<template>
  <div>
    <div class="page-header">
      <h3>Notifications</h3>
      <el-button v-if="notifications.length > 0" type="primary" size="small" @click="markAllRead">
        Mark All Read
      </el-button>
    </div>

    <el-card>
      <div v-if="notifications.length === 0" class="empty-state">
        <el-empty description="No notifications" />
      </div>

      <div v-else class="notif-list">
        <div v-for="n in notifications" :key="n.id"
          class="notif-item" :class="{ unread: !n.isRead }"
          @click="markRead(n)">
          <div class="notif-header">
            <el-tag :type="typeTag(n.type)" size="small">{{ n.type }}</el-tag>
            <span class="notif-time">{{ formatTime(n.createdAt) }}</span>
          </div>
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-content">{{ n.content }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../utils/axios'

const notifications = ref([])

onMounted(async () => {
  try {
    const res = await http.get('/notifications')
    notifications.value = res.data || []
  } catch (e) {}
})

function typeTag(type) {
  const map = { OVERDUE: 'danger', FINE: 'warning', RESERVATION_READY: 'success', SYSTEM: 'info' }
  return map[type] || 'info'
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString()
}

async function markRead(n) {
  if (n.isRead) return
  try {
    await http.put(`/notifications/${n.id}/read`)
    n.isRead = true
  } catch (e) {}
}

async function markAllRead() {
  try {
    await http.put('/notifications/read-all')
    notifications.value.forEach(n => n.isRead = true)
    ElMessage.success('All notifications marked as read')
  } catch (e) {}
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.empty-state { padding: 40px 0; }
.notif-item { padding: 14px 16px; border-bottom: 1px solid #ebeef5; cursor: pointer; transition: background .2s; }
.notif-item:hover { background: #f5f7fa; }
.notif-item.unread { background: #ecf5ff; }
.notif-item.unread:hover { background: #d9ecff; }
.notif-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.notif-time { font-size: 12px; color: #909399; }
.notif-title { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.notif-content { font-size: 13px; color: #606266; }
</style>

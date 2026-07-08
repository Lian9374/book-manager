<template>
  <div>
    <div class="page-header">
      <h3>My Borrows</h3>
      <span class="borrow-limit">
        Active: <strong>{{ activeCount }}</strong> / {{ maxAllowed }}
      </span>
    </div>

    <el-alert v-if="activeCount >= maxAllowed" title="You have reached the maximum borrowing limit" type="warning" show-icon style="margin-bottom:16px" />

    <el-card>
      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="book.title" label="Book" min-width="180" />
        <el-table-column prop="book.author" label="Author" width="120" />
        <el-table-column prop="borrowDate" label="Borrowed" width="110" />
        <el-table-column prop="dueDate" label="Due Date" width="110">
          <template #default="{row}">
            <span :class="{ 'overdue-text': isOverdue(row), 'soon-text': isDueSoon(row) && !isOverdue(row) }">
              {{ row.dueDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Remaining" width="100">
          <template #default="{row}">
            <el-tag v-if="row.status === 'RETURNED'" type="success" size="small">Returned</el-tag>
            <el-tag v-else-if="isOverdue(row)" type="danger" size="small">{{ overdueDays(row) }}d overdue</el-tag>
            <el-tag v-else-if="isDueSoon(row)" type="warning" size="small">{{ daysUntilDue(row) }}d left</el-tag>
            <el-tag v-else type="info" size="small">{{ daysUntilDue(row) }}d left</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="renewedCount" label="Renewed" width="80" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="120" fixed="right">
          <template #default="{row}">
            <el-button v-if="canRenew(row)" size="small" type="primary" @click="handleRenew(row)">
              Renew
            </el-button>
            <span v-else-if="row.status === 'RETURNED'" class="text-muted">✓</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyBorrows, renewBook } from '../../api'
import { ElMessage } from 'element-plus'
import http from '../../utils/axios'

const records = ref([])
const loading = ref(false)
const activeCount = ref(0)
const maxAllowed = ref(5)

onMounted(async () => {
  await fetchRecords()
  await fetchActiveCount()
})

async function fetchRecords() {
  loading.value = true
  try {
    const res = await getMyBorrows()
    records.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function fetchActiveCount() {
  try {
    const res = await http.get('/borrows/my/active-count')
    const d = res.data
    activeCount.value = d.activeCount
    maxAllowed.value = d.maxAllowed
  } catch (e) {}
}

function isOverdue(row) {
  if (row.status === 'RETURNED') return false
  return new Date(row.dueDate) < new Date()
}

function isDueSoon(row) {
  if (row.status === 'RETURNED') return false
  const due = new Date(row.dueDate)
  const now = new Date()
  const diff = Math.ceil((due - now) / (1000 * 60 * 60 * 24))
  return diff >= 0 && diff <= 3
}

function daysUntilDue(row) {
  const due = new Date(row.dueDate)
  const now = new Date()
  return Math.ceil((due - now) / (1000 * 60 * 60 * 24))
}

function overdueDays(row) {
  const due = new Date(row.dueDate)
  const now = new Date()
  return Math.floor((now - due) / (1000 * 60 * 60 * 24))
}

function canRenew(row) {
  return (row.status === 'BORROWING' || row.status === 'RENEWED') && !isOverdue(row)
}

function statusType(status) {
  return { BORROWING: 'warning', RETURNED: 'success', OVERDUE: 'danger', RENEWED: 'info', LOST: 'danger' }[status] || 'info'
}

async function handleRenew(row) {
  try {
    await renewBook(row.id)
    ElMessage.success('Renewed successfully')
    await fetchRecords()
  } catch (e) {}
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.borrow-limit { font-size: 14px; color: #606266; }
.overdue-text { color: #F56C6C; font-weight: bold; }
.soon-text { color: #E6A23C; font-weight: bold; }
.text-muted { color: #909399; }
</style>

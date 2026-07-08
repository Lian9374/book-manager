<template>
  <div>
    <h3 style="margin-bottom:16px">Borrow Record Management</h3>
    <el-card>
      <el-form :inline="true" :model="filter" class="search-form">
        <el-form-item>
          <el-select v-model="filter.status" placeholder="All Statuses" clearable style="width:150px" @change="fetchRecords">
            <el-option label="Borrowing" value="BORROWING" />
            <el-option label="Overdue" value="OVERDUE" />
            <el-option label="Renewed" value="RENEWED" />
            <el-option label="Returned" value="RETURNED" />
            <el-option label="Lost" value="LOST" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchRecords">Filter</el-button>
          <el-button @click="resetFilter">Reset</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="records" stripe v-loading="loading" @expand-change="onExpand">
        <el-table-column type="expand">
          <template #default="{row}">
            <div style="padding:8px 20px">
              <p><strong>Borrower:</strong> {{ row.user?.realName }} ({{ row.user?.username }})</p>
              <p><strong>Book:</strong> {{ row.book?.title }} by {{ row.book?.author }}</p>
              <p><strong>ISBN:</strong> {{ row.book?.isbn }}</p>
              <p><strong>Renewed:</strong> {{ row.renewedCount }} time(s)</p>
              <p v-if="row.returnDate"><strong>Returned:</strong> {{ row.returnDate }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="book.title" label="Book" min-width="180" />
        <el-table-column prop="user.realName" label="Borrower" width="120" />
        <el-table-column prop="borrowDate" label="Borrowed" width="110" />
        <el-table-column prop="dueDate" label="Due Date" width="110">
          <template #default="{row}">
            <span :class="{ 'overdue-text': isOverdue(row) }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Overdue" width="80">
          <template #default="{row}">
            <el-tag v-if="isOverdue(row) && row.status !== 'RETURNED'" type="danger" size="small">
              {{ overdueDays(row) }}d
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{row}">
            <el-button v-if="canReturn(row)" size="small" type="success" @click="handleReturn(row)">
              Return
            </el-button>
            <el-button v-if="canReturn(row)" size="small" type="danger" @click="handleReportLost(row)">
              Lost
            </el-button>
            <span v-if="row.status === 'RETURNED' || row.status === 'LOST'" class="text-muted">
              {{ row.status === 'RETURNED' ? '✓ Returned' : '✗ Lost' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAllBorrows, returnBook } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../utils/axios'

const records = ref([])
const loading = ref(false)
const filter = ref({ status: null })

onMounted(fetchRecords)

async function fetchRecords() {
  loading.value = true
  try {
    const params = {}
    if (filter.value.status) params.status = filter.value.status
    const res = await getAllBorrows(params)
    records.value = res.data || []
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filter.value = { status: null }
  fetchRecords()
}

function isOverdue(row) {
  if (!row.dueDate || row.status === 'RETURNED') return false
  return new Date(row.dueDate) < new Date()
}

function overdueDays(row) {
  if (!row.dueDate) return 0
  const due = new Date(row.dueDate)
  const now = new Date()
  return Math.max(0, Math.floor((now - due) / (1000 * 60 * 60 * 24)))
}

function canReturn(row) {
  return ['BORROWING', 'OVERDUE', 'RENEWED'].includes(row.status)
}

function statusType(status) {
  return { BORROWING: 'warning', RETURNED: 'success', OVERDUE: 'danger', RENEWED: 'info', LOST: 'danger' }[status] || 'info'
}

async function handleReturn(row) {
  try {
    await ElMessageBox.confirm(
      `Return "${row.book?.title}" borrowed by ${row.user?.realName}?`,
      'Confirm Return', { type: 'info' }
    )
    await returnBook(row.id)
    ElMessage.success('Book returned successfully')
    await fetchRecords()
  } catch (e) { /* cancelled */ }
}

async function handleReportLost(row) {
  try {
    await ElMessageBox.confirm(
      `Mark "${row.book?.title}" as LOST? This will affect inventory.`,
      'Confirm Lost', { type: 'warning' }
    )
    await http.put(`/borrows/${row.id}/report-lost`)
    ElMessage.success('Marked as lost')
    await fetchRecords()
  } catch (e) { /* cancelled */ }
}

function onExpand(row, expandedRows) {
  // expand logic if needed
}
</script>

<style scoped>
.search-form { margin-bottom: 0; }
.overdue-text { color: #F56C6C; font-weight: bold; }
.text-muted { color: #909399; font-size: 13px; }
</style>

<template>
  <div>
    <h3 style="margin-bottom:16px">My Borrows</h3>
    <el-card>
      <el-table :data="records" stripe v-loading="loading">
        <el-table-column prop="book.title" label="Book" min-width="180" />
        <el-table-column prop="book.author" label="Author" width="120" />
        <el-table-column prop="borrowDate" label="Borrow Date" width="110" />
        <el-table-column prop="dueDate" label="Due Date" width="110" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="120">
          <template #default="{row}">
            <el-button v-if="row.status === 'BORROWING' || row.status === 'RENEWED'"
              size="small" type="primary" @click="handleRenew(row)">Renew</el-button>
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

const records = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyBorrows()
    records.value = res.data
  } finally {
    loading.value = false
  }
})

function statusType(status) {
  return { BORROWING: 'warning', RETURNED: 'success', OVERDUE: 'danger', RENEWED: 'info', LOST: 'danger' }[status] || 'info'
}

async function handleRenew(row) {
  try {
    await renewBook(row.id)
    ElMessage.success('Renewed successfully')
    const res = await getMyBorrows()
    records.value = res.data
  } catch (e) {}
}
</script>

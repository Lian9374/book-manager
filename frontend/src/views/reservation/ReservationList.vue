<template>
  <div>
    <h3 style="margin-bottom:16px">My Reservations</h3>
    <el-card>
      <el-table :data="reservations" stripe v-loading="loading">
        <el-table-column prop="book.title" label="Book" min-width="180" />
        <el-table-column prop="book.author" label="Author" width="120" />
        <el-table-column prop="reserveDate" label="Reserved" width="110" />
        <el-table-column prop="expireDate" label="Expires" width="110" />
        <el-table-column prop="fulfillDeadline" label="Fulfill By" width="110" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'FULFILLED' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100">
          <template #default="{row}">
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleCancel(row)">
              Cancel
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyReservations, cancelReservation } from '../../api'
import { ElMessage } from 'element-plus'

const reservations = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyReservations()
    reservations.value = res.data
  } finally {
    loading.value = false
  }
})

async function handleCancel(row) {
  try {
    await cancelReservation(row.id)
    ElMessage.success('Reservation cancelled')
    const res = await getMyReservations()
    reservations.value = res.data
  } catch (e) {}
}
</script>

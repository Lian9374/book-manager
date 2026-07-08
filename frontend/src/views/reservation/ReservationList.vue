<template>
  <div>
    <div class="page-header">
      <h3>{{ isAdmin ? 'All Reservations' : 'My Reservations' }}</h3>
      <el-select v-if="isAdmin" v-model="filterStatus" placeholder="Filter by status" clearable style="width:160px" @change="fetchData">
        <el-option label="Pending" value="PENDING" />
        <el-option label="Fulfilled" value="FULFILLED" />
        <el-option label="Cancelled" value="CANCELLED" />
        <el-option label="Expired" value="EXPIRED" />
      </el-select>
    </div>

    <el-card>
      <el-table :data="reservations" stripe v-loading="loading">
        <el-table-column prop="book.title" label="Book" min-width="180" />
        <el-table-column prop="book.author" label="Author" width="120" />
        <el-table-column v-if="isAdmin" prop="user.realName" label="User" width="120" />
        <el-table-column prop="reserveDate" label="Reserved" width="110" />
        <el-table-column prop="expireDate" label="Expires" width="110" />
        <el-table-column prop="fulfillDeadline" label="Fulfill By" width="110">
          <template #default="{row}">
            <span v-if="row.fulfillDeadline" :class="{ 'soon-text': isFulfillDeadlineSoon(row) }">
              {{ row.fulfillDeadline }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100" v-if="!isAdmin">
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
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import { getMyReservations, getAllReservations, cancelReservation } from '../../api'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin || authStore.isLibrarian)
const reservations = ref([])
const loading = ref(false)
const filterStatus = ref(null)

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    if (isAdmin.value) {
      const params = {}
      if (filterStatus.value) params.status = filterStatus.value
      const res = await getAllReservations(params)
      reservations.value = res.data || []
    } else {
      const res = await getMyReservations()
      reservations.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

function isFulfillDeadlineSoon(row) {
  if (!row.fulfillDeadline || row.status !== 'PENDING') return false
  const deadline = new Date(row.fulfillDeadline)
  const now = new Date()
  const diff = Math.ceil((deadline - now) / (1000 * 60 * 60 * 24))
  return diff >= 0 && diff <= 1
}

function statusTag(status) {
  return { PENDING: 'warning', FULFILLED: 'success', CANCELLED: 'info', EXPIRED: 'danger' }[status] || 'info'
}

async function handleCancel(row) {
  try {
    await cancelReservation(row.id)
    ElMessage.success('Reservation cancelled')
    await fetchData()
  } catch (e) {}
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.soon-text { color: #E6A23C; font-weight: bold; }
.text-muted { color: #909399; }
</style>

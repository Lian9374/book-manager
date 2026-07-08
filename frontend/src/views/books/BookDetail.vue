<template>
  <div v-loading="loading">
    <el-button @click="$router.back()" style="margin-bottom:16px">← Back</el-button>
    <el-card v-if="book">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ISBN">{{ book.isbn }}</el-descriptions-item>
        <el-descriptions-item label="Title">{{ book.title }}</el-descriptions-item>
        <el-descriptions-item label="Author">{{ book.author }}</el-descriptions-item>
        <el-descriptions-item label="Publisher">{{ book.publisher || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Category">{{ book.category?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Status">
          <el-tag :type="book.status === 'AVAILABLE' ? 'success' : 'warning'">{{ book.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Available">{{ book.availableCopies }}/{{ book.totalCopies }}</el-descriptions-item>
        <el-descriptions-item label="Location">{{ book.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Reservation Queue" :span="1">
          <span v-if="reservationCount > 0">
            <el-tag type="warning">{{ reservationCount }} waiting</el-tag>
            <span v-if="queuePosition > 0" style="margin-left:8px;color:#409EFF">
              You are #{{ queuePosition }} in queue
            </span>
          </span>
          <span v-else class="text-muted">No reservations</span>
        </el-descriptions-item>
        <el-descriptions-item label="Description" :span="2">{{ book.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:20px" v-if="authStore.isReader">
        <el-button type="primary" :disabled="book.availableCopies <= 0" @click="handleBorrow">
          Borrow This Book
        </el-button>
        <el-button v-if="book.availableCopies <= 0" type="success" :disabled="queuePosition > 0" @click="handleReserve">
          Reserve This Book
        </el-button>
        <el-tag v-if="queuePosition > 0" type="warning" style="margin-left:8px">
          Already reserved — position #{{ queuePosition }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getBook, borrowBook, reserveBook } from '../../api'
import { useAuthStore } from '../../store/auth'
import { ElMessage } from 'element-plus'
import http from '../../utils/axios'

const route = useRoute()
const authStore = useAuthStore()
const book = ref(null)
const loading = ref(false)
const reservationCount = ref(0)
const queuePosition = ref(0)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getBook(route.params.id)
    book.value = res.data

    // Fetch reservation info
    const countRes = await http.get(`/reservations/book/${route.params.id}/count`)
    reservationCount.value = countRes.data.pendingCount

    if (authStore.isReader) {
      try {
        const posRes = await http.get('/reservations/queue-position', { params: { bookId: route.params.id } })
        queuePosition.value = posRes.data.position
      } catch (e) { /* not in queue */ }
    }
  } finally {
    loading.value = false
  }
})

async function handleBorrow() {
  try {
    await borrowBook({ bookId: book.value.id })
    ElMessage.success('Borrowed successfully!')
    const res = await getBook(route.params.id)
    book.value = res.data
  } catch (e) {}
}

async function handleReserve() {
  try {
    await reserveBook({ bookId: book.value.id })
    ElMessage.success('Reserved successfully!')
    const res = await getBook(route.params.id)
    book.value = res.data
    // Refresh queue info
    const countRes = await http.get(`/reservations/book/${route.params.id}/count`)
    reservationCount.value = countRes.data.pendingCount
    const posRes = await http.get('/reservations/queue-position', { params: { bookId: route.params.id } })
    queuePosition.value = posRes.data.position
  } catch (e) {}
}
</script>

<style scoped>
.text-muted { color: #909399; }
</style>

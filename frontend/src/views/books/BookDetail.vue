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
        <el-descriptions-item label="Description" :span="2">{{ book.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top:20px" v-if="authStore.isReader">
        <el-button type="primary" :disabled="book.availableCopies <= 0" @click="handleBorrow">
          Borrow This Book
        </el-button>
        <el-button type="success" :disabled="book.availableCopies > 0" @click="handleReserve">
          Reserve This Book
        </el-button>
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

const route = useRoute()
const authStore = useAuthStore()
const book = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getBook(route.params.id)
    book.value = res.data
  } finally {
    loading.value = false
  }
})

async function handleBorrow() {
  try {
    await borrowBook({ bookId: book.value.id })
    ElMessage.success('Borrowed successfully!')
    // Refresh
    const res = await getBook(route.params.id)
    book.value = res.data
  } catch (e) {}
}

async function handleReserve() {
  try {
    await reserveBook({ bookId: book.value.id })
    ElMessage.success('Reserved successfully!')
  } catch (e) {}
}
</script>

<template>
  <div>
    <div class="page-header">
      <h3>Book List</h3>
      <el-button v-if="canManage" type="primary" @click="$router.push('/books/add')">
        <el-icon><Plus /></el-icon> Add Book
      </el-button>
    </div>

    <el-card>
      <el-form :inline="true" :model="search">
        <el-form-item>
          <el-input v-model="search.keyword" placeholder="Title / Author / ISBN" clearable @clear="fetchBooks" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchBooks">Search</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="books" stripe v-loading="loading">
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="author" label="Author" width="120" />
        <el-table-column prop="publisher" label="Publisher" width="120" />
        <el-table-column label="Available" width="100">
          <template #default="{row}">{{ row.availableCopies }}/{{ row.totalCopies }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="110">
          <template #default="{row}">
            <el-tag :type="row.status === 'AVAILABLE' ? 'success' : row.status === 'BORROWED' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200">
          <template #default="{row}">
            <el-button size="small" @click="$router.push(`/books/${row.id}`)">Detail</el-button>
            <el-button v-if="canManage" size="small" type="primary" @click="editBook(row)">Edit</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchBooks"
        style="margin-top:20px; justify-content:center"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../store/auth'
import { getBooks } from '../../api'

const router = useRouter()
const authStore = useAuthStore()
const canManage = computed(() => authStore.isLibrarian || authStore.isAdmin)

const books = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const search = ref({ keyword: '' })

async function fetchBooks() {
  loading.value = true
  try {
    const res = await getBooks({ keyword: search.value.keyword, page: page.value - 1, size: size.value })
    const d = res.data
    books.value = d.content
    total.value = d.totalElements
  } finally {
    loading.value = false
  }
}

function editBook(book) {
  router.push({ path: '/books/add', query: { id: book.id } })
}

onMounted(fetchBooks)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>

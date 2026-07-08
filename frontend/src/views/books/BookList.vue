<template>
  <div>
    <div class="page-header">
      <h3>Book List</h3>
      <div style="display:flex; gap:10px">
        <el-button v-if="isAdmin" @click="$router.push('/categories')">
          <el-icon><FolderOpened /></el-icon> Categories
        </el-button>
        <el-button v-if="canManage" type="primary" @click="$router.push('/books/add')">
          <el-icon><Plus /></el-icon> Add Book
        </el-button>
      </div>
    </div>

    <el-card>
      <el-form :inline="true" :model="search" class="search-form">
        <el-form-item>
          <el-input v-model="search.keyword" placeholder="Title / Author / ISBN" clearable
            style="width:260px" @clear="fetchBooks" @keyup.enter="fetchBooks" />
        </el-form-item>
        <el-form-item>
          <el-tree-select v-model="search.categoryId" :data="categoryOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly clearable placeholder="All Categories" style="width:180px"
            @change="fetchBooks" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="search.status" placeholder="All Statuses" clearable style="width:150px" @change="fetchBooks">
            <el-option label="Available" value="AVAILABLE" />
            <el-option label="Borrowed" value="BORROWED" />
            <el-option label="Reserved" value="RESERVED" />
            <el-option label="Damaged" value="DAMAGED" />
            <el-option label="Lost" value="LOST" />
            <el-option label="Withdrawn" value="WITHDRAWN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchBooks">Search</el-button>
          <el-button @click="resetSearch">Reset</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="books" stripe v-loading="loading">
        <el-table-column prop="isbn" label="ISBN" width="150" />
        <el-table-column prop="title" label="Title" min-width="200" />
        <el-table-column prop="author" label="Author" width="120" />
        <el-table-column prop="publisher" label="Publisher" width="120" />
        <el-table-column label="Category" width="130">
          <template #default="{row}">{{ row.category?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="Available" width="100">
          <template #default="{row}">{{ row.availableCopies }}/{{ row.totalCopies }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="110">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="240">
          <template #default="{row}">
            <el-button size="small" @click="$router.push(`/books/${row.id}`)">Detail</el-button>
            <el-button v-if="canManage" size="small" type="primary" @click="editBook(row)">Edit</el-button>
            <el-button v-if="canManage" size="small" type="danger" @click="handleDelete(row)">Delete</el-button>
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
import { getBooks, getCategories, deleteBook } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const canManage = computed(() => authStore.isLibrarian || authStore.isAdmin)
const isAdmin = computed(() => authStore.isAdmin)

const books = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const categoryOptions = ref([])
const search = ref({ keyword: '', categoryId: null, status: null })

async function fetchBooks() {
  loading.value = true
  try {
    const res = await getBooks({
      keyword: search.value.keyword || undefined,
      categoryId: search.value.categoryId || undefined,
      status: search.value.status || undefined,
      page: page.value - 1,
      size: size.value
    })
    const d = res.data
    books.value = d.content
    total.value = d.totalElements
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getCategories()
    categoryOptions.value = res.data || []
  } catch (e) {}
}

function resetSearch() {
  search.value = { keyword: '', categoryId: null, status: null }
  page.value = 1
  fetchBooks()
}

function statusTagType(status) {
  const map = { AVAILABLE: 'success', BORROWED: 'warning', RESERVED: 'info', DAMAGED: 'danger', LOST: 'danger', WITHDRAWN: 'info' }
  return map[status] || 'info'
}

function editBook(book) {
  router.push({ path: '/books/add', query: { id: book.id } })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`Delete "${row.title}"? This cannot be undone.`, 'Confirm Delete', { type: 'warning' })
    await deleteBook(row.id)
    ElMessage.success('Book deleted')
    await fetchBooks()
  } catch (e) { /* cancelled */ }
}

onMounted(() => {
  fetchBooks()
  loadCategories()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.search-form { margin-bottom: 0; }
</style>

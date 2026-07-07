<template>
  <div>
    <div class="page-header">
      <h3>{{ isEdit ? 'Edit Book' : 'Add New Book' }}</h3>
    </div>
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px" style="max-width:700px">
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" />
        </el-form-item>
        <el-form-item label="Title" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="Author" prop="author">
          <el-input v-model="form.author" />
        </el-form-item>
        <el-form-item label="Publisher">
          <el-input v-model="form.publisher" />
        </el-form-item>
        <el-form-item label="Publish Date">
          <el-date-picker v-model="form.publishDate" type="date" placeholder="Pick a date" />
        </el-form-item>
        <el-form-item label="Category">
          <el-tree-select v-model="form.categoryId" :data="categories"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly clearable placeholder="Select category" />
        </el-form-item>
        <el-form-item label="Total Copies" prop="totalCopies">
          <el-input-number v-model="form.totalCopies" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="Location">
          <el-input v-model="form.location" placeholder="Shelf location" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">Save</el-button>
          <el-button @click="$router.back()">Cancel</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addBook, updateBook, getBook, getCategories } from '../../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const categories = ref([])
const isEdit = computed(() => !!route.query.id)

const form = ref({
  isbn: '', title: '', author: '', publisher: '', publishDate: null,
  categoryId: null, totalCopies: 1, location: '', description: ''
})

const rules = {
  isbn: [{ required: true, message: 'ISBN required' }],
  title: [{ required: true, message: 'Title required' }],
  author: [{ required: true, message: 'Author required' }],
  totalCopies: [{ required: true, message: 'Total copies required' }]
}

onMounted(async () => {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch (e) {}

  if (isEdit.value) {
    try {
      const res = await getBook(route.query.id)
      const b = res.data
      Object.assign(form.value, {
        isbn: b.isbn, title: b.title, author: b.author, publisher: b.publisher || '',
        publishDate: b.publishDate, categoryId: b.category?.id, totalCopies: b.totalCopies,
        location: b.location || '', description: b.description || ''
      })
    } catch (e) {
      ElMessage.error('Failed to load book')
    }
  }
})

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form.value }
    if (isEdit.value) {
      await updateBook(route.query.id, payload)
      ElMessage.success('Book updated')
    } else {
      await addBook(payload)
      ElMessage.success('Book added')
    }
    router.push('/books')
  } catch (e) {
    ElMessage.error('Save failed')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
</style>

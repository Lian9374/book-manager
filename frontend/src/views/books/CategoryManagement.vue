<template>
  <div>
    <div class="page-header">
      <h3>Category Management</h3>
      <el-button type="primary" @click="showAddDialog(null)">
        <el-icon><Plus /></el-icon> Add Root Category
      </el-button>
    </div>

    <el-card>
      <el-table :data="categories" stripe row-key="id" v-loading="loading"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }" default-expand-all>
        <el-table-column prop="name" label="Name" min-width="200" />
        <el-table-column prop="sortOrder" label="Sort Order" width="100" />
        <el-table-column label="Actions" width="220">
          <template #default="{row}">
            <el-button size="small" @click="showAddDialog(row)">Add Sub</el-button>
            <el-button size="small" type="primary" @click="showEditDialog(row)">Edit</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit Category' : 'Add Category'" width="420px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="Name" prop="name">
          <el-input v-model="form.name" placeholder="Category name" />
        </el-form-item>
        <el-form-item label="Parent">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="Sort Order">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const parentCategory = ref(null)
const formRef = ref(null)

const form = ref({ name: '', parentId: null, sortOrder: 0 })
const rules = { name: [{ required: true, message: 'Name required', trigger: 'blur' }] }

const parentName = computed(() => parentCategory.value?.name || '(Root)')

onMounted(fetchCategories)

async function fetchCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function showAddDialog(parent) {
  editingId.value = null
  parentCategory.value = parent
  form.value = { name: '', parentId: parent?.id || null, sortOrder: 0 }
  dialogVisible.value = true
}

function showEditDialog(row) {
  editingId.value = row.id
  parentCategory.value = null // not changing parent on edit
  form.value = { name: row.name, parentId: null, sortOrder: row.sortOrder }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { name: form.value.name, sortOrder: form.value.sortOrder })
      ElMessage.success('Category updated')
    } else {
      await createCategory({ name: form.value.name, parentId: form.value.parentId, sortOrder: form.value.sortOrder })
      ElMessage.success('Category created')
    }
    dialogVisible.value = false
    await fetchCategories()
  } catch (e) {} finally { saving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`Delete category "${row.name}"?`, 'Confirm', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('Category deleted')
    await fetchCategories()
  } catch (e) { /* cancelled or error */ }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>

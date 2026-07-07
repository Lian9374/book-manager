<template>
  <div>
    <h3 style="margin-bottom:16px">User Management</h3>
    <el-card>
      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="Username" width="120" />
        <el-table-column prop="realName" label="Full Name" width="120" />
        <el-table-column prop="email" label="Email" width="180" />
        <el-table-column label="Roles" width="200">
          <template #default="{row}">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin-right:4px">
              {{ r.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="180">
          <template #default="{row}">
            <el-button size="small" @click="handleToggle(row)">{{ row.status === 'ACTIVE' ? 'Disable' : 'Enable' }}</el-button>
            <el-button size="small" type="primary" @click="showRoleDialog(row)">Roles</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="roleDialogVisible" title="Edit Roles" width="400px">
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox label="ADMIN" style="margin:10px" />
        <el-checkbox label="LIBRARIAN" style="margin:10px" />
        <el-checkbox label="READER" style="margin:10px" />
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveRoles">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, toggleUserStatus, updateUserRoles } from '../../api'
import { ElMessage } from 'element-plus'

const users = ref([])
const loading = ref(false)
const roleDialogVisible = ref(false)
const selectedRoles = ref([])
const editingUserId = ref(null)

onMounted(async () => {
  await fetchUsers()
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers()
    users.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleToggle(row) {
  try {
    await toggleUserStatus(row.id)
    ElMessage.success('Status toggled')
    await fetchUsers()
  } catch (e) {}
}

function showRoleDialog(row) {
  editingUserId.value = row.id
  selectedRoles.value = row.roles.map(r => r.roleName)
  roleDialogVisible.value = true
}

async function saveRoles() {
  try {
    await updateUserRoles(editingUserId.value, selectedRoles.value)
    ElMessage.success('Roles updated')
    roleDialogVisible.value = false
    await fetchUsers()
  } catch (e) {}
}
</script>

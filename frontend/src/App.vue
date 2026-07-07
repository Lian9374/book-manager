<template>
  <div id="app-container">
    <!-- Sidebar for authenticated users -->
    <el-container v-if="authStore.isLoggedIn">
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <el-icon :size="24"><Reading /></el-icon>
          <span>Book Manager</span>
        </div>
        <el-menu
          :default-active="currentRoute"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>Dashboard</span>
          </el-menu-item>
          <el-menu-item index="/books">
            <el-icon><Notebook /></el-icon>
            <span>Books</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isLibrarian || authStore.isAdmin" index="/borrows">
            <el-icon><List /></el-icon>
            <span>Borrow Records</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isReader" index="/my-borrows">
            <el-icon><Tickets /></el-icon>
            <span>My Borrows</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isReader" index="/reservations">
            <el-icon><Calendar /></el-icon>
            <span>Reservations</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isReader" index="/fines">
            <el-icon><Money /></el-icon>
            <span>Fines</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isLibrarian || authStore.isAdmin" index="/statistics">
            <el-icon><DataAnalysis /></el-icon>
            <span>Statistics</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isAdmin" index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>User Management</span>
          </el-menu-item>
          <el-menu-item v-if="authStore.isAdmin" index="/config">
            <el-icon><Setting /></el-icon>
            <span>System Config</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header">
          <div class="header-right">
            <el-dropdown>
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ authStore.userInfo?.realName || authStore.userInfo?.username }}
                <el-tag size="small" v-for="role in authStore.roles" :key="role" style="margin-left:4px">
                  {{ role }}
                </el-tag>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="showPasswordDialog = true">
                    <el-icon><Lock /></el-icon> Change Password
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon> Logout
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- Full page for guest routes -->
    <div v-else class="guest-layout">
      <router-view />
    </div>

    <!-- Change Password Dialog -->
    <el-dialog v-model="showPasswordDialog" title="Change Password" width="420px" :close-on-click-modal="false">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="140px">
        <el-form-item label="Current Password" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="New Password" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">Cancel</el-button>
        <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">Confirm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './store/auth'
import { changePassword } from './api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentRoute = computed(() => route.path)

// Change Password
const showPasswordDialog = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('Passwords do not match'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: 'Current password is required', trigger: 'blur' }],
  newPassword: [
    { required: true, message: 'New password is required', trigger: 'blur' },
    { min: 6, message: 'At least 6 characters', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm new password', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('Password changed successfully')
    showPasswordDialog.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    // Error handled by interceptor
  } finally {
    changingPassword.value = false
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }

#app-container {
  height: 100vh;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.sidebar {
  background-color: #304156;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  gap: 10px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
}

.guest-layout {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.el-main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}
</style>

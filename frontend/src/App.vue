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
          <el-menu-item index="/reservations" v-if="authStore.isReader">
            <el-icon><Calendar /></el-icon>
            <span>Reservations</span>
          </el-menu-item>
          <el-menu-item index="/fines" v-if="authStore.isReader">
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
                  <el-dropdown-item @click="handleLogout">Logout</el-dropdown-item>
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
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const currentRoute = computed(() => route.path)

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

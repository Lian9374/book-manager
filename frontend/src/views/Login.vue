<template>
  <el-card class="login-card" shadow="always">
    <h2 class="login-title">Book Manager Login</h2>
    <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
      <el-form-item label="Username" prop="username">
        <el-input v-model="form.username" placeholder="Enter username" prefix-icon="User" />
      </el-form-item>
      <el-form-item label="Password" prop="password">
        <el-input v-model="form.password" type="password" placeholder="Enter password" show-password prefix-icon="Lock" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">
          Login
        </el-button>
      </el-form-item>
      <div style="text-align:center">
        <el-button link type="primary" @click="$router.push('/register')">
          Don't have an account? Register
        </el-button>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: 'Username is required', trigger: 'blur' }],
  password: [{ required: true, message: 'Password is required', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await authStore.login(form.username, form.password)
    ElMessage.success(`Welcome, ${data.realName}!`)
    router.push('/')
  } catch (e) {
    ElMessage.error('Login failed: ' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-card {
  width: 420px;
  padding: 20px;
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #303133;
}
</style>

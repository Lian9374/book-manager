<template>
  <el-card class="register-card" shadow="always">
    <h2 class="title">Reader Registration</h2>
    <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
      <el-form-item label="Username" prop="username">
        <el-input v-model="form.username" placeholder="3-50 characters" />
      </el-form-item>
      <el-form-item label="Password" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="At least 6 characters" />
      </el-form-item>
      <el-form-item label="Full Name" prop="realName">
        <el-input v-model="form.realName" placeholder="Your real name" />
      </el-form-item>
      <el-form-item label="Email" prop="email">
        <el-input v-model="form.email" placeholder="Optional" />
      </el-form-item>
      <el-form-item label="Phone" prop="phone">
        <el-input v-model="form.phone" placeholder="Optional" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleRegister" style="width:100%">
          Register
        </el-button>
      </el-form-item>
      <div style="text-align:center">
        <el-button link type="primary" @click="$router.push('/login')">
          Already have an account? Login
        </el-button>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '', password: '', realName: '', email: '', phone: ''
})
const rules = {
  username: [{ required: true, message: 'Username is required', trigger: 'blur' }, { min: 3, max: 50 }],
  password: [{ required: true, message: 'Password is required', trigger: 'blur' }, { min: 6 }],
  realName: [{ required: true, message: 'Full name is required', trigger: 'blur' }]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register(form)
    ElMessage.success('Registration successful! Please login.')
    router.push('/login')
  } catch (e) {
    ElMessage.error('Registration failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-card { width: 460px; padding: 20px; }
.title { text-align: center; margin-bottom: 20px; color: #303133; }
</style>

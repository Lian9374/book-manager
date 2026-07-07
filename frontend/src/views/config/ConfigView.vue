<template>
  <div>
    <h3 style="margin-bottom:16px">System Configuration</h3>
    <el-card>
      <el-form label-width="220px" style="max-width:600px">
        <el-form-item v-for="(val, key) in configs" :key="key" :label="key">
          <el-input v-model="configs[key]" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveConfigs">Save</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getConfigs, updateConfigs } from '../../api'
import { ElMessage } from 'element-plus'

const configs = ref({})
const saving = ref(false)

onMounted(async () => {
  try {
    const res = await getConfigs()
    configs.value = res.data
  } catch (e) {}
})

async function saveConfigs() {
  saving.value = true
  try {
    await updateConfigs(configs.value)
    ElMessage.success('Configuration saved')
  } finally {
    saving.value = false
  }
}
</script>

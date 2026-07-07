<template>
  <div>
    <h3 style="margin-bottom:16px">My Fines</h3>
    <el-alert v-if="unpaidAmount > 0" :title="'You have unpaid fines: ¥' + unpaidAmount" type="error" show-icon style="margin-bottom:16px" />
    <el-card>
      <el-table :data="fines" stripe v-loading="loading">
        <el-table-column prop="reason" label="Reason" min-width="200" />
        <el-table-column prop="amount" label="Amount (¥)" width="120" />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 'UNPAID' ? 'danger' : row.status === 'PAID' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paidAt" label="Paid At" width="160" />
        <el-table-column prop="createdAt" label="Created" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyFines, getUnpaidAmount } from '../../api'

const fines = ref([])
const unpaidAmount = ref(0)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const [finesRes, amountRes] = await Promise.all([getMyFines(), getUnpaidAmount()])
    fines.value = finesRes.data
    unpaidAmount.value = amountRes.data.unpaidAmount
  } finally {
    loading.value = false
  }
})
</script>

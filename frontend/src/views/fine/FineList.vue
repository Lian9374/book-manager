<template>
  <div>
    <div class="page-header">
      <h3>{{ isAdmin ? 'Fine Management' : 'My Fines' }}</h3>
    </div>

    <!-- Admin stats row -->
    <el-row :gutter="20" v-if="isAdmin" style="margin-bottom:20px">
      <el-col :span="6" v-for="stat in statsCards" :key="stat.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" :style="{color:stat.color}">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Unpaid alert for readers -->
    <el-alert v-if="!isAdmin && unpaidAmount > 0"
      :title="`You have unpaid fines: ¥${unpaidAmount}. Please settle before borrowing.`"
      type="error" show-icon style="margin-bottom:16px" closable />

    <el-card>
      <!-- Admin filters -->
      <el-form :inline="true" v-if="isAdmin" class="search-form">
        <el-form-item>
          <el-select v-model="filter.status" placeholder="All Statuses" clearable style="width:140px" @change="fetchData">
            <el-option label="Unpaid" value="UNPAID" />
            <el-option label="Paid" value="PAID" />
            <el-option label="Waived" value="WAIVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">Filter</el-button>
          <el-button @click="resetFilter">Reset</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="fines" stripe v-loading="loading">
        <el-table-column v-if="isAdmin" prop="user.realName" label="User" width="120" />
        <el-table-column prop="borrowRecord.book.title" label="Book" min-width="160" />
        <el-table-column prop="amount" label="Amount (¥)" width="110">
          <template #default="{row}">
            <span :class="{ 'amount-unpaid': row.status === 'UNPAID' }">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="Reason" min-width="160" />
        <el-table-column prop="status" label="Status" width="90">
          <template #default="{row}">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paidAt" label="Paid At" width="160" />
        <el-table-column prop="createdAt" label="Created" width="160" />
        <el-table-column v-if="isAdmin" label="Actions" width="160" fixed="right">
          <template #default="{row}">
            <el-button v-if="row.status === 'UNPAID'" size="small" type="success" @click="handlePay(row)">
              Pay
            </el-button>
            <el-button v-if="row.status === 'UNPAID'" size="small" type="warning" @click="handleWaive(row)">
              Waive
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../store/auth'
import { getMyFines, getUnpaidAmount } from '../../api'
import { ElMessage } from 'element-plus'
import http from '../../utils/axios'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin || authStore.isLibrarian)

const fines = ref([])
const unpaidAmount = ref(0)
const loading = ref(false)
const filter = ref({ status: null })
const statsCards = ref([
  { label: 'Unpaid', value: 0, color: '#F56C6C' },
  { label: 'Paid', value: 0, color: '#67C23A' },
  { label: 'Waived', value: 0, color: '#909399' },
  { label: 'Total Unpaid (¥)', value: '0', color: '#E6A23C' }
])

onMounted(async () => {
  await fetchData()
  if (isAdmin.value) await fetchStats()
})

async function fetchData() {
  loading.value = true
  try {
    if (isAdmin.value) {
      const params = {}
      if (filter.value.status) params.status = filter.value.status
      const res = await http.get('/fines', { params })
      fines.value = res.data || []
    } else {
      const [finesRes, amountRes] = await Promise.all([getMyFines(), getUnpaidAmount()])
      fines.value = finesRes.data || []
      unpaidAmount.value = amountRes.data.unpaidAmount || 0
    }
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await http.get('/fines/stats')
    const d = res.data
    statsCards.value = [
      { label: 'Unpaid', value: d.unpaidCount, color: '#F56C6C' },
      { label: 'Paid', value: d.paidCount, color: '#67C23A' },
      { label: 'Waived', value: d.waivedCount, color: '#909399' },
      { label: 'Total Unpaid (¥)', value: '¥' + d.totalUnpaid, color: '#E6A23C' }
    ]
  } catch (e) {}
}

function resetFilter() {
  filter.value = { status: null }
  fetchData()
}

function statusTag(status) {
  return { UNPAID: 'danger', PAID: 'success', WAIVED: 'info' }[status] || 'info'
}

async function handlePay(row) {
  try {
    await http.put(`/fines/${row.id}/pay`)
    ElMessage.success('Fine marked as paid')
    await fetchData()
    await fetchStats()
  } catch (e) {}
}

async function handleWaive(row) {
  try {
    await http.put(`/fines/${row.id}/waive`)
    ElMessage.success('Fine waived')
    await fetchData()
    await fetchStats()
  } catch (e) {}
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.search-form { margin-bottom: 0; }
.stat-card { text-align: center; padding: 10px; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
.amount-unpaid { color: #F56C6C; font-weight: bold; }
</style>

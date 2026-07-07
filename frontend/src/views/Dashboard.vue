<template>
  <div>
    <h3 style="margin-bottom:20px">Dashboard</h3>
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px" v-if="isLibrarianOrAdmin">
      <el-col :span="12">
        <el-card>
          <template #header>Borrow Trend (Last 7 Days)</template>
          <v-chart :option="trendOption" style="height:300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>Popular Books</template>
          <v-chart :option="popularOption" style="height:300px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import { getDashboard, getBorrowTrend, getPopularBooks } from '../api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const authStore = useAuthStore()
const isLibrarianOrAdmin = computed(() => authStore.isLibrarian || authStore.isAdmin)

const stats = ref([
  { label: 'Total Books', value: 0 },
  { label: 'Available', value: 0 },
  { label: 'Active Borrows', value: 0 },
  { label: 'Total Users', value: 0 }
])

const trendOption = ref({})
const popularOption = ref({})

onMounted(async () => {
  try {
    const dash = await getDashboard()
    const d = dash.data
    stats.value = [
      { label: 'Total Books', value: d.totalBooks },
      { label: 'Available', value: d.availableBooks },
      { label: 'Active Borrows', value: d.activeBorrows },
      { label: 'Total Users', value: d.totalUsers }
    ]
  } catch (e) { /* dashboard not critical */ }

  if (isLibrarianOrAdmin.value) {
    try {
      const trend = await getBorrowTrend(7)
      const days = Object.keys(trend.data)
      const counts = Object.values(trend.data)
      trendOption.value = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: days },
        yAxis: { type: 'value' },
        series: [{ data: counts, type: 'bar', itemStyle: { color: '#409EFF' } }]
      }
    } catch (e) {}

    try {
      const popular = await getPopularBooks(5)
      popularOption.value = {
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          data: popular.data.map(b => ({ name: b.title, value: b.borrowCount }))
        }]
      }
    } catch (e) {}
  }
})
</script>

<style scoped>
.stat-card { text-align: center; padding: 10px; }
.stat-value { font-size: 32px; font-weight: bold; color: #409EFF; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>

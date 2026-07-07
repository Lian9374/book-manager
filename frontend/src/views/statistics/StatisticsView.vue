<template>
  <div>
    <h3 style="margin-bottom:20px">Statistics</h3>
    <el-row :gutter="20">
      <el-col :span="8" v-for="stat in summary" :key="stat.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>Borrow Trend (30 Days)</template>
          <v-chart :option="trendOption" style="height:350px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>Top 10 Popular Books</template>
          <v-chart :option="popularOption" style="height:350px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBorrowTrend, getPopularBooks, getOverdueReport } from '../../api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const summary = ref([
  { label: 'Overdue Records', value: 0 },
  { label: 'Overdue Rate', value: '0%' },
  { label: 'Unpaid Fines', value: '¥0' }
])

const trendOption = ref({})
const popularOption = ref({})

onMounted(async () => {
  try {
    const overdue = await getOverdueReport()
    const d = overdue.data
    summary.value = [
      { label: 'Overdue Records', value: d.overdueCount },
      { label: 'Overdue Rate', value: d.overdueRate + '%' },
      { label: 'Unpaid Fines', value: '¥' + d.totalUnpaidFines }
    ]
  } catch (e) {}

  try {
    const trend = await getBorrowTrend(30)
    const days = Object.keys(trend.data)
    trendOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: days, axisLabel: { rotate: 45 } },
      yAxis: { type: 'value' },
      series: [{ data: Object.values(trend.data), type: 'bar', itemStyle: { color: '#409EFF' } }]
    }
  } catch (e) {}

  try {
    const popular = await getPopularBooks(10)
    popularOption.value = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: popular.data.map(b => b.title).reverse(),
        axisLabel: { width: 100, overflow: 'truncate' }
      },
      series: [{
        data: popular.data.map(b => b.borrowCount).reverse(),
        type: 'bar',
        itemStyle: { color: '#67C23A' }
      }]
    }
  } catch (e) {}
})
</script>

<style scoped>
.stat-card { text-align: center; padding: 10px; }
.stat-value { font-size: 32px; font-weight: bold; color: #E6A23C; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>

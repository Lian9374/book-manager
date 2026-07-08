<template>
  <div>
    <h3 style="margin-bottom:20px">Statistics Dashboard</h3>

    <!-- Summary cards -->
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in summaryCards" :key="card.label">
        <el-card shadow="hover">
          <div class="summary-card">
            <div class="summary-value" :style="{color:card.color}">{{ card.value }}</div>
            <div class="summary-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 1: Trend + Monthly -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>Borrow / Return Trend (30 Days)</span>
          </template>
          <v-chart :option="trendOption" style="height:340px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>Monthly Borrows ({{ currentYear }})</template>
          <v-chart :option="monthlyOption" style="height:340px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 2: Category + Book Status -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header>Books by Category</template>
          <v-chart :option="categoryOption" style="height:320px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>Book Status Distribution</template>
          <v-chart :option="statusOption" style="height:320px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 3: Popular Books + Top Readers -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header>Top 10 Popular Books</template>
          <v-chart :option="popularOption" style="height:320px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>Top 10 Active Readers</template>
          <v-chart :option="readersOption" style="height:320px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 4: Overdue report -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header>Overdue Report</template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="Overdue Records">{{ overdueData.overdueCount }}</el-descriptions-item>
            <el-descriptions-item label="Overdue Rate">{{ overdueData.overdueRate }}%</el-descriptions-item>
            <el-descriptions-item label="Unpaid Fines">¥{{ overdueData.totalUnpaidFines }}</el-descriptions-item>
            <el-descriptions-item label="Collected Fines">¥{{ overdueData.totalCollectedFines }}</el-descriptions-item>
            <el-descriptions-item label="Total Borrows">{{ overdueData.totalBorrows }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>Fine Overview</template>
          <v-chart :option="fineOption" style="height:180px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../utils/axios'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, PieChart, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const currentYear = new Date().getFullYear()
const summaryCards = ref([])
const trendOption = ref({})
const monthlyOption = ref({})
const categoryOption = ref({})
const statusOption = ref({})
const popularOption = ref({})
const readersOption = ref({})
const fineOption = ref({})
const overdueData = ref({ overdueCount: 0, overdueRate: 0, totalUnpaidFines: 0, totalCollectedFines: 0, totalBorrows: 0 })

onMounted(async () => {
  await Promise.all([
    loadDashboard(),
    loadTrend(),
    loadMonthly(),
    loadCategories(),
    loadStatus(),
    loadPopular(),
    loadReaders(),
    loadOverdue()
  ])
})

async function loadDashboard() {
  try {
    const res = await http.get('/statistics/dashboard')
    const d = res.data
    summaryCards.value = [
      { label: 'Total Books', value: d.totalBooks, color: '#409EFF' },
      { label: 'Available', value: d.availableBooks, color: '#67C23A' },
      { label: 'Active Borrows', value: d.activeBorrows, color: '#E6A23C' },
      { label: 'Overdue', value: d.overdueCount, color: '#F56C6C' },
      { label: 'Reservations', value: d.pendingReservations, color: '#909399' },
      { label: 'Total Users', value: d.totalUsers, color: '#722ED1' }
    ]
  } catch (e) {}
}

async function loadTrend() {
  try {
    const borrowRes = await http.get('/statistics/borrow-trend', { params: { days: 30 } })
    const returnRes = await http.get('/statistics/return-trend', {
      params: {
        start: new Date(Date.now() - 30*86400000).toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
      }
    }).catch(() => ({ data: {} }))

    const days = Object.keys(borrowRes.data)
    trendOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['Borrows', 'Returns'] },
      xAxis: { type: 'category', data: days, axisLabel: { rotate: 45, fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [
        { name: 'Borrows', data: Object.values(borrowRes.data), type: 'line', smooth: true, itemStyle: { color: '#409EFF' } },
        { name: 'Returns', data: Object.values(returnRes.data || {}), type: 'line', smooth: true, itemStyle: { color: '#67C23A' } }
      ]
    }
  } catch (e) {}
}

async function loadMonthly() {
  try {
    const res = await http.get('/statistics/monthly-comparison')
    const months = Object.keys(res.data)
    monthlyOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: months },
      yAxis: { type: 'value' },
      series: [{ data: Object.values(res.data), type: 'bar', itemStyle: { color: '#409EFF', borderRadius: 4 } }]
    }
  } catch (e) {}
}

async function loadCategories() {
  try {
    const res = await http.get('/statistics/category-distribution')
    categoryOption.value = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: res.data.map(d => ({ name: d.name, value: d.value }))
      }]
    }
  } catch (e) {}
}

async function loadStatus() {
  try {
    const res = await http.get('/statistics/book-status-distribution')
    const data = Object.entries(res.data || {})
    const colorMap = { AVAILABLE: '#67C23A', BORROWED: '#E6A23C', RESERVED: '#409EFF', DAMAGED: '#F56C6C', LOST: '#303133', WITHDRAWN: '#909399' }
    statusOption.value = {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '65%',
        data: data.map(([k, v]) => ({ name: k, value: v, itemStyle: { color: colorMap[k] || '#909399' } }))
      }]
    }
  } catch (e) {}
}

async function loadPopular() {
  try {
    const res = await http.get('/statistics/popular-books', { params: { limit: 10 } })
    const items = res.data || []
    popularOption.value = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: items.map(b => b.title).reverse(), axisLabel: { width: 120, overflow: 'truncate' } },
      series: [{ data: items.map(b => b.borrowCount).reverse(), type: 'bar', itemStyle: { color: '#409EFF', borderRadius: 3 } }]
    }
  } catch (e) {}
}

async function loadReaders() {
  try {
    const res = await http.get('/statistics/top-readers', { params: { limit: 10 } })
    const items = res.data || []
    readersOption.value = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: items.map(r => r.realName || r.username).reverse() },
      series: [{ data: items.map(r => r.borrowCount).reverse(), type: 'bar', itemStyle: { color: '#67C23A', borderRadius: 3 } }]
    }
  } catch (e) {}
}

async function loadOverdue() {
  try {
    const res = await http.get('/statistics/overdue-report')
    overdueData.value = res.data
    fineOption.value = {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['45%', '75%'],
        data: [
          { name: 'Unpaid', value: res.data.totalUnpaidFines || 0, itemStyle: { color: '#F56C6C' } },
          { name: 'Collected', value: res.data.totalCollectedFines || 0, itemStyle: { color: '#67C23A' } }
        ]
      }]
    }
  } catch (e) {}
}
</script>

<style scoped>
.summary-card { text-align: center; padding: 8px 0; }
.summary-value { font-size: 26px; font-weight: bold; }
.summary-label { font-size: 12px; color: #909399; margin-top: 6px; }
</style>

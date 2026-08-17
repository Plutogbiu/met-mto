<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  CircleCheck,
  CircleClose,
  Clock,
  DataAnalysis,
  Document,
  Loading,
  Refresh,
} from '@element-plus/icons-vue'
import { getDashboardOverview } from '../../api/dashboard'

const router = useRouter()
const loading = ref(false)
const overview = ref(createEmptyOverview())
const statusPieRef = ref(null)
let statusPieChart = null
const filters = reactive({
  dateRange: currentMonthRange(),
  type: '',
})

const typeOptions = [
  { label: '全部工单', value: '' },
  { label: '现场工单', value: 'onsite' },
  { label: '日常巡检', value: 'inspection' },
]

const indicatorCards = computed(() => [
  { label: '工单总数', value: overview.value.totalCount, status: '', icon: Document, tone: 'blue' },
  { label: '待处理', value: overview.value.pendingCount, status: 'pending', icon: Clock, tone: 'orange' },
  { label: '处理中', value: overview.value.processingCount, status: 'processing', icon: Loading, tone: 'cyan' },
  { label: '已完成', value: overview.value.completedCount, status: 'completed', icon: CircleCheck, tone: 'green' },
  { label: '已作废', value: overview.value.closedCount, status: 'closed', icon: CircleClose, tone: 'gray' },
])

const statusPieData = computed(() => [
  { label: '待处理', status: 'pending', count: overview.value.pendingCount, color: '#e88b2c' },
  { label: '处理中', status: 'processing', count: overview.value.processingCount, color: '#159a9c' },
  { label: '已完成', status: 'completed', count: overview.value.completedCount, color: '#30a46c' },
  { label: '已作废', status: 'closed', count: overview.value.closedCount, color: '#98a2b3' },
])
const statusPieTotal = computed(() => statusPieData.value.reduce((sum, item) => sum + item.count, 0))

const maxCustomerCount = computed(() => Math.max(...overview.value.customerRanks.map((item) => item.workOrderCount), 0))
const maxTrendCount = computed(() => Math.max(
  ...overview.value.trends.flatMap((item) => [item.createdCount, item.completedCount]),
  1,
))
const trendChartWidth = computed(() => Math.max(720, overview.value.trends.length * 56))
const trendChartHeight = 240
const trendPlot = { left: 38, right: 16, top: 16, bottom: 42 }
const trendGridLines = computed(() => [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
  ratio,
  value: Math.round(maxTrendCount.value * (1 - ratio)),
})))
const trendPoints = computed(() => {
  const trends = overview.value.trends
  const plotWidth = trendChartWidth.value - trendPlot.left - trendPlot.right
  const plotHeight = trendChartHeight - trendPlot.top - trendPlot.bottom
  const xStep = trends.length > 1 ? plotWidth / (trends.length - 1) : 0
  const point = (value, index) => ({
    x: trendPlot.left + (trends.length > 1 ? index * xStep : plotWidth / 2),
    y: trendPlot.top + plotHeight - (value / maxTrendCount.value) * plotHeight,
  })
  return {
    created: trends.map((item, index) => point(item.createdCount, index)),
    completed: trends.map((item, index) => point(item.completedCount, index)),
  }
})
const trendCreatedPolyline = computed(() => trendPoints.value.created.map((point) => `${point.x},${point.y}`).join(' '))
const trendCompletedPolyline = computed(() => trendPoints.value.completed.map((point) => `${point.x},${point.y}`).join(' '))
const trendLabelStep = computed(() => Math.max(1, Math.ceil(overview.value.trends.length / 12)))
const trendUnitLabel = computed(() => ({
  day: '按日',
  week: '按周',
  month: '按月',
}[overview.value.trendUnit] || '按日'))

function createEmptyOverview() {
  return {
    totalCount: 0,
    pendingCount: 0,
    processingCount: 0,
    completedCount: 0,
    closedCount: 0,
    completionRate: 0,
    customerRanks: [],
    engineerRanks: [],
    trendUnit: 'day',
    trends: [],
  }
}

function currentMonthRange() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  return [formatDate(start), formatDate(now)]
}

function formatDate(value) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function loadOverview() {
  loading.value = true
  try {
    const result = await getDashboardOverview({
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1],
      type: filters.type || undefined,
    })
    overview.value = {
      ...createEmptyOverview(),
      ...result.data,
      customerRanks: result.data?.customerRanks || [],
      engineerRanks: result.data?.engineerRanks || [],
      trends: result.data?.trends || [],
    }
    await nextTick()
    renderStatusPie()
  } finally {
    loading.value = false
  }
}

function renderStatusPie() {
  if (!statusPieRef.value) {
    return
  }
  if (!statusPieChart) {
    statusPieChart = echarts.init(statusPieRef.value)
  }
  statusPieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>工单数：{c}（{d}%）',
    },
    series: [{
      type: 'pie',
      radius: ['54%', '78%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      data: statusPieData.value.map((item) => ({
        name: item.label,
        value: item.count,
        itemStyle: { color: item.color },
      })),
    }],
  })
}

function resizeStatusPie() {
  statusPieChart?.resize()
}

function handleDateRangeChange(value) {
  if (!value?.length) {
    filters.dateRange = currentMonthRange()
  }
  loadOverview()
}

function customerBarWidth(count) {
  if (!maxCustomerCount.value || !count) {
    return '0%'
  }
  return `${Math.max(8, Math.round(count / maxCustomerCount.value * 100))}%`
}

function openCustomerWorkOrders(customer) {
  router.push({
    path: '/work-orders',
    query: {
      customerSiteId: String(customer.customerSiteId),
      customerSiteName: customer.customerSiteName,
      createdStart: filters.dateRange?.[0],
      createdEnd: filters.dateRange?.[1],
      type: filters.type || undefined,
    },
  })
}

function openEngineerWorkOrders(engineer) {
  router.push({
    path: '/work-orders',
    query: {
      engineerId: String(engineer.userId),
      engineerName: engineer.engineerName,
      createdStart: filters.dateRange?.[0],
      createdEnd: filters.dateRange?.[1],
      type: filters.type || undefined,
    },
  })
}

function openIndicatorWorkOrders(item) {
  router.push({
    path: '/work-orders',
    query: {
      status: item.status || undefined,
      createdStart: filters.dateRange?.[0],
      createdEnd: filters.dateRange?.[1],
      type: filters.type || undefined,
    },
  })
}

function openStatusWorkOrders(item) {
  openIndicatorWorkOrders(item)
}

watch(
  () => [overview.value.pendingCount, overview.value.processingCount, overview.value.completedCount, overview.value.closedCount],
  renderStatusPie,
)

onMounted(() => {
  window.addEventListener('resize', resizeStatusPie)
  loadOverview()
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeStatusPie)
  statusPieChart?.dispose()
  statusPieChart = null
})
</script>

<template>
  <section class="page-header dashboard-header">
    <div>
      <h1>数据看板</h1>
      <p>按工单创建时间统计当前工单状态与客户问题分布</p>
    </div>
    <div class="dashboard-actions">
      <el-date-picker
        v-model="filters.dateRange"
        class="dashboard-date-range"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="handleDateRangeChange"
      />
      <el-select v-model="filters.type" class="dashboard-type-filter" @change="loadOverview">
        <el-option v-for="item in typeOptions" :key="item.value || 'all'" :label="item.label" :value="item.value" />
      </el-select>
      <el-button :icon="Refresh" :loading="loading" @click="loadOverview">刷新</el-button>
    </div>
  </section>

  <section v-loading="loading" class="dashboard-content">
    <div class="indicator-grid">
      <button v-for="item in indicatorCards" :key="item.label" type="button" class="indicator-card" :class="`tone-${item.tone}`" @click="openIndicatorWorkOrders(item)">
        <div>
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
        </div>
        <span class="indicator-icon"><el-icon><component :is="item.icon" /></el-icon></span>
      </button>
      <article class="completion-card">
        <div class="completion-heading">
          <div>
            <p>完成率</p>
            <strong>{{ overview.completionRate || 0 }}<small>%</small></strong>
          </div>
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <el-progress :percentage="Number(overview.completionRate || 0)" :show-text="false" :stroke-width="7" />
        <span>已完成 {{ overview.completedCount }} / {{ overview.totalCount }} 条</span>
      </article>
    </div>

    <div class="analytics-grid">
    <section class="trend-panel">
      <div class="panel-heading">
        <div>
          <h2>工单趋势</h2>
          <p>{{ trendUnitLabel }}展示工单创建与完成数量</p>
        </div>
        <div class="trend-legend">
          <span><i class="legend-created" />创建</span>
          <span><i class="legend-completed" />完成</span>
        </div>
      </div>

      <div v-if="overview.trends.length" class="trend-scroll">
        <svg
          class="trend-chart"
          :viewBox="`0 0 ${trendChartWidth} ${trendChartHeight}`"
          :style="{ width: `${trendChartWidth}px` }"
          role="img"
          aria-label="工单创建与完成趋势"
        >
          <g v-for="gridLine in trendGridLines" :key="gridLine.ratio">
            <line
              :x1="trendPlot.left"
              :x2="trendChartWidth - trendPlot.right"
              :y1="trendPlot.top + (trendChartHeight - trendPlot.top - trendPlot.bottom) * gridLine.ratio"
              :y2="trendPlot.top + (trendChartHeight - trendPlot.top - trendPlot.bottom) * gridLine.ratio"
              class="trend-grid-line"
            />
            <text
              :x="trendPlot.left - 8"
              :y="trendPlot.top + (trendChartHeight - trendPlot.top - trendPlot.bottom) * gridLine.ratio + 4"
              class="trend-axis-label"
              text-anchor="end"
            >{{ gridLine.value }}</text>
          </g>
          <polyline :points="trendCreatedPolyline" class="trend-line trend-line-created" />
          <polyline :points="trendCompletedPolyline" class="trend-line trend-line-completed" />
          <g v-for="(trend, index) in overview.trends" :key="trend.period">
            <template v-if="index % trendLabelStep === 0 || index === overview.trends.length - 1">
              <text :x="trendPoints.created[index]?.x" :y="trendChartHeight - 14" class="trend-axis-label" text-anchor="middle">{{ trend.label }}</text>
            </template>
            <circle :cx="trendPoints.created[index]?.x" :cy="trendPoints.created[index]?.y" r="3.5" class="trend-point trend-point-created">
              <title>{{ trend.label }}：创建 {{ trend.createdCount }}</title>
            </circle>
            <circle :cx="trendPoints.completed[index]?.x" :cy="trendPoints.completed[index]?.y" r="3.5" class="trend-point trend-point-completed">
              <title>{{ trend.label }}：完成 {{ trend.completedCount }}</title>
            </circle>
          </g>
        </svg>
      </div>
      <el-empty v-else description="当前日期范围暂无趋势数据" :image-size="80" />
    </section>

    <section class="engineer-rank-panel">
      <div class="panel-heading">
        <div>
          <h2>现场工程师处理排行</h2>
          <p>按筛选范围内指派工单数排序，点击查看明细</p>
        </div>
        <span class="panel-total">共 {{ overview.engineerRanks.length }} 人</span>
      </div>
      <div v-if="overview.engineerRanks.length" class="engineer-rank-list">
        <button
          v-for="(engineer, index) in overview.engineerRanks"
          :key="engineer.userId"
          class="engineer-rank-item"
          type="button"
          @click="openEngineerWorkOrders(engineer)"
        >
          <span class="rank-number">{{ index + 1 }}</span>
          <span class="engineer-name" :title="engineer.engineerName">{{ engineer.engineerName }}</span>
          <span class="engineer-count"><strong>{{ engineer.workOrderCount }}</strong> 条</span>
          <span class="engineer-completed">完成 {{ engineer.completedCount }}</span>
        </button>
      </div>
      <el-empty v-else description="当前日期范围暂无工程师数据" :image-size="80" />
    </section>
    </div>

    <div class="secondary-grid">
    <section class="customer-rank-panel">
      <div class="panel-heading">
        <div>
          <h2>客户工单数量 Top 10</h2>
          <p>点击客户可查看对应日期范围内的工单</p>
        </div>
        <span class="panel-total">共 {{ overview.customerRanks.length }} 个客户</span>
      </div>

      <div v-if="overview.customerRanks.length" class="customer-rank-list">
        <button
          v-for="(customer, index) in overview.customerRanks"
          :key="customer.customerSiteId"
          class="customer-rank-item"
          type="button"
          @click="openCustomerWorkOrders(customer)"
        >
          <span class="rank-number">{{ index + 1 }}</span>
          <span class="customer-name" :title="customer.customerSiteName">{{ customer.customerSiteName }}</span>
          <span class="rank-bar-track"><span class="rank-bar" :style="{ width: customerBarWidth(customer.workOrderCount) }" /></span>
          <strong>{{ customer.workOrderCount }}</strong>
        </button>
      </div>
      <el-empty v-else description="当前日期范围暂无工单数据" :image-size="96" />
    </section>
      <section class="status-pie-panel">
        <div class="panel-heading">
          <div>
            <h2>工单状态分布</h2>
            <p>点击图例查看对应状态工单</p>
          </div>
          <span class="panel-total">共 {{ statusPieTotal }} 条</span>
        </div>
        <div class="status-pie-content">
          <div ref="statusPieRef" class="status-pie-chart" />
          <div class="status-pie-legend">
            <button
              v-for="item in statusPieData"
              :key="item.status"
              type="button"
              class="status-pie-legend-item"
              @click="openStatusWorkOrders(item)"
            >
              <span class="status-pie-dot" :style="{ background: item.color }" />
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.dashboard-header {
  margin-bottom: 20px;
}

.dashboard-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dashboard-date-range {
  width: 280px;
}

.dashboard-type-filter {
  width: 128px;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 1fr);
  gap: 18px;
}

.secondary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 1fr);
  gap: 18px;
}

.indicator-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.indicator-card,
.completion-card,
.customer-rank-panel,
.trend-panel,
.engineer-rank-panel,
.status-pie-panel {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
}

.indicator-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 126px;
  padding: 20px;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s, transform .2s;
}

.indicator-card:hover {
  border-color: #b7d0f7;
  box-shadow: 0 5px 16px rgba(47, 128, 237, .08);
  transform: translateY(-1px);
}

.indicator-card p,
.completion-card p,
.panel-heading p {
  margin: 0;
  color: #667085;
  font-size: 13px;
}

.indicator-card strong,
.completion-card strong {
  display: block;
  margin-top: 10px;
  color: #172033;
  font-size: 30px;
  line-height: 1;
}

.indicator-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  font-size: 20px;
}

.tone-blue .indicator-icon { background: #e8f1ff; color: #2f80ed; }
.tone-orange .indicator-icon { background: #fff3e6; color: #e88b2c; }
.tone-cyan .indicator-icon { background: #e5f8f7; color: #159a9c; }
.tone-green .indicator-icon { background: #eaf8ee; color: #30a46c; }
.tone-gray .indicator-icon { background: #eef1f5; color: #667085; }

.completion-card {
  min-height: 126px;
  padding: 18px 20px;
}

.completion-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.completion-heading > .el-icon {
  color: #2f80ed;
  font-size: 22px;
}

.completion-card strong { margin-top: 8px; }
.completion-card small { margin-left: 2px; font-size: 14px; font-weight: 600; }
.completion-card .el-progress { margin-top: 14px; }
.completion-card > span { display: block; margin-top: 8px; color: #667085; font-size: 12px; }

.customer-rank-panel { padding: 22px 24px; }
.trend-panel { padding: 22px 24px 18px; }
.engineer-rank-panel { padding: 22px 24px 14px; }
.status-pie-panel { padding: 22px 24px 14px; }

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef0f3;
}

.panel-heading h2 { margin: 0 0 6px; color: #172033; font-size: 16px; }
.panel-total { color: #667085; font-size: 13px; white-space: nowrap; }

.trend-legend {
  display: flex;
  gap: 14px;
  color: #667085;
  font-size: 12px;
}

.trend-legend span { display: inline-flex; align-items: center; gap: 5px; }
.trend-legend i { width: 8px; height: 8px; border-radius: 2px; }
.legend-created, .trend-created { background: #77aaf5; }
.legend-completed, .trend-completed { background: #30a46c; }

.trend-scroll { overflow-x: auto; padding-top: 20px; }

.trend-chart {
  display: block;
  height: 240px;
}

.trend-grid-line { stroke: #edf1f5; stroke-width: 1; }
.trend-axis-label { fill: #98a2b3; font-size: 11px; }
.trend-line { fill: none; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2.5; }
.trend-line-created { stroke: #77aaf5; }
.trend-line-completed { stroke: #30a46c; }
.trend-point { stroke: #fff; stroke-width: 2; }
.trend-point-created { fill: #77aaf5; }
.trend-point-completed { fill: #30a46c; }

.customer-rank-list { padding-top: 6px; }

.customer-rank-item {
  display: grid;
  width: 100%;
  grid-template-columns: 34px minmax(180px, 300px) minmax(160px, 1fr) 48px;
  align-items: center;
  gap: 12px;
  padding: 13px 8px;
  border: 0;
  border-bottom: 1px solid #f0f2f5;
  background: transparent;
  color: #344054;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.customer-rank-item:hover { background: #f7faff; }
.customer-rank-item:last-child { border-bottom: 0; }
.rank-number { color: #98a2b3; font-size: 13px; text-align: center; }
.customer-rank-item:nth-child(-n + 3) .rank-number { color: #2f80ed; font-weight: 700; }
.customer-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.rank-bar-track { display: block; height: 8px; overflow: hidden; border-radius: 4px; background: #edf1f6; }
.rank-bar { display: block; height: 100%; border-radius: inherit; background: #2f80ed; }
.customer-rank-item strong { color: #172033; font-size: 14px; text-align: right; }

.status-pie-content {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(130px, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 250px;
}

.status-pie-chart {
  width: 100%;
  height: 230px;
}

.status-pie-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-pie-legend-item {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 9px 6px;
  border: 0;
  background: transparent;
  color: #344054;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.status-pie-legend-item:hover { background: #f7faff; }
.status-pie-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-pie-legend-item strong { color: #172033; font-size: 14px; }

.engineer-rank-list { padding-top: 6px; }

.engineer-rank-item {
  display: grid;
  width: 100%;
  grid-template-columns: 28px minmax(90px, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  padding: 12px 6px;
  border: 0;
  border-bottom: 1px solid #f0f2f5;
  background: transparent;
  color: #344054;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.engineer-rank-item:hover { background: #f7faff; }
.engineer-rank-item:last-child { border-bottom: 0; }
.engineer-rank-item:nth-child(-n + 3) .rank-number { color: #2f80ed; font-weight: 700; }
.engineer-name { overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.engineer-count { color: #172033; font-size: 13px; white-space: nowrap; }
.engineer-count strong { font-size: 16px; }
.engineer-completed { color: #30a46c; font-size: 12px; white-space: nowrap; }

@media (max-width: 1280px) {
  .indicator-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .analytics-grid { grid-template-columns: 1fr; }
  .secondary-grid { grid-template-columns: 1fr; }
}

@media (max-width: 760px) {
  .dashboard-header, .dashboard-actions { align-items: stretch; flex-direction: column; }
  .dashboard-date-range { width: 100%; }
  .dashboard-type-filter { width: 100%; }
  .indicator-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .customer-rank-panel { padding: 18px; }
  .trend-panel, .engineer-rank-panel { padding: 18px; }
  .status-pie-panel { padding: 18px; }
  .customer-rank-item { grid-template-columns: 28px minmax(90px, 1fr) 72px 36px; gap: 8px; }
  .engineer-rank-item { grid-template-columns: 24px minmax(80px, 1fr) auto; gap: 7px; }
  .engineer-completed { display: none; }
  .status-pie-content { grid-template-columns: 1fr; }
  .status-pie-chart { height: 210px; }
}
</style>

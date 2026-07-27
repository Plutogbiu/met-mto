<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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
  { label: '工单总数', value: overview.value.totalCount, icon: Document, tone: 'blue' },
  { label: '待处理', value: overview.value.pendingCount, icon: Clock, tone: 'orange' },
  { label: '处理中', value: overview.value.processingCount, icon: Loading, tone: 'cyan' },
  { label: '已完成', value: overview.value.completedCount, icon: CircleCheck, tone: 'green' },
  { label: '已作废', value: overview.value.closedCount, icon: CircleClose, tone: 'gray' },
])

const maxCustomerCount = computed(() => Math.max(...overview.value.customerRanks.map((item) => item.workOrderCount), 0))
const maxTrendCount = computed(() => Math.max(
  ...overview.value.trends.flatMap((item) => [item.createdCount, item.completedCount]),
  0,
))
const trendGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${overview.value.trends.length || 1}, minmax(42px, 1fr))`,
  minWidth: `${Math.max(100, (overview.value.trends.length || 1) * 48)}px`,
}))
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
      trends: result.data?.trends || [],
    }
  } finally {
    loading.value = false
  }
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

function trendBarHeight(count) {
  if (!maxTrendCount.value || !count) {
    return '2px'
  }
  return `${Math.max(6, Math.round(count / maxTrendCount.value * 100))}%`
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

onMounted(loadOverview)
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
      <article v-for="item in indicatorCards" :key="item.label" class="indicator-card" :class="`tone-${item.tone}`">
        <div>
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
        </div>
        <span class="indicator-icon"><el-icon><component :is="item.icon" /></el-icon></span>
      </article>
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
        <div class="trend-chart" :style="trendGridStyle">
          <div v-for="trend in overview.trends" :key="trend.period" class="trend-column" :title="`${trend.label}：创建 ${trend.createdCount}，完成 ${trend.completedCount}`">
            <div class="trend-bars">
              <span class="trend-bar trend-created" :style="{ height: trendBarHeight(trend.createdCount) }" />
              <span class="trend-bar trend-completed" :style="{ height: trendBarHeight(trend.completedCount) }" />
            </div>
            <span class="trend-label">{{ trend.label }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="当前日期范围暂无趋势数据" :image-size="80" />
    </section>

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

.indicator-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.indicator-card,
.completion-card,
.customer-rank-panel,
.trend-panel {
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
  display: grid;
  align-items: end;
  height: 220px;
  gap: 6px;
}

.trend-column { display: grid; grid-template-rows: 182px 28px; min-width: 0; }

.trend-bars {
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 4px;
  height: 182px;
  border-bottom: 1px solid #dfe5ec;
}

.trend-bar { width: min(12px, 32%); min-height: 2px; border-radius: 3px 3px 0 0; }
.trend-label { overflow: hidden; padding-top: 8px; color: #98a2b3; font-size: 11px; text-align: center; text-overflow: ellipsis; white-space: nowrap; }

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

@media (max-width: 1280px) {
  .indicator-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 760px) {
  .dashboard-header, .dashboard-actions { align-items: stretch; flex-direction: column; }
  .dashboard-date-range { width: 100%; }
  .dashboard-type-filter { width: 100%; }
  .indicator-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .customer-rank-panel { padding: 18px; }
  .customer-rank-item { grid-template-columns: 28px minmax(90px, 1fr) 72px 36px; gap: 8px; }
}
</style>

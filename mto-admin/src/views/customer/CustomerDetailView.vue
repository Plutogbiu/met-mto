<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowLeft, Location, Refresh } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { getCustomerSite } from '../../api/customerSite'
import { getWorkOrderPage } from '../../api/workOrder'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const onsiteLoading = ref(false)
const inspectionLoading = ref(false)
const customer = ref(null)
const onsiteOrders = ref([])
const inspectionOrders = ref([])
const onsiteTotal = ref(0)
const inspectionTotal = ref(0)

const onsitePage = reactive({
  page: 1,
  size: 5,
})

const inspectionPage = reactive({
  page: 1,
  size: 5,
})

const priorityOptions = [
  { label: '普通', value: 'normal', tag: 'info' },
  { label: '紧急', value: 'urgent', tag: 'danger' },
]

const statusOptions = [
  { label: '待处理', value: 'pending', tag: 'info' },
  { label: '处理中', value: 'processing', tag: 'warning' },
  { label: '已完成', value: 'completed', tag: 'success' },
  { label: '已关闭', value: 'closed', tag: 'info' },
]

const coordinateText = computed(() => {
  if (!customer.value?.longitude || !customer.value?.latitude) {
    return '-'
  }
  return `${customer.value.longitude}, ${customer.value.latitude}`
})

function optionLabel(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

function optionTag(options, value) {
  return options.find((item) => item.value === value)?.tag || 'info'
}

function engineerNames(row) {
  const names = (row.engineers || []).map((item) => item.realName || item.username).filter(Boolean)
  return names.length ? names.join('、') : '-'
}

function openWorkOrder(row) {
  router.push(`/work-orders/${row.id}`)
}

async function loadCustomer() {
  const result = await getCustomerSite(route.params.id)
  customer.value = result.data
}

async function loadOnsiteOrders() {
  onsiteLoading.value = true
  try {
    const result = await getWorkOrderPage({
      customerSiteId: route.params.id,
      type: 'onsite',
      page: onsitePage.page,
      size: onsitePage.size,
    })
    onsiteOrders.value = result.data?.records || []
    onsiteTotal.value = result.data?.total || 0
  } finally {
    onsiteLoading.value = false
  }
}

async function loadInspectionOrders() {
  inspectionLoading.value = true
  try {
    const result = await getWorkOrderPage({
      customerSiteId: route.params.id,
      type: 'inspection',
      page: inspectionPage.page,
      size: inspectionPage.size,
    })
    inspectionOrders.value = result.data?.records || []
    inspectionTotal.value = result.data?.total || 0
  } finally {
    inspectionLoading.value = false
  }
}

async function loadDetail() {
  loading.value = true
  try {
    await Promise.all([loadCustomer(), loadOnsiteOrders(), loadInspectionOrders()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <section class="detail-header">
    <div>
      <el-button :icon="ArrowLeft" @click="router.push('/customers')">返回</el-button>
      <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
    </div>
  </section>

  <section v-loading="loading" class="customer-detail">
    <section class="detail-panel">
      <div class="panel-title">
        <div>
          <h1>{{ customer?.name || '客户详情' }}</h1>
          <p>{{ customer?.address || customer?.locationAddress || '-' }}</p>
        </div>
        <el-tag :type="customer?.status === 1 ? 'success' : 'info'" effect="light">
          {{ customer?.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </div>

      <el-descriptions v-if="customer" :column="2" border>
        <el-descriptions-item label="联系人">{{ customer.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ customer.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ customer.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="定位地址" :span="2">{{ customer.locationAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="定位坐标">
          <span class="coordinate">
            <el-icon><Location /></el-icon>
            {{ coordinateText }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="定位备注">{{ customer.locationRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ customer.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="record-grid">
      <section class="detail-panel">
        <div class="panel-title compact">
          <h2>现场工单记录</h2>
          <span>{{ onsiteTotal }} 条</span>
        </div>
        <el-table v-loading="onsiteLoading" :data="onsiteOrders" row-key="id">
          <el-table-column prop="orderNo" label="工单编号" min-width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWorkOrder(row)">{{ row.orderNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="deviceName" label="设备" min-width="140" show-overflow-tooltip />
          <el-table-column label="优先级" width="90">
            <template #default="{ row }">
              <el-tag :type="optionTag(priorityOptions, row.priority)" effect="light">
                {{ optionLabel(priorityOptions, row.priority) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="optionTag(statusOptions, row.status)" effect="light">
                {{ optionLabel(statusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="工程师" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ engineerNames(row) }}</template>
          </el-table-column>
          <el-table-column prop="estimatedArrivalTime" label="预计到达" min-width="170" />
          <el-table-column prop="completedAt" label="完成时间" min-width="170" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWorkOrder(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="onsitePage.page"
            v-model:page-size="onsitePage.size"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            :total="onsiteTotal"
            @change="loadOnsiteOrders"
          />
        </div>
      </section>

      <section class="detail-panel">
        <div class="panel-title compact">
          <h2>巡检工单记录</h2>
          <span>{{ inspectionTotal }} 条</span>
        </div>
        <el-table v-loading="inspectionLoading" :data="inspectionOrders" row-key="id">
          <el-table-column prop="orderNo" label="巡检编号" min-width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWorkOrder(row)">{{ row.orderNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="optionTag(statusOptions, row.status)" effect="light">
                {{ optionLabel(statusOptions, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="工程师" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ engineerNames(row) }}</template>
          </el-table-column>
          <el-table-column prop="estimatedArrivalTime" label="预计到达" min-width="170" />
          <el-table-column prop="completedAt" label="完成时间" min-width="170" />
          <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openWorkOrder(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="inspectionPage.page"
            v-model:page-size="inspectionPage.size"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            :total="inspectionTotal"
            @change="loadInspectionOrders"
          />
        </div>
      </section>
    </section>
  </section>
</template>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.customer-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-panel {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title.compact {
  margin-bottom: 12px;
}

.panel-title h1,
.panel-title h2 {
  margin: 0;
}

.panel-title h1 {
  font-size: 22px;
}

.panel-title h2 {
  font-size: 16px;
}

.panel-title p {
  margin: 6px 0 0;
  color: #667085;
}

.panel-title span {
  color: #667085;
  font-size: 13px;
}

.coordinate {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>

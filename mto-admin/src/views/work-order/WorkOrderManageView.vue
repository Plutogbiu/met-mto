<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCustomerSitePage } from '../../api/customerSite'
import { getDevicePage } from '../../api/device'
import { getUserPage } from '../../api/user'
import { createWorkOrder, getWorkOrderPage, updateWorkOrder, updateWorkOrderStatus } from '../../api/workOrder'

const loading = ref(false)
const router = useRouter()
const dialogVisible = ref(false)
const customerLoading = ref(false)
const deviceLoading = ref(false)
const editingId = ref(null)
const workOrders = ref([])
const customerOptions = ref([])
const deviceOptions = ref([])
const engineerOptions = ref([])
const total = ref(0)

const typeOptions = [
  { label: '现场工单', value: 'onsite', tag: 'primary' },
  { label: '日常巡检', value: 'inspection', tag: 'success' },
]

const priorityOptions = [
  { label: '普通', value: 'normal', tag: 'info' },
  { label: '紧急', value: 'urgent', tag: 'danger' },
]

const maintenanceContentOptions = [
  { label: '保内免费', value: 'warranty_free' },
  { label: '保外收费', value: 'out_warranty_paid' },
  { label: '保外免费', value: 'out_warranty_free' },
  { label: '保内收费', value: 'warranty_paid' },
]

const statusOptions = [
  { label: '待处理', value: 'pending', tag: 'info' },
  { label: '处理中', value: 'processing', tag: 'warning' },
  { label: '已完成', value: 'completed', tag: 'success' },
  { label: '已关闭', value: 'closed', tag: 'info' },
]

const filters = reactive({
  keyword: '',
  type: '',
  status: '',
  createdRange: [],
  page: 1,
  size: 10,
})

const form = reactive({
  type: 'onsite',
  customerSiteId: null,
  deviceId: null,
  priority: 'normal',
  status: 'pending',
  maintenanceContent: '',
  content: '',
  notice: '',
  estimatedArrivalTime: '',
  estimatedCompleteTime: '',
  engineerIds: [],
})

const dialogTitle = computed(() => (editingId.value ? '编辑工单' : '新增工单'))
const isOnsiteOrder = computed(() => form.type === 'onsite')

function optionLabel(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

function optionTag(options, value) {
  return options.find((item) => item.value === value)?.tag || 'info'
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    type: 'onsite',
    customerSiteId: null,
    deviceId: null,
    priority: 'normal',
    status: 'pending',
    maintenanceContent: '',
    content: '',
    notice: '',
    estimatedArrivalTime: '',
    estimatedCompleteTime: '',
    engineerIds: [],
  })
  deviceOptions.value = []
}

function buildPayload() {
  return {
    type: form.type,
    customerSiteId: form.customerSiteId,
    deviceId: form.type === 'onsite' ? form.deviceId : null,
    priority: form.priority,
    status: editingId.value ? form.status : undefined,
    maintenanceContent: form.maintenanceContent,
    content: form.content,
    notice: form.notice,
    estimatedArrivalTime: form.estimatedArrivalTime || null,
    estimatedCompleteTime: form.type === 'onsite' ? form.estimatedCompleteTime || null : null,
    engineerIds: form.engineerIds,
  }
}

async function searchCustomers(keyword = '') {
  customerLoading.value = true
  try {
    const result = await getCustomerSitePage({
      keyword: keyword || undefined,
      status: 1,
      page: 1,
      size: 20,
    })
    customerOptions.value = result.data?.records || []
  } finally {
    customerLoading.value = false
  }
}

async function searchDevices(keyword = '') {
  deviceLoading.value = true
  try {
    const result = await getDevicePage({
      keyword: keyword || undefined,
      status: 1,
      page: 1,
      size: 20,
    })
    deviceOptions.value = result.data?.records || []
  } finally {
    deviceLoading.value = false
  }
}

function ensureCustomerOption(row) {
  if (!row.customerSiteId || customerOptions.value.some((item) => item.id === row.customerSiteId)) {
    return
  }
  customerOptions.value.push({
    id: row.customerSiteId,
    name: row.customerSiteName,
  })
}

function ensureDeviceOption(row) {
  if (!row.deviceId || deviceOptions.value.some((item) => item.id === row.deviceId)) {
    return
  }
  deviceOptions.value.push({
    id: row.deviceId,
    name: row.deviceName,
  })
}

async function loadEngineers() {
  const result = await getUserPage({
    role: 'field_engineer',
    status: 1,
    page: 1,
    size: 100,
  })
  engineerOptions.value = result.data?.records || []
}

async function loadWorkOrders() {
  loading.value = true
  try {
    const result = await getWorkOrderPage({
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status || undefined,
      createdStart: filters.createdRange?.[0] || undefined,
      createdEnd: filters.createdRange?.[1] || undefined,
      page: filters.page,
      size: filters.size,
    })
    workOrders.value = result.data?.records || []
    total.value = result.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadWorkOrders()
}

function openDetail(row) {
  router.push(`/work-orders/${row.id}`)
}

async function openCreate() {
  resetForm()
  await Promise.all([searchCustomers(), searchDevices(), loadEngineers()])
  dialogVisible.value = true
}

async function openEdit(row) {
  resetForm()
  await Promise.all([searchCustomers(row.customerSiteName || ''), loadEngineers()])
  ensureCustomerOption(row)
  Object.assign(form, {
    type: row.type || 'onsite',
    customerSiteId: row.customerSiteId || null,
    deviceId: row.deviceId || null,
    priority: row.priority || 'normal',
    status: row.status || 'pending',
    maintenanceContent: row.maintenanceContent || '',
    content: row.content || '',
    notice: row.notice || '',
    estimatedArrivalTime: row.estimatedArrivalTime || '',
    estimatedCompleteTime: row.estimatedCompleteTime || '',
    engineerIds: (row.engineers || []).map((item) => item.userId),
  })
  editingId.value = row.id
  await searchDevices(row.deviceName || '')
  ensureDeviceOption(row)
  dialogVisible.value = true
}

function handleTypeChange() {
  if (form.type !== 'onsite') {
    form.deviceId = null
    form.estimatedCompleteTime = ''
  }
}

async function saveWorkOrder() {
  if (!form.customerSiteId) {
    ElMessage.warning('请选择客户')
    return
  }
  if (!form.estimatedArrivalTime) {
    ElMessage.warning('请设置预计到达时间')
    return
  }
  if (form.type === 'onsite' && !form.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  if (form.type === 'onsite' && !form.estimatedCompleteTime) {
    ElMessage.warning('请设置预估完成时间')
    return
  }
  if (!form.engineerIds.length) {
    ElMessage.warning('请选择至少一名现场实施工程师')
    return
  }
  if (!form.content) {
    ElMessage.warning('请填写工单内容')
    return
  }
  if (!form.maintenanceContent) {
    ElMessage.warning('请选择维保内容')
    return
  }

  if (editingId.value) {
    await updateWorkOrder(editingId.value, buildPayload())
    ElMessage.success('工单已更新')
  } else {
    await createWorkOrder(buildPayload())
    ElMessage.success('工单已新增')
  }
  dialogVisible.value = false
  await loadWorkOrders()
}

async function changeStatus(row, status) {
  const text = optionLabel(statusOptions, status)
  await ElMessageBox.confirm(`确认将工单“${row.orderNo}”改为${text}？`, '状态变更', {
    type: 'warning',
  })
  await updateWorkOrderStatus(row.id, status)
  ElMessage.success('状态已更新')
  await loadWorkOrders()
}

watch(
  () => form.type,
  () => handleTypeChange(),
)

onMounted(async () => {
  await Promise.all([searchCustomers(), searchDevices(), loadEngineers()])
  await loadWorkOrders()
})
</script>

<template>
  <section class="page-header">
    <div>
      <h1>工单管理</h1>
      <p>创建现场工单和巡检工单，并指派一名或多名现场实施工程师</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="openCreate">新增工单</el-button>
  </section>

  <section class="toolbar">
    <el-input
      v-model="filters.keyword"
      class="keyword"
      clearable
      placeholder="搜索编号、客户或设备"
      :prefix-icon="Search"
      @keyup.enter="search"
    />
    <el-select v-model="filters.type" class="work-order-filter" placeholder="类型" clearable>
      <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-select v-model="filters.status" class="work-order-filter" placeholder="状态" clearable>
      <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-date-picker
      v-model="filters.createdRange"
      class="date-range"
      style="width: 260px"
      type="daterange"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      value-format="YYYY-MM-DD"
      range-separator="至"
    />
    <el-button type="primary" :icon="Search" @click="search">查询</el-button>
    <el-button :icon="Refresh" @click="loadWorkOrders">刷新</el-button>
  </section>

  <section class="table-panel">
    <el-table v-loading="loading" :data="workOrders" row-key="id" height="calc(100vh - 284px)">
      <el-table-column prop="orderNo" label="工单编号" min-width="200" fixed />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag :type="optionTag(typeOptions, row.type)" effect="light">
            {{ optionLabel(typeOptions, row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="customerSiteName" label="客户" min-width="180" show-overflow-tooltip />
      <el-table-column prop="deviceName" label="设备" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.deviceName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="指派工程师" min-width="220">
        <template #default="{ row }">
          <div class="engineer-tags">
            <el-tag v-for="engineer in row.engineers" :key="engineer.userId" effect="plain">
              {{ engineer.realName || engineer.username }}
            </el-tag>
            <span v-if="!row.engineers?.length" class="muted">未指派</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="100">
        <template #default="{ row }">
          <el-tag :type="optionTag(priorityOptions, row.priority)" effect="light">
            {{ optionLabel(priorityOptions, row.priority) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="optionTag(statusOptions, row.status)" effect="light">
            {{ optionLabel(statusOptions, row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="estimatedArrivalTime" label="预计到达" min-width="170" show-overflow-tooltip />
      <el-table-column label="预估完成" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.estimatedCompleteTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'pending'" link type="warning" @click="changeStatus(row, 'processing')">
            开始处理
          </el-button>
          <el-button v-if="row.status !== 'completed'" link type="success" @click="changeStatus(row, 'completed')">
            完成
          </el-button>
          <el-button v-if="row.status !== 'closed'" link type="info" @click="changeStatus(row, 'closed')">
            关闭
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.size"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @change="loadWorkOrders"
      />
    </div>
  </section>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" destroy-on-close>
    <el-form label-position="top" class="work-order-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="工单类型" required>
            <el-select v-model="form.type" class="full-control">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="煤矿客户" required>
            <el-select
              v-model="form.customerSiteId"
              class="full-control"
              filterable
              remote
              :remote-method="searchCustomers"
              :loading="customerLoading"
              placeholder="输入客户名称搜索"
            >
              <el-option
                v-for="customer in customerOptions"
                :key="customer.id"
                :label="customer.name"
                :value="customer.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="优先级">
            <el-select v-model="form.priority" class="full-control">
              <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="预计到达时间" required>
            <el-date-picker
              v-model="form.estimatedArrivalTime"
              class="full-control"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择预计到达时间"
            />
          </el-form-item>
        </el-col>

         <el-col :span="8">
          <el-form-item label="预估完成时间" required>
            <el-date-picker
              v-model="form.estimatedCompleteTime"
              class="full-control"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择预估完成时间"
            />
          </el-form-item>
        </el-col>
        
       

        
      </el-row>

      <el-row v-if="isOnsiteOrder" :gutter="16">
        <el-col :span="12">
          <el-form-item label="设备" required>
            <el-select
              v-model="form.deviceId"
              class="full-control"
              filterable
              remote
              :remote-method="searchDevices"
              :loading="deviceLoading"
              placeholder="输入设备名称、型号或编号搜索"
            >
              <el-option
                v-for="device in deviceOptions"
                :key="device.id"
                :label="[device.name, device.model, device.serialNo].filter(Boolean).join(' / ')"
                :value="device.id"
              />
            </el-select>
          </el-form-item>
        </el-col>

         <el-col v-if="editingId" :span="4" >
          <el-form-item label="状态">
            <el-select v-model="form.status" class="full-control">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col v-else :span="4">
          <el-form-item label="状态">
            <el-tag type="info" effect="light">待处理</el-tag>
          </el-form-item>
        </el-col>

      </el-row>

      <el-form-item label="维保内容" required>
        <el-select
          v-model="form.maintenanceContent"
          class="full-control"
          clearable
          placeholder="请选择"
        >
          <el-option
            v-for="item in maintenanceContentOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="指派工程师" required>
        <el-select
          v-model="form.engineerIds"
          class="full-control"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="可同时选择多个现场实施工程师"
        >
          <el-option
            v-for="engineer in engineerOptions"
            :key="engineer.id"
            :label="engineer.realName || engineer.username"
            :value="engineer.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="工单内容" required>
        <el-input v-model="form.content" type="textarea" :rows="4" placeholder="描述问题、现场要求或巡检要求" />
      </el-form-item>

      <el-form-item label="注意事项">
        <el-input v-model="form.notice" type="textarea" :rows="3" placeholder="现场安全、联系、设备等注意事项" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveWorkOrder">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.work-order-filter {
  width: 140px;
}

.date-range,
:deep(.date-range.el-date-editor--daterange) {
  width: 260px;
  flex: 0 0 260px;
}

:deep(.toolbar) {
  flex-wrap: wrap;
}

.work-order-form .el-form-item {
  margin-bottom: 16px;
}

.full-control {
  width: 100%;
}

.engineer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Download, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCustomerSitePage } from '../../api/customerSite'
import { getDevicePage } from '../../api/device'
import { getUserPage } from '../../api/user'
import {
  createWorkOrder,
  createWorkOrderExportDownloadTicket,
  createWorkOrderExportTask,
  deleteWorkOrder,
  downloadWorkOrderReceipt,
  getWorkOrderExportTask,
  getWorkOrderPage,
  getWorkOrderStatusSummary,
  updateWorkOrder,
  updateWorkOrderStatus,
  voidWorkOrder,
} from '../../api/workOrder'

const loading = ref(false)
const router = useRouter()
const route = useRoute()
const dialogVisible = ref(false)
const customerLoading = ref(false)
const deviceLoading = ref(false)
const editingId = ref(null)
const workOrders = ref([])
const customerOptions = ref([])
const deviceOptions = ref([])
const engineerOptions = ref([])
const total = ref(0)
const statusSummary = ref(createEmptyStatusSummary())
const currentUser = ref(readCurrentUser())
const workOrderTable = ref(null)
const selectedWorkOrderIds = ref([])
const batchExportMode = ref(false)
const exportTaskDialogVisible = ref(false)
const exportTaskCreating = ref(false)
const exportTaskDownloading = ref(false)
const exportTask = ref(null)
let exportTaskTimer = null
let restoringSelection = false

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
  { label: '已作废', value: 'closed', tag: 'info' },
]

const editableStatusOptions = statusOptions.filter((item) => item.value !== 'closed')

const filters = reactive({
  keyword: '',
  type: '',
  status: '',
  customerSiteId: null,
  customerSiteName: '',
  engineerId: null,
  engineerName: '',
  createdRange: [],
  completedRange: [],
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
  estimatedArrivalTime: '',
  estimatedCompleteTime: '',
  engineerIds: [],
})

const summaryCards = computed(() => [
  { label: '工单总数', value: statusSummary.value.totalCount, status: '', tone: 'blue' },
  { label: '待处理', value: statusSummary.value.pendingCount, status: 'pending', tone: 'orange' },
  { label: '处理中', value: statusSummary.value.processingCount, status: 'processing', tone: 'cyan' },
  { label: '已完成', value: statusSummary.value.completedCount, status: 'completed', tone: 'green' },
  { label: '已作废', value: statusSummary.value.closedCount, status: 'closed', tone: 'gray' },
])

function createEmptyStatusSummary() {
  return { totalCount: 0, pendingCount: 0, processingCount: 0, completedCount: 0, closedCount: 0 }
}

const dialogTitle = computed(() => (editingId.value ? '编辑工单' : '新增工单'))
const isOnsiteOrder = computed(() => form.type === 'onsite')
const selectedExportCount = computed(() => selectedWorkOrderIds.value.length)
const exportProgress = computed(() => {
  if (!exportTask.value?.totalCount) {
    return 0
  }
  return Math.min(100, Math.round(((exportTask.value.successCount || 0) + (exportTask.value.failedCount || 0)) / exportTask.value.totalCount * 100))
})

function readCurrentUser() {
  const value = localStorage.getItem('mto-admin-user')
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
}

function hasPermission(permission) {
  return currentUser.value?.permissions?.includes(permission)
}

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
    const params = {
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      status: filters.status || undefined,
      customerSiteId: filters.customerSiteId || undefined,
      engineerId: filters.engineerId || undefined,
      createdStart: filters.createdRange?.[0] || undefined,
      createdEnd: filters.createdRange?.[1] || undefined,
      completedStart: filters.completedRange?.[0] || undefined,
      completedEnd: filters.completedRange?.[1] || undefined,
      page: filters.page,
      size: filters.size,
    }
    const summaryParams = { ...params, status: undefined, page: undefined, size: undefined }
    const [result, summaryResult] = await Promise.all([
      getWorkOrderPage(params),
      getWorkOrderStatusSummary(summaryParams),
    ])
    workOrders.value = result.data?.records || []
    total.value = result.data?.total || 0
    statusSummary.value = { ...createEmptyStatusSummary(), ...(summaryResult.data || {}) }
    await restoreTableSelection()
  } finally {
    loading.value = false
  }
}

function selectSummaryStatus(status) {
  filters.status = status
  filters.page = 1
  clearExportSelection()
  loadWorkOrders()
}

function search() {
  filters.page = 1
  clearExportSelection()
  loadWorkOrders()
}

function applyRouteFilters() {
  const customerSiteId = Number(route.query.customerSiteId)
  filters.customerSiteId = Number.isInteger(customerSiteId) && customerSiteId > 0 ? customerSiteId : null
  filters.customerSiteName = typeof route.query.customerSiteName === 'string' ? route.query.customerSiteName : ''
  if (['pending', 'processing', 'completed', 'closed'].includes(route.query.status)) {
    filters.status = route.query.status
  }
  const engineerId = Number(route.query.engineerId)
  filters.engineerId = Number.isInteger(engineerId) && engineerId > 0 ? engineerId : null
  filters.engineerName = typeof route.query.engineerName === 'string' ? route.query.engineerName : ''
  if (typeof route.query.createdStart === 'string' && typeof route.query.createdEnd === 'string') {
    filters.createdRange = [route.query.createdStart, route.query.createdEnd]
  }
  if (route.query.type === 'onsite' || route.query.type === 'inspection') {
    filters.type = route.query.type
  }
}

function clearCustomerSiteFilter() {
  filters.customerSiteId = null
  filters.customerSiteName = ''
  const { customerSiteId, customerSiteName, ...query } = route.query
  router.replace({ query })
  search()
}

function clearEngineerFilter() {
  filters.engineerId = null
  filters.engineerName = ''
  const { engineerId, engineerName, ...query } = route.query
  router.replace({ query })
  search()
}

function currentListQuery() {
  const query = {
    keyword: filters.keyword || undefined,
    type: filters.type || undefined,
    status: filters.status || undefined,
    customerSiteId: filters.customerSiteId ? String(filters.customerSiteId) : undefined,
    customerSiteName: filters.customerSiteName || undefined,
    engineerId: filters.engineerId ? String(filters.engineerId) : undefined,
    engineerName: filters.engineerName || undefined,
    createdStart: filters.createdRange?.[0] || undefined,
    createdEnd: filters.createdRange?.[1] || undefined,
    completedStart: filters.completedRange?.[0] || undefined,
    completedEnd: filters.completedRange?.[1] || undefined,
  }
  return Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== ''))
}

function isExportSelectable(row) {
  return row.status === 'completed'
}

function handleSelectionChange(rows) {
  if (restoringSelection) {
    return
  }
  const selected = new Set(selectedWorkOrderIds.value)
  workOrders.value.filter(isExportSelectable).forEach((row) => selected.delete(row.id))
  rows.forEach((row) => selected.add(row.id))
  selectedWorkOrderIds.value = Array.from(selected)
}

async function restoreTableSelection() {
  await nextTick()
  if (!workOrderTable.value) {
    return
  }
  restoringSelection = true
  workOrderTable.value.clearSelection()
  const selected = new Set(selectedWorkOrderIds.value)
  workOrders.value.filter((row) => selected.has(row.id) && isExportSelectable(row)).forEach((row) => {
    workOrderTable.value.toggleRowSelection(row, true)
  })
  restoringSelection = false
}

function clearExportSelection() {
  selectedWorkOrderIds.value = []
  workOrderTable.value?.clearSelection()
}

function taskStatusLabel(status) {
  return {
    pending: '排队中',
    processing: '生成中',
    success: '已完成',
    failed: '生成失败',
    expired: '文件已过期',
  }[status] || '-'
}

function taskStatusType(status) {
  return {
    pending: 'warning',
    processing: 'primary',
    success: 'success',
    failed: 'danger',
    expired: 'info',
  }[status] || 'info'
}

function formatFileSize(size) {
  if (!size) {
    return '-'
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function stopExportTaskPolling() {
  if (exportTaskTimer) {
    window.clearInterval(exportTaskTimer)
    exportTaskTimer = null
  }
}

function startExportTaskPolling() {
  stopExportTaskPolling()
  if (!['pending', 'processing'].includes(exportTask.value?.status)) {
    return
  }
  exportTaskTimer = window.setInterval(() => {
    void loadExportTask()
  }, 2000)
}

async function loadExportTask() {
  const taskId = exportTask.value?.id
  if (!taskId) {
    return
  }
  try {
    const result = await getWorkOrderExportTask(taskId)
    exportTask.value = result.data
    exportTaskDialogVisible.value = true
    if (['success', 'failed', 'expired'].includes(exportTask.value?.status)) {
      stopExportTaskPolling()
    }
  } catch (error) {
    stopExportTaskPolling()
  }
}

async function createExportTask(payload) {
  exportTaskCreating.value = true
  try {
    const result = await createWorkOrderExportTask(payload)
    exportTask.value = result.data
    exportTaskDialogVisible.value = true
    startExportTaskPolling()
    ElMessage.success(`已创建导出任务，共 ${result.data.totalCount} 条工单`)
  } finally {
    exportTaskCreating.value = false
  }
}

async function exportSelectedOrders() {
  if (!selectedExportCount.value) {
    return
  }
  await ElMessageBox.confirm(
    `将后台异步生成 ${selectedExportCount.value} 份已完成工单回执，并打包为 ZIP 文件。生成期间可继续使用系统。`,
    '确认批量导出',
    { type: 'warning', confirmButtonText: '开始导出', cancelButtonText: '取消' },
  )
  await createExportTask({ workOrderIds: selectedWorkOrderIds.value })
  batchExportMode.value = false
  clearExportSelection()
}

function startBatchExportMode() {
  batchExportMode.value = true
}

function cancelBatchExportMode() {
  batchExportMode.value = false
  clearExportSelection()
}

function receiptFallbackFileName(row) {
  const typeName = row.type === 'inspection' ? '日常巡检' : '现场工单'
  const createdAt = String(row.createdAt || '').replace(/[^0-9]/g, '').slice(0, 14) || '未知时间'
  return `物链易通-${row.customerSiteName || '-'}-${typeName}-${createdAt}_${row.orderNo || '-'}.pdf`
}

function responseFileName(response, fallbackFileName) {
  const disposition = response.headers?.['content-disposition'] || ''
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (encoded?.[1]) {
    return decodeURIComponent(encoded[1])
  }
  const plain = disposition.match(/filename="?([^";]+)"?/i)
  return plain?.[1] || fallbackFileName
}

async function exportReceipt(row) {
  try {
    const response = await downloadWorkOrderReceipt(row.id)
    const contentType = response.headers?.['content-type'] || ''
    if (!contentType.includes('application/pdf')) {
      const message = await response.data.text().then((text) => {
        try {
          return JSON.parse(text)?.message || '回执 PDF 导出失败'
        } catch (error) {
          return '回执 PDF 导出失败'
        }
      })
      throw new Error(message)
    }
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = responseFileName(response, receiptFallbackFileName(row))
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('回执 PDF 已开始下载')
  } catch (error) {
    ElMessage.error(error?.message || '回执 PDF 导出失败')
  }
}

async function downloadExportTask() {
  if (!exportTask.value?.id || exportTaskDownloading.value) {
    return
  }
  exportTaskDownloading.value = true
  try {
    const result = await createWorkOrderExportDownloadTicket(exportTask.value.id)
    const link = document.createElement('a')
    link.href = result.data.downloadUrl
    document.body.appendChild(link)
    link.click()
    link.remove()
    ElMessage.success('ZIP 文件已开始下载')
  } catch (error) {
    ElMessage.error(error?.message || 'ZIP 文件下载失败')
  } finally {
    exportTaskDownloading.value = false
  }
}

function hasMoreActions(row) {
  return (row.status === 'pending' && hasPermission('work-order:status'))
    || (['pending', 'processing'].includes(row.status) && hasPermission('work-order:complete'))
    || (!['completed', 'closed'].includes(row.status) && hasPermission('work-order:status'))
    || hasPermission('work-order:delete')
}

function openDetail(row) {
  router.push({
    path: `/work-orders/${row.id}`,
    query: currentListQuery(),
  })
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

async function voidOrder(row) {
  const { value } = await ElMessageBox.prompt(
    `确认作废工单“${row.orderNo}”？作废后将保留历史记录。`,
    '作废工单',
    {
      confirmButtonText: '确认作废',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入作废原因',
      inputValidator: (value) => Boolean(value && value.trim()),
      inputErrorMessage: '请填写作废原因',
      type: 'warning',
    },
  )
  await voidWorkOrder(row.id, value.trim())
  ElMessage.success('工单已作废')
  await loadWorkOrders()
}

async function deleteOrder(row) {
  await ElMessageBox.confirm(
    `确认永久删除工单“${row.orderNo}”？删除后不可恢复，相关指派、流程记录和附件记录也会同步删除。`,
    '删除工单',
    {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    },
  )
  await deleteWorkOrder(row.id)
  ElMessage.success('工单已删除')
  await loadWorkOrders()
}

watch(
  () => form.type,
  () => handleTypeChange(),
)

onMounted(async () => {
  applyRouteFilters()
  await Promise.all([searchCustomers(), searchDevices(), loadEngineers()])
  await loadWorkOrders()
})

onUnmounted(() => {
  stopExportTaskPolling()
})
</script>

<template>
  <section class="page-header">
    <div>
      <h1>工单管理</h1>
      <p>创建现场工单和巡检工单，并指派一名或多名现场实施工程师</p>
    </div>
    <div class="header-actions">
      <template v-if="batchExportMode">
        <span class="batch-selection-count">已选择 {{ selectedExportCount }} 条已完成工单</span>
        <el-button
          type="primary"
          :icon="Download"
          :disabled="!selectedExportCount"
          :loading="exportTaskCreating"
          @click="exportSelectedOrders"
        >
          导出已选
        </el-button>
        <el-button @click="cancelBatchExportMode">取消</el-button>
      </template>
      <el-button
        v-else-if="hasPermission('work-order:receipt-batch-export')"
        :icon="Download"
        @click="startBatchExportMode"
      >
        批量导出
      </el-button>
      <el-button v-if="hasPermission('work-order:create')" type="primary" :icon="Plus" @click="openCreate">新增工单</el-button>
    </div>
  </section>

  <section class="work-order-summary-grid">
    <button
      v-for="item in summaryCards"
      :key="item.label"
      type="button"
      class="work-order-summary-card"
      :class="`tone-${item.tone}`"
      @click="selectSummaryStatus(item.status)"
    >
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </button>
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
    <div class="date-filter">
      <span>创建</span>
      <el-date-picker
        v-model="filters.createdRange"
        class="date-range"
        type="daterange"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        range-separator="至"
      />
    </div>
    <div class="date-filter">
      <span>完成</span>
      <el-date-picker
        v-model="filters.completedRange"
        class="date-range"
        type="daterange"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        range-separator="至"
      />
    </div>
    <el-button type="primary" :icon="Search" @click="search">查询</el-button>
    <el-button :icon="Refresh" @click="loadWorkOrders">刷新</el-button>
    <el-tag v-if="filters.customerSiteId" closable effect="plain" @close="clearCustomerSiteFilter">
      客户：{{ filters.customerSiteName || filters.customerSiteId }}
    </el-tag>
    <el-tag v-if="filters.engineerId" closable effect="plain" @close="clearEngineerFilter">
      工程师：{{ filters.engineerName || filters.engineerId }}
    </el-tag>
  </section>

  <section class="table-panel">
    <el-table
      ref="workOrderTable"
      v-loading="loading"
      :data="workOrders"
      row-key="id"
      height="calc(100vh - 284px)"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="batchExportMode" type="selection" width="52" fixed :selectable="isExportSelectable" reserve-selection />
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
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="hasPermission('work-order:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-dropdown v-if="hasMoreActions(row) || (row.status === 'completed' && hasPermission('work-order:receipt-export'))" class="row-more" trigger="click">
              <el-button link type="primary" class="more-trigger">更多<el-icon class="more-icon"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-if="row.status === 'completed' && hasPermission('work-order:receipt-export')"
                  @click="exportReceipt(row)"
                >
                  导出回执 PDF
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="row.status === 'pending' && hasPermission('work-order:status')"
                  @click="changeStatus(row, 'processing')"
                >
                  开始处理
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="['pending', 'processing'].includes(row.status) && hasPermission('work-order:complete')"
                  @click="changeStatus(row, 'completed')"
                >
                  完成工单
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="!['completed', 'closed'].includes(row.status) && hasPermission('work-order:status')"
                  divided
                  @click="voidOrder(row)"
                >
                  作废工单
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="hasPermission('work-order:delete')"
                  :divided="['completed', 'closed'].includes(row.status) || hasPermission('work-order:status')"
                  class="danger-item"
                  @click="deleteOrder(row)"
                >
                  删除工单
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.size"
        :page-sizes="[10, 20, 50, 100]"
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
              <el-option v-for="item in editableStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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

    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveWorkOrder">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="exportTaskDialogVisible" title="工单回执导出" width="480px" :close-on-click-modal="false">
    <div v-if="exportTask" class="export-task-panel">
      <div class="export-task-header">
        <span>{{ exportTask.taskNo }}</span>
        <el-tag :type="taskStatusType(exportTask.status)" effect="light">{{ taskStatusLabel(exportTask.status) }}</el-tag>
      </div>
      <el-progress :percentage="exportProgress" :status="exportTask.status === 'failed' ? 'exception' : exportTask.status === 'success' ? 'success' : undefined" />
      <div class="export-task-summary">
        <span>共 {{ exportTask.totalCount }} 条</span>
        <span>成功 {{ exportTask.successCount || 0 }} 条</span>
        <span>失败 {{ exportTask.failedCount || 0 }} 条</span>
      </div>
      <p v-if="exportTask.status === 'success'" class="export-task-note">
        ZIP 大小：{{ formatFileSize(exportTask.fileSize) }}，文件将在 {{ exportTask.expireAt }} 后自动清理。
      </p>
      <p v-if="exportTask.errorMessage" class="export-task-error">{{ exportTask.errorMessage }}</p>
    </div>
    <template #footer>
      <el-button @click="exportTaskDialogVisible = false">关闭</el-button>
      <el-button
        v-if="exportTask?.status === 'success'"
        type="primary"
        :icon="Download"
        :loading="exportTaskDownloading"
        @click="downloadExportTask"
      >
        下载 ZIP
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.work-order-filter {
  width: 140px;
}

.work-order-summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 16px;
}

.work-order-summary-card {
  display: flex;
  min-height: 78px;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
  color: #667085;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s, transform .2s;
}

.work-order-summary-card:hover {
  border-color: #b7d0f7;
  box-shadow: 0 5px 16px rgba(47, 128, 237, .08);
  transform: translateY(-1px);
}

.work-order-summary-card strong {
  color: #172033;
  font-size: 24px;
  line-height: 1;
}

.work-order-summary-card.tone-orange strong { color: #e88b2c; }
.work-order-summary-card.tone-cyan strong { color: #159a9c; }
.work-order-summary-card.tone-green strong { color: #30a46c; }
.work-order-summary-card.tone-gray strong { color: #667085; }

.header-actions,
.date-filter,
.export-task-header,
.export-task-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-filter {
  color: #667085;
  font-size: 13px;
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

.row-actions {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}

.row-actions :deep(.el-button + .el-button),
.row-more {
  margin-left: 12px;
}

.row-more,
.more-trigger {
  display: inline-flex;
  align-items: center;
}

.more-trigger {
  height: auto;
  padding: 0;
}

.more-icon {
  margin-left: 2px;
  font-size: 12px;
}

.export-task-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.export-task-header {
  justify-content: space-between;
  color: #344054;
  font-size: 13px;
}

.export-task-summary {
  color: #667085;
  font-size: 13px;
}

.export-task-note,
.export-task-error {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}

.export-task-note {
  color: #667085;
}

.export-task-error {
  color: #d92d20;
}

:deep(.danger-item) {
  color: #d92d20;
}

@media (max-width: 900px) {
  .work-order-summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .work-order-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>

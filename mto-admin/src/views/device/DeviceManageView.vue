<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDevice, getDevicePage, updateDevice, updateDeviceStatus } from '../../api/device'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const devices = ref([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})

const form = reactive({
  name: '',
  model: '',
  serialNo: '',
  status: 1,
})

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    model: '',
    serialNo: '',
    status: 1,
  })
}

function buildPayload() {
  return {
    name: form.name,
    model: form.model,
    serialNo: form.serialNo,
    status: form.status,
  }
}

async function loadDevices() {
  loading.value = true
  try {
    const result = await getDevicePage({
      keyword: filters.keyword || undefined,
      status: filters.status === '' ? undefined : filters.status,
      page: filters.page,
      size: filters.size,
    })
    devices.value = result.data?.records || []
    total.value = result.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadDevices()
}

async function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name || '',
    model: row.model || '',
    serialNo: row.serialNo || '',
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

async function saveDevice() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写设备名称')
    return
  }

  if (editingId.value) {
    await updateDevice(editingId.value, buildPayload())
    ElMessage.success('设备已更新')
  } else {
    await createDevice(buildPayload())
    ElMessage.success('设备已新增')
  }
  dialogVisible.value = false
  await loadDevices()
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText}“${row.name}”？`, '操作确认', {
    type: 'warning',
  })
  await updateDeviceStatus(row.id, nextStatus)
  ElMessage.success(`${actionText}成功`)
  await loadDevices()
}

onMounted(async () => {
  await loadDevices()
})
</script>

<template>
  <section class="page-header">
    <div>
      <h1>设备管理</h1>
      <p>维护独立设备档案，供现场工单选择设备</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="openCreate">新增设备</el-button>
  </section>

  <section class="toolbar">
    <el-input
      v-model="filters.keyword"
      class="keyword"
      clearable
      placeholder="搜索设备、型号、编号或客户"
      :prefix-icon="Search"
      @keyup.enter="search"
    />
    <el-select v-model="filters.status" class="status-filter" placeholder="状态" clearable>
      <el-option label="启用" :value="1" />
      <el-option label="停用" :value="0" />
    </el-select>
    <el-button type="primary" :icon="Search" @click="search">查询</el-button>
    <el-button :icon="Refresh" @click="loadDevices">刷新</el-button>
  </section>

  <section class="table-panel">
    <el-table v-loading="loading" :data="devices" row-key="id" height="calc(100vh - 284px)">
      <el-table-column prop="name" label="设备名称" min-width="180" fixed />
      <el-table-column prop="model" label="型号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="serialNo" label="编号/序列号" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
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
        @change="loadDevices"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" :title="editingId ? '编辑设备' : '新增设备'" width="720px" destroy-on-close>
    <el-form label-position="top" class="device-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="设备名称" required>
            <el-input v-model="form.name" placeholder="例如摄像头、交换机、传感器" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="设备型号">
            <el-input v-model="form.model" placeholder="设备型号" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="编号/序列号">
            <el-input v-model="form.serialNo" placeholder="设备编号或序列号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveDevice">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.device-form .el-form-item {
  margin-bottom: 16px;
}

.full-control {
  width: 100%;
}
</style>

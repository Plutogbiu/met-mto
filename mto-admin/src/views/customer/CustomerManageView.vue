<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Edit, Location, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCustomerSite,
  getCustomerSitePage,
  updateCustomerSite,
  updateCustomerSiteStatus,
} from '../../api/customerSite'
import { getMapConfig, getPlaceSuggestions } from '../../api/map'

const loading = ref(false)
const router = useRouter()
const dialogVisible = ref(false)
const mapDialogVisible = ref(false)
const editingId = ref(null)
const mapConfig = ref(null)
const mapContainerRef = ref(null)
const customerSites = ref([])
const mapLoading = ref(false)
const mapSearchLoading = ref(false)
const mapPlaceResults = ref([])
const selectedPlace = ref(null)
const total = ref(0)

let mapInstance = null
let markerInstance = null
let tencentMapScriptPromise = null

const filters = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10,
})

const form = reactive({
  name: '',
  address: '',
  contactName: '',
  contactPhone: '',
  longitude: '',
  latitude: '',
  locationAddress: '',
  locationRemark: '',
  remark: '',
  status: 1,
})

const mapSearchForm = reactive({
  keyword: '',
  region: '全国',
})

const dialogTitle = computed(() => (editingId.value ? '编辑客户' : '新增客户'))
const currentCoordinate = computed(() => {
  if (!form.longitude || !form.latitude) {
    return '未选择'
  }
  return `${form.longitude}, ${form.latitude}`
})
const selectedPlaceAddress = computed(() => {
  if (!selectedPlace.value) {
    return '未选择位置'
  }
  return selectedPlace.value.address || selectedPlace.value.title
})

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    address: '',
    contactName: '',
    contactPhone: '',
    longitude: '',
    latitude: '',
    locationAddress: '',
    locationRemark: '',
    remark: '',
    status: 1,
  })
}

function buildPayload() {
  return {
    ...form,
    longitude: form.longitude === '' ? null : Number(form.longitude),
    latitude: form.latitude === '' ? null : Number(form.latitude),
  }
}

async function openMapPicker() {
  mapSearchForm.keyword = form.name || form.address || form.locationAddress || ''
  mapSearchForm.region = '全国'
  selectedPlace.value = buildSelectedPlaceFromForm()
  mapPlaceResults.value = []
  mapDialogVisible.value = true
  await nextTick()
  initTencentMap()
  if (mapSearchForm.keyword) {
    searchMapPlaces()
  }
}

async function ensureMapConfig() {
  if (mapConfig.value) {
    return mapConfig.value
  }
  const result = await getMapConfig()
  mapConfig.value = result.data
  return mapConfig.value
}

async function loadTencentMapScript() {
  if (window.TMap) {
    return
  }
  const config = await ensureMapConfig()
  if (!config?.key) {
    throw new Error('腾讯地图 Key 未配置')
  }
  if (!tencentMapScriptPromise) {
    tencentMapScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `https://map.qq.com/api/gljs?v=1.exp&key=${encodeURIComponent(config.key)}`
      script.async = true
      script.onload = resolve
      script.onerror = () => reject(new Error('腾讯地图加载失败'))
      document.head.appendChild(script)
    })
  }
  await tencentMapScriptPromise
}

async function initTencentMap() {
  mapLoading.value = true
  try {
    await loadTencentMapScript()
    await nextTick()
    if (!mapContainerRef.value || !window.TMap) {
      return
    }
    destroyTencentMap()
    const center = toTencentLatLng(selectedPlace.value) || new window.TMap.LatLng(39.904690, 116.407170)
    mapInstance = new window.TMap.Map(mapContainerRef.value, {
      center,
      zoom: selectedPlace.value ? 16 : 11,
    })
    updateMapMarker(center)
    mapInstance.on('click', (event) => {
      pickMapCoordinate(event.latLng)
    })
  } catch (error) {
    ElMessage.warning(error.message || '腾讯地图加载失败，请使用搜索结果选址')
  } finally {
    mapLoading.value = false
  }
}

function toTencentLatLng(place) {
  if (!place?.longitude || !place?.latitude || !window.TMap) {
    return null
  }
  return new window.TMap.LatLng(Number(place.latitude), Number(place.longitude))
}

function updateMapMarker(position) {
  if (!mapInstance || !position || !window.TMap) {
    return
  }
  if (markerInstance?.setMap) {
    markerInstance.setMap(null)
  }
  markerInstance = new window.TMap.MultiMarker({
    map: mapInstance,
    geometries: [
      {
        id: 'selected-site',
        position,
      },
    ],
  })
}

function pickMapCoordinate(latLng) {
  if (!latLng) {
    return
  }
  const latitude = Number(readMapLatitude(latLng)).toFixed(6)
  const longitude = Number(readMapLongitude(latLng)).toFixed(6)
  selectedPlace.value = {
    title: selectedPlace.value?.title || mapSearchForm.keyword || '地图选点',
    address: selectedPlace.value?.address || '',
    longitude,
    latitude,
  }
  updateMapMarker(latLng)
}

function readMapLatitude(latLng) {
  return typeof latLng.getLat === 'function' ? latLng.getLat() : latLng.lat
}

function readMapLongitude(latLng) {
  return typeof latLng.getLng === 'function' ? latLng.getLng() : latLng.lng
}

function destroyTencentMap() {
  if (markerInstance?.setMap) {
    markerInstance.setMap(null)
  }
  markerInstance = null
  if (mapInstance?.destroy) {
    mapInstance.destroy()
  }
  mapInstance = null
}

function buildSelectedPlaceFromForm() {
  if (!form.longitude || !form.latitude) {
    return null
  }
  return {
    title: form.name || form.locationAddress || form.address,
    address: form.locationAddress || form.address,
    longitude: form.longitude,
    latitude: form.latitude,
  }
}

async function searchMapPlaces() {
  const keyword = mapSearchForm.keyword.trim()
  if (!keyword) {
    ElMessage.warning('请输入要搜索的客户地址或地点名称')
    return
  }
  mapSearchLoading.value = true
  try {
    const result = await getPlaceSuggestions({
      keyword,
      region: mapSearchForm.region || '全国',
    })
    mapPlaceResults.value = result.data || []
    if (!mapPlaceResults.value.length) {
      ElMessage.warning('未搜索到相关位置')
    }
  } finally {
    mapSearchLoading.value = false
  }
}

function selectMapPlace(place) {
  if (!place.longitude || !place.latitude) {
    ElMessage.warning('该位置缺少坐标信息')
    return
  }
  selectedPlace.value = {
    ...place,
    longitude: Number(place.longitude).toFixed(6),
    latitude: Number(place.latitude).toFixed(6),
  }
  const position = toTencentLatLng(selectedPlace.value)
  if (position && mapInstance) {
    mapInstance.setCenter(position)
    mapInstance.setZoom(16)
    updateMapMarker(position)
  }
}

function confirmMapPlace() {
  if (!selectedPlace.value?.longitude || !selectedPlace.value?.latitude) {
    ElMessage.warning('请先选择一个带坐标的位置')
    return
  }
  const address = selectedPlace.value.address || selectedPlace.value.title
  form.longitude = selectedPlace.value.longitude
  form.latitude = selectedPlace.value.latitude
  form.address = address
  form.locationAddress = address
  if (!form.name && selectedPlace.value.title) {
    form.name = selectedPlace.value.title
  }
  mapDialogVisible.value = false
  ElMessage.success('定位坐标已更新')
}

async function loadCustomerSites() {
  loading.value = true
  try {
    const result = await getCustomerSitePage({
      keyword: filters.keyword || undefined,
      status: filters.status === '' ? undefined : filters.status,
      page: filters.page,
      size: filters.size,
    })
    customerSites.value = result.data?.records || []
    total.value = result.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadCustomerSites()
}

function openDetail(row) {
  router.push(`/customers/${row.id}`)
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name || '',
    address: row.address || '',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    longitude: row.longitude ?? '',
    latitude: row.latitude ?? '',
    locationAddress: row.locationAddress || '',
    locationRemark: row.locationRemark || '',
    remark: row.remark || '',
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

async function saveCustomerSite() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写客户名称')
    return
  }

  if (editingId.value) {
    await updateCustomerSite(editingId.value, buildPayload())
    ElMessage.success('客户已更新')
  } else {
    await createCustomerSite(buildPayload())
    ElMessage.success('客户已新增')
  }
  dialogVisible.value = false
  await loadCustomerSites()
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText}“${row.name}”？`, '操作确认', {
    type: 'warning',
  })
  await updateCustomerSiteStatus(row.id, nextStatus)
  ElMessage.success(`${actionText}成功`)
  await loadCustomerSites()
}

onMounted(() => {
  loadCustomerSites()
})
</script>

<template>
  <section class="page-header">
    <div>
      <h1>客户管理</h1>
      <p>维护客户现场、联系人、坐标和导航所需的基础档案</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="openCreate">新增客户</el-button>
  </section>

  <section class="toolbar">
    <el-input
      v-model="filters.keyword"
      class="keyword"
      clearable
      placeholder="搜索客户名称"
      :prefix-icon="Search"
      @keyup.enter="search"
    />
    <el-select v-model="filters.status" class="status-filter" placeholder="状态" clearable>
      <el-option label="启用" :value="1" />
      <el-option label="停用" :value="0" />
    </el-select>
    <el-button type="primary" :icon="Search" @click="search">查询</el-button>
    <el-button :icon="Refresh" @click="loadCustomerSites">刷新</el-button>
  </section>

  <section class="table-panel">
    <el-table v-loading="loading" :data="customerSites" row-key="id" height="calc(100vh - 284px)">
      <el-table-column label="客户名称" min-width="180" fixed>
        <template #default="{ row }">
          <el-button link type="primary" class="name-link" @click="openDetail(row)">{{ row.name }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="260" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="150" />
      <el-table-column label="坐标" min-width="190">
        <template #default="{ row }">
          <span v-if="row.longitude && row.latitude">{{ row.longitude }}, {{ row.latitude }}</span>
          <span v-else class="muted">未维护</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
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
        @change="loadCustomerSites"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="780px" destroy-on-close>
    <el-form label-position="top" class="customer-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="客户名称" required>
            <el-input v-model="form.name" placeholder="请输入客户名称" />
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

      <el-form-item label="详细地址">
        <el-input v-model="form.address" placeholder="选址后自动填充，也可手动微调" />
      </el-form-item>

      <section class="location-card">
        <div>
          <span>定位坐标</span>
          <strong>{{ currentCoordinate }}</strong>
          <p>{{ form.locationAddress || '通过地图选择客户现场入口、矿门口或实际打卡点' }}</p>
        </div>
        <el-button type="primary" :icon="Location" plain @click="openMapPicker">地图选址</el-button>
      </section>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="现场联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" placeholder="联系电话" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="经度">
            <el-input v-model="form.longitude" placeholder="选址后自动填充" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="纬度">
            <el-input v-model="form.latitude" placeholder="选址后自动填充" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="定位地址">
        <el-input v-model="form.locationAddress" placeholder="地图解析或手动维护的地址" />
      </el-form-item>

      <el-form-item label="定位备注">
        <el-input v-model="form.locationRemark" placeholder="例如矿门口、办公楼、调度室" />
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="特殊说明" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveCustomerSite">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="mapDialogVisible"
    title="地图选址"
    width="920px"
    class="map-picker-dialog"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <section class="map-picker">
      <div class="map-search-bar">
        <el-input
          v-model="mapSearchForm.keyword"
          clearable
          placeholder="输入客户名称、矿区名称或详细地址"
          :prefix-icon="Search"
          @keyup.enter="searchMapPlaces"
        />
        <el-input v-model="mapSearchForm.region" class="map-region" placeholder="搜索城市/区域" />
        <el-button type="primary" :icon="Search" :loading="mapSearchLoading" @click="searchMapPlaces">
          搜索
        </el-button>
      </div>

      <div class="map-picker-body">
        <div class="map-result-list" v-loading="mapSearchLoading">
          <button
            v-for="place in mapPlaceResults"
            :key="`${place.title}-${place.longitude}-${place.latitude}`"
            type="button"
            class="map-result-item"
            :class="{ active: selectedPlace?.longitude === Number(place.longitude).toFixed(6) && selectedPlace?.latitude === Number(place.latitude).toFixed(6) }"
            @click="selectMapPlace(place)"
          >
            <strong>{{ place.title }}</strong>
            <span>{{ place.address || '暂无详细地址' }}</span>
            <small>{{ [place.province, place.city, place.district].filter(Boolean).join(' / ') }}</small>
          </button>
          <el-empty v-if="!mapSearchLoading && !mapPlaceResults.length" description="搜索客户地址后选择位置" />
        </div>

        <div class="map-canvas-panel" v-loading="mapLoading">
          <div ref="mapContainerRef" class="map-canvas"></div>
        </div>

        <aside class="map-selected-panel">
          <span>已选位置</span>
          <strong>{{ selectedPlace?.title || '未选择' }}</strong>
          <p>{{ selectedPlaceAddress }}</p>

          <el-form v-if="selectedPlace" label-position="top" class="map-coordinate-form">
            <el-form-item label="经度">
              <el-input v-model="selectedPlace.longitude" placeholder="选择后自动填充" />
            </el-form-item>
            <el-form-item label="纬度">
              <el-input v-model="selectedPlace.latitude" placeholder="选择后自动填充" />
            </el-form-item>
            <el-form-item label="定位地址">
              <el-input v-model="selectedPlace.address" placeholder="可手动微调地址" />
            </el-form-item>
          </el-form>
          <el-empty v-else description="请先在左侧选择位置" />
        </aside>
      </div>
    </section>

    <template #footer>
      <el-button @click="mapDialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedPlace" @click="confirmMapPlace">确认选址</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.map-picker {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.map-search-bar {
  display: grid;
  grid-template-columns: 1fr 150px auto;
  gap: 12px;
}

.map-picker-body {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr) 280px;
  gap: 16px;
  min-height: 420px;
}

.map-result-list {
  min-height: 420px;
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.map-canvas-panel {
  min-height: 420px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #eef2f7;
}

.map-canvas {
  width: 100%;
  height: 420px;
}

.map-result-item {
  display: block;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid #eef0f3;
  background: #ffffff;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
}

.map-result-item:hover,
.map-result-item.active {
  background: #eef6ff;
}

.map-result-item strong,
.map-result-item span,
.map-result-item small {
  display: block;
}

.map-result-item strong {
  margin-bottom: 6px;
  font-size: 14px;
}

.map-result-item span {
  color: #4b5563;
  line-height: 1.5;
}

.map-result-item small {
  margin-top: 6px;
  color: #8a94a6;
}

.map-selected-panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.map-selected-panel > span {
  color: #8a94a6;
  font-size: 13px;
}

.map-selected-panel > strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 16px;
}

.map-selected-panel > p {
  min-height: 44px;
  margin: 8px 0 16px;
  color: #4b5563;
  line-height: 1.6;
}

.map-coordinate-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

@media (max-width: 860px) {
  .map-search-bar,
  .map-picker-body {
    grid-template-columns: 1fr;
  }

  .map-region {
    width: 100%;
  }
}
</style>

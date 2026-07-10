<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, Picture, Refresh } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { getAttachments } from '../../api/attachment'
import { getWorkOrder, getWorkOrderRecords } from '../../api/workOrder'
import { el } from 'element-plus/es/locale/index.mjs'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const workOrder = ref(null)
const records = ref([])
const attachments = ref([])

const typeOptions = [
  { label: '现场工单', value: 'onsite', tag: 'primary' },
  { label: '日常巡检', value: 'inspection', tag: 'success' },
]

const priorityOptions = [
  { label: '普通', value: 'normal', tag: 'info' },
  { label: '紧急', value: 'urgent', tag: 'danger' },
]

const statusOptions = [
  { label: '待处理', value: 'pending', tag: 'info' },
  { label: '处理中', value: 'processing', tag: 'warning' },
  { label: '已完成', value: 'completed', tag: 'success' },
  { label: '已作废', value: 'closed', tag: 'info' },
]

const maintenanceContentOptions = [
  { label: '保内免费', value: 'warranty_free' },
  { label: '保外收费', value: 'out_warranty_paid' },
  { label: '保外免费', value: 'out_warranty_free' },
  { label: '保内收费', value: 'warranty_paid' },
]

const recordTypeMap = {
  create: '创建工单',
  update: '更新工单',
  status: '状态变更',
  void: '作废工单',
  process: '处理记录',
}

const attachmentCategoryOptions = {
  onsite: [
    { label: '原故障图片', value: 'fault_before' },
    { label: '处理后图片', value: 'fault_after' },
    { label: '现场处理图片', value: 'site_process' },
    { label: '工单处理回执', value: 'work_receipt' },
  ],
  inspection: [
    { label: '巡检图片', value: 'inspection_photo' },
    { label: '巡检处理回执', value: 'inspection_receipt' },
  ],
}

const attachmentGroups = computed(() => {
  const type = workOrder.value?.type || 'onsite'
  return (attachmentCategoryOptions[type] || attachmentCategoryOptions.onsite).map((group) => ({
    ...group,
    files: attachments.value.filter((item) => item.category === group.value),
  }))
})

const uncategorizedAttachments = computed(() => {
  const categoryValues = new Set((attachmentCategoryOptions[workOrder.value?.type] || []).map((item) => item.value))
  return attachments.value.filter((item) => !item.category || !categoryValues.has(item.category))
})

const allPreviewUrls = computed(() => attachments.value.map((item) => item.url).filter(Boolean))

function optionLabel(options, value) {
  return options.find((item) => item.value === value)?.label || value || '-'
}

function optionTag(options, value) {
  return options.find((item) => item.value === value)?.tag || 'info'
}

function recordTypeLabel(type) {
  return recordTypeMap[type] || type || '-'
}

function hasRecordLocation(record) {
  return Boolean(record?.longitude && record?.latitude)
}

function tencentMapUrl(record) {
  if (!hasRecordLocation(record)) {
    return ''
  }
  const title = encodeURIComponent('工单打卡位置')
  const address = encodeURIComponent(record.locationAddress || workOrder.value?.customerSiteName || '')
  return `https://apis.map.qq.com/uri/v1/marker?marker=coord:${record.latitude},${record.longitude};title:${title};addr:${address}&referer=MET-MTO`
}

async function loadDetail() {
  loading.value = true
  try {
    const id = route.params.id
    const [orderResult, recordResult, attachmentResult] = await Promise.all([
      getWorkOrder(id),
      getWorkOrderRecords(id),
      getAttachments({ bizType: 'work_order', bizId: id }),
    ])
    workOrder.value = orderResult.data
    records.value = recordResult.data || []
    attachments.value = attachmentResult.data || []
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
      <el-button :icon="ArrowLeft" @click="router.push('/work-orders')">返回</el-button>
      <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
    </div>
  </section>

  <section v-loading="loading" class="detail-layout">
    <div class="detail-main">
      <section class="detail-panel">
        <div class="panel-title">
          <div>
            <h1>{{ workOrder?.orderNo || '工单详情' }}</h1>
            <p>{{ workOrder?.customerSiteName || '-' }}</p>
          </div>
          <el-tag :type="optionTag(statusOptions, workOrder?.status)" effect="light">
            {{ optionLabel(statusOptions, workOrder?.status) }}
          </el-tag>
        </div>

        <el-descriptions v-if="workOrder" :column="2" border>
          <el-descriptions-item label="类型">
            <el-tag :type="optionTag(typeOptions, workOrder.type)" effect="light">
              {{ optionLabel(typeOptions, workOrder.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="optionTag(priorityOptions, workOrder.priority)" effect="light">
              {{ optionLabel(priorityOptions, workOrder.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="设备">{{ workOrder.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计到达">{{ workOrder.estimatedArrivalTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预估完成">{{ workOrder.estimatedCompleteTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ workOrder.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ workOrder.completedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户地址" :span="2">{{ workOrder.customerAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维保内容" :span="2">
            <el-tag :type="optionTag(maintenanceContentOptions, workOrder.maintenanceContent)" effect="light">
              {{ optionLabel(maintenanceContentOptions, workOrder.maintenanceContent) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="工单内容" :span="2">{{ workOrder.content || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注意事项" :span="2">{{ workOrder.notice || '-' }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="detail-panel">
        <div class="panel-title compact">
          <h2>图片资料审查</h2>
          <span class="panel-count">{{ attachments.length }} 张</span>
        </div>
        <div class="attachment-groups">
          <div v-for="group in attachmentGroups" :key="group.value" class="attachment-group">
            <div class="group-title">
              <span>{{ group.label }}</span>
              <small>{{ group.files.length }} 张</small>
            </div>
            <div v-if="group.files.length" class="image-grid">
              <el-image
                v-for="attachment in group.files"
                :key="attachment.id"
                :src="attachment.url"
                :preview-src-list="allPreviewUrls"
                fit="cover"
                class="attachment-image"
              />
            </div>
            <div v-else class="empty-group">
              <el-icon><Picture /></el-icon>
              <span>暂无图片</span>
            </div>
          </div>

          <div v-if="uncategorizedAttachments.length" class="attachment-group">
            <div class="group-title">
              <span>未分类附件</span>
              <small>{{ uncategorizedAttachments.length }} 个</small>
            </div>
            <div class="image-grid">
              <el-image
                v-for="attachment in uncategorizedAttachments"
                :key="attachment.id"
                :src="attachment.url"
                :preview-src-list="allPreviewUrls"
                fit="cover"
                class="attachment-image"
              />
            </div>
          </div>
        </div>
      </section>
    </div>

    <aside class="detail-side">
      <section class="detail-panel">
        <div class="panel-title compact">
          <h2>指派工程师</h2>
        </div>
        <div class="engineer-list">
          <el-tag v-for="engineer in workOrder?.engineers || []" :key="engineer.userId" effect="plain">
            {{ engineer.realName || engineer.username }}
          </el-tag>
          <span v-if="!workOrder?.engineers?.length" class="muted">暂无</span>
        </div>
      </section>

      <section class="detail-panel">
        <div class="panel-title compact">
          <h2>流程与处理记录</h2>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="record in records"
            :key="record.id"
            :timestamp="record.createdAt"
            placement="top"
          >
            <div class="record-item">
              <strong>{{ recordTypeLabel(record.recordType) }}</strong>
              <p v-if="record.statusBefore || record.statusAfter" class="record-status">
                {{ optionLabel(statusOptions, record.statusBefore) }} -> {{ optionLabel(statusOptions, record.statusAfter) }}
              </p>
              <p v-if="record.content">{{ record.content }}</p>
              <div v-if="hasRecordLocation(record)" class="checkin-location">
                <div>
                  <strong>打卡位置</strong>
                  <p>{{ record.locationAddress || '暂无定位地址' }}</p>
                  <small>{{ record.latitude }}, {{ record.longitude }}</small>
                </div>
                <el-link type="primary" :href="tencentMapUrl(record)" target="_blank">
                  打开地图
                </el-link>
              </div>
              <small>{{ record.operatorName || '系统' }}</small>
              <div v-if="record.attachments?.length" class="record-images">
                <el-image
                  v-for="attachment in record.attachments"
                  :key="attachment.id"
                  :src="attachment.url"
                  :preview-src-list="record.attachments.map((item) => item.url)"
                  fit="cover"
                  class="record-image"
                />
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </section>
    </aside>
  </section>
</template>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 16px;
}

.detail-main,
.detail-side {
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

.panel-count {
  color: #667085;
  font-size: 13px;
}

.attachment-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.attachment-group {
  min-height: 132px;
  padding: 12px;
  border: 1px solid #eef0f4;
  border-radius: 8px;
  background: #fbfcfe;
}

.group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #1f2937;
  font-weight: 600;
}

.group-title small {
  color: #98a2b3;
  font-weight: 400;
}

.image-grid,
.record-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-image {
  width: 88px;
  height: 88px;
  border-radius: 6px;
}

.empty-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 88px;
  color: #98a2b3;
  border: 1px dashed #d0d5dd;
  border-radius: 6px;
  background: #ffffff;
}

.engineer-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.muted {
  color: #98a2b3;
}

.record-item p {
  margin: 8px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.record-item small,
.record-status {
  color: #98a2b3;
}

.checkin-location {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.checkin-location strong,
.checkin-location p,
.checkin-location small {
  display: block;
}

.checkin-location strong {
  color: #172033;
  font-size: 13px;
}

.checkin-location p {
  margin: 4px 0 2px;
  color: #4b5563;
  font-size: 13px;
}

.record-images {
  margin-top: 10px;
}

.record-image {
  width: 78px;
  height: 78px;
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .attachment-groups {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <view class="page mto-page detail-page">
    <view class="nav-row">
      <view class="back-button" @click="goBack">
        <u-icon name="arrow-left" color="#172033" size="20" />
      </view>
      <view class="nav-title">工单详情</view>
      <view class="nav-placeholder" />
    </view>

    <view class="summary-card" v-if="detail">
      <view class="summary-head">
        <view class="summary-main">
          <view class="customer-name">{{ detail.customerSiteName || '未命名客户' }}</view>
          <view class="order-no">{{ detail.orderNo }}</view>
        </view>
        <view class="status-badge" :class="statusClass(detail.status)">
          {{ statusText(detail.status) }}
        </view>
      </view>

      <view class="address-row">
        <u-icon name="map" color="#8A94A6" size="17" />
        <text>{{ detail.customerAddress || '暂无地址' }}</text>
      </view>

      <view class="tag-row">
        <view class="tag-item">
          <u-icon name="file-text" color="#165DFF" size="17" />
          <text>{{ typeText(detail.type) }}</text>
        </view>
        <view class="tag-item">
          <u-icon name="camera" color="#15B8A6" size="17" />
          <text>{{ detail.deviceName || '无设备' }}</text>
        </view>
      </view>
    </view>

    <view class="process-panel" v-if="detail && !isFinished">
      <view class="panel-title">现场处理</view>
      <view class="process-list">
        <view class="process-item active" @click="goCheckin">
          <view class="process-icon blue">
            <u-icon name="map-fill" color="#165DFF" size="22" />
          </view>
          <view class="process-text">
            <view class="process-title">现场打卡</view>
            <view class="process-desc">到达客户现场后记录当前位置</view>
          </view>
          <u-icon name="arrow-right" color="#B4BDCB" size="15" />
        </view>
        <view class="process-item" @click="goUpload">
          <view class="process-icon cyan">
            <u-icon name="photo" color="#15B8A6" size="22" />
          </view>
          <view class="process-text">
            <view class="process-title">上传附件</view>
            <view class="process-desc">{{ uploadDesc }}</view>
          </view>
          <u-icon name="arrow-right" color="#B4BDCB" size="15" />
        </view>
      </view>
    </view>

    <view class="panel" v-if="detail">
      <view class="panel-title">基本信息</view>
      <view class="info-list">
        <view class="info-row">
          <text class="info-label">工单类型</text>
          <text class="info-value">{{ typeText(detail.type) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">优先级</text>
          <text class="info-value" :class="{ urgent: detail.priority === 'urgent' }">
            {{ priorityText(detail.priority) }}
          </text>
        </view>
        <view class="info-row">
          <text class="info-label">维保内容</text>
          <text class="info-value">{{ maintenanceContentText(detail.maintenanceContent) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">预计到达</text>
          <text class="info-value">{{ formatTime(detail.estimatedArrivalTime) }}</text>
        </view>
        <view v-if="detail.type === 'onsite'" class="info-row">
          <text class="info-label">预计完成</text>
          <text class="info-value">{{ formatTime(detail.estimatedCompleteTime) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">创建时间</text>
          <text class="info-value">{{ formatTime(detail.createdAt) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">参与工程师</text>
          <text class="info-value">{{ engineersText }}</text>
        </view>
      </view>
    </view>

    <view class="panel" v-if="detail">
      <view class="panel-title">处理说明</view>
      <view class="content-block">
        <view class="block-label-row">
          <view class="block-label">工单内容</view>
          <view v-if="!isFinished" class="edit-content" @click="openContentEditor">
            <u-icon name="edit-pen" color="#165DFF" size="15" />
            <text>编辑</text>
          </view>
        </view>
        <view class="block-text">{{ detail.content || '暂无' }}</view>
      </view>
    </view>

    <view v-if="contentEditorVisible" class="editor-mask" @click.self="closeContentEditor">
      <view class="editor-sheet">
        <view class="editor-head">
          <view class="editor-title">编辑工单内容</view>
          <view class="editor-close" @click="closeContentEditor">
            <u-icon name="close" color="#64748B" size="18" />
          </view>
        </view>
        <u-textarea
          v-model="editingContent"
          placeholder="请输入工单内容"
          maxlength="500"
          autoHeight
          border="none"
          :customStyle="contentEditorStyle"
        />
        <view class="editor-actions">
          <view class="editor-cancel" @click="closeContentEditor">取消</view>
          <view class="editor-submit" :class="{ disabled: savingContent }" @click="saveContent">
            <u-loading-icon v-if="savingContent" color="#fff" size="17" />
            <text>{{ savingContent ? '保存中' : '保存' }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <view>
          <view class="panel-title">现场附件</view>
          <view class="panel-sub">原图、处理图和回执记录</view>
        </view>
      </view>
      <view v-if="loadingAttachments" class="loading-box">
        <u-loading-icon text="加载中" textSize="24" />
      </view>
      <view v-else-if="!groupedAttachments.length" class="empty-box">
        <u-empty mode="data" text="暂无附件" />
      </view>
      <view v-else class="attachment-list">
        <view v-for="group in groupedAttachments" :key="group.category" class="attachment-group">
          <view class="group-head">
            <text class="group-title">{{ group.label }}</text>
            <text class="group-count">{{ group.items.length }} 张</text>
          </view>
          <view class="thumb-grid">
            <image
              v-for="item in group.items"
              :key="item.id"
              class="thumb"
              :src="fileUrl(item.url)"
              mode="aspectFill"
              @click="previewAttachment(group.items, item.url)"
            />
          </view>
        </view>
      </view>
    </view>

    <view
      v-if="detail && !isFinished"
      class="complete-button"
      :class="{ disabled: completing }"
      @click="completeWorkOrder"
    >
      <u-loading-icon v-if="completing" color="#fff" size="18" />
      <text>{{ completing ? '提交中' : '完成工单' }}</text>
    </view>
    <view v-if="completeMessage" class="complete-message">{{ completeMessage }}</view>
  </view>
</template>

<script>
import {
  completeWorkOrder as completeWorkOrderApi,
  fetchAttachments,
  fetchWorkOrderDetail,
  fetchWorkOrderRecords,
  updateWorkOrderContent,
} from '../../api/work-order'
import { fileUrl } from '../../utils/request'

const ATTACHMENT_LABELS = {
  fault_before: '原故障图片',
  fault_after: '处理后图片',
  site_process: '现场处理图片',
  work_receipt: '工单处理回执',
  inspection_photo: '巡检图片',
  inspection_receipt: '巡检回执',
}

const MAINTENANCE_CONTENT_LABELS = {
  warranty_free: '保内免费',
  out_warranty_paid: '保外收费',
  out_warranty_free: '保外免费',
  warranty_paid: '保内收费',
}

export default {
  data() {
    return {
      id: '',
      detail: null,
      attachments: [],
      records: [],
      loadingAttachments: false,
      completing: false,
      completeMessage: '',
      contentEditorVisible: false,
      savingContent: false,
      editingContent: '',
      contentEditorStyle: {
        minHeight: '220rpx',
        padding: '18rpx',
        borderRadius: '16rpx',
        background: '#F6F8FB',
      },
    }
  },
  computed: {
    isFinished() {
      return ['completed', 'closed'].includes(this.detail?.status)
    },
    engineersText() {
      return (this.detail?.engineers || [])
        .map((item) => item.realName || item.username)
        .join('，') || '-'
    },
    uploadDesc() {
      return this.detail?.type === 'inspection' ? '上传巡检图片和巡检回执' : '上传故障、处理和回执图片'
    },
    groupedAttachments() {
      const map = new Map()
      this.attachments.forEach((item) => {
        const key = item.category || 'other'
        if (!map.has(key)) {
          map.set(key, {
            category: key,
            label: ATTACHMENT_LABELS[key] || '其他附件',
            items: [],
          })
        }
        map.get(key).items.push(item)
      })
      return Array.from(map.values())
    },
    hasCheckinRecord() {
      return this.records.some((item) => {
        const content = item.content || ''
        return Boolean(item.longitude && item.latitude) || content.includes('现场打卡')
      })
    },
  },
  onLoad(options) {
    this.id = options.id
    this.loadDetail()
  },
  onShow() {
    if (this.id) {
      this.loadDetail()
    }
  },
  methods: {
    fileUrl,
    async loadDetail() {
      if (!this.id) return
      const [data, records] = await Promise.all([
        fetchWorkOrderDetail(this.id),
        fetchWorkOrderRecords(this.id),
      ])
      this.detail = data
      this.records = records || []
      this.loadingAttachments = true
      try {
        this.attachments = await fetchAttachments({
          bizType: 'work_order',
          bizId: this.id,
        })
      } finally {
        this.loadingAttachments = false
      }
    },
    goBack() {
      uni.navigateBack({
        fail: () => uni.reLaunch({ url: '/pages/work-order/list' }),
      })
    },
    goCheckin() {
      uni.navigateTo({ url: `/pages/work-order/checkin?id=${this.id}` })
    },
    goUpload() {
      uni.navigateTo({ url: `/pages/work-order/upload?id=${this.id}&type=${this.detail?.type || ''}` })
    },
    openContentEditor() {
      this.editingContent = this.detail?.content || ''
      this.contentEditorVisible = true
    },
    closeContentEditor() {
      if (this.savingContent) return
      this.contentEditorVisible = false
    },
    async saveContent() {
      const content = this.editingContent.trim()
      if (!content) {
        uni.showToast({ title: '请填写工单内容', icon: 'none' })
        return
      }
      if (this.savingContent) return
      this.savingContent = true
      try {
        await updateWorkOrderContent(this.id, content)
        this.contentEditorVisible = false
        uni.showToast({ title: '已保存', icon: 'success' })
        await this.loadDetail()
      } finally {
        this.savingContent = false
      }
    },
    async completeWorkOrder() {
      if (this.completing) return
      if (!this.hasCheckinRecord) {
        uni.showToast({
          title: '请先完成现场打卡',
          icon: 'none',
        })
        return
      }
      uni.showModal({
        title: '确认完成工单',
        content: '完成后工单将进入已完成状态，后台会记录完成时间。',
        confirmText: '确认完成',
        confirmColor: '#165DFF',
        success: (res) => {
          if (res.confirm) {
            this.submitComplete()
          }
        },
      })
    },
    async submitComplete() {
      this.completing = true
      this.completeMessage = ''
      try {
        await completeWorkOrderApi(this.id)
        this.completeMessage = '工单已完成'
        await this.loadDetail()
      } finally {
        this.completing = false
      }
    },
    previewAttachment(list, currentUrl) {
      const urls = list.map((item) => fileUrl(item.url))
      uni.previewImage({
        current: fileUrl(currentUrl),
        urls,
      })
    },
    typeText(type) {
      return type === 'inspection' ? '巡检工单' : '现场工单'
    },
    priorityText(priority) {
      const map = { urgent: '紧急', normal: '普通' }
      return map[priority] || priority || '-'
    },
    maintenanceContentText(value) {
      return MAINTENANCE_CONTENT_LABELS[value] || value || '暂无'
    },
    statusText(status) {
      const map = {
        pending: '待处理',
        processing: '处理中',
        completed: '已完成',
        closed: '已作废',
      }
      return map[status] || status || '-'
    },
    statusClass(status) {
      const map = {
        pending: 'warning',
        processing: 'processing',
        completed: 'success',
        closed: 'info',
      }
      return map[status] || 'info'
    },
    formatTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ')
    },
  },
}
</script>

<style lang="scss" scoped>
.detail-page {
  padding-bottom: calc(156rpx + env(safe-area-inset-bottom));
}

.nav-row {
  display: grid;
  grid-template-columns: 72rpx 1fr 72rpx;
  align-items: center;
  margin-bottom: 18rpx;
}

.back-button,
.nav-placeholder {
  width: 72rpx;
  height: 72rpx;
}

.back-button {
  display: grid;
  place-items: center;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(20, 31, 52, 0.05);
}

.nav-title {
  color: #172033;
  font-size: 32rpx;
  font-weight: 800;
  text-align: center;
}

.summary-card,
.process-panel,
.panel {
  margin-bottom: 18rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.summary-card {
  padding: 26rpx;
  border-left: 6rpx solid #15b8a6;
}

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.summary-main {
  min-width: 0;
}

.customer-name {
  overflow: hidden;
  color: #172033;
  font-size: 36rpx;
  font-weight: 800;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-no {
  margin-top: 8rpx;
  color: #8a94a6;
  font-size: 24rpx;
}

.status-badge {
  flex-shrink: 0;
  padding: 9rpx 16rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 800;
}

.status-badge.processing {
  background: #eaf1ff;
  color: #165dff;
}

.status-badge.warning {
  background: #fff7ed;
  color: #c76a00;
}

.status-badge.success {
  background: #edf9f0;
  color: #16803a;
}

.status-badge.info {
  background: #f0f2f5;
  color: #6b7280;
}

.address-row {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 20rpx;
  color: #6b7280;
  font-size: 24rpx;
  line-height: 1.55;
}

.address-row text {
  min-width: 0;
  flex: 1;
}

.tag-row {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 12rpx;
  margin-top: 20rpx;
}

.tag-item {
  display: flex;
  min-width: 0;
  height: 66rpx;
  align-items: center;
  gap: 9rpx;
  padding: 0 16rpx;
  border-radius: 15rpx;
  background: #f6f8fb;
  color: #344054;
  font-size: 24rpx;
  font-weight: 700;
}

.tag-item text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel {
  padding: 26rpx;
}

.process-panel {
  padding: 26rpx;
}

.process-list {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
}

.process-item {
  display: flex;
  min-height: 96rpx;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx;
  border: 1px solid #edf0f5;
  border-radius: 18rpx;
  background: #fafbfc;
}

.process-item.active {
  background: #f7faff;
}

.process-icon {
  display: grid;
  width: 58rpx;
  height: 58rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 16rpx;
}

.process-icon.blue {
  background: #eaf1ff;
}

.process-icon.cyan {
  background: #e9fbf8;
}

.process-text {
  min-width: 0;
  flex: 1;
}

.process-title {
  color: #172033;
  font-size: 27rpx;
  font-weight: 800;
}

.process-desc {
  margin-top: 6rpx;
  overflow: hidden;
  color: #8a94a6;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.panel-title {
  color: #172033;
  font-size: 30rpx;
  font-weight: 800;
}

.panel-sub {
  margin-top: 6rpx;
  color: #8a94a6;
  font-size: 22rpx;
}

.info-list {
  margin-top: 4rpx;
}

.info-row {
  display: flex;
  min-height: 70rpx;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-bottom: 1px solid #f0f2f5;
  font-size: 25rpx;
}

.info-row:last-child {
  border-bottom: 0;
}

.info-label {
  flex-shrink: 0;
  color: #8a94a6;
}

.info-value {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #344054;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
}

.info-value.urgent {
  color: #dc2626;
}

.content-block + .content-block {
  margin-top: 22rpx;
}

.block-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.block-label {
  margin-bottom: 10rpx;
  color: #344054;
  font-size: 25rpx;
  font-weight: 800;
}

.edit-content {
  display: flex;
  align-items: center;
  gap: 5rpx;
  color: #165dff;
  font-size: 23rpx;
  font-weight: 700;
}

.block-text {
  min-height: 74rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #f6f8fb;
  color: #6b7280;
  font-size: 25rpx;
  line-height: 1.7;
}

.editor-mask {
  position: fixed;
  z-index: 1000;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.34);
}

.editor-sheet {
  width: 100%;
  box-sizing: border-box;
  padding: 30rpx 24rpx calc(30rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: #fff;
}

.editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.editor-title {
  color: #172033;
  font-size: 31rpx;
  font-weight: 800;
}

.editor-close {
  display: grid;
  width: 56rpx;
  height: 56rpx;
  place-items: center;
  border-radius: 16rpx;
  background: #f6f8fb;
}

.editor-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 26rpx;
}

.editor-cancel,
.editor-submit {
  display: flex;
  height: 82rpx;
  align-items: center;
  justify-content: center;
  border-radius: 16rpx;
  font-size: 27rpx;
  font-weight: 700;
}

.editor-cancel {
  flex: 1;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.editor-submit {
  flex: 1.5;
  gap: 10rpx;
  background: #165dff;
  color: #fff;
}

.editor-submit.disabled {
  opacity: 0.72;
}

.loading-box,
.empty-box {
  padding: 54rpx 0;
}

.attachment-group + .attachment-group {
  margin-top: 24rpx;
}

.group-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14rpx;
  color: #344054;
  font-size: 25rpx;
  font-weight: 800;
}

.group-count {
  color: #8a94a6;
  font-size: 23rpx;
  font-weight: 500;
}

.thumb-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.thumb {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 16rpx;
  background: #eef2f6;
}

.complete-button {
  display: flex;
  height: 88rpx;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-bottom: 18rpx;
  border-radius: 18rpx;
  background: #165dff;
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  box-shadow: 0 12rpx 26rpx rgba(22, 93, 255, 0.2);
}

.complete-button.disabled {
  opacity: 0.78;
}

.complete-message {
  margin-bottom: 18rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  background: #f4fbf6;
  color: #16803a;
  font-size: 24rpx;
  font-weight: 700;
}

</style>

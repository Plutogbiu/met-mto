<template>
  <view class="page mto-page upload-page">
    <view class="summary-card">
      <view class="page-title">附件上传</view>
      <view class="summary-line">
        <text class="label">工单编号</text>
        <text class="value">{{ detail?.orderNo || '-' }}</text>
      </view>
      <view class="summary-line">
        <text class="label">工单类型</text>
        <text class="value">{{ typeText(detail?.type) }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-title">选择附件类型</view>
      <view class="category-list">
        <view
          v-for="item in categories"
          :key="item.value"
          class="category-item"
          :class="{ active: form.category === item.value }"
          @click="selectCategory(item.value)"
        >
          <text>{{ item.label }}</text>
          <u-icon
            v-if="form.category === item.value"
            name="checkmark-circle-fill"
            color="#165DFF"
            size="20"
          />
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <view>
          <view class="panel-title">上传图片</view>
          <view class="panel-sub">已选择 {{ selectedImages.length }} 张，最多一次选择 9 张</view>
        </view>
      </view>

      <view class="pick-card" @click="chooseImage">
        <view class="pick-icon">
          <u-icon name="photo" color="#165DFF" size="26" />
        </view>
        <view class="pick-text">
          <view class="pick-title">选择图片</view>
          <view class="pick-desc">支持相册或拍照，可一次选择多张</view>
        </view>
        <u-icon name="arrow-right" color="#B4BDCB" size="15" />
      </view>

      <view v-if="selectedImages.length" class="preview-section">
        <view class="section-label">待上传图片</view>
        <view class="image-grid">
          <view v-for="(item, index) in selectedImages" :key="item.id" class="image-cell">
            <image :src="item.path" mode="aspectFill" @click="previewSelected(index)" />
            <view class="remove-button" v-if="item.status !== 'uploading'" @click.stop="removeSelected(index)">
              <u-icon name="close" color="#fff" size="12" />
            </view>
            <view class="image-status" :class="item.status">
              {{ statusLabel(item.status) }}
            </view>
          </view>
        </view>
      </view>

      <view v-if="errorText" class="message-box error">{{ errorText }}</view>
      <view v-if="successText" class="message-box success">{{ successText }}</view>

      <view class="upload-button" :class="{ disabled: loading }" @click="submitUpload">
        <u-loading-icon v-if="loading" color="#fff" size="18" />
        <text>{{ loading ? '上传中' : '上传附件' }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <view>
          <view class="panel-title">已上传图片</view>
          <view class="panel-sub">当前工单已保存的附件</view>
        </view>
        <view class="refresh-button" @click="loadUploaded">
          <u-icon name="reload" color="#165DFF" size="16" />
        </view>
      </view>

      <view v-if="loadingUploaded" class="empty-box">
        <u-loading-icon text="加载中" textSize="24" />
      </view>
      <view v-else-if="!uploadedGroups.length" class="empty-box">
        <u-empty mode="data" text="暂无已上传图片" />
      </view>
      <view v-else class="uploaded-list">
        <view v-for="group in uploadedGroups" :key="group.category" class="uploaded-group">
          <view class="uploaded-head">
            <text>{{ group.label }}</text>
            <text>{{ group.items.length }} 张</text>
          </view>
          <view class="image-grid">
            <view v-for="item in group.items" :key="item.id" class="image-cell uploaded">
              <image :src="fileUrl(item.url)" mode="aspectFill" @click="previewUploaded(group.items, item.url)" />
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { fetchWorkOrderDetail } from '../../api/work-order'
import { fetchAttachments, uploadAttachment } from '../../api/attachment'
import { fileUrl } from '../../utils/request'

export default {
  data() {
    return {
      id: '',
      detail: null,
      loading: false,
      loadingUploaded: false,
      selectedImages: [],
      uploadedAttachments: [],
      errorText: '',
      successText: '',
      form: {
        category: '',
      },
    }
  },
  computed: {
    categories() {
      if (this.detail?.type === 'inspection') {
        return [
          { label: '巡检图片', value: 'inspection_photo' },
          { label: '巡检回执', value: 'inspection_receipt' },
        ]
      }
      return [
        { label: '原故障图片', value: 'fault_before' },
        { label: '处理后图片', value: 'fault_after' },
        { label: '现场处理图片', value: 'site_process' },
        { label: '工单处理回执', value: 'work_receipt' },
      ]
    },
    uploadedGroups() {
      const map = new Map()
      this.uploadedAttachments.forEach((item) => {
        const category = item.category || 'other'
        if (!map.has(category)) {
          map.set(category, {
            category,
            label: this.categoryLabel(category),
            items: [],
          })
        }
        map.get(category).items.push(item)
      })
      return Array.from(map.values())
    },
  },
  onLoad(options) {
    this.id = options.id
    this.loadDetail()
  },
  methods: {
    fileUrl,
    async loadDetail() {
      this.detail = await fetchWorkOrderDetail(this.id)
      this.form.category = this.categories[0]?.value || ''
      this.loadUploaded()
    },
    async loadUploaded() {
      if (!this.id) return
      this.loadingUploaded = true
      try {
        this.uploadedAttachments = await fetchAttachments({
          bizType: 'work_order',
          bizId: this.id,
        })
      } finally {
        this.loadingUploaded = false
      }
    },
    selectCategory(value) {
      if (this.form.category === value) return
      this.form.category = value
      this.selectedImages = []
      this.errorText = ''
      this.successText = ''
    },
    chooseImage() {
      const remain = Math.max(1, 9 - this.selectedImages.length)
      uni.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const paths = res.tempFilePaths || []
          const now = Date.now()
          const additions = paths.map((path, index) => ({
            id: `${now}-${index}`,
            path,
            status: 'pending',
          }))
          this.selectedImages = [...this.selectedImages, ...additions].slice(0, 9)
          this.errorText = ''
          this.successText = ''
        },
      })
    },
    removeSelected(index) {
      this.selectedImages.splice(index, 1)
    },
    async submitUpload() {
      if (this.loading) return
      if (!this.form.category) {
        this.errorText = '请选择附件类型'
        return
      }
      if (!this.selectedImages.length) {
        this.errorText = '请先选择图片'
        return
      }

      this.loading = true
      this.errorText = ''
      this.successText = ''
      let successCount = 0
      let failCount = 0
      try {
        for (const item of this.selectedImages) {
          if (item.status === 'uploaded') continue
          item.status = 'uploading'
          try {
            await uploadAttachment({
              filePath: item.path,
              bizType: 'work_order',
              bizId: this.id,
              category: this.form.category,
            })
            item.status = 'uploaded'
            successCount += 1
          } catch (error) {
            item.status = 'failed'
            failCount += 1
          }
        }
        await this.loadUploaded()
        this.selectedImages = this.selectedImages.filter((item) => item.status !== 'uploaded')
        if (successCount) {
          this.successText = `已上传 ${successCount} 张图片`
        }
        if (failCount) {
          this.errorText = `${failCount} 张图片上传失败，可重新上传`
        }
      } finally {
        this.loading = false
      }
    },
    previewSelected(index) {
      uni.previewImage({
        current: this.selectedImages[index].path,
        urls: this.selectedImages.map((item) => item.path),
      })
    },
    previewUploaded(list, currentUrl) {
      uni.previewImage({
        current: fileUrl(currentUrl),
        urls: list.map((item) => fileUrl(item.url)),
      })
    },
    statusLabel(status) {
      const map = {
        pending: '待上传',
        uploading: '上传中',
        uploaded: '已上传',
        failed: '失败',
      }
      return map[status] || '待上传'
    },
    categoryLabel(value) {
      return this.categories.find((item) => item.value === value)?.label || '其他附件'
    },
    typeText(type) {
      return type === 'inspection' ? '巡检工单' : '现场工单'
    },
  },
}
</script>

<style lang="scss" scoped>
.upload-page {
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

.summary-card,
.panel {
  margin-bottom: 18rpx;
  padding: 26rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.page-title {
  margin-bottom: 20rpx;
  color: #172033;
  font-size: 36rpx;
  font-weight: 800;
}

.summary-line {
  display: flex;
  min-height: 52rpx;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  color: #344054;
  font-size: 26rpx;
}

.label {
  flex-shrink: 0;
  color: #8a94a6;
}

.value {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
}

.panel-head {
  display: flex;
  align-items: flex-start;
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

.category-list {
  display: grid;
  gap: 14rpx;
}

.category-item {
  display: flex;
  min-height: 76rpx;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 0 20rpx;
  border: 1px solid #edf0f5;
  border-radius: 17rpx;
  background: #fafbfc;
  color: #344054;
  font-size: 26rpx;
  font-weight: 700;
}

.category-item.active {
  border-color: rgba(22, 93, 255, 0.36);
  background: #eaf1ff;
  color: #165dff;
}

.pick-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  border: 1px dashed rgba(22, 93, 255, 0.34);
  border-radius: 18rpx;
  background: #f7faff;
}

.pick-icon {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 18rpx;
  background: #eaf1ff;
}

.pick-text {
  min-width: 0;
  flex: 1;
}

.pick-title {
  color: #172033;
  font-size: 28rpx;
  font-weight: 800;
}

.pick-desc {
  margin-top: 6rpx;
  color: #8a94a6;
  font-size: 23rpx;
}

.preview-section {
  margin-top: 22rpx;
}

.section-label {
  margin-bottom: 14rpx;
  color: #344054;
  font-size: 25rpx;
  font-weight: 800;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.image-cell {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1;
  border-radius: 16rpx;
  background: #eef2f6;
}

.image-cell image {
  width: 100%;
  height: 100%;
}

.remove-button {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  display: grid;
  width: 34rpx;
  height: 34rpx;
  place-items: center;
  border-radius: 999rpx;
  background: rgba(23, 32, 51, 0.72);
}

.image-status {
  position: absolute;
  left: 8rpx;
  bottom: 8rpx;
  padding: 5rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(23, 32, 51, 0.72);
  color: #fff;
  font-size: 20rpx;
}

.image-status.uploaded {
  background: rgba(22, 128, 58, 0.84);
}

.image-status.failed {
  background: rgba(220, 38, 38, 0.84);
}

.image-status.uploading {
  background: rgba(22, 93, 255, 0.84);
}

.message-box {
  margin-top: 18rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  font-size: 24rpx;
  line-height: 1.5;
}

.message-box.error {
  background: #fff7ed;
  color: #b45309;
}

.message-box.success {
  background: #f4fbf6;
  color: #16803a;
}

.upload-button {
  display: flex;
  height: 88rpx;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-top: 22rpx;
  border-radius: 18rpx;
  background: #165dff;
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  box-shadow: 0 12rpx 26rpx rgba(22, 93, 255, 0.2);
}

.upload-button.disabled {
  opacity: 0.78;
}

.refresh-button {
  display: grid;
  width: 58rpx;
  height: 58rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 16rpx;
  background: #eaf1ff;
}

.empty-box {
  padding: 48rpx 0;
}

.uploaded-list {
  display: grid;
  gap: 24rpx;
}

.uploaded-head {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 14rpx;
  color: #344054;
  font-size: 25rpx;
  font-weight: 800;
}

.uploaded-head text:last-child {
  color: #8a94a6;
  font-size: 23rpx;
  font-weight: 500;
}
</style>

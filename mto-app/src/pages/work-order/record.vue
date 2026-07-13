<template>
  <view class="page mto-page">
    <view class="panel">
      <view class="panel-title">处理记录</view>
      <view class="line">工单：{{ detail?.orderNo || '-' }}</view>
      <view class="line">状态：{{ statusText(detail?.status) }}</view>
    </view>

    <view class="panel">
      <view class="panel-title">新增记录</view>
      <u-textarea
        v-model="content"
        placeholder="填写本次处理说明"
        autoHeight
        maxlength="500"
        border="surround"
      />
      <view class="buttons">
        <u-button type="primary" :loading="saving" @click="submitRecord">提交记录</u-button>
      </view>
    </view>

    <view class="panel">
      <view class="panel-title">历史记录</view>
      <view v-if="loading" class="loading-box">
        <u-loading-icon text="加载中" textSize="24" />
      </view>
      <view v-else-if="!records.length" class="empty-box">
        <u-empty mode="data" text="暂无记录" />
      </view>
      <view v-else class="record-list">
        <view v-for="item in records" :key="item.id" class="record-item">
          <view class="record-top">
            <text class="record-name">{{ item.operatorName || '系统' }}</text>
            <text class="record-time">{{ formatTime(item.createdAt) }}</text>
          </view>
          <view class="record-type">{{ recordTypeText(item.recordType) }}</view>
          <view class="record-content">{{ item.content || '无' }}</view>
        </view>
      </view>
    </view>
    <AppTabBar active="orders" />
  </view>
</template>

<script>
import AppTabBar from '../../components/AppTabBar.vue'
import { createWorkOrderRecord, fetchWorkOrderDetail, fetchWorkOrderRecords } from '../../api/work-order'

export default {
  components: { AppTabBar },
  data() {
    return {
      id: '',
      detail: null,
      loading: false,
      saving: false,
      content: '',
      records: [],
    }
  },
  onLoad(options) {
    this.id = options.id
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        this.detail = await fetchWorkOrderDetail(this.id)
        this.records = await fetchWorkOrderRecords(this.id)
      } finally {
        this.loading = false
      }
    },
    async submitRecord() {
      if (!this.content.trim()) {
        uni.showToast({ title: '请填写处理说明', icon: 'none' })
        return
      }
      this.saving = true
      try {
        await createWorkOrderRecord(this.id, this.content.trim())
        this.content = ''
        uni.showToast({ title: '已提交', icon: 'success' })
        this.loadData()
      } finally {
        this.saving = false
      }
    },
    recordTypeText(value) {
      const map = {
        create: '创建记录',
        update: '更新记录',
        status: '状态变更',
        void: '作废工单',
        process: '处理记录',
      }
      return map[value] || value || '-'
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
    formatTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ')
    },
  },
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
}

.panel {
  margin-bottom: 20rpx;
  padding: 26rpx;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16rpx 40rpx rgba(16, 24, 40, 0.06);
}

.panel-title {
  margin-bottom: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.line {
  margin-bottom: 10rpx;
  color: #344054;
}

.buttons {
  margin-top: 20rpx;
}

.loading-box,
.empty-box {
  padding: 40rpx 0;
}

.record-list {
  display: grid;
  gap: 16rpx;
}

.record-item {
  padding: 18rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1px solid #eef0f4;
}

.record-top {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
}

.record-name {
  font-weight: 600;
  color: #172033;
}

.record-time {
  color: #98a2b3;
}

.record-type {
  margin-top: 8rpx;
  color: #2f80ed;
}

.record-content {
  margin-top: 10rpx;
  line-height: 1.7;
  color: #667085;
}
</style>

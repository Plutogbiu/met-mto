<template>
  <view class="page mto-page">
    <view class="page-head">
      <view v-if="mineMode" class="back-button" @click="goProfile">
        <u-icon name="arrow-left" color="#172033" size="19" />
      </view>
      <view>
        <view class="eyebrow">{{ mineMode ? '我的工单' : '现场工单' }}</view>
        <view class="head-title">{{ mineMode ? '历史任务查询' : '任务工作台' }}</view>
      </view>
      <view class="head-actions">
        <view class="refresh-button" :class="{ loading: refreshing }" @click="manualRefresh">
          <u-icon name="reload" color="#165DFF" size="17" />
        </view>
        <view class="head-count">{{ pendingCountText }}</view>
      </view>
    </view>

    <view class="filter-card">
      <view class="type-row" @click="showTypeSheet = true">
        <view class="type-left">
          <u-icon name="grid" color="#165DFF" size="18" />
          <text>{{ activeTypeText }}</text>
        </view>
        <u-icon name="arrow-down" color="#8A94A6" size="14" />
      </view>

      <scroll-view scroll-x class="status-scroll" show-scrollbar="false">
        <view class="status-row">
          <view
            v-for="item in statusTabs"
            :key="item.value"
            class="status-chip"
            :class="{ active: query.status === item.value }"
            @click="selectStatus(item.value)"
          >
            {{ item.label }}
          </view>
        </view>
      </scroll-view>

      <view class="search-box">
        <u-icon name="search" color="#8A94A6" size="19" />
        <view class="search-input">
          <u-input
            v-model="query.keyword"
            placeholder="搜索工单编号/煤矿名称"
            border="none"
            clearable
            @confirm="reload"
          />
        </view>
        <view class="search-button" @click="reload">查询</view>
      </view>

      <view v-if="mineMode" class="history-filter">
        <view class="date-range-row">
          <picker mode="date" fields="day" :value="startDate" @change="onStartDateChange">
            <view class="date-picker">
              <u-icon name="calendar" color="#165DFF" size="18" />
              <text>{{ startDate || '开始日期' }}</text>
            </view>
          </picker>
          <picker mode="date" fields="day" :value="endDate" @change="onEndDateChange">
            <view class="date-picker">
              <u-icon name="calendar" color="#165DFF" size="18" />
              <text>{{ endDate || '结束日期' }}</text>
            </view>
          </picker>
        </view>
        <view class="date-action-row">
          <view class="range-text">{{ rangeText }}</view>
          <view class="reset-button" @click="resetHistoryFilters">重置</view>
        </view>

        <view class="history-total">
          当前筛选共 {{ total }} 条工单
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading-box">
      <u-loading-icon text="加载中" textSize="26" />
    </view>

    <view v-else-if="!list.length" class="empty-box">
      <u-empty mode="data" text="暂无工单" />
    </view>

    <view v-else class="list">
      <view v-for="item in list" :key="item.id" class="order-card" @click="openDetail(item.id)">
        <view class="card-head">
          <view class="card-main">
            <view class="customer-name">{{ item.customerSiteName || '未命名客户' }}</view>
            <view class="order-no">{{ item.orderNo || '-' }}</view>
          </view>
          <view class="status-badge" :class="statusClass(item.status)">
            {{ statusText(item.status) }}
          </view>
        </view>

        <view class="info-row">
          <view class="info-tag">
            <u-icon name="file-text" color="#165DFF" size="17" />
            <text>{{ typeText(item.type) }}</text>
          </view>
          <view class="info-tag device">
            <u-icon name="camera" color="#15B8A6" size="17" />
            <text>{{ item.deviceName || '无设备' }}</text>
          </view>
        </view>

        <view class="meta-row">
          <view class="meta-item">
            <u-icon name="clock" color="#8A94A6" size="15" />
            <text>预计到达：{{ formatTime(item.estimatedArrivalTime) }}</text>
          </view>
          <view class="handler">
            <text>{{ engineerNames(item) }}</text>
            <u-icon name="arrow-right" color="#A3ACBA" size="14" />
          </view>
        </view>
      </view>
    </view>

    <u-action-sheet
      :show="showTypeSheet"
      :actions="typeActions"
      cancelText="取消"
      @close="showTypeSheet = false"
      @select="onTypeSelect"
    />
    <AppTabBar active="orders" />
  </view>
</template>

<script>
import AppTabBar from '../../components/AppTabBar.vue'
import { fetchWorkOrders } from '../../api/work-order'
import { getAuthUser } from '../../utils/auth'

export default {
  components: { AppTabBar },
  data() {
    return {
      loading: false,
      refreshing: false,
      total: 0,
      list: [],
      mineMode: false,
      showTypeSheet: false,
      query: {
        keyword: '',
        type: '',
        status: '',
        createdStart: '',
        createdEnd: '',
        engineerId: '',
        unfinishedOnly: false,
        page: 1,
        size: 20,
      },
      startDate: '',
      endDate: '',
      typeTabs: [
        { label: '全部工单', value: '' },
        { label: '现场工单', value: 'onsite' },
        { label: '巡检工单', value: 'inspection' },
      ],
      normalStatusTabs: [
        { label: '全部', value: '' },
        { label: '待处理', value: 'pending' },
        { label: '处理中', value: 'processing' },
      ],
      historyStatusTabs: [
        { label: '全部', value: '' },
        { label: '待处理', value: 'pending' },
        { label: '处理中', value: 'processing' },
        { label: '已完成', value: 'completed' },
        { label: '已关闭', value: 'closed' },
      ],
    }
  },
  computed: {
    statusTabs() {
      return this.mineMode ? this.historyStatusTabs : this.normalStatusTabs
    },
    pendingCountText() {
      if (this.mineMode) {
        return `共 ${this.total} 条`
      }
      return `${this.total} 条未完成`
    },
    activeTypeText() {
      return this.typeTabs.find((item) => item.value === this.query.type)?.label || '全部工单'
    },
    rangeText() {
      if (this.startDate && this.endDate) {
        return `${this.startDate} 至 ${this.endDate}`
      }
      if (this.startDate) {
        return `从 ${this.startDate} 开始`
      }
      if (this.endDate) {
        return `截至 ${this.endDate}`
      }
      return '按创建日期范围筛选'
    },
    typeActions() {
      return this.typeTabs.map((item) => ({
        name: item.label,
        value: item.value,
      }))
    },
  },
  onLoad(options) {
    this.applyRouteOptions(options || {})
  },
  onShow() {
    if (!getAuthUser()) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.reload()
  },
  methods: {
    applyRouteOptions(options) {
      const user = getAuthUser()
      this.mineMode = options.mine === '1'
      const allowedStatuses = (this.mineMode ? this.historyStatusTabs : this.normalStatusTabs).map((item) => item.value)
      this.query.status = allowedStatuses.includes(options.status) ? options.status : ''
      this.query.type = options.type || ''
      this.query.createdStart = ''
      this.query.createdEnd = ''
      this.startDate = ''
      this.endDate = ''
      this.query.engineerId = user?.userId || ''
      this.query.unfinishedOnly = !this.mineMode
    },
    async reload(options = {}) {
      const silent = Boolean(options.silent)
      const showToast = Boolean(options.showToast)
      if (this.loading || this.refreshing) return
      if (silent) {
        this.refreshing = true
      } else {
        this.loading = true
      }
      try {
        this.query.page = 1
        const params = { ...this.query }
        Object.keys(params).forEach((key) => {
          if (params[key] === '') {
            delete params[key]
          }
        })
        const data = await fetchWorkOrders(params)
        this.list = data.records || []
        this.total = data.total || 0
        if (showToast) {
          uni.showToast({
            title: '已刷新',
            icon: 'none',
          })
        }
      } finally {
        this.loading = false
        this.refreshing = false
        uni.stopPullDownRefresh()
      }
    },
    manualRefresh() {
      this.reload({
        silent: this.list.length > 0,
        showToast: true,
      })
    },
    selectType(type) {
      this.query.type = type
      this.reload()
    },
    onTypeSelect(item) {
      this.showTypeSheet = false
      this.selectType(item.value || '')
    },
    selectStatus(status) {
      this.query.status = status
      this.reload()
    },
    openDetail(id) {
      uni.navigateTo({ url: `/pages/work-order/detail?id=${id}` })
    },
    goProfile() {
      uni.reLaunch({ url: '/pages/profile/profile' })
    },
    onStartDateChange(event) {
      this.startDate = event.detail.value
      if (this.endDate && this.startDate > this.endDate) {
        this.endDate = this.startDate
      }
      this.applyDateRange()
    },
    onEndDateChange(event) {
      this.endDate = event.detail.value
      if (this.startDate && this.endDate < this.startDate) {
        this.startDate = this.endDate
      }
      this.applyDateRange()
    },
    applyDateRange() {
      this.query.createdStart = this.startDate ? `${this.startDate}T00:00:00` : ''
      this.query.createdEnd = this.endDate ? `${this.endDate}T23:59:59` : ''
      if ((this.startDate && this.endDate) || (!this.startDate && !this.endDate)) {
        this.reload()
      }
    },
    resetHistoryFilters() {
      this.query.createdStart = ''
      this.query.createdEnd = ''
      this.startDate = ''
      this.endDate = ''
      this.reload()
    },
    typeText(type) {
      return type === 'inspection' ? '巡检工单' : '现场工单'
    },
    engineerNames(item) {
      const names = (item.engineers || [])
        .map((engineer) => engineer.realName || engineer.username)
        .filter(Boolean)
      return names.length ? names.join('、') : '未指派'
    },
    statusText(status) {
      const map = {
        pending: '待处理',
        processing: '处理中',
        completed: '已完成',
        closed: '已关闭',
      }
      return map[status] || status || '未知'
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
      if (!value) {
        return '-'
      }
      return String(value).replace('T', ' ')
    },
  },
  onPullDownRefresh() {
    this.reload()
  },
}
</script>

<style lang="scss" scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 24rpx;
  padding: 18rpx 4rpx 4rpx;
}

.back-button {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 18rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(20, 31, 52, 0.05);
}

.eyebrow {
  color: #165dff;
  font-size: 24rpx;
  font-weight: 700;
}

.head-title {
  margin-top: 6rpx;
  color: #172033;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 1.2;
}

.head-count {
  flex-shrink: 0;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #eaf1ff;
  color: #165dff;
  font-size: 24rpx;
  font-weight: 800;
}

.head-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 12rpx;
}

.refresh-button {
  display: grid;
  width: 62rpx;
  height: 62rpx;
  place-items: center;
  border: 1px solid #dbe5f5;
  border-radius: 18rpx;
  background: #fff;
  box-shadow: 0 8rpx 20rpx rgba(20, 31, 52, 0.05);
}

.refresh-button.loading {
  animation: refresh-spin 0.8s linear infinite;
}

@keyframes refresh-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.filter-card {
  margin-bottom: 22rpx;
  padding: 20rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.type-row {
  display: flex;
  height: 72rpx;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx;
  border-radius: 16rpx;
  background: #f6f8fb;
  color: #172033;
  font-size: 27rpx;
  font-weight: 700;
}

.type-left {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10rpx;
}

.status-scroll {
  margin-top: 18rpx;
  white-space: nowrap;
}

.status-row {
  display: inline-flex;
  gap: 12rpx;
}

.status-chip {
  display: inline-flex;
  height: 56rpx;
  align-items: center;
  justify-content: center;
  padding: 0 22rpx;
  border: 1px solid #e8edf4;
  border-radius: 999rpx;
  background: #fff;
  color: #6b7280;
  font-size: 24rpx;
}

.status-chip.active {
  border-color: #165dff;
  background: #165dff;
  color: #fff;
  font-weight: 700;
}

.search-box {
  display: flex;
  min-height: 78rpx;
  align-items: center;
  gap: 12rpx;
  margin-top: 18rpx;
  padding: 0 14rpx;
  border-radius: 16rpx;
  background: #f6f8fb;
}

.search-input {
  min-width: 0;
  flex: 1;
}

.search-button {
  display: flex;
  width: 108rpx;
  height: 56rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  background: #165dff;
  color: #fff;
  font-size: 25rpx;
  font-weight: 800;
}

.history-filter {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
}

.date-range-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
}

.date-picker {
  display: flex;
  height: 70rpx;
  align-items: center;
  gap: 10rpx;
  padding: 0 18rpx;
  border-radius: 16rpx;
  background: #eaf1ff;
  color: #165dff;
  font-size: 25rpx;
  font-weight: 800;
}

.date-picker text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-action-row {
  display: grid;
  grid-template-columns: 1fr 116rpx;
  gap: 12rpx;
  align-items: center;
}

.range-text {
  min-width: 0;
  overflow: hidden;
  color: #6b7280;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reset-button {
  display: flex;
  height: 70rpx;
  align-items: center;
  justify-content: center;
  border-radius: 16rpx;
  background: #f6f8fb;
  color: #6b7280;
  font-size: 25rpx;
  font-weight: 800;
}

.history-total {
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: #f8fafc;
  color: #6b7280;
  font-size: 23rpx;
}

.loading-box,
.empty-box {
  padding: 90rpx 0;
}

.list {
  display: grid;
  gap: 18rpx;
}

.order-card {
  padding: 24rpx;
  border: 1px solid #edf0f5;
  border-left: 6rpx solid #15b8a6;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.card-main {
  min-width: 0;
}

.customer-name {
  overflow: hidden;
  color: #172033;
  font-size: 32rpx;
  font-weight: 800;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-no {
  margin-top: 7rpx;
  color: #8a94a6;
  font-size: 23rpx;
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

.info-row {
  display: grid;
  grid-template-columns: 0.86fr 1.14fr;
  gap: 12rpx;
  margin-top: 22rpx;
}

.info-tag {
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

.info-tag text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
  margin-top: 22rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.meta-item {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 7rpx;
}

.meta-item text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.handler {
  display: flex;
  max-width: 190rpx;
  flex-shrink: 0;
  align-items: center;
  gap: 4rpx;
  color: #344054;
  font-weight: 700;
}

.handler text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<template>
  <view class="page mto-page checkin-page">
    <view class="nav-row">
      <view class="back-button" @click="goBack">
        <u-icon name="arrow-left" color="#172033" size="20" />
      </view>
      <view class="nav-title">打卡</view>
      <view class="nav-placeholder" />
    </view>

    <view class="summary-card" v-if="detail">
      <view class="customer-name">{{ detail.customerSiteName || '未命名客户' }}</view>
      <view class="order-no">{{ detail.orderNo || '-' }}</view>
      <view class="address-row">
        <u-icon name="map" color="#8A94A6" size="16" />
        <text>{{ detail.customerAddress || '暂无地址' }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-title">当前位置</view>
      <view class="location-card" :class="location.state">
        <view class="location-icon">
          <u-icon :name="locationIcon" :color="locationColor" size="24" />
        </view>
        <view class="location-text">
          <view class="location-status">{{ location.status }}</view>
          <view class="location-time">{{ location.time || location.message || '等待定位' }}</view>
        </view>
      </view>

      <view v-if="location.message" class="hint-box" :class="location.state">
        {{ location.message }}
      </view>

      <view class="map-shell">
        <map
          v-if="hasLocation"
          class="location-map"
          :latitude="mapCenter.latitude"
          :longitude="mapCenter.longitude"
          :markers="mapMarkers"
          :scale="16"
          show-location
        />
        <view v-else class="map-empty">
          <u-icon name="map" color="#98A2B3" size="24" />
          <text>定位成功后显示当前位置</text>
        </view>
      </view>

      <view class="coordinate-grid">
        <view>
          <text>纬度</text>
          <strong>{{ location.latitude || '-' }}</strong>
        </view>
        <view>
          <text>经度</text>
          <strong>{{ location.longitude || '-' }}</strong>
        </view>
      </view>

      <view class="location-actions">
        <view class="secondary-button" @click="fetchLocation">
          <u-icon name="reload" color="#165DFF" size="17" />
          <text>重新定位</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-title">打卡备注</view>
      <u-textarea
        v-model="memo"
        placeholder="可填写现场情况、到达说明等"
        maxlength="200"
        autoHeight
        border="none"
        :customStyle="textareaStyle"
      />
      <view class="submit-button" :class="{ disabled: saving }" @click="submitCheckin">
        <u-loading-icon v-if="saving" color="#fff" size="18" />
        <text>{{ saving ? '提交中' : '确认打卡' }}</text>
      </view>
    </view>

    <view v-if="successVisible" class="success-tip">
      <u-icon name="checkmark-circle-fill" color="#16803A" size="18" />
      <text>打卡信息已记录</text>
    </view>
  </view>
</template>

<script>
import { createWorkOrderRecord, fetchWorkOrderDetail } from '../../api/work-order'

export default {
  data() {
    return {
      id: '',
      detail: null,
      memo: '',
      saving: false,
      successVisible: false,
      textareaStyle: {
        minHeight: '168rpx',
        padding: '18rpx',
        borderRadius: '16rpx',
        background: '#F6F8FB',
      },
      location: {
        state: 'idle',
        status: '未定位',
        latitude: '',
        longitude: '',
        time: '',
        address: '',
        message: '',
      },
    }
  },
  computed: {
    locationIcon() {
      const map = {
        success: 'checkmark-circle-fill',
        loading: 'map-fill',
        fail: 'info-circle-fill',
        idle: 'map',
      }
      return map[this.location.state] || 'map'
    },
    locationColor() {
      const map = {
        success: '#16803A',
        loading: '#165DFF',
        fail: '#D97706',
        idle: '#8A94A6',
      }
      return map[this.location.state] || '#8A94A6'
    },
    hasLocation() {
      return Boolean(this.location.latitude && this.location.longitude)
    },
    mapCenter() {
      return {
        latitude: Number(this.location.latitude || 0),
        longitude: Number(this.location.longitude || 0),
      }
    },
    mapMarkers() {
      if (!this.hasLocation) {
        return []
      }
      return [
        {
          id: 1,
          latitude: this.mapCenter.latitude,
          longitude: this.mapCenter.longitude,
          title: '当前位置',
          width: 28,
          height: 28,
        },
      ]
    },
  },
  onLoad(options) {
    this.id = options.id
    this.loadDetail()
    this.fetchLocation()
  },
  methods: {
    async loadDetail() {
      if (!this.id) {
        return
      }
      this.detail = await fetchWorkOrderDetail(this.id)
    },
    goBack() {
      uni.navigateBack({
        fail: () => uni.reLaunch({ url: '/pages/work-order/list' }),
      })
    },
    fetchLocation() {
      this.location = {
        ...this.location,
        state: 'loading',
        status: '定位中',
        message: '',
        time: '正在获取当前位置',
      }
      uni.getLocation({
        type: 'gcj02',
        isHighAccuracy: true,
        success: (res) => {
          this.location = {
            state: 'success',
            status: '定位成功',
            latitude: Number(res.latitude).toFixed(6),
            longitude: Number(res.longitude).toFixed(6),
            time: new Date().toLocaleString(),
            address: res.address || '',
            message: '',
          }
        },
        fail: (error) => {
          this.location = {
            state: 'fail',
            status: '定位失败',
            latitude: '',
            longitude: '',
            time: '请检查定位权限后重试',
            address: '',
            message: this.locationFailText(error),
          }
        },
      })
    },
    async submitCheckin() {
      if (this.saving) return
      if (!this.location.latitude || !this.location.longitude) {
        this.location = {
          ...this.location,
          state: 'fail',
          status: '未获取位置',
          time: '请先确认当前位置',
          message: '请先重新定位，获取当前位置后再打卡',
        }
        return
      }
      this.saving = true
      try {
        const content = [
          '现场打卡',
          this.memo.trim() ? `备注：${this.memo.trim()}` : '',
        ].filter(Boolean).join('\n')
        await createWorkOrderRecord(this.id, {
          content,
          longitude: this.location.longitude,
          latitude: this.location.latitude,
          locationAddress: this.location.address || '',
        })
        this.successVisible = true
        setTimeout(() => {
          this.successVisible = false
          uni.navigateBack()
        }, 650)
      } finally {
        this.saving = false
      }
    },
    locationFailText(error) {
      const message = error?.errMsg || ''
      if (message.includes('auth') || message.includes('permission') || message.includes('denied')) {
        return '定位权限未开启，请允许浏览器或 App 访问当前位置'
      }
      return '浏览器调试可能无法获取定位，请允许位置权限后重新定位'
    },
  },
}
</script>

<style lang="scss" scoped>
.checkin-page {
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
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
.panel {
  margin-bottom: 18rpx;
  padding: 26rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.customer-name {
  color: #172033;
  font-size: 36rpx;
  font-weight: 800;
}

.order-no {
  margin-top: 8rpx;
  color: #8a94a6;
  font-size: 24rpx;
}

.address-row {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 18rpx;
  color: #6b7280;
  font-size: 24rpx;
  line-height: 1.55;
}

.address-row text {
  min-width: 0;
  flex: 1;
}

.panel-title {
  margin-bottom: 18rpx;
  color: #172033;
  font-size: 30rpx;
  font-weight: 800;
}

.location-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  border: 1px solid #edf0f5;
  border-radius: 18rpx;
  background: #fafbfc;
}

.location-card.success {
  border-color: rgba(22, 128, 58, 0.2);
  background: #f4fbf6;
}

.location-card.loading {
  border-color: rgba(22, 93, 255, 0.22);
  background: #f7faff;
}

.location-card.fail {
  border-color: rgba(217, 119, 6, 0.24);
  background: #fffaf2;
}

.location-icon {
  display: grid;
  width: 60rpx;
  height: 60rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 17rpx;
  background: #fff;
}

.location-text {
  min-width: 0;
  flex: 1;
}

.location-status {
  color: #172033;
  font-size: 29rpx;
  font-weight: 800;
}

.location-time {
  margin-top: 6rpx;
  overflow: hidden;
  color: #8a94a6;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hint-box {
  margin-top: 14rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  background: #fff7ed;
  color: #b45309;
  font-size: 23rpx;
  line-height: 1.5;
}

.hint-box.success {
  background: #f4fbf6;
  color: #16803a;
}

.map-shell {
  height: 360rpx;
  margin-top: 18rpx;
  overflow: hidden;
  border: 1px solid #edf0f5;
  border-radius: 18rpx;
  background: #f7f9fc;
}

.location-map,
.map-empty {
  width: 100%;
  height: 100%;
}

.map-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: #98a2b3;
  font-size: 24rpx;
}

.coordinate-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.coordinate-grid view {
  padding: 18rpx;
  border: 1px solid #edf0f5;
  border-radius: 17rpx;
  background: #fff;
}

.coordinate-grid text,
.coordinate-grid strong {
  display: block;
}

.coordinate-grid text {
  color: #8a94a6;
  font-size: 23rpx;
}

.coordinate-grid strong {
  margin-top: 8rpx;
  color: #344054;
  font-size: 26rpx;
}

.location-actions {
  display: block;
  margin-top: 18rpx;
}

.secondary-button {
  display: flex;
  height: 76rpx;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border: 1px solid #dbe5f5;
  border-radius: 17rpx;
  background: #fff;
  color: #344054;
  font-size: 25rpx;
  font-weight: 800;
}

.submit-button {
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

.submit-button.disabled {
  opacity: 0.78;
}

.success-tip {
  position: fixed;
  left: 50%;
  bottom: calc(54rpx + env(safe-area-inset-bottom));
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 18rpx 24rpx;
  border: 1px solid rgba(22, 128, 58, 0.2);
  border-radius: 999rpx;
  background: #fff;
  color: #16803a;
  font-size: 25rpx;
  font-weight: 800;
  box-shadow: 0 16rpx 34rpx rgba(20, 31, 52, 0.14);
  transform: translateX(-50%);
}
</style>

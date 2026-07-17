<template>
  <view class="page mto-page developer-page">
    <view class="nav-row">
      <view class="back-button" @click="goBack">
        <u-icon name="arrow-left" color="#172033" size="20" />
      </view>
      <view class="nav-title">开发者设置</view>
      <view class="nav-placeholder" />
    </view>

    <view class="intro">
      <view class="intro-icon">
        <u-icon name="setting" color="#165DFF" size="24" />
      </view>
      <view>
        <view class="intro-title">服务连接</view>
        <view class="intro-desc">修改后用于登录、工单和附件服务</view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-title">接口地址</view>
      <view class="field-label">服务地址</view>
      <u-input
        v-model="apiBaseUrl"
        border="none"
        clearable
        placeholder="例如：http://192.168.1.100:8082/api"
        :customStyle="inputStyle"
      />
      <view class="field-hint">可填写服务器地址，未填写 /api 时会自动补全。</view>

      <view class="save-button" @click="save">
        <u-icon name="checkmark-circle" color="#fff" size="18" />
        <text>保存并切换</text>
      </view>
      <view class="reset-button" @click="reset">
        <u-icon name="reload" color="#165DFF" size="17" />
        <text>恢复默认地址</text>
      </view>
    </view>

    <view class="notice">
      <u-icon name="info-circle" color="#64748B" size="17" />
      <text>切换服务后需要重新登录，当前登录状态会被清除。</text>
    </view>
  </view>
</template>

<script>
import {
  API_BASE_URL,
  getApiBaseUrl,
  resetApiBaseUrl,
  saveApiBaseUrl,
} from '../../config'
import { clearAuth } from '../../utils/auth'

export default {
  data() {
    return {
      apiBaseUrl: getApiBaseUrl(),
      inputStyle: {
        minHeight: '82rpx',
        padding: '0 22rpx',
        border: '1px solid #E5E7EB',
        borderRadius: '16rpx',
        background: '#F8FAFC',
      },
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    save() {
      const savedUrl = saveApiBaseUrl(this.apiBaseUrl)
      if (!savedUrl) {
        uni.showToast({ title: '请输入正确的接口地址', icon: 'none' })
        return
      }
      this.apiBaseUrl = savedUrl
      clearAuth()
      uni.showModal({
        title: '接口地址已保存',
        content: '应用将返回登录页，请使用新服务重新登录。',
        showCancel: false,
        success: () => {
          uni.reLaunch({ url: '/pages/login/login' })
        },
      })
    },
    reset() {
      uni.showModal({
        title: '恢复默认地址',
        content: `确定恢复为 ${API_BASE_URL} 吗？`,
        success: (result) => {
          if (!result.confirm) return
          this.apiBaseUrl = resetApiBaseUrl()
          clearAuth()
          uni.showModal({
            title: '已恢复默认地址',
            content: '应用将返回登录页，请重新登录。',
            showCancel: false,
            success: () => {
              uni.reLaunch({ url: '/pages/login/login' })
            },
          })
        },
      })
    },
  },
}
</script>

<style lang="scss" scoped>
.developer-page {
  padding-top: calc(24rpx + env(safe-area-inset-top));
}

.nav-row {
  display: flex;
  height: 72rpx;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.back-button {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  place-items: center;
}

.nav-title {
  color: #172033;
  font-size: 34rpx;
  font-weight: 800;
}

.nav-placeholder {
  width: 64rpx;
}

.intro {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 20rpx;
  padding: 24rpx;
  border-radius: 22rpx;
  background: #eaf1ff;
}

.intro-icon {
  display: grid;
  width: 68rpx;
  height: 68rpx;
  place-items: center;
  border-radius: 18rpx;
  background: #fff;
}

.intro-title {
  color: #172033;
  font-size: 29rpx;
  font-weight: 800;
}

.intro-desc {
  margin-top: 6rpx;
  color: #64748b;
  font-size: 23rpx;
}

.panel {
  padding: 26rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.panel-title {
  margin-bottom: 22rpx;
  color: #172033;
  font-size: 30rpx;
  font-weight: 800;
}

.field-label {
  margin-bottom: 10rpx;
  color: #475569;
  font-size: 24rpx;
  font-weight: 700;
}

.field-hint {
  margin-top: 12rpx;
  color: #98a2b3;
  font-size: 22rpx;
  line-height: 1.5;
}

.save-button,
.reset-button {
  display: flex;
  height: 82rpx;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  border-radius: 16rpx;
  font-size: 27rpx;
  font-weight: 700;
}

.save-button {
  margin-top: 28rpx;
  background: #165dff;
  color: #fff;
  box-shadow: 0 12rpx 24rpx rgba(22, 93, 255, 0.18);
}

.reset-button {
  margin-top: 16rpx;
  border: 1px solid #dbe5ff;
  color: #165dff;
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  margin: 22rpx 8rpx;
  color: #64748b;
  font-size: 22rpx;
  line-height: 1.55;
}
</style>

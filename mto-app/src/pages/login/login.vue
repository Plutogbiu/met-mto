<template>
  <view class="login-page">
    <view class="top-area">
      <view class="brand-mark">M</view>
      <view class="brand-text">
        <view class="title">MTO 运维助手</view>
        <view class="subtitle">工单处理 · 现场打卡 · 巡检回执</view>
      </view>
    </view>

    <view class="login-card">
      <view class="card-head">
        <view>
          <view class="card-title">账号登录</view>
          <view class="card-desc">内部运维人员使用</view>
        </view>
      </view>

      <view class="form-field">
        <view class="field-label">账号</view>
        <u-input
          v-model="form.username"
          placeholder="请输入账号"
          border="none"
          clearable
          :customStyle="inputStyle"
        />
      </view>

      <view class="form-field">
        <view class="field-label">密码</view>
        <u-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          border="none"
          clearable
          :customStyle="inputStyle"
        />
      </view>

      <view class="login-button" :class="{ loading }" @click="handleLogin">
        <u-loading-icon v-if="loading" color="#fff" size="18" />
        <text>{{ loading ? '登录中' : '登录' }}</text>
      </view>
    </view>

    <view class="developer-entry" @click="openDeveloperSettings">
      <u-icon name="setting" color="#98A2B3" size="15" />
      <text>开发者设置</text>
    </view>
  </view>
</template>

<script>
import { login } from '../../api/auth'
import { setAuth } from '../../utils/auth'

export default {
  data() {
    return {
      loading: false,
      inputStyle: {
        padding: '0',
        minHeight: '76rpx',
        background: 'transparent',
      },
      form: {
        username: '',
        password: '',
      },
    }
  },
  methods: {
    async handleLogin() {
      if (this.loading) return
      if (!this.form.username || !this.form.password) {
        uni.showToast({ title: '请输入账号和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const data = await login(this.form)
        setAuth(data)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/work-order/list' })
        }, 200)
      } finally {
        this.loading = false
      }
    },
    openDeveloperSettings() {
      uni.navigateTo({ url: '/pages/settings/developer' })
    },
  },
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 120rpx 32rpx 48rpx;
  background:
    radial-gradient(circle at 18% 8%, rgba(22, 93, 255, 0.12), transparent 34%),
    linear-gradient(180deg, #f9fbff 0%, #f6f7fb 100%);
}

.top-area {
  display: flex;
  align-items: center;
  gap: 22rpx;
  margin-bottom: 70rpx;
}

.brand-mark {
  display: grid;
  width: 92rpx;
  height: 92rpx;
  place-items: center;
  border-radius: 24rpx;
  background: #165dff;
  color: #fff;
  font-size: 42rpx;
  font-weight: 800;
  box-shadow: 0 16rpx 34rpx rgba(22, 93, 255, 0.22);
}

.brand-text {
  min-width: 0;
}

.title {
  color: #172033;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.2;
}

.subtitle {
  margin-top: 10rpx;
  color: #6b7280;
  font-size: 25rpx;
}

.login-card {
  padding: 34rpx;
  border: 1px solid #edf0f5;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 18rpx 44rpx rgba(20, 31, 52, 0.08);
}

.card-head {
  margin-bottom: 28rpx;
}

.card-title {
  color: #172033;
  font-size: 34rpx;
  font-weight: 800;
}

.card-desc {
  margin-top: 8rpx;
  color: #8a94a6;
  font-size: 24rpx;
}

.form-field {
  margin-bottom: 22rpx;
  padding: 18rpx 22rpx;
  border: 1px solid #e8edf4;
  border-radius: 18rpx;
  background: #f8fafc;
}

.field-label {
  margin-bottom: 4rpx;
  color: #6b7280;
  font-size: 23rpx;
}

.login-button {
  display: flex;
  height: 88rpx;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-top: 30rpx;
  border-radius: 18rpx;
  background: #165dff;
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
  box-shadow: 0 14rpx 30rpx rgba(22, 93, 255, 0.22);
}

.login-button.loading {
  opacity: 0.82;
}

.developer-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 34rpx;
  color: #98a2b3;
  font-size: 23rpx;
}
</style>

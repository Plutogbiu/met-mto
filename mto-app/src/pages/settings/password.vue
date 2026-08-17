<template>
  <view class="page mto-page password-page">
    <view class="nav-row">
      <view class="back-button" @click="goBack">
        <u-icon name="arrow-left" color="#172033" size="20" />
      </view>
      <view class="nav-title">修改密码</view>
      <view class="nav-placeholder" />
    </view>

    <view class="intro">
      <view class="intro-icon">
        <u-icon name="lock" color="#D97706" size="24" />
      </view>
      <view>
        <view class="intro-title">更新登录密码</view>
        <view class="intro-desc">修改成功后需要使用新密码重新登录</view>
      </view>
    </view>

    <view class="panel">
      <view class="field-label">旧密码</view>
      <u-input
        v-model="form.oldPassword"
        type="password"
        passwordIcon
        placeholder="请输入当前密码"
        border="none"
        :customStyle="inputStyle"
      />

      <view class="field-label field-gap">新密码</view>
      <u-input
        v-model="form.newPassword"
        type="password"
        passwordIcon
        placeholder="请输入新密码"
        border="none"
        :customStyle="inputStyle"
      />

      <view class="field-label field-gap">确认新密码</view>
      <u-input
        v-model="form.confirmPassword"
        type="password"
        passwordIcon
        placeholder="请再次输入新密码"
        border="none"
        :customStyle="inputStyle"
      />

      <view class="submit-button" :class="{ disabled: saving }" @click="submit">
        <u-loading-icon v-if="saving" color="#fff" size="18" />
        <text>{{ saving ? '提交中' : '确认修改' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { changePassword } from '../../api/auth'
import {
  clearAuth,
  getAuthUser,
  getRememberedCredentials,
  saveRememberedCredentials,
} from '../../utils/auth'

export default {
  data() {
    return {
      saving: false,
      form: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: '',
      },
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
    async submit() {
      if (this.saving) return
      if (!this.form.oldPassword || !this.form.newPassword || !this.form.confirmPassword) {
        uni.showToast({ title: '请完整填写密码信息', icon: 'none' })
        return
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
        return
      }
      if (this.form.oldPassword === this.form.newPassword) {
        uni.showToast({ title: '新密码不能与旧密码相同', icon: 'none' })
        return
      }

      this.saving = true
      try {
        const user = getAuthUser()
        await changePassword({
          oldPassword: this.form.oldPassword,
          newPassword: this.form.newPassword,
        })
        const remembered = getRememberedCredentials()
        if (remembered && remembered.username === user?.username) {
          saveRememberedCredentials({
            username: remembered.username,
            password: this.form.newPassword,
          })
        }
        clearAuth()
        uni.showModal({
          title: '密码修改成功',
          content: '请使用新密码重新登录。',
          showCancel: false,
          success: () => {
            uni.reLaunch({ url: '/pages/login/login' })
          },
        })
      } finally {
        this.saving = false
      }
    },
  },
}
</script>

<style lang="scss" scoped>
.password-page {
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
  background: #fff7e8;
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
  color: #8a6b32;
  font-size: 23rpx;
}

.panel {
  padding: 26rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.field-label {
  margin-bottom: 10rpx;
  color: #475569;
  font-size: 24rpx;
  font-weight: 700;
}

.field-gap {
  margin-top: 22rpx;
}

.submit-button {
  display: flex;
  height: 82rpx;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  margin-top: 30rpx;
  border-radius: 16rpx;
  background: #165dff;
  color: #fff;
  font-size: 27rpx;
  font-weight: 700;
  box-shadow: 0 12rpx 24rpx rgba(22, 93, 255, 0.18);
}

.submit-button.disabled {
  opacity: 0.72;
}
</style>

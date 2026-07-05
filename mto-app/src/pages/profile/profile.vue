<template>
  <view class="page mto-page">
    <view class="profile-head">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-main">
        <view class="name">{{ user?.realName || '未登录' }}</view>
        <view class="account">{{ user?.username || '-' }}（账号）</view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <view>
          <view class="section-title">功能区</view>
          <view class="section-desc">常用操作入口</view>
        </view>
      </view>

      <view class="feature-grid">
        <view
          v-for="item in featureEntries"
          :key="item.label"
          class="feature-card"
          @click="handleFeature(item.action)"
        >
          <view class="feature-icon" :class="item.tone">
            <u-icon :name="item.icon" :color="item.color" size="24" />
          </view>
          <view class="feature-title">{{ item.label }}</view>
          <view class="feature-desc">{{ item.desc }}</view>
        </view>
      </view>
    </view>

    <AppTabBar active="profile" />
  </view>
</template>

<script>
import AppTabBar from '../../components/AppTabBar.vue'
import { logout } from '../../api/auth'
import { clearAuth, getAuthUser } from '../../utils/auth'

export default {
  components: { AppTabBar },
  data() {
    return {
      user: null,
      featureEntries: [
        {
          label: '我的工单',
          desc: '全部工单',
          icon: 'file-text',
          tone: 'blue',
          color: '#165DFF',
          action: 'orders',
        },
        {
          label: '设置',
          desc: '账号设置',
          icon: 'setting',
          tone: 'gray',
          color: '#64748B',
          action: 'settings',
        },
        {
          label: '退出登录',
          desc: '安全退出',
          icon: 'close-circle',
          tone: 'red',
          color: '#DC2626',
          action: 'logout',
        },
      ],
    }
  },
  computed: {
    avatarText() {
      const name = this.user?.realName || this.user?.username || 'M'
      return String(name).slice(0, 1).toUpperCase()
    },
  },
  onShow() {
    this.user = getAuthUser()
  },
  methods: {
    handleFeature(action) {
      if (action === 'orders') {
        uni.reLaunch({ url: '/pages/work-order/list?mine=1' })
        return
      }
      if (action === 'settings') {
        this.showDeveloping()
        return
      }
      if (action === 'logout') {
        this.handleLogout()
      }
    },
    async handleLogout() {
      try {
        await logout()
      } catch (error) {
        // ignore logout network issues
      }
      clearAuth()
      uni.reLaunch({ url: '/pages/login/login' })
    },
    showDeveloping() {
      uni.showToast({ title: '功能规划中', icon: 'none' })
    },
  },
}
</script>

<style lang="scss" scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 22rpx;
  margin-bottom: 22rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  background: #165dff;
  box-shadow: 0 14rpx 34rpx rgba(22, 93, 255, 0.2);
}

.avatar {
  display: grid;
  width: 92rpx;
  height: 92rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 24rpx;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 40rpx;
  font-weight: 800;
}

.profile-main {
  min-width: 0;
  flex: 1;
}

.name {
  overflow: hidden;
  color: #fff;
  font-size: 36rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account {
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 24rpx;
}

.section-card {
  margin-bottom: 20rpx;
  padding: 26rpx;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.section-head {
  margin-bottom: 22rpx;
}

.section-title {
  color: #172033;
  font-size: 32rpx;
  font-weight: 800;
}

.section-desc {
  margin-top: 6rpx;
  color: #8a94a6;
  font-size: 23rpx;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.feature-card {
  display: flex;
  min-height: 174rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 9rpx;
  padding: 18rpx 10rpx;
  border: 1px solid #edf0f5;
  border-radius: 20rpx;
  background: #fafbfc;
}

.feature-icon {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 18rpx;
}

.feature-icon.blue {
  background: #eaf1ff;
}

.feature-icon.gray {
  background: #f1f5f9;
}

.feature-icon.red {
  background: #fff1f2;
}

.feature-title {
  max-width: 100%;
  overflow: hidden;
  color: #172033;
  font-size: 25rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feature-desc {
  max-width: 100%;
  overflow: hidden;
  color: #8a94a6;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

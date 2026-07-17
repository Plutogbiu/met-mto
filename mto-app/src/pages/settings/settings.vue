<template>
  <view class="page mto-page settings-page">
    <view class="nav-row">
      <view class="back-button" @click="goBack">
        <u-icon name="arrow-left" color="#172033" size="20" />
      </view>
      <view class="nav-title">设置</view>
      <view class="nav-placeholder" />
    </view>

    <view class="section-title">账号与连接</view>
    <view class="settings-panel">
      <view
        v-for="item in settingEntries"
        :key="item.key"
        class="setting-item"
        @click="handleSetting(item.key)"
      >
        <view class="setting-icon" :class="item.tone">
          <u-icon :name="item.icon" :color="item.color" size="21" />
        </view>
        <view class="setting-content">
          <view class="setting-name">{{ item.name }}</view>
          <view class="setting-desc">{{ item.desc }}</view>
        </view>
        <u-icon name="arrow-right" color="#B4BDCB" size="16" />
      </view>
    </view>

    <view class="section-title about-title">应用信息</view>
    <view class="settings-panel">
      <view class="setting-item static-item">
        <view class="setting-icon gray">
          <u-icon name="info-circle" color="#64748B" size="21" />
        </view>
        <view class="setting-content">
          <view class="setting-name">关于 MTO 运维助手</view>
          <view class="setting-desc">内部运维现场协作工具</view>
        </view>
        <view class="version">v1.0</view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      settingEntries: [
        {
          key: 'password',
          name: '修改密码',
          desc: '修改当前账号登录密码',
          icon: 'lock',
          tone: 'orange',
          color: '#D97706',
        },
        {
          key: 'developer',
          name: '开发者设置',
          desc: '配置服务接口地址',
          icon: 'setting',
          tone: 'blue',
          color: '#165DFF',
        },
      ],
    }
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    handleSetting(key) {
      if (key === 'developer') {
        uni.navigateTo({ url: '/pages/settings/developer' })
        return
      }
      if (key === 'password') {
        uni.showToast({ title: '修改密码功能待接入', icon: 'none' })
      }
    },
  },
}
</script>

<style lang="scss" scoped>
.settings-page {
  padding-top: calc(24rpx + env(safe-area-inset-top));
}

.nav-row {
  display: flex;
  height: 72rpx;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 34rpx;
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

.section-title {
  margin: 0 8rpx 14rpx;
  color: #64748b;
  font-size: 23rpx;
  font-weight: 700;
}

.about-title {
  margin-top: 32rpx;
}

.settings-panel {
  overflow: hidden;
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.setting-item {
  display: flex;
  min-height: 116rpx;
  align-items: center;
  gap: 18rpx;
  padding: 0 24rpx;
}

.setting-item + .setting-item {
  border-top: 1px solid #f0f2f5;
}

.setting-icon {
  display: grid;
  width: 64rpx;
  height: 64rpx;
  flex-shrink: 0;
  place-items: center;
  border-radius: 18rpx;
}

.setting-icon.blue {
  background: #eaf1ff;
}

.setting-icon.orange {
  background: #fff7e8;
}

.setting-icon.gray {
  background: #f1f5f9;
}

.setting-content {
  min-width: 0;
  flex: 1;
}

.setting-name {
  color: #172033;
  font-size: 27rpx;
  font-weight: 700;
}

.setting-desc {
  margin-top: 6rpx;
  overflow: hidden;
  color: #8a94a6;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.static-item {
  min-height: 104rpx;
}

.version {
  color: #98a2b3;
  font-size: 22rpx;
}
</style>

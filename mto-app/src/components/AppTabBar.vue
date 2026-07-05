<template>
  <view class="tab-shell">
    <view
      v-for="item in tabs"
      :key="item.key"
      class="tab-item"
      :class="{ active: active === item.key }"
      @click="go(item)"
    >
      <view class="icon-wrap">
        <u-icon :name="item.icon" :size="22" />
      </view>
      <text>{{ item.label }}</text>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    active: {
      type: String,
      default: 'orders',
    },
  },
  data() {
    return {
      tabs: [
        { key: 'orders', label: '工单', icon: 'file-text', url: '/pages/work-order/list' },
        { key: 'profile', label: '我的', icon: 'account', url: '/pages/profile/profile' },
      ],
    }
  },
  methods: {
    go(item) {
      if (this.active === item.key) {
        return
      }
      uni.reLaunch({ url: item.url })
    },
  },
}
</script>

<style lang="scss" scoped>
.tab-shell {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  padding: 10rpx 36rpx calc(10rpx + env(safe-area-inset-bottom));
  border-top: 1px solid #e8edf4;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 28rpx rgba(20, 31, 52, 0.06);
  backdrop-filter: blur(18px);
}

.tab-item {
  display: flex;
  min-height: 92rpx;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 5rpx;
  color: #8a94a6;
  font-size: 23rpx;
}

.icon-wrap {
  display: grid;
  width: 52rpx;
  height: 44rpx;
  place-items: center;
  border-radius: 999rpx;
}

.tab-item.active {
  color: #165dff;
  font-weight: 700;
}

.tab-item.active .icon-wrap {
  background: #eaf1ff;
}
</style>

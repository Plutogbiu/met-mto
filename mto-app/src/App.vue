<script>
export default {
  onLaunch() {
    console.log('MTO app launch')
  },
}
</script>

<style lang="scss">
@import "uview-plus/index.scss";

page {
  background: #f6f7fb;
  color: #172033;
  font-size: 28rpx;
  font-family:
    -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC",
    "Microsoft YaHei", "Segoe UI", sans-serif;
}

.mto-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: calc(36rpx + env(safe-area-inset-top)) 24rpx calc(142rpx + env(safe-area-inset-bottom));
  background: #f6f7fb;
}

.mto-card {
  border: 1px solid #edf0f5;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(20, 31, 52, 0.05);
}

.mto-title {
  color: #172033;
  font-weight: 700;
}

.mto-muted {
  color: #6b7280;
}
</style>

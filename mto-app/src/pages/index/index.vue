<template>
  <view class="page" />
</template>

<script>
import { isLoggedIn } from '../../utils/auth'

export default {
  onLoad() {
    const target = isLoggedIn() ? '/pages/work-order/list' : '/pages/login/login'
    uni.reLaunch({ url: target })
  },
}
</script>

<style scoped>
.page {
  width: 100%;
  height: 100vh;
}
</style>

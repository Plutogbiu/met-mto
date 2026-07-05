<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { login as loginApi } from '../../api/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const result = await loginApi(form)
    const loginData = result.data
    localStorage.setItem('mto-admin-token', loginData.token)
    localStorage.setItem('mto-admin-token-expire-at', String(Date.now() + loginData.expiresIn * 1000))
    localStorage.setItem(
      'mto-admin-user',
      JSON.stringify({
        userId: loginData.userId,
        username: loginData.username,
        realName: loginData.realName,
        role: loginData.role,
        permissions: loginData.permissions || [],
      }),
    )
    router.push(firstAllowedPath(loginData.permissions || []))
  } finally {
    loading.value = false
  }
}

function firstAllowedPath(permissions) {
  if (permissions.includes('work-order:list')) {
    return '/work-orders'
  }
  if (permissions.includes('customer:list')) {
    return '/customers'
  }
  if (permissions.includes('device:list')) {
    return '/devices'
  }
  if (permissions.includes('user:list')) {
    return '/users'
  }
  return '/login'
}
</script>

<template>
  <main class="login-page">
    <div class="login-shell">
      <section class="login-visual">
        <div class="login-brand">
          <div class="brand-mark">M</div>
          <div>
            <h1>MET MTO</h1>
            <p>运维内部工单系统</p>
          </div>
        </div>

        <div class="login-copy">
          <p class="login-kicker">FIELD SERVICE</p>
          <h2>现场服务记录，从客户现场开始。</h2>
          <p>打卡、工单、巡检、图片和报告统一沉淀，让每一次维保都有据可查。</p>
        </div>
      </section>

      <section class="login-panel">
        <div class="login-card">
          <div class="login-card-header">
            <p>WELCOME BACK</p>
            <h2>登录后台</h2>
          </div>

          <el-form label-position="top" @keyup.enter="login">
            <el-form-item label="账号">
              <el-input v-model="form.username" size="large" placeholder="请输入账号">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="密码">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                show-password
                placeholder="请输入密码"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="login"
            >
              登录
              <el-icon class="login-button-icon"><ArrowRight /></el-icon>
            </el-button>
          </el-form>
        </div>
      </section>
    </div>
  </main>
</template>

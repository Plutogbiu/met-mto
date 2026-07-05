<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cpu, Document, OfficeBuilding, SwitchButton, User } from '@element-plus/icons-vue'
import { logout as logoutApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => route.path)
const currentUser = computed(() => {
  const value = localStorage.getItem('mto-admin-user')
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
})

function hasPermission(permission) {
  return currentUser.value?.permissions?.includes(permission)
}

async function logout() {
  try {
    await logoutApi()
  } finally {
    localStorage.removeItem('mto-admin-token')
    localStorage.removeItem('mto-admin-token-expire-at')
    localStorage.removeItem('mto-admin-user')
    router.push('/login')
  }
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="220px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">M</div>
        <div>
          <h1>MET MTO</h1>
          <p>运维工单系统</p>
        </div>
      </div>

      <el-menu router :default-active="activeMenu" class="menu">
        <el-menu-item v-if="hasPermission('work-order:list')" index="/work-orders">
          <el-icon><Document /></el-icon>
          <span>工单管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('customer:list')" index="/customers">
          <el-icon><OfficeBuilding /></el-icon>
          <span>客户管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('device:list')" index="/devices">
          <el-icon><Cpu /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item v-if="hasPermission('user:list')" index="/users">
          <el-icon><User /></el-icon>
          <span>人员管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h2>内部运维后台</h2>
          <p>现场工单、打卡、巡检和报告管理</p>
        </div>
        <el-button :icon="SwitchButton" @click="logout">退出</el-button>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

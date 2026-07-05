import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layout/AdminLayout.vue'
import LoginView from '../views/login/LoginView.vue'
import CustomerDetailView from '../views/customer/CustomerDetailView.vue'
import CustomerManageView from '../views/customer/CustomerManageView.vue'
import DeviceManageView from '../views/device/DeviceManageView.vue'
import UserManageView from '../views/user/UserManageView.vue'
import WorkOrderDetailView from '../views/work-order/WorkOrderDetailView.vue'
import WorkOrderManageView from '../views/work-order/WorkOrderManageView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/work-orders',
    children: [
      {
        path: 'work-orders',
        name: 'workOrders',
        component: WorkOrderManageView,
        meta: { title: '工单管理', permission: 'work-order:list' },
      },
      {
        path: 'work-orders/:id',
        name: 'workOrderDetail',
        component: WorkOrderDetailView,
        meta: { title: '工单详情', permission: 'work-order:detail' },
      },
      {
        path: 'customers',
        name: 'customers',
        component: CustomerManageView,
        meta: { title: '客户管理', permission: 'customer:list' },
      },
      {
        path: 'customers/:id',
        name: 'customerDetail',
        component: CustomerDetailView,
        meta: { title: '客户详情', permission: 'customer:detail' },
      },
      {
        path: 'devices',
        name: 'devices',
        component: DeviceManageView,
        meta: { title: '设备管理', permission: 'device:list' },
      },
      {
        path: 'users',
        name: 'users',
        component: UserManageView,
        meta: { title: '人员管理', permission: 'user:list' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('mto-admin-token')
  const expireAt = Number(localStorage.getItem('mto-admin-token-expire-at') || 0)
  const user = parseAdminUser()
  const isLoggedIn = Boolean(token) && expireAt > Date.now()
  const isBackendAllowed = user?.role !== 'field_engineer'
  if (to.name !== 'login' && (!isLoggedIn || !isBackendAllowed)) {
    localStorage.removeItem('mto-admin-token')
    localStorage.removeItem('mto-admin-token-expire-at')
    localStorage.removeItem('mto-admin-user')
    return { name: 'login' }
  }
  if (to.name === 'login' && isLoggedIn) {
    const path = firstAllowedPath(user)
    if (path !== '/login') {
      return { path }
    }
    return true
  }
  if (to.meta?.permission && !hasPermission(user, to.meta.permission)) {
    return { path: firstAllowedPath(user) }
  }
  return true
})

function parseAdminUser() {
  const value = localStorage.getItem('mto-admin-user')
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
}

function hasPermission(user, permission) {
  return Boolean(user?.permissions?.includes(permission))
}

function firstAllowedPath(user) {
  if (hasPermission(user, 'work-order:list')) {
    return '/work-orders'
  }
  if (hasPermission(user, 'customer:list')) {
    return '/customers'
  }
  if (hasPermission(user, 'device:list')) {
    return '/devices'
  }
  if (hasPermission(user, 'user:list')) {
    return '/users'
  }
  return '/login'
}

export default router

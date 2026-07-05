<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Edit, Key, Plus, Refresh, Search, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  getUserPage,
  getUserPermissions,
  resetUserPassword,
  updateUser,
  updateUserPermissions,
  updateUserStatus,
} from '../../api/user'

const loading = ref(false)
const dialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)
const permissionTreeRef = ref(null)
const editingId = ref(null)
const resettingUser = ref(null)
const permissionUser = ref(null)
const permissionItems = ref([])
const users = ref([])
const total = ref(0)
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

const roleOptions = [
  { label: '管理员', value: 'admin', tag: 'danger' },
  { label: '线上运维', value: 'online_ops', tag: 'warning' },
  { label: '现场实施', value: 'field_engineer', tag: 'success' },
]

const filters = reactive({
  keyword: '',
  role: '',
  status: '',
  page: 1,
  size: 10,
})

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  role: 'field_engineer',
  status: 1,
})

const passwordForm = reactive({
  password: '',
})

const dialogTitle = computed(() => (editingId.value ? '编辑人员' : '新增人员'))
const permissionDialogTitle = computed(() => {
  const name = permissionUser.value?.realName || permissionUser.value?.username || ''
  return name ? `权限分配 - ${name}` : '权限分配'
})
const permissionTreeProps = {
  label: 'label',
  children: 'children',
}
const permissionTree = computed(() => {
  const moduleMap = new Map()
  permissionItems.value.forEach((item) => {
    const moduleName = item.module || '其他'
    if (!moduleMap.has(moduleName)) {
      moduleMap.set(moduleName, {
        id: `module:${moduleName}`,
        label: moduleName,
        children: [],
      })
    }
    moduleMap.get(moduleName).children.push({
      id: item.code,
      label: item.name,
      code: item.code,
      permission: item,
    })
  })
  return Array.from(moduleMap.values())
})

function roleLabel(role) {
  return roleOptions.find((item) => item.value === role)?.label || role || '-'
}

function roleTag(role) {
  return roleOptions.find((item) => item.value === role)?.tag || 'info'
}

function hasPermission(permission) {
  return currentUser.value?.permissions?.includes(permission)
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    phone: '',
    role: 'field_engineer',
    status: 1,
  })
}

function buildPayload() {
  return {
    username: form.username.trim(),
    password: form.password,
    realName: form.realName,
    phone: form.phone,
    role: form.role,
    status: form.status,
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await getUserPage({
      keyword: filters.keyword || undefined,
      role: filters.role || undefined,
      status: filters.status === '' ? undefined : filters.status,
      page: filters.page,
      size: filters.size,
    })
    users.value = result.data?.records || []
    total.value = result.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadUsers()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    username: row.username || '',
    password: '',
    realName: row.realName || '',
    phone: row.phone || '',
    role: row.role || 'field_engineer',
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

async function saveUser() {
  if (!form.username.trim()) {
    ElMessage.warning('请填写账号')
    return
  }
  if (!editingId.value && !form.password.trim()) {
    ElMessage.warning('请填写初始密码')
    return
  }
  if (!form.role) {
    ElMessage.warning('请选择角色')
    return
  }

  if (editingId.value) {
    await updateUser(editingId.value, buildPayload())
    ElMessage.success('人员已更新')
  } else {
    await createUser(buildPayload())
    ElMessage.success('人员已新增')
  }
  dialogVisible.value = false
  await loadUsers()
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${actionText}“${row.realName || row.username}”？`, '操作确认', {
    type: 'warning',
  })
  await updateUserStatus(row.id, nextStatus)
  ElMessage.success(`${actionText}成功`)
  await loadUsers()
}

function openResetPassword(row) {
  resettingUser.value = row
  passwordForm.password = ''
  passwordDialogVisible.value = true
}

async function submitResetPassword() {
  if (!passwordForm.password.trim()) {
    ElMessage.warning('请填写新密码')
    return
  }
  await resetUserPassword(resettingUser.value.id, passwordForm.password)
  ElMessage.success('密码已重置')
  passwordDialogVisible.value = false
}

async function openPermission(row) {
  permissionUser.value = row
  permissionItems.value = []
  permissionDialogVisible.value = true
  permissionLoading.value = true
  try {
    const result = await getUserPermissions(row.id)
    permissionUser.value = result.data
    permissionItems.value = result.data?.permissions || []
    await nextTick()
    permissionTreeRef.value?.setCheckedKeys(
      permissionItems.value.filter((item) => item.granted).map((item) => item.code),
    )
  } finally {
    permissionLoading.value = false
  }
}

async function savePermissions() {
  if (!permissionUser.value?.userId) {
    return
  }
  const checkedKeys = new Set(permissionTreeRef.value?.getCheckedKeys(true) || [])
  await updateUserPermissions(
    permissionUser.value.userId,
    permissionItems.value.map((item) => {
      const checked = checkedKeys.has(item.code)
      let effect = 'inherit'
      if (item.roleGranted && !checked) {
        effect = 'deny'
      } else if (!item.roleGranted && checked) {
        effect = 'allow'
      }
      return {
        code: item.code,
        effect,
      }
    }),
  )
  ElMessage.success('权限已保存')
  permissionDialogVisible.value = false
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <section class="page-header">
    <div>
      <h1>人员管理</h1>
      <p>维护后台账号、角色和现场实施人员基础档案</p>
    </div>
    <el-button v-if="hasPermission('user:create')" type="primary" :icon="Plus" @click="openCreate">
      新增人员
    </el-button>
  </section>

  <section class="toolbar">
    <el-input
      v-model="filters.keyword"
      class="keyword"
      clearable
      placeholder="搜索账号、姓名或手机号"
      :prefix-icon="Search"
      @keyup.enter="search"
    />
    <el-select v-model="filters.role" class="role-filter" placeholder="角色" clearable>
      <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-select v-model="filters.status" class="status-filter" placeholder="状态" clearable>
      <el-option label="启用" :value="1" />
      <el-option label="停用" :value="0" />
    </el-select>
    <el-button type="primary" :icon="Search" @click="search">查询</el-button>
    <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
  </section>

  <section class="table-panel">
    <el-table v-loading="loading" :data="users" row-key="id" height="calc(100vh - 284px)">
      <el-table-column prop="username" label="账号" min-width="150" fixed />
      <el-table-column prop="realName" label="姓名" min-width="130">
        <template #default="{ row }">
          <span>{{ row.realName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="150">
        <template #default="{ row }">
          <span>{{ row.phone || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="roleTag(row.role)" effect="light">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button v-if="hasPermission('user:edit')" link type="primary" :icon="Edit" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-if="hasPermission('user:password')"
            link
            type="primary"
            :icon="Key"
            @click="openResetPassword(row)"
          >
            重置密码
          </el-button>
          <el-button v-if="hasPermission('user:permission')" link type="primary" :icon="Setting" @click="openPermission(row)">
            权限
          </el-button>
          <el-button
            v-if="hasPermission('user:status')"
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.size"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @change="loadUsers"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
    <el-form label-position="top" class="user-form">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="账号" required>
            <el-input v-model="form.username" placeholder="登录账号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="editingId ? '登录密码' : '初始密码'" :required="!editingId">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="editingId ? '不填写则不修改' : '请设置初始密码'"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="姓名">
            <el-input v-model="form.realName" placeholder="人员姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="联系电话" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="角色" required>
            <el-select v-model="form.role" class="full-control" placeholder="请选择角色">
              <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button :label="1">启用</el-radio-button>
              <el-radio-button :label="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveUser">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px" destroy-on-close>
    <el-form label-position="top">
      <el-form-item label="人员">
        <el-input :model-value="resettingUser?.realName || resettingUser?.username" disabled />
      </el-form-item>
      <el-form-item label="新密码" required>
        <el-input v-model="passwordForm.password" type="password" show-password placeholder="请输入新密码" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitResetPassword">确认重置</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="permissionDialogVisible" :title="permissionDialogTitle" width="860px" destroy-on-close>
    <div class="permission-summary">
      <el-tag :type="roleTag(permissionUser?.role)" effect="light">
        {{ roleLabel(permissionUser?.role) }}
      </el-tag>
      <span>继承角色默认权限，可对单个用户额外允许或拒绝。</span>
    </div>

    <div v-loading="permissionLoading" class="permission-tree-panel">
      <el-tree
        ref="permissionTreeRef"
        :data="permissionTree"
        :props="permissionTreeProps"
        node-key="id"
        show-checkbox
        default-expand-all
      >
        <template #default="{ data }">
          <div class="permission-node" :class="{ module: !data.permission }">
            <span>{{ data.label }}</span>
            <template v-if="data.permission">
              <small>{{ data.code }}</small>
              <el-tag v-if="data.permission.roleGranted" type="success" effect="light">角色默认</el-tag>
              <el-tag v-else type="info" effect="light">需额外授权</el-tag>
            </template>
          </div>
        </template>
      </el-tree>
    </div>

    <template #footer>
      <el-button @click="permissionDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="savePermissions">保存权限</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.role-filter {
  width: 150px;
}

.user-form .el-form-item {
  margin-bottom: 16px;
}

.full-control {
  width: 100%;
}

.permission-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  color: #667085;
  font-size: 13px;
}

.permission-tree-panel {
  min-height: 360px;
  max-height: 560px;
  overflow: auto;
  padding: 10px 8px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfcfe;
}

.permission-node {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.permission-node.module {
  color: #172033;
  font-weight: 700;
}

.permission-node small {
  color: #98a2b3;
  font-size: 12px;
}
</style>

<template>
  <div>
    <div class="page-header">
      <div class="page-titles">
        <h2>用户管理</h2>
        <p class="page-desc">系统用户账号维护与角色分配</p>
      </div>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="users" v-loading="loading" border>
      <el-table-column prop="userNo" label="用户编号" width="140" />
      <el-table-column prop="username" label="登录账号" width="150" />
      <el-table-column prop="displayName" label="姓名" width="120" />
      <el-table-column label="部门" min-width="140">
        <template #default="{ row }">
          {{ departmentName(row.departmentId) || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column prop="mobile" label="手机号" width="140" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          {{ roleNames(row.roleIds) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <div class="op-btns">
            <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑用户' : '新增用户'"
      width="680px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户编号" required>
              <el-input v-model="form.userNo" placeholder="请输入用户编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录账号" required>
              <el-input v-model="form.username" placeholder="请输入登录账号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" required>
              <el-input v-model="form.displayName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-select
                v-model="form.departmentId"
                clearable
                filterable
                placeholder="请选择部门"
                style="width: 100%"
              >
                <el-option
                  v-for="department in departments"
                  :key="department.id"
                  :label="department.name"
                  :value="department.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.mobile" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '不修改请留空' : '不填默认密码 123456'"
          />
        </el-form-item>

        <el-form-item label="角色">
          <el-select
            v-model="form.roleIds"
            multiple
            filterable
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  deleteUser,
  listUsers,
  updateUser,
  type UserCreateRequest,
  type UserResponse,
  type UserUpdateRequest
} from '../../api/user'
import {
  listDepartments,
  type DepartmentResponse
} from '../../api/department'
import {
  listRoles,
  type RoleResponse
} from '../../api/role'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const users = ref<UserResponse[]>([])
const departments = ref<DepartmentResponse[]>([])
const roles = ref<RoleResponse[]>([])

const form = reactive<UserCreateRequest & { id?: number; password: string }>({
  id: undefined,
  userNo: '',
  username: '',
  displayName: '',
  departmentId: undefined,
  email: '',
  mobile: '',
  password: '',
  status: 1,
  roleIds: []
})

function departmentName(departmentId?: number) {
  return departments.value.find((item) => item.id === departmentId)?.name
}

function roleNames(roleIds: number[]) {
  return roleIds
    .map((id) => roles.value.find((role) => role.id === id)?.name)
    .filter(Boolean)
    .join('、')
}

async function loadData() {
  loading.value = true
  try {
    const [userList, departmentList, roleList] = await Promise.all([
      listUsers(),
      listDepartments(),
      listRoles()
    ])
    users.value = userList
    departments.value = departmentList
    roles.value = roleList
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.userNo = ''
  form.username = ''
  form.displayName = ''
  form.departmentId = undefined
  form.email = ''
  form.mobile = ''
  form.password = ''
  form.status = 1
  form.roleIds = []
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserResponse) {
  resetForm()
  form.id = row.id
  form.userNo = row.userNo
  form.username = row.username
  form.displayName = row.displayName
  form.departmentId = row.departmentId
  form.email = row.email || ''
  form.mobile = row.mobile || ''
  form.status = row.status
  form.roleIds = [...row.roleIds]
  dialogVisible.value = true
}

async function save() {
  if (!form.userNo.trim() || !form.username.trim() || !form.displayName.trim()) {
    ElMessage.warning('请填写用户编号、登录账号和姓名')
    return
  }

  saving.value = true
  try {
    if (form.id) {
      const payload: UserUpdateRequest = {
        userNo: form.userNo,
        username: form.username,
        displayName: form.displayName,
        departmentId: form.departmentId,
        email: form.email,
        mobile: form.mobile,
        password: form.password || undefined,
        status: form.status,
        roleIds: form.roleIds
      }
      await updateUser(form.id, payload)
      ElMessage.success('用户更新成功')
    } else {
      const payload: UserCreateRequest = {
        userNo: form.userNo,
        username: form.username,
        displayName: form.displayName,
        departmentId: form.departmentId,
        email: form.email,
        mobile: form.mobile,
        password: form.password || undefined,
        status: form.status,
        roleIds: form.roleIds
      }
      await createUser(payload)
      ElMessage.success('用户新增成功')
    }

    dialogVisible.value = false
    await loadData()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: UserResponse) {
  try {
    await ElMessageBox.confirm(`确定删除用户“${row.displayName}”吗？`, '删除确认', {
      type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // 用户取消删除
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>

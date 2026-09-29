<template>
  <div>
    <div class="page-header">
      <div class="page-titles">
        <h2>角色管理</h2>
        <p class="page-desc">系统角色定义与功能权限配置</p>
      </div>
      <el-button type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="roles" v-loading="loading" border>
      <el-table-column prop="code" label="角色编码" width="180" />
      <el-table-column prop="name" label="角色名称" width="160" />
      <el-table-column prop="description" label="描述" min-width="220" />
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
      :title="form.id ? '编辑角色' : '新增角色'"
      width="520px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色编码" required>
          <el-input v-model="form.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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
  createRole,
  deleteRole,
  listRoles,
  updateRole,
  type RoleCreateRequest,
  type RoleResponse,
  type RoleUpdateRequest
} from '../../api/role'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const roles = ref<RoleResponse[]>([])

const form = reactive<RoleUpdateRequest & { id?: number }>({
  id: undefined,
  code: '',
  name: '',
  description: '',
  status: 1
})

async function loadData() {
  loading.value = true
  try {
    roles.value = await listRoles()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.code = ''
  form.name = ''
  form.description = ''
  form.status = 1
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: RoleResponse) {
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.description = row.description || ''
  form.status = row.status
  dialogVisible.value = true
}

async function save() {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('请填写角色编码和角色名称')
    return
  }

  saving.value = true
  try {
    if (form.id) {
      const payload: RoleUpdateRequest = {
        code: form.code,
        name: form.name,
        description: form.description,
        status: form.status
      }
      await updateRole(form.id, payload)
      ElMessage.success('角色更新成功')
    } else {
      const payload: RoleCreateRequest = {
        code: form.code,
        name: form.name,
        description: form.description
      }
      await createRole(payload)
      ElMessage.success('角色新增成功')
    }

    dialogVisible.value = false
    await loadData()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: RoleResponse) {
  try {
    await ElMessageBox.confirm(`确定删除角色“${row.name}”吗？`, '删除确认', {
      type: 'warning'
    })
    await deleteRole(row.id)
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

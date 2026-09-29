<template>
  <div>
    <div class="page-header">
      <div class="page-titles">
        <h2>部门管理</h2>
        <p class="page-desc">组织部门架构与层级维护</p>
      </div>
      <el-button type="primary" @click="openCreate">新增部门</el-button>
    </div>

    <div class="table-panel">
      <el-table :data="departments" v-loading="loading" border>
      <el-table-column prop="code" label="部门编码" width="160" />
      <el-table-column prop="name" label="部门名称" min-width="160" />
      <el-table-column label="上级部门" min-width="140">
        <template #default="{ row }">
          {{ parentName(row.parentId) || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
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
      :title="form.id ? '编辑部门' : '新增部门'"
      width="520px"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="部门编码" required>
          <el-input v-model="form.code" placeholder="请输入部门编码" />
        </el-form-item>

        <el-form-item label="部门名称" required>
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>

        <el-form-item label="上级部门">
          <el-select
            v-model="form.parentId"
            clearable
            filterable
            placeholder="请选择上级部门"
            style="width: 100%"
          >
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDepartment,
  deleteDepartment,
  listDepartments,
  updateDepartment,
  type DepartmentCreateRequest,
  type DepartmentResponse
} from '../../api/department'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const departments = ref<DepartmentResponse[]>([])

const form = reactive<DepartmentCreateRequest & { id?: number }>({
  id: undefined,
  code: '',
  name: '',
  parentId: undefined,
  sortOrder: 0
})

const parentOptions = computed(() => {
  return departments.value.filter((item) => item.id !== form.id)
})

function parentName(parentId?: number) {
  return departments.value.find((item) => item.id === parentId)?.name
}

async function loadData() {
  loading.value = true
  try {
    departments.value = await listDepartments()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.code = ''
  form.name = ''
  form.parentId = undefined
  form.sortOrder = 0
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: DepartmentResponse) {
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.parentId = row.parentId
  form.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

async function save() {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('请填写部门编码和名称')
    return
  }

  saving.value = true
  try {
    const payload: DepartmentCreateRequest = {
      code: form.code,
      name: form.name,
      parentId: form.parentId,
      sortOrder: form.sortOrder
    }

    if (form.id) {
      await updateDepartment(form.id, payload)
      ElMessage.success('部门更新成功')
    } else {
      await createDepartment(payload)
      ElMessage.success('部门新增成功')
    }

    dialogVisible.value = false
    await loadData()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DepartmentResponse) {
  try {
    await ElMessageBox.confirm(`确定删除部门“${row.name}”吗？`, '删除确认', {
      type: 'warning'
    })
    await deleteDepartment(row.id)
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

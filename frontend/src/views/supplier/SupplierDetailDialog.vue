<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="1200px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="application" class="detail-body">
      <el-alert
        :type="selectedSupplier ? 'success' : 'info'"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
      >
        <template #title>
          {{ application.applicationNo }} - {{ application.title }}
          <template v-if="selectedSupplier">
            （已选定中标供应商：{{ selectedSupplier.supplierName }}）
          </template>
        </template>
      </el-alert>

      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索供应商名称 / 联系人 / 电话"
          clearable
          style="width: 280px"
          @keyup.enter="doSearch"
        >
          <template #append>
            <el-button @click="doSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="filteredCandidates" size="small" border v-loading="loading" :row-class-name="rowClassName">
        <el-table-column label="#" type="index" width="46" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="nowrap-text">{{ row.supplierName }}</span>
            <el-tag v-if="row.selected" type="success" size="small" style="margin-left: 6px">已选定</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="90" />
        <el-table-column prop="contactPhone" label="电话" width="130" />
        <el-table-column label="报价(元)" width="120" align="right">
          <template #default="{ row }">
            {{ row.quotedAmount == null ? '—' : Number(row.quotedAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
          </template>
        </el-table-column>
        <el-table-column label="交期(天)" width="90" align="center">
          <template #default="{ row }">{{ row.deliveryDays == null ? '—' : row.deliveryDays }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="110" show-overflow-tooltip />
        <el-table-column v-if="canEdit && !selectedSupplier" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" link @click="removeSupplier(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <span class="empty-text">暂无候选供应商</span>
        </template>
      </el-table>
    </div>

    <CandidateSupplierEditDialog
      v-model="editVisible"
      :application-id="application?.id || 0"
      :supplier="editingSupplier"
      @success="loadCandidates"
    />

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listApplicationSuppliers,
  deleteApplicationSupplier,
  type ApplicationSupplierResponse,
  type ApplicationResponse
} from '../../api/application'
import { hasAnyRole } from '../../utils/auth'
import CandidateSupplierEditDialog from './CandidateSupplierEditDialog.vue'

const props = defineProps<{
  modelValue: boolean
  application: ApplicationResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const title = props.application
  ? `供应商详情 - ${props.application.applicationNo}`
  : '供应商详情'

// 只有采购员和管理员可以编辑（采购负责人只能查看）
const roleCodes = JSON.parse(localStorage.getItem('roleCodes') || '[]')
const isPureManager = roleCodes.includes('PURCHASE_MANAGER') && !roleCodes.includes('BUYER') && !roleCodes.includes('ADMIN')
const canEdit = hasAnyRole(['BUYER', 'ADMIN']) && !isPureManager
const loading = ref(false)
const keyword = ref('')
const candidates = ref<ApplicationSupplierResponse[]>([])
const editVisible = ref(false)
const editingSupplier = ref<ApplicationSupplierResponse | null>(null)

const filteredCandidates = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return candidates.value
  return candidates.value.filter(c =>
    (c.supplierName || '').toLowerCase().includes(kw) ||
    (c.contactPerson || '').toLowerCase().includes(kw) ||
    (c.contactPhone || '').toLowerCase().includes(kw)
  )
})

// 已选定的中标供应商
const selectedSupplier = computed(() => candidates.value.find(c => c.selected) || null)

function rowClassName({ row }: { row: ApplicationSupplierResponse }) {
  return row.selected ? 'selected-row' : ''
}

watch(
  () => props.modelValue,
  open => {
    if (open) {
      keyword.value = ''
      loadCandidates()
    }
  }
)

async function loadCandidates() {
  if (!props.application) return
  loading.value = true
  try {
    const list = await listApplicationSuppliers(props.application.id)
    candidates.value = [...list].sort((a, b) => {
      if (a.quotedAmount == null && b.quotedAmount == null) return 0
      if (a.quotedAmount == null) return 1
      if (b.quotedAmount == null) return -1
      return Number(a.quotedAmount) - Number(b.quotedAmount)
    })
  } catch {
    candidates.value = []
  } finally {
    loading.value = false
  }
}

function doSearch() {
  // 前端过滤，无需重新请求
}

function openEdit(row: ApplicationSupplierResponse) {
  editingSupplier.value = row
  editVisible.value = true
}

async function removeSupplier(row: ApplicationSupplierResponse) {
  if (!props.application) return
  try {
    await ElMessageBox.confirm(`确定删除候选供应商"${row.supplierName}"吗？`, '删除确认', { type: 'warning' })
    await deleteApplicationSupplier(props.application.id, row.id)
    ElMessage.success('已删除')
    await loadCandidates()
    emit('success')
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.nowrap-text {
  display: inline-block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.detail-body {
  min-height: 120px;
}

.search-bar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}

.empty-text {
  color: #909399;
  font-size: 13px;
}

:deep(.selected-row) {
  background-color: #f0f9eb;
}

:deep(.selected-row:hover > td) {
  background-color: #e1f3d8 !important;
}
</style>

<template>
  <div>
    <div class="page-header">
      <div class="page-titles">
        <h2>采购申请</h2>
        <p class="page-desc">采购申请发起、提交与审批跟踪</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="openCreate">
        发起申请
      </el-button>
    </div>

    <div class="table-panel">
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索申请单号或标题"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchStatus"
          placeholder="全部状态"
          clearable
          style="width: 160px"
        >
          <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="applicationNo" label="申请单号" width="200" />
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column label="申请部门" width="120">
        <template #default="{ row }">{{ row.departmentName || '#' + row.departmentId }}</template>
      </el-table-column>
      <el-table-column label="申请人" width="100">
        <template #default="{ row }">{{ row.applicantName || '#' + row.applicantId }}</template>
      </el-table-column>
      <el-table-column label="审批人" width="100">
        <template #default="{ row }">{{ row.approverName || '未指定' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="120">
        <template #default="{ row }">{{ row.currency ?? 'CNY' }} {{ Number(row.totalAmount ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="statusTagType[row.status] || 'info'" effect="light">
            {{ statusMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <div class="op-btns">
            <el-button
              v-if="canCreate && row.status === 'DRAFT'"
              size="small"
              type="primary"
              link
              @click="handleSubmit(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="canReview && (row.status === 'SUBMITTED' || row.status === 'PENDING_APPROVAL')"
              size="small"
              type="primary"
              link
              @click="handleApprove(row)"
            >
              审批
            </el-button>
            <el-button
              v-if="canCreate && (row.status === 'REJECTED' || row.status === 'WITHDRAWN')"
              size="small"
              type="primary"
              link
              @click="handleReapply(row)"
            >
              重新申请
            </el-button>
            <el-button
              v-if="isAdmin && row.status !== 'APPROVED'"
              size="small"
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="isAdmin"
              size="small"
              type="primary"
              link
              @click="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              v-if="isAdmin && row.status !== 'APPROVED'"
              size="small"
              type="danger"
              link
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <ApplicationCreateDialog v-model="createVisible" :edit-id="editId" :prefill="dialogPrefill" @success="loadData" />
    <ApprovalDialog v-model="approvalVisible" :target="approvalTarget" @success="loadData" />
    <ApplicationDetailDialog v-model="detailVisible" :target="detailTarget" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ApplicationCreateDialog from './ApplicationCreateDialog.vue'
import ApprovalDialog from './ApprovalDialog.vue'
import ApplicationDetailDialog from './ApplicationDetailDialog.vue'
import { hasAnyRole } from '../../utils/auth'
import {
  listApplications,
  getApplication,
  submitApplication,
  deleteApplication,
  type ApplicationResponse,
  type ApplicationItemRequest,
  type AttachmentInfo
} from '../../api/application'

/** 与 ApplicationCreateDialog 的 prefill prop 契约对齐的预填表单类型 */
type DialogPrefill = {
  title?: string
  departmentId?: number
  approverId?: number
  requiredDate?: string
  purpose?: string
  urgent?: boolean
  items?: ApplicationItemRequest[]
  attachments?: AttachmentInfo[]
}

const statusMap: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  VALIDATING: '校验中',
  PENDING_APPROVAL: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回'
}

const statusTagType: Record<string, 'info' | 'warning' | 'primary' | 'success' | 'danger'> = {
  DRAFT: 'info',
  SUBMITTED: 'primary',
  VALIDATING: 'warning',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  WITHDRAWN: 'info'
}

function formatDate(value: string | undefined | null): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const rows = ref<ApplicationResponse[]>([])
const loading = ref(false)
const createVisible = ref(false)
const dialogPrefill = ref<DialogPrefill | null>(null)
const editId = ref<number | null>(null)
const approvalVisible = ref(false)
const approvalTarget = ref<ApplicationResponse | null>(null)
const detailVisible = ref(false)
const detailTarget = ref<ApplicationResponse | null>(null)
const searchKeyword = ref('')
const searchStatus = ref<string | undefined>(undefined)

const isAdmin = hasAnyRole(['ADMIN'])
const canCreate = hasAnyRole(['APPLICANT'])
const canReview = hasAnyRole(['APPROVER', 'ADMIN'])

async function loadData() {
  loading.value = true
  try {
    const data = await listApplications(0, 20, {
      keyword: searchKeyword.value?.trim() || undefined,
      status: searchStatus.value || undefined
    })
    rows.value = data.content
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  searchKeyword.value = ''
  searchStatus.value = undefined
  loadData()
}

async function handleSubmit(row: ApplicationResponse) {
  await submitApplication(row.id)
  ElMessage.success('提交成功')
  await loadData()
}

/** 审批按钮：打开人工审批对话框（通过/驳回都在对话框里操作） */
function handleApprove(row: ApplicationResponse) {
  approvalTarget.value = row
  approvalVisible.value = true
}

async function handleReapply(row: ApplicationResponse) {
  try {
    await ElMessageBox.confirm(
      `基于申请「${row.title}」的内容重新发起？`,
      '重新申请',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch { return }

  try {
    const detail = await getApplication(row.id)
    // 把后端详情映射成表单数据结构
    dialogPrefill.value = {
      title: detail.title,
      departmentId: detail.departmentId,
      // 后端 LocalDate 可能是 YYYY-MM-DD 或 ISO 格式，兼容一下
      requiredDate: detail.requiredDate?.slice(0, 10) ?? '',
      purpose: detail.purpose ?? '',
      urgent: detail.applicationType === 'URGENT',
      items: detail.items.map(item => ({
        materialCode: item.materialCode,
        materialName: item.materialName,
        specification: item.specification,
        unit: item.unit,
        quantity: item.quantity,
        estimatedUnitPrice: item.estimatedUnitPrice,
        estimatedAmount: item.estimatedAmount,
        remark: item.remark
      }))
    }
    createVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '加载申请详情失败')
  }
}

// 发起申请按钮：清空 prefill + editId 再开弹窗
function openCreate() {
  editId.value = null
  dialogPrefill.value = null
  createVisible.value = true
}

/** 管理员编辑：拉详情 → 回填 → 开编辑弹窗 */
async function handleEdit(row: ApplicationResponse) {
  try {
    const detail = await getApplication(row.id)
    editId.value = detail.id
    dialogPrefill.value = {
      title: detail.title,
      departmentId: detail.departmentId,
      approverId: detail.approverId,
      requiredDate: detail.requiredDate?.slice(0, 10) ?? '',
      purpose: detail.purpose ?? '',
      urgent: detail.applicationType === 'URGENT',
      items: detail.items.map(item => ({
        materialCode: item.materialCode,
        materialName: item.materialName,
        specification: item.specification,
        unit: item.unit,
        quantity: item.quantity,
        estimatedUnitPrice: item.estimatedUnitPrice,
        estimatedAmount: item.estimatedAmount,
        remark: item.remark
      })),
      attachments: detail.attachments
    }
    createVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '加载申请详情失败')
  }
}

/** 管理员查看：打开详情弹窗（内部自动拉取最新详情） */
function handleView(row: ApplicationResponse) {
  detailTarget.value = row
  detailVisible.value = true
}

/** 管理员软删除 */
async function handleDelete(row: ApplicationResponse) {
  try {
    await ElMessageBox.confirm(
      `确定删除申请「${row.title}」？此操作不可恢复（数据将被软删除）。`,
      '删除确认',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch { return }

  try {
    await deleteApplication(row.id)
    ElMessage.success('已删除')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? '删除失败')
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>

<template>
  <el-dialog
    :model-value="modelValue"
    title="申请详情"
    width="900px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="detail-body">
      <!-- 表头字段区：申请单 + 状态 + 人员/部门 + 金额时间 + 事由 + 审批意见 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号" label-width="110px">
          {{ detail?.applicationNo ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType[detail?.status ?? ''] || 'info'" effect="light">
            {{ statusMap[detail?.status ?? ''] ?? detail?.status ?? '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请标题" :span="2">
          {{ detail?.title ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="申请部门">
          {{ deptName(detail?.departmentId) }}
        </el-descriptions-item>
        <el-descriptions-item label="申请类型">
          <el-tag :type="detail?.applicationType === 'URGENT' ? 'danger' : 'info'" effect="plain">
            {{ detail?.applicationType === 'URGENT' ? '紧急' : '普通' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请人">
          {{ userName(detail?.applicantId) }}
        </el-descriptions-item>
        <el-descriptions-item label="审批人">
          {{ userName(detail?.approverId) }}
        </el-descriptions-item>
        <el-descriptions-item label="金额合计">
          <span class="amount-text">
            {{ detail?.currency ?? 'CNY' }} {{ formatAmount(detail?.totalAmount) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="期望到货">
          {{ formatDate(detail?.requiredDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDateTime(detail?.submittedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(detail?.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="申请事由" :span="2">
          {{ detail?.purpose || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">
          <span :class="{ 'reject-text': detail?.status === 'REJECTED' }">
            {{ detail?.approvalComment || '（未填写）' }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 明细区：全部明细字段 -->
      <p class="section-title">采购明细</p>
      <el-table :data="detail?.items ?? []" size="small" border>
        <el-table-column prop="lineNo" label="#" width="46" />
        <el-table-column prop="materialCode" label="物料编码" min-width="110" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格型号" min-width="100" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="64" />
        <el-table-column prop="quantity" label="数量" width="76" align="right" />
        <el-table-column label="预估单价" width="96" align="right">
          <template #default="{ row }">{{ formatAmount(row.estimatedUnitPrice) }}</template>
        </el-table-column>
        <el-table-column label="预估金额" width="104" align="right">
          <template #default="{ row }">{{ formatAmount(row.estimatedAmount) }}</template>
        </el-table-column>
        <el-table-column label="行到货日期" width="106">
          <template #default="{ row }">{{ formatDate(row.requiredDate) }}</template>
        </el-table-column>
        <el-table-column prop="suggestSupplierCode" label="建议供应商编码" min-width="116" show-overflow-tooltip />
        <el-table-column prop="suggestSupplierName" label="建议供应商名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="110" show-overflow-tooltip />
      </el-table>

      <!-- 附件区 -->
      <p class="section-title">附件</p>
      <el-table :data="detail?.attachments ?? []" size="small" border>
        <el-table-column label="#" type="index" width="46" />
        <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="file-name">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="96" align="right">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="168">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button
                v-if="isPreviewable(row.contentType)"
                size="small"
                type="primary"
                link
                @click="handlePreview(row)"
              >
                预览
              </el-button>
              <el-button size="small" type="primary" link @click="handleDownload(row)">
                下载
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <span class="empty-text">无附件</span>
        </template>
      </el-table>

      <!-- 候选供应商（询价名单） -->
      <div class="section-head">
        <p class="section-title">候选供应商</p>
        <el-button v-if="canEditSupplier" size="small" type="primary" plain @click="supplierFormVisible = !supplierFormVisible">
          {{ supplierFormVisible ? '收起' : '录入供应商' }}
        </el-button>
      </div>

      <el-form
        v-if="supplierFormVisible && canEditSupplier"
        :model="supplierForm"
        inline
        class="supplier-form"
        size="small"
      >
        <el-form-item label="供应商名称" required>
          <el-input v-model="supplierForm.supplierName" placeholder="必填，手动输入" style="width: 170px" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="supplierForm.contactPerson" placeholder="选填" style="width: 110px" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="supplierForm.contactPhone" placeholder="选填" style="width: 130px" />
        </el-form-item>
        <el-form-item label="报价(元)">
          <el-input-number v-model="supplierForm.quotedAmount" :min="0" :precision="2" :controls="false" style="width: 130px" />
        </el-form-item>
        <el-form-item label="交期(天)">
          <el-input-number v-model="supplierForm.deliveryDays" :min="0" :controls="false" style="width: 100px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="supplierForm.remark" placeholder="选填" style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingSupplier" @click="addSupplier">添加</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="candidateSuppliers" size="small" border v-loading="supplierLoading">
        <el-table-column label="#" type="index" width="46" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="160" show-overflow-tooltip />
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
        <el-table-column v-if="canEditSupplier" label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="removeSupplier(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="empty-text">暂无候选供应商</span>
        </template>
      </el-table>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getApplication,
  listApplicationSuppliers,
  addApplicationSupplier,
  deleteApplicationSupplier,
  type ApplicationResponse,
  type ApplicationDetailResponse,
  type AttachmentInfo,
  type ApplicationSupplierResponse
} from '../../api/application'
import { fetchAttachment } from '../../api/file'
import { listUsers } from '../../api/user'
import { listDepartments } from '../../api/department'
import { hasAnyRole } from '../../utils/auth'

const props = defineProps<{
  modelValue: boolean
  target: ApplicationResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const detail = ref<ApplicationDetailResponse | null>(null)
const loading = ref(false)

// 候选供应商（询价名单）
const candidateSuppliers = ref<ApplicationSupplierResponse[]>([])
const supplierLoading = ref(false)
const savingSupplier = ref(false)
const supplierFormVisible = ref(false)
const canEditSupplier = hasAnyRole(['APPLICANT', 'ADMIN'])
const supplierForm = ref({
  supplierName: '',
  contactPerson: '',
  contactPhone: '',
  quotedAmount: undefined as number | undefined,
  deliveryDays: undefined as number | undefined,
  remark: ''
})

// ID → 名称映射，查不到时降级显示 #ID
const userMap = ref<Record<number, string>>({})
const deptMap = ref<Record<number, string>>({})

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

// 每次打开都重新拉取，保证看到的是最新数据
watch(
  () => props.modelValue,
  open => {
    if (open) loadDetail()
  }
)

async function loadDetail() {
  if (!props.target) return
  loading.value = true
  try {
    // 详情和名称映射并行加载；用户/部门接口仅管理员可用，
    // 用 allSettled 保证映射接口失败时不影响详情本身展示
    const [detailRes, usersRes, deptsRes] = await Promise.allSettled([
      getApplication(props.target.id),
      listUsers(),
      listDepartments()
    ])
    if (detailRes.status === 'rejected') {
      ElMessage.error(detailRes.reason?.response?.data?.message ?? '加载申请详情失败')
      return
    }
    detail.value = detailRes.value
    if (usersRes.status === 'fulfilled') {
      userMap.value = Object.fromEntries(
        usersRes.value.map(u => [u.id, u.displayName || u.username])
      )
    }
    if (deptsRes.status === 'fulfilled') {
      deptMap.value = Object.fromEntries(
        deptsRes.value.map(d => [d.id, d.name])
      )
    }
    await loadCandidates()
  } finally {
    loading.value = false
  }
}

async function loadCandidates() {
  if (!props.target) return
  supplierLoading.value = true
  try {
    candidateSuppliers.value = await listApplicationSuppliers(props.target.id)
  } catch {
    candidateSuppliers.value = []
  } finally {
    supplierLoading.value = false
  }
}

function resetSupplierForm() {
  supplierForm.value = {
    supplierName: '',
    contactPerson: '',
    contactPhone: '',
    quotedAmount: undefined,
    deliveryDays: undefined,
    remark: ''
  }
}

async function addSupplier() {
  if (!props.target || !supplierForm.value.supplierName.trim()) {
    ElMessage.warning('请填写供应商名称')
    return
  }
  savingSupplier.value = true
  try {
    await addApplicationSupplier(props.target.id, {
      supplierName: supplierForm.value.supplierName.trim(),
      contactPerson: supplierForm.value.contactPerson || undefined,
      contactPhone: supplierForm.value.contactPhone || undefined,
      quotedAmount: supplierForm.value.quotedAmount,
      deliveryDays: supplierForm.value.deliveryDays,
      remark: supplierForm.value.remark || undefined
    })
    ElMessage.success('候选供应商已添加')
    resetSupplierForm()
    await loadCandidates()
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    savingSupplier.value = false
  }
}

async function removeSupplier(row: ApplicationSupplierResponse) {
  if (!props.target) return
  try {
    await ElMessageBox.confirm(`确定删除候选供应商“${row.supplierName}”吗？`, '删除确认', { type: 'warning' })
    await deleteApplicationSupplier(props.target.id, row.id)
    ElMessage.success('已删除')
    await loadCandidates()
  } catch {
    // 用户取消
  }
}

function userName(id: number | undefined): string {
  if (id == null) return '未指定'
  return userMap.value[id] ?? `#${id}`
}

function deptName(id: number | undefined): string {
  if (id == null) return '-'
  return deptMap.value[id] ?? `#${id}`
}

function formatAmount(value: number | undefined | null): string {
  return value == null ? '-' : value.toFixed(2)
}

/** 纯日期 YYYY-MM-DD；空值返回 - */
function formatDate(value: string | undefined | null): string {
  return value ? value.slice(0, 10) : '-'
}

/** 日期 + 时分秒 */
function formatDateTime(value: string | undefined | null): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/** 文件大小转成人类可读单位 */
function formatFileSize(bytes: number | undefined | null): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

/** 只有浏览器原生能打开的图片和 PDF 提供预览，其余类型只给下载 */
function isPreviewable(contentType?: string): boolean {
  return !!contentType && (contentType.startsWith('image/') || contentType === 'application/pdf')
}

/** 用拿到的二进制生成临时地址，用完要 revoke 释放内存 */
function saveBlob(blob: Blob): string {
  const url = URL.createObjectURL(blob)
  // 新标签页/下载是异步消费，延迟 60 秒回收，避免过早释放导致空白
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
  return url
}

async function handlePreview(row: AttachmentInfo) {
  try {
    const blob = await fetchAttachment(row.id, 'inline')
    window.open(saveBlob(blob), '_blank')
  } catch {
    ElMessage.error('附件预览失败，请尝试下载后查看')
  }
}

async function handleDownload(row: AttachmentInfo) {
  try {
    const blob = await fetchAttachment(row.id, 'download')
    const link = document.createElement('a')
    link.href = saveBlob(blob)
    link.download = row.fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
  } catch {
    ElMessage.error('附件下载失败')
  }
}
</script>

<style scoped>
.detail-body {
  min-height: 200px;
}

.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  font-size: 14px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-head .section-title {
  margin-bottom: 8px;
}

.supplier-form {
  margin: 4px 0 10px;
  padding: 10px 12px 0;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.supplier-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.amount-text {
  color: #f56c6c;
  font-weight: 600;
}

.reject-text {
  color: #f56c6c;
}

.file-name {
  color: #303133;
}

.empty-text {
  color: #909399;
  font-size: 13px;
}
</style>

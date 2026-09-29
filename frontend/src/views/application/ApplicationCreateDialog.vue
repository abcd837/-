<template>
  <el-dialog
    :model-value="modelValue"
    :title="editId ? '编辑申请' : prefill ? '重新申请' : '发起采购申请'"
    width="980px"
    @update:model-value="$emit('update:modelValue', $event)"
    @open="loadDepartments"
    @closed="resetForm"
  >
    <el-form :model="form" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="申请标题" required>
            <el-input v-model="form.title" placeholder="请输入申请标题" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="申请部门" required>
            <el-select
              v-model="form.departmentId"
              filterable
              placeholder="请选择申请部门"
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

        <el-col :span="12">
          <el-form-item label="审批人" required>
            <el-select
              v-model="form.approverId"
              filterable
              placeholder="请选择审批人"
              style="width: 100%"
            >
              <el-option
                v-for="u in approvers"
                :key="u.id"
                :label="u.displayName || u.username"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="期望到货时间">
            <el-date-picker
              v-model="form.requiredDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选填"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="紧急标识">
            <el-switch
              v-model="form.urgent"
              active-text="紧急"
              inactive-text="普通"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="申请事由">
        <el-input
          v-model="form.purpose"
          type="textarea"
          :rows="3"
          placeholder="请输入申请事由"
        />
      </el-form-item>

      <el-divider content-position="left">采购明细</el-divider>

      <div
        v-for="(item, index) in form.items"
        :key="index"
        class="item-row"
      >
        <el-input v-model="item.materialCode" placeholder="物料编码" />
        <el-input v-model="item.materialName" placeholder="物料名称" />
        <el-input v-model="item.specification" placeholder="规格型号" />
        <el-input v-model="item.unit" placeholder="单位" />
        <el-input-number
          v-model="item.quantity"
          :min="1"
          :precision="1"
          placeholder="申请数量"
          @change="recalculateAmount(item)"
        />
        <el-input-number
          v-model="item.estimatedUnitPrice"
          :min="0"
          :precision="4"
          placeholder="预估单价"
          @change="recalculateAmount(item)"
        />
        <el-input-number
          :model-value="item.estimatedAmount"
          :precision="2"
          disabled
          placeholder="预估金额"
        />
        <el-input v-model="item.remark" placeholder="备注" />
        <el-button type="danger" @click="removeItem(index)">删除</el-button>
      </div>

      <div class="detail-actions">
        <el-button @click="addItem">添加明细</el-button>
        <div class="total-amount">
          总金额：<span>¥{{ totalAmount.toFixed(2) }}</span>
        </div>
      </div>

      <el-divider content-position="left">附件</el-divider>

      <!-- 编辑模式：已保存的附件，点 x 表示本次保存时删除 -->
      <div v-if="existingAttachments.length" class="existing-files">
        <div v-for="att in existingAttachments" :key="att.id" class="existing-file">
          <el-icon><Document /></el-icon>
          <span class="existing-file__name">{{ att.fileName }}</span>
          <span class="existing-file__size">{{ formatFileSize(att.fileSize) }}</span>
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click="removeExisting(att.id)"
          >
            移除
          </el-button>
        </div>
      </div>

      <el-upload
        v-model:file-list="fileList"
        multiple
        drag
        :auto-upload="false"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.zip"
      >
        <div class="el-upload__text">
          拖拽文件到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            可上传技术规格说明书、需求说明文档、图片、表格、压缩包等附件
          </div>
        </template>
      </el-upload>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">
        {{ editId ? '保存修改' : '保存草稿' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Delete } from '@element-plus/icons-vue'
import type { UploadUserFile } from 'element-plus'
import {
  createApplication,
  updateApplication,
  type ApplicationCreateRequest,
  type ApplicationItemRequest,
  type AttachmentInfoRequest,
  type AttachmentInfo
} from '../../api/application'
import { uploadFile } from '../../api/file'
import {
  listDepartments,
  type DepartmentResponse
} from '../../api/department'
import {
  listApprovers
} from '../../api/user'

const props = defineProps<{
  modelValue: boolean
  /** 编辑模式下申请的 id；同时存在 prefill 时优先 editId（直接走 update 而非 create） */
  editId?: number | null
  /** 重新申请 / 编辑时预填的表单数据 */
  prefill?: {
    title?: string
    departmentId?: number
    approverId?: number
    requiredDate?: string
    purpose?: string
    urgent?: boolean
    items?: ApplicationItemRequest[]
    /** 编辑模式下已保存的附件，重新申请（新建草稿）不带 */
    attachments?: AttachmentInfo[]
  } | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'success'): void
}>()

const saving = ref(false)
const fileList = ref<UploadUserFile[]>([])
/** 编辑回显的已保存附件；保存时仍在这里的按“保留”提交，被移除的通知后端删除 */
const existingAttachments = ref<AttachmentInfo[]>([])
const departments = ref<DepartmentResponse[]>([])
const approvers = ref<{ id: number; displayName?: string; username: string }[]>([])

function emptyItem(): ApplicationItemRequest {
  return {
    materialName: '',
    quantity: 0,
    estimatedUnitPrice: 0,
    estimatedAmount: 0
  }
}

function emptyForm() {
  return {
    title: '',
    departmentId: undefined as number | undefined,
    approverId: undefined as number | undefined,
    requiredDate: '',
    purpose: '',
    urgent: false,
    items: [emptyItem()]
  }
}

const form = reactive(emptyForm())

/** 重新申请时把后端详情数据回填到表单 */
function applyPrefill() {
  if (!props.prefill) return
  const p = props.prefill
  Object.assign(form, {
    title: p.title ?? '',
    departmentId: p.departmentId,
    approverId: p.approverId,
    requiredDate: p.requiredDate ?? '',
    purpose: p.purpose ?? '',
    urgent: p.urgent ?? false,
    items: p.items && p.items.length > 0
      ? p.items.map(item => ({ ...item }))
      : [emptyItem()]
  })
  existingAttachments.value = p.attachments ? p.attachments.map(a => ({ ...a })) : []
}

watch(() => props.modelValue, (val) => {
  if (val) {
    nextTick(() => {
      if (props.prefill) applyPrefill()
      else resetForm()
    })
  }
})

const totalAmount = computed(() => {
  return form.items.reduce((sum, item) => {
    return sum + Number(item.estimatedAmount || 0)
  }, 0)
})

function resetForm() {
  Object.assign(form, emptyForm())
  fileList.value = []
  existingAttachments.value = []
}

function removeExisting(id: number) {
  existingAttachments.value = existingAttachments.value.filter(a => a.id !== id)
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function addItem() {
  form.items.push(emptyItem())
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

function recalculateAmount(item: ApplicationItemRequest) {
  const quantity = Number(item.quantity || 0)
  const unitPrice = Number(item.estimatedUnitPrice || 0)
  item.estimatedAmount = Number((quantity * unitPrice).toFixed(2))
}

function close() {
  emit('update:modelValue', false)
}

async function loadDepartments() {
  departments.value = await listDepartments()
  try {
    approvers.value = await listApprovers()
  } catch {
    approvers.value = []
  }
}

async function uploadAttachments(): Promise<AttachmentInfoRequest[]> {
  const attachments: AttachmentInfoRequest[] = []

  for (const fileItem of fileList.value) {
    if (!fileItem.raw) {
      continue
    }
    const uploaded = await uploadFile(fileItem.raw)
    attachments.push({
      fileName: uploaded.fileName,
      storedName: uploaded.storedName,
      filePath: uploaded.filePath,
      fileSize: uploaded.fileSize,
      contentType: uploaded.contentType
    })
  }

  return attachments
}

async function save() {
  saving.value = true
  try {
    if (!form.departmentId) {
      ElMessage.warning('请选择申请部门')
      return
    }

    if (!form.approverId) {
      ElMessage.warning('请选择审批人')
      return
    }

    if (!form.title.trim()) {
      ElMessage.warning('请输入申请标题')
      return
    }

    // 校验明细：物料名称必填，数量必须大于 0
    const invalidItem = form.items.find(
      item => !item.materialName?.trim() || !item.quantity || item.quantity <= 0
    )
    if (invalidItem) {
      ElMessage.warning('请完善采购明细：物料名称必填、数量必须大于 0')
      return
    }

    const newAttachments = await uploadAttachments()
    // 编辑时：保留的已有附件只回传 id，新上传的带完整元数据，后端按差异对账
    const keptAttachments: AttachmentInfoRequest[] = props.editId
      ? existingAttachments.value.map(a => ({ id: a.id }))
      : []

    const payload: ApplicationCreateRequest = {
      title: form.title,
      departmentId: form.departmentId as number,
      approverId: form.approverId,
      applicationType: form.urgent ? 'URGENT' : 'NORMAL',
      purpose: form.purpose,
      requiredDate: form.requiredDate || undefined,
      items: form.items,
      attachments: [...keptAttachments, ...newAttachments]
    }

    if (props.editId) {
      await updateApplication(props.editId, payload)
      ElMessage.success('修改保存成功')
    } else {
      await createApplication(payload)
      ElMessage.success('草稿保存成功')
    }
    emit('update:modelValue', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.item-row {
  display: grid;
  grid-template-columns:
    1.1fr 1.2fr 1fr 0.65fr 1.1fr 1.1fr 1.1fr 1fr auto;
  gap: 8px;
  margin-bottom: 10px;
}

.detail-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.total-amount {
  font-size: 16px;
  font-weight: 600;
}

.total-amount span {
  color: #f56c6c;
}

.existing-files {
  margin-bottom: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 6px 12px;
}

.existing-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

.existing-file + .existing-file {
  border-top: 1px dashed var(--el-border-color-lighter);
}

.existing-file__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.existing-file__size {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="820px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="application" class="candidate-body">
      <el-alert
        :type="selectedSupplier ? 'success' : 'info'"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
      >
        <template #title>
          {{ application.applicationNo }} - {{ application.title }}
          <template v-if="selectedSupplier">
            （已选定中标供应商：{{ selectedSupplier.supplierName }}，询价名单已冻结）
          </template>
        </template>
      </el-alert>

      <div class="section-head">
        <p class="section-title">候选供应商（询价名单）</p>
        <el-button v-if="canOperate" size="small" type="primary" plain @click="formVisible = !formVisible">
          {{ formVisible ? '收起' : '录入供应商' }}
        </el-button>
      </div>

      <el-form
        v-if="formVisible && canOperate"
        :model="form"
        inline
        class="supplier-form"
        size="small"
      >
        <el-form-item label="供应商名称" required>
          <el-input v-model="form.supplierName" placeholder="必填，手动输入" style="width: 170px" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="选填" style="width: 110px" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.contactPhone" placeholder="选填" style="width: 130px" />
        </el-form-item>
        <el-form-item label="报价(元)">
          <el-input-number v-model="form.quotedAmount" :min="0" :precision="2" :controls="false" style="width: 130px" />
        </el-form-item>
        <el-form-item label="交期(天)">
          <el-input-number v-model="form.deliveryDays" :min="0" :controls="false" style="width: 100px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="选填" style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="addSupplier">添加</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="candidates" size="small" border v-loading="loading">
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
        <el-table-column v-if="canOperate" label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="removeSupplier(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="empty-text">暂无候选供应商，点击右上角"录入供应商"添加</span>
        </template>
      </el-table>
    </div>

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
  addApplicationSupplier,
  deleteApplicationSupplier,
  type ApplicationSupplierResponse,
  type ApplicationResponse
} from '../../api/application'
import { hasAnyRole } from '../../utils/auth'

const props = defineProps<{
  modelValue: boolean
  application: ApplicationResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const title = props.application
  ? `录入供应商 - ${props.application.applicationNo}`
  : '录入供应商'

const canEdit = hasAnyRole(['BUYER', 'ADMIN'])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const candidates = ref<ApplicationSupplierResponse[]>([])

// 已选定的中标供应商；定标后询价名单冻结，禁止再录入和删除
const selectedSupplier = computed(() => candidates.value.find(c => c.selected) || null)
const canOperate = computed(() => canEdit && !selectedSupplier.value)

const form = ref({
  supplierName: '',
  contactPerson: '',
  contactPhone: '',
  quotedAmount: undefined as number | undefined,
  deliveryDays: undefined as number | undefined,
  remark: ''
})

watch(
  () => props.modelValue,
  open => {
    if (open) {
      formVisible.value = false
      loadCandidates()
    }
  }
)

async function loadCandidates() {
  if (!props.application) return
  loading.value = true
  try {
    const list = await listApplicationSuppliers(props.application.id)
    // 按报价从低到高排序，未报价的排后面
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

function resetForm() {
  form.value = {
    supplierName: '',
    contactPerson: '',
    contactPhone: '',
    quotedAmount: undefined,
    deliveryDays: undefined,
    remark: ''
  }
}

async function addSupplier() {
  if (!props.application) return
  if (!form.value.supplierName.trim()) {
    ElMessage.warning('请填写供应商名称')
    return
  }
  saving.value = true
  try {
    await addApplicationSupplier(props.application.id, {
      supplierName: form.value.supplierName.trim(),
      contactPerson: form.value.contactPerson || undefined,
      contactPhone: form.value.contactPhone || undefined,
      quotedAmount: form.value.quotedAmount,
      deliveryDays: form.value.deliveryDays,
      remark: form.value.remark || undefined
    })
    ElMessage.success('候选供应商已添加')
    resetForm()
    await loadCandidates()
    emit('success')
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
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
.candidate-body {
  min-height: 120px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  margin: 0;
  font-weight: 600;
  font-size: 14px;
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

.empty-text {
  color: #909399;
  font-size: 13px;
}
</style>

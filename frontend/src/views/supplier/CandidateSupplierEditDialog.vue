<template>
  <el-dialog
    :model-value="modelValue"
    :title="`编辑供应商 - ${supplier?.supplierName || ''}`"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form :model="form" label-width="100px" v-if="supplier">
      <el-form-item label="供应商名称" required>
        <el-input v-model="form.supplierName" placeholder="必填" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="form.contactPerson" placeholder="选填" />
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="form.contactPhone" placeholder="选填" />
      </el-form-item>
      <el-form-item label="报价(元)">
        <el-input-number v-model="form.quotedAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="交期(天)">
        <el-input-number v-model="form.deliveryDays" :min="0" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateApplicationSupplier, type ApplicationSupplierResponse } from '../../api/application'

const props = defineProps<{
  modelValue: boolean
  applicationId: number
  supplier: ApplicationSupplierResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const saving = ref(false)
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
    if (open && props.supplier) {
      form.value = {
        supplierName: props.supplier.supplierName || '',
        contactPerson: props.supplier.contactPerson || '',
        contactPhone: props.supplier.contactPhone || '',
        quotedAmount: props.supplier.quotedAmount ?? undefined,
        deliveryDays: props.supplier.deliveryDays ?? undefined,
        remark: props.supplier.remark || ''
      }
    }
  }
)

async function save() {
  if (!props.supplier) return
  if (!form.value.supplierName.trim()) {
    ElMessage.warning('请填写供应商名称')
    return
  }
  saving.value = true
  try {
    await updateApplicationSupplier(props.applicationId, props.supplier.id, {
      supplierName: form.value.supplierName.trim(),
      contactPerson: form.value.contactPerson || undefined,
      contactPhone: form.value.contactPhone || undefined,
      quotedAmount: form.value.quotedAmount,
      deliveryDays: form.value.deliveryDays,
      remark: form.value.remark || undefined
    })
    ElMessage.success('已更新')
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

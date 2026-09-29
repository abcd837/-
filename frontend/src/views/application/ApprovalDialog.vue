<template>
  <el-dialog
    :model-value="modelValue"
    title="审批采购申请"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="target" class="approval-body">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="申请单号">{{ target.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ target.title }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ target.applicantName || '#' + target.applicantId }}</el-descriptions-item>
        <el-descriptions-item label="金额">
          {{ target.currency ?? 'CNY' }} {{ Number(target.totalAmount ?? 0).toFixed(2) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form label-position="top" style="margin-top: 16px">
        <el-form-item label="审批意见">
          <el-input
            v-model="comment"
            type="textarea"
            :rows="4"
            placeholder="驳回时请填写理由，通过可不填"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="danger" :loading="submitting" @click="handleReject">驳回</el-button>
      <el-button type="success" :loading="submitting" @click="handleApprove">通过</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveApplication,
  rejectApplication,
  type ApplicationResponse
} from '../../api/application'

const props = defineProps<{
  modelValue: boolean
  target: ApplicationResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const comment = ref('')
const submitting = ref(false)

watch(
  () => props.modelValue,
  open => {
    if (open) comment.value = ''
  }
)

async function handleApprove() {
  if (!props.target) return
  submitting.value = true
  try {
    await approveApplication(props.target.id, { comment: comment.value || undefined })
    ElMessage.success('已通过')
    emit('success')
    emit('update:modelValue', false)
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function handleReject() {
  if (!props.target) return
  if (!comment.value.trim()) {
    ElMessage.warning('驳回时请填写理由')
    return
  }
  submitting.value = true
  try {
    await rejectApplication(props.target.id, { comment: comment.value })
    ElMessage.success('已驳回')
    emit('success')
    emit('update:modelValue', false)
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.approval-body {
  min-height: 120px;
}
</style>

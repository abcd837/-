<template>
  <div>
    <div class="page-header">
      <h2>供应商管理</h2>
      <div class="header-actions">
        <el-select
          v-model="supplierSelected"
          placeholder="选定状态"
          clearable
          style="width: 130px"
          @change="onFilterChange"
        >
          <el-option label="已选定" :value="true" />
          <el-option label="未选定" :value="false" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="申请单号 / 标题"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
        />
        <el-button @click="loadData">搜索</el-button>
      </div>
    </div>

    <el-table :data="applications" v-loading="loading" border>
      <el-table-column prop="applicationNo" label="申请单号" width="160" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column prop="departmentName" label="部门" width="130" show-overflow-tooltip />
      <el-table-column label="金额" width="130" align="right">
        <template #default="{ row }">
          {{ row.currency ?? 'CNY' }} {{ formatAmount(row.totalAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="候选供应商" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="candidateCount[row.id] > 0" type="success" effect="plain">
            {{ candidateCount[row.id] }} 家
          </el-tag>
          <span v-else class="muted">未录入</span>
        </template>
      </el-table-column>
      <el-table-column label="选定状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="selectedMap[row.id]" type="success">已选定</el-tag>
          <el-tag v-else type="info" effect="plain">未选定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="opColumnWidth" fixed="right">
        <template #default="{ row }">
          <div class="op-btns">
            <el-tooltip
              :disabled="!selectedMap[row.id]"
              content="已选定中标供应商，询价名单已冻结"
              placement="top"
            >
              <span>
                <el-button v-if="canEdit" size="small" type="primary" :disabled="!!selectedMap[row.id]" @click="openCandidate(row)">录入供应商</el-button>
              </span>
            </el-tooltip>
            <el-button size="small" type="success" plain @click="openDetail(row)">查看详情</el-button>
            <el-button
              v-if="canViewComparison && candidateCount[row.id] > 0"
              size="small"
              type="warning"
              @click="goComparison(row)"
            >比价分析</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <CandidateSupplierDialog
      v-model="dialogVisible"
      :application="currentApp"
      @success="loadData"
    />

    <SupplierDetailDialog
      v-model="detailVisible"
      :application="currentApp"
      @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  listApplications,
  listApplicationSuppliers,
  type ApplicationResponse
} from '../../api/application'
import { hasAnyRole } from '../../utils/auth'
import CandidateSupplierDialog from './CandidateSupplierDialog.vue'
import SupplierDetailDialog from './SupplierDetailDialog.vue'

const router = useRouter()
const canEdit = hasAnyRole(['BUYER', 'ADMIN'])
const canViewComparison = hasAnyRole(['PURCHASE_MANAGER', 'ADMIN'])

// 根据角色动态计算操作列宽度
const opColumnWidth = computed(() => {
  if (canEdit && canViewComparison) return 340  // 管理员：3个按钮
  if (canViewComparison) return 220             // 采购负责人：2个按钮
  return 240                                     // 采购员：2个按钮
})

const loading = ref(false)
const keyword = ref('')
const supplierSelected = ref<boolean | ''>('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const applications = ref<ApplicationResponse[]>([])
const candidateCount = reactive<Record<number, number>>({})
const selectedMap = reactive<Record<number, boolean>>({})

const dialogVisible = ref(false)
const detailVisible = ref(false)
const currentApp = ref<ApplicationResponse | null>(null)

function formatAmount(value: number | undefined | null): string {
  return value == null ? '-' : Number(value).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listApplications(page.value - 1, size.value, {
      keyword: keyword.value.trim() || undefined,
      status: 'APPROVED',
      supplierSelected: supplierSelected.value === '' ? undefined : supplierSelected.value
    })
    applications.value = res.content ?? []
    total.value = res.totalElements ?? 0
    await loadCandidateCounts()
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  loadData()
}

/** 并行拉取当前页每张申请单的候选供应商数量和选定状态 */
async function loadCandidateCounts() {
  if (applications.value.length === 0) return
  const infos = await Promise.all(
    applications.value.map(app =>
      listApplicationSuppliers(app.id)
        .then(list => ({ count: list.length, hasSelected: list.some(s => s.selected) }))
        .catch(() => ({ count: 0, hasSelected: false }))
    )
  )
  applications.value.forEach((app, i) => {
    candidateCount[app.id] = infos[i].count
    selectedMap[app.id] = infos[i].hasSelected
  })
}

function openCandidate(row: ApplicationResponse) {
  currentApp.value = row
  dialogVisible.value = true
}

function openDetail(row: ApplicationResponse) {
  currentApp.value = row
  detailVisible.value = true
}

function goComparison(row: ApplicationResponse) {
  router.push(`/suppliers/comparison/${row.id}`)
}

onMounted(loadData)
</script>

<style scoped>
.op-btns {
  display: flex;
  gap: 6px;
  white-space: nowrap;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.muted {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}
</style>

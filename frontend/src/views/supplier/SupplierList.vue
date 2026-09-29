<template>
  <div class="supplier-page">
    <!-- 页面标题 -->
    <div class="page-head">
      <h2>供应商管理</h2>
      <p>已审批采购申请 · 询价录入与定标管理</p>
    </div>

    <!-- 清单表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <div class="table-title">采购申请清单</div>
        <div class="table-filters">
          <el-select
            v-model="supplierStage"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="onFilterChange"
          >
            <el-option label="待录入" value="NOT_ENTERED" />
            <el-option label="待选定" value="PENDING_SELECTION" />
            <el-option label="已选定" value="SELECTED" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="申请单号 / 标题"
            clearable
            style="width: 240px"
            @keyup.enter="onSearch"
          />
          <el-button type="primary" @click="onSearch">搜索</el-button>
        </div>
      </div>
      <el-table :data="applications" v-loading="loading">
        <el-table-column prop="applicationNo" label="申请单号" width="180" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="departmentName" label="部门" width="130" show-overflow-tooltip />
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            {{ row.currency ?? 'CNY' }} {{ formatAmount(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="stageTagType[row.id] ?? 'info'" effect="light">
              {{ stageLabel[row.id] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="opColumnWidth" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-tooltip
                :disabled="!isSelected(row.id)"
                content="已选定中标供应商，询价名单已冻结"
                placement="top"
              >
                <span>
                  <el-button v-if="canEdit" size="small" type="primary" link :disabled="isSelected(row.id)" @click="openCandidate(row)">录入</el-button>
                </span>
              </el-tooltip>
              <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="canViewComparison && candidateCount[row.id] > 0"
                size="small"
                type="primary"
                link
                @click="goComparison(row)"
              >比价</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">暂无符合条件的采购申请</span>
        </template>
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
  if (canEdit && canViewComparison) return 180  // 管理员：录入/详情/比价
  return 130                                    // 采购员或采购负责人：两个按钮
})

const loading = ref(false)
const keyword = ref('')
const supplierStage = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const applications = ref<ApplicationResponse[]>([])
const candidateCount = reactive<Record<number, number>>({})
// 每张申请单的状态文案与标签颜色：待录入 / 待选定 / 已选定
const stageLabel = reactive<Record<number, string>>({})
const stageTagType = reactive<Record<number, 'info' | 'warning' | 'success'>>({})

const dialogVisible = ref(false)
const detailVisible = ref(false)
const currentApp = ref<ApplicationResponse | null>(null)

function formatAmount(value: number | undefined | null): string {
  return value == null ? '-' : Number(value).toFixed(2)
}

/** 该申请单是否已选定中标供应商（用于冻结录入入口） */
function isSelected(id: number): boolean {
  return stageTagType[id] === 'success'
}

async function loadData() {
  loading.value = true
  try {
    const res = await listApplications(page.value - 1, size.value, {
      keyword: keyword.value.trim() || undefined,
      status: 'APPROVED',
      supplierStage: supplierStage.value || undefined
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

function onSearch() {
  page.value = 1
  loadData()
}

/** 并行拉取当前页每张申请单的候选供应商数量，计算待录入/待选定/已选定状态 */
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
    if (infos[i].hasSelected) {
      stageLabel[app.id] = '已选定'
      stageTagType[app.id] = 'success'
    } else if (infos[i].count > 0) {
      stageLabel[app.id] = '待选定'
      stageTagType[app.id] = 'warning'
    } else {
      stageLabel[app.id] = '待录入'
      stageTagType[app.id] = 'info'
    }
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
.supplier-page {
  padding: 4px 4px 20px;
}

/* ── 页头 ── */
.page-head h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2f5016;
}

.page-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

/* ── 表格卡片 ── */
.table-card {
  margin-top: 18px;
  background: #fff;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 2px 10px rgba(47, 80, 22, 0.05);
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.table-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-title {
  font-size: 15px;
  font-weight: 700;
  color: #2f5016;
  white-space: nowrap;
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

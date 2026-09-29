<template>
  <div class="comparison-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
        <h2>比价分析</h2>
      </div>
      <div class="header-info" v-if="application">
        <el-tag type="info" size="large">{{ application.applicationNo }}</el-tag>
        <span class="title">{{ application.title }}</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>正在加载供应商数据...</p>
    </div>

    <!-- 无数据提示 -->
    <el-empty v-else-if="candidates.length === 0" description="暂无候选供应商数据，无法进行比价分析">
      <el-button type="primary" @click="goBack">返回列表</el-button>
    </el-empty>

    <!-- 比价分析内容 -->
    <template v-else>
      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ candidates.length }}</div>
            <div class="stat-label">候选供应商</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value price">{{ formatPrice(minPrice) }}</div>
            <div class="stat-label">最低报价</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value price">{{ formatPrice(maxPrice) }}</div>
            <div class="stat-label">最高报价</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value delivery">{{ minDelivery }}天</div>
            <div class="stat-label">最短交期</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 核心图表：报价对比 + 散点图 -->
      <el-row :gutter="16" class="charts-row">
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <PriceComparisonChart :suppliers="candidates" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <PriceVsDeliveryChart :suppliers="candidates" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 雷达图：供应商 ≥ 3 家才显示 -->
      <el-row v-if="candidates.length >= 3" :gutter="16" class="charts-row">
        <el-col :span="24">
          <el-card shadow="never" class="chart-card">
            <SupplierRadarChart :suppliers="candidates" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 箱线图：供应商 ≥ 5 家才显示 -->
      <el-row v-if="candidates.length >= 5" :gutter="16" class="charts-row">
        <el-col :span="24">
          <el-card shadow="never" class="chart-card">
            <PriceBoxplotChart :suppliers="candidates" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 综合排名表格 -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="card-header">
            <span>综合排名</span>
            <el-tooltip content="评分规则：价格权重60%，交期权重40%，排名第1得100分，每降1名扣10分">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </template>

        <el-table :data="rankedSuppliers" border stripe>
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ $index }">
              <el-tag v-if="$index === 0" type="success" effect="dark" size="large">🥇 1</el-tag>
              <el-tag v-else-if="$index === 1" type="warning" effect="dark" size="large">🥈 2</el-tag>
              <el-tag v-else-if="$index === 2" type="info" effect="dark" size="large">🥉 3</el-tag>
              <span v-else>{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="supplierName" label="供应商名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="contactPerson" label="联系人" width="100" />
          <el-table-column prop="contactPhone" label="电话" width="130" />
          <el-table-column label="报价(元)" width="140" align="right" sortable :sort-method="(a: any, b: any) => (a.quotedAmount || 0) - (b.quotedAmount || 0)">
            <template #default="{ row }">
              <span :class="{ 'best-price': row.priceRank === 1 }">
                {{ row.quotedAmount == null ? '—' : `¥${Number(row.quotedAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="价格排名" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.priceRank === 1 ? 'success' : row.priceRank === 2 ? 'warning' : 'info'">
                {{ row.priceRank != null ? `第${row.priceRank}名` : '—' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="交期(天)" width="100" align="center" sortable :sort-method="(a: any, b: any) => (a.deliveryDays || 999) - (b.deliveryDays || 999)">
            <template #default="{ row }">
              <span :class="{ 'best-delivery': row.deliveryRank === 1 }">
                {{ row.deliveryDays == null ? '—' : `${row.deliveryDays}天` }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="交期排名" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.deliveryRank === 1 ? 'success' : row.deliveryRank === 2 ? 'warning' : 'info'">
                {{ row.deliveryRank != null ? `第${row.deliveryRank}名` : '—' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="综合评分" width="120" align="center" sortable :sort-method="(a: any, b: any) => b.score - a.score">
            <template #default="{ row }">
              <el-progress
                :percentage="row.score"
                :color="getScoreColor(row.score)"
                :stroke-width="18"
                :text-inside="true"
              />
            </template>
          </el-table-column>
          <el-table-column label="推荐" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.score >= 80" type="success" effect="dark">
                <el-icon><Star /></el-icon> 推荐
              </el-tag>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" align="center" fixed="right">
            <template #default="{ row }">
              <div class="op-btns">
                <template v-if="row.selected">
                  <el-button type="success" size="small" link disabled>
                    <el-icon><Check /></el-icon> 已选定
                  </el-button>
                  <el-button
                    type="warning"
                    size="small"
                    link
                    :loading="selectingId === row.id"
                    @click="unselectSupplier(row)"
                  >取消选定</el-button>
                </template>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  link
                  :loading="selectingId === row.id"
                  @click="selectSupplier(row)"
                >选定</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 已选定供应商提示 -->
      <el-alert
        v-if="selectedSupplier"
        type="success"
        :closable="false"
        show-icon
        class="selected-alert"
      >
        <template #title>
          已选定中标供应商：<b>{{ selectedSupplier.supplierName }}</b>
          <span v-if="selectedSupplier.quotedAmount != null">
            （报价：¥{{ Number(selectedSupplier.quotedAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}）
          </span>
        </template>
      </el-alert>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Loading, QuestionFilled, Star, Check } from '@element-plus/icons-vue'
import {
  getApplication,
  listApplicationSuppliers,
  selectApplicationSupplier,
  unselectApplicationSupplier,
  type ApplicationResponse,
  type ApplicationSupplierResponse
} from '../../api/application'
import PriceComparisonChart from '../../components/charts/PriceComparisonChart.vue'
import PriceVsDeliveryChart from '../../components/charts/PriceVsDeliveryChart.vue'
import PriceBoxplotChart from '../../components/charts/PriceBoxplotChart.vue'
import SupplierRadarChart from '../../components/charts/SupplierRadarChart.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const application = ref<ApplicationResponse | null>(null)
const candidates = ref<ApplicationSupplierResponse[]>([])
const selectingId = ref<number | null>(null)

// 已选定的供应商
const selectedSupplier = computed(() => {
  return candidates.value.find(s => s.selected) || null
})

interface RankedSupplier extends ApplicationSupplierResponse {
  priceRank: number | null
  deliveryRank: number | null
  score: number
}

const applicationId = computed(() => Number(route.params.id))

// 统计数据
const minPrice = computed(() => {
  const prices = candidates.value.filter(s => s.quotedAmount != null).map(s => Number(s.quotedAmount))
  return prices.length > 0 ? Math.min(...prices) : 0
})

const maxPrice = computed(() => {
  const prices = candidates.value.filter(s => s.quotedAmount != null).map(s => Number(s.quotedAmount))
  return prices.length > 0 ? Math.max(...prices) : 0
})

const minDelivery = computed(() => {
  const days = candidates.value.filter(s => s.deliveryDays != null).map(s => Number(s.deliveryDays))
  return days.length > 0 ? Math.min(...days) : 0
})

// 计算排名和综合评分
const rankedSuppliers = computed<RankedSupplier[]>(() => {
  const validPrice = candidates.value.filter(s => s.quotedAmount != null)
  const validDelivery = candidates.value.filter(s => s.deliveryDays != null)

  const priceRankMap = new Map<number, number>()
  const deliveryRankMap = new Map<number, number>()

  validPrice
    .slice()
    .sort((a, b) => Number(a.quotedAmount) - Number(b.quotedAmount))
    .forEach((s, index) => priceRankMap.set(s.id, index + 1))

  validDelivery
    .slice()
    .sort((a, b) => Number(a.deliveryDays) - Number(b.deliveryDays))
    .forEach((s, index) => deliveryRankMap.set(s.id, index + 1))

  return candidates.value.map(s => {
    const priceRank = priceRankMap.get(s.id) ?? null
    const deliveryRank = deliveryRankMap.get(s.id) ?? null

    let score = 0
    if (priceRank != null && deliveryRank != null) {
      const priceScore = Math.max(0, 100 - (priceRank - 1) * 10)
      const deliveryScore = Math.max(0, 100 - (deliveryRank - 1) * 10)
      score = priceScore * 0.6 + deliveryScore * 0.4
    } else if (priceRank != null) {
      score = Math.max(0, 100 - (priceRank - 1) * 10) * 0.6
    } else if (deliveryRank != null) {
      score = Math.max(0, 100 - (deliveryRank - 1) * 10) * 0.4
    }

    return { ...s, priceRank, deliveryRank, score }
  }).sort((a, b) => b.score - a.score)
})

function formatPrice(value: number): string {
  if (value >= 10000) return `¥${(value / 10000).toFixed(1)}w`
  if (value >= 1000) return `¥${(value / 1000).toFixed(1)}k`
  return `¥${value.toLocaleString()}`
}

function getScoreColor(score: number): string {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

function goBack() {
  router.push('/suppliers')
}

async function selectSupplier(supplier: ApplicationSupplierResponse) {
  try {
    await ElMessageBox.confirm(
      `确定选择「${supplier.supplierName}」作为中标供应商吗？`,
      '选定供应商',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  selectingId.value = supplier.id
  try {
    await selectApplicationSupplier(applicationId.value, supplier.id)
    ElMessage.success(`已选定 ${supplier.supplierName}`)
    // 刷新数据
    await loadData()
  } catch {
    // 错误由拦截器处理
  } finally {
    selectingId.value = null
  }
}

async function unselectSupplier(supplier: ApplicationSupplierResponse) {
  try {
    await ElMessageBox.confirm(
      `确定取消选定「${supplier.supplierName}」吗？`,
      '取消选定',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  selectingId.value = supplier.id
  try {
    await unselectApplicationSupplier(applicationId.value, supplier.id)
    ElMessage.success(`已取消选定 ${supplier.supplierName}`)
    // 刷新数据
    await loadData()
  } catch {
    // 错误由拦截器处理
  } finally {
    selectingId.value = null
  }
}

async function loadData() {
  loading.value = true
  try {
    const [app, suppliers] = await Promise.all([
      getApplication(applicationId.value),
      listApplicationSuppliers(applicationId.value)
    ])
    application.value = app
    candidates.value = suppliers
  } catch {
    // 错误由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.comparison-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info .title {
  font-size: 16px;
  color: #606266;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #909399;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  padding: 8px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-value.price {
  color: #e6a23c;
}

.stat-value.delivery {
  color: #67c23a;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.charts-row {
  margin-bottom: 16px;
}

.chart-card {
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.best-price {
  color: #67c23a;
  font-weight: 600;
}

.best-delivery {
  color: #67c23a;
  font-weight: 600;
}

.muted {
  color: #c0c4cc;
}

.selected-alert {
  margin-top: 16px;
  border-radius: 8px;
}
</style>

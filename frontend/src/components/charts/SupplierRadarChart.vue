<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, computed } from 'vue'
import * as echarts from 'echarts'
import type { ApplicationSupplierResponse } from '../../api/application'

const props = defineProps<{
  suppliers: ApplicationSupplierResponse[]
}>()

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

// 颜色池
const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

// 计算各维度评分
const radarData = computed(() => {
  if (props.suppliers.length === 0) return []

  const validPrice = props.suppliers.filter(s => s.quotedAmount != null)
  const validDelivery = props.suppliers.filter(s => s.deliveryDays != null)

  // 价格排名（越低越好）
  const priceRankMap = new Map<number, number>()
  validPrice
    .slice()
    .sort((a, b) => Number(a.quotedAmount) - Number(b.quotedAmount))
    .forEach((s, i) => priceRankMap.set(s.id, i + 1))

  // 交期排名（越短越好）
  const deliveryRankMap = new Map<number, number>()
  validDelivery
    .slice()
    .sort((a, b) => Number(a.deliveryDays) - Number(b.deliveryDays))
    .forEach((s, i) => deliveryRankMap.set(s.id, i + 1))

  // 价格统计（用于计算价格优势）
  const prices = validPrice.map(s => Number(s.quotedAmount))
  const minPrice = Math.min(...prices)
  const maxPrice = Math.max(...prices)
  const avgPrice = prices.reduce((a, b) => a + b, 0) / prices.length

  // 交期统计
  const days = validDelivery.map(s => Number(s.deliveryDays))
  const minDays = days.length > 0 ? Math.min(...days) : 0
  const maxDays = days.length > 0 ? Math.max(...days) : 0

  return props.suppliers.map((s, index) => {
    const priceRank = priceRankMap.get(s.id) ?? null
    const deliveryRank = deliveryRankMap.get(s.id) ?? null
    const price = s.quotedAmount != null ? Number(s.quotedAmount) : null
    const delivery = s.deliveryDays != null ? Number(s.deliveryDays) : null

    // 五个维度评分（0-100）
    const scores = {
      // 1. 价格竞争力：报价越低分越高
      priceScore: price != null
        ? Math.max(0, Math.min(100, ((maxPrice - price) / (maxPrice - minPrice || 1)) * 100))
        : 0,

      // 2. 交付能力：交期越短分越高
      deliveryScore: delivery != null
        ? Math.max(0, Math.min(100, ((maxDays - delivery) / (maxDays - minDays || 1)) * 100))
        : 0,

      // 3. 价格优势（相对平均价）：低于平均分高
      priceAdvantage: price != null
        ? Math.max(0, Math.min(100, ((avgPrice - price) / avgPrice) * 100 + 50))
        : 0,

      // 4. 价格排名分
      priceRankScore: priceRank != null
        ? Math.max(0, 100 - (priceRank - 1) * (100 / Math.max(1, validPrice.length - 1)))
        : 0,

      // 5. 交期排名分
      deliveryRankScore: deliveryRank != null
        ? Math.max(0, 100 - (deliveryRank - 1) * (100 / Math.max(1, validDelivery.length - 1)))
        : 0
    }

    return {
      name: s.supplierName,
      value: [
        scores.priceScore,
        scores.deliveryScore,
        scores.priceAdvantage,
        scores.priceRankScore,
        scores.deliveryRankScore
      ],
      itemStyle: { color: colors[index % colors.length] },
      lineStyle: { color: colors[index % colors.length], width: 2 },
      areaStyle: { color: colors[index % colors.length], opacity: 0.15 }
    }
  })
})

function initChart() {
  if (!chartRef.value || props.suppliers.length === 0) return

  chartInstance = echarts.init(chartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '供应商综合实力雷达图',
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 600 }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const dims = ['价格竞争力', '交付能力', '价格优势', '价格排名', '交期排名']
        let html = `<b>${params.name}</b><br/>`
        params.value.forEach((v: number, i: number) => {
          html += `${dims[i]}：${v.toFixed(1)}分<br/>`
        })
        return html
      }
    },
    legend: {
      bottom: 10,
      type: 'scroll',
      textStyle: { fontSize: 11 }
    },
    radar: {
      indicator: [
        { name: '价格竞争力', max: 100 },
        { name: '交付能力', max: 100 },
        { name: '价格优势', max: 100 },
        { name: '价格排名', max: 100 },
        { name: '交期排名', max: 100 }
      ],
      radius: '60%',
      center: ['50%', '50%'],
      splitNumber: 5,
      axisName: {
        fontSize: 11,
        color: '#666'
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(84, 112, 198, 0.05)', 'rgba(84, 112, 198, 0.1)']
        }
      },
      axisLine: {
        lineStyle: { color: 'rgba(0, 0, 0, 0.1)' }
      },
      splitLine: {
        lineStyle: { color: 'rgba(0, 0, 0, 0.1)' }
      }
    },
    series: [
      {
        type: 'radar',
        data: radarData.value,
        symbol: 'circle',
        symbolSize: 5,
        emphasis: {
          lineStyle: { width: 4 },
          areaStyle: { opacity: 0.3 }
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

function resizeChart() {
  chartInstance?.resize()
}

watch(() => props.suppliers, () => {
  chartInstance?.dispose()
  initChart()
}, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 380px;
}
</style>

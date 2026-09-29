<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ApplicationSupplierResponse } from '../../api/application'

const props = defineProps<{
  suppliers: ApplicationSupplierResponse[]
}>()

const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value || props.suppliers.length === 0) return

  chartInstance = echarts.init(chartRef.value)

  // 获取所有有效报价
  const prices = props.suppliers
    .filter(s => s.quotedAmount != null)
    .map(s => Number(s.quotedAmount))
    .sort((a, b) => a - b)

  if (prices.length === 0) return

  // 计算箱线图数据：[最小值, Q1, 中位数, Q3, 最大值]
  const min = prices[0]
  const max = prices[prices.length - 1]
  const q1 = quantile(prices, 0.25)
  const median = quantile(prices, 0.5)
  const q3 = quantile(prices, 0.75)

  // 找出异常值（超出 1.5*IQR 的值）
  const iqr = q3 - q1
  const lowerBound = q1 - 1.5 * iqr
  const upperBound = q3 + 1.5 * iqr
  const outliers = prices.filter(p => p < lowerBound || p > upperBound)

  // 供应商名称映射（用于 tooltip）
  const supplierMap = new Map<number, string>()
  props.suppliers.forEach(s => {
    if (s.quotedAmount != null) {
      supplierMap.set(Number(s.quotedAmount), s.supplierName)
    }
  })

  const option: echarts.EChartsOption = {
    title: {
      text: '报价分布（箱线图）',
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 600 }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.seriesName === '箱线图') {
          const values = params.data
          return `报价统计<br/>
            最大值：¥${values[4].toLocaleString()}<br/>
            上四分位(Q3)：¥${values[3].toLocaleString()}<br/>
            中位数：¥${values[2].toLocaleString()}<br/>
            下四分位(Q1)：¥${values[1].toLocaleString()}<br/>
            最小值：¥${values[0].toLocaleString()}`
        } else {
          const supplierName = supplierMap.get(params.data[1]) || '未知'
          return `异常报价<br/>${supplierName}<br/>¥${params.data[1].toLocaleString()}`
        }
      }
    },
    grid: {
      left: '15%',
      right: '15%',
      bottom: '12%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['报价分布'],
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '金额(元)',
      nameTextStyle: { fontSize: 11 },
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) return `¥${(value / 10000).toFixed(1)}w`
          if (value >= 1000) return `¥${(value / 1000).toFixed(1)}k`
          return `¥${value}`
        },
        fontSize: 10
      }
    },
    series: [
      {
        name: '箱线图',
        type: 'boxplot',
        data: [[min, q1, median, q3, max]],
        itemStyle: {
          color: 'rgba(84, 112, 198, 0.3)',
          borderColor: '#5470c6',
          borderWidth: 2
        },
        boxWidth: ['30%', '50%']
      },
      {
        name: '异常值',
        type: 'scatter',
        data: outliers.map(p => [0, p]),
        symbolSize: 12,
        itemStyle: {
          color: '#ee6666',
          borderColor: '#fff',
          borderWidth: 2
        },
        tooltip: {
          formatter: (params: any) => {
            const supplierName = supplierMap.get(params.data[1]) || '未知'
            return `⚠️ 异常报价<br/>${supplierName}<br/>¥${params.data[1].toLocaleString()}`
          }
        }
      },
      // 所有报价点（散点显示）
      {
        name: '所有报价',
        type: 'scatter',
        data: prices.map((p, i) => [(Math.random() - 0.5) * 0.3, p]),
        symbolSize: 8,
        itemStyle: {
          color: 'rgba(84, 112, 198, 0.5)'
        },
        tooltip: {
          show: false
        },
        z: 1
      }
    ]
  }

  chartInstance.setOption(option)
}

// 计算分位数
function quantile(sortedArr: number[], q: number): number {
  const pos = (sortedArr.length - 1) * q
  const base = Math.floor(pos)
  const rest = pos - base
  if (sortedArr[base + 1] !== undefined) {
    return sortedArr[base] + rest * (sortedArr[base + 1] - sortedArr[base])
  }
  return sortedArr[base]
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
  height: 320px;
}
</style>

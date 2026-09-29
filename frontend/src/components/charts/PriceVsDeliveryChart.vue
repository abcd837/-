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

  // 过滤出既有报价又有交期的供应商
  const validSuppliers = props.suppliers.filter(s => s.quotedAmount != null && s.deliveryDays != null)
  
  const data = validSuppliers.map(s => ({
    name: s.supplierName,
    value: [Number(s.quotedAmount), Number(s.deliveryDays)]
  }))

  const option: echarts.EChartsOption = {
    title: {
      text: '报价 vs 交期（左下角为最优）',
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 600 }
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}<br/>报价：¥${Number(params.value[0]).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}<br/>交期：${params.value[1]} 天`
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '12%',
      top: '18%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '报价(元)',
      nameLocation: 'middle',
      nameGap: 28,
      nameTextStyle: { fontSize: 11 },
      axisLabel: {
        formatter: (value: number) => {
          if (value >= 10000) return `¥${(value / 10000).toFixed(1)}w`
          if (value >= 1000) return `¥${(value / 1000).toFixed(1)}k`
          return `¥${value}`
        },
        fontSize: 10
      },
      splitNumber: 5
    },
    yAxis: {
      type: 'value',
      name: '交期(天)',
      nameLocation: 'middle',
      nameGap: 35,
      nameTextStyle: { fontSize: 11 },
      axisLabel: {
        formatter: '{value}',
        fontSize: 10
      },
      splitNumber: 5
    },
    series: [
      {
        name: '供应商',
        type: 'scatter',
        symbolSize: 24,
        data: data,
        itemStyle: {
          color: (params: any) => {
            const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
            return colors[params.dataIndex % colors.length]
          },
          borderColor: '#fff',
          borderWidth: 2,
          shadowBlur: 4,
          shadowColor: 'rgba(0,0,0,0.2)'
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params: any) => params.name,
          fontSize: 10,
          color: '#333',
          distance: 8
        },
        emphasis: {
          scale: 1.5,
          label: {
            show: true,
            fontSize: 12,
            fontWeight: 'bold'
          }
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
  height: 400px;
}
</style>

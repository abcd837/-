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

  const validSuppliers = props.suppliers.filter(s => s.deliveryDays != null)
  const names = validSuppliers.map(s => s.supplierName)
  const days = validSuppliers.map(s => Number(s.deliveryDays))

  const option: echarts.EChartsOption = {
    title: {
      text: '交期对比',
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 600 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>交期：${p.value} 天`
      }
    },
    grid: {
      left: '8%',
      right: '12%',
      bottom: '10%',
      top: '18%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '天数',
      nameTextStyle: { fontSize: 11 },
      axisLabel: {
        formatter: '{value}',
        fontSize: 10
      },
      splitNumber: 5
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        fontSize: 11,
        width: 80,
        overflow: 'truncate'
      }
    },
    series: [
      {
        name: '交期',
        type: 'bar',
        data: days.map((days, index) => ({
          value: days,
          itemStyle: {
            color: days <= 15 ? '#91cc75' : days <= 30 ? '#fac858' : '#ee6666',
            borderRadius: [0, 4, 4, 0]
          }
        })),
        barWidth: '50%',
        label: {
          show: true,
          position: 'right',
          formatter: '{c} 天',
          fontSize: 10,
          color: '#333'
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
  height: 300px;
}
</style>

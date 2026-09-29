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

  const validSuppliers = props.suppliers.filter(s => s.quotedAmount != null)
  const names = validSuppliers.map(s => s.supplierName)
  const prices = validSuppliers.map(s => Number(s.quotedAmount))

  const option: echarts.EChartsOption = {
    title: {
      text: '报价对比',
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 600 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>报价：¥${Number(p.value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
      }
    },
    grid: {
      left: '8%',
      right: '8%',
      bottom: '10%',
      top: '18%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: {
        interval: 0,
        rotate: names.length > 3 ? 25 : 0,
        fontSize: 11,
        width: 80,
        overflow: 'truncate'
      }
    },
    yAxis: {
      type: 'value',
      name: '金额(元)',
      nameTextStyle: { fontSize: 11 },
      axisLabel: {
        formatter: (value: number) => `¥${value.toLocaleString()}`,
        fontSize: 10
      },
      splitNumber: 5
    },
    series: [
      {
        name: '报价',
        type: 'bar',
        data: prices,
        barWidth: '40%',
        itemStyle: {
          color: (params: any) => {
            const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
            return colors[params.dataIndex % colors.length]
          },
          borderRadius: [4, 4, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params: any) => `¥${Number(params.value).toLocaleString()}`,
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

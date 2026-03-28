<script setup>
import { 
  ShoppingBag, 
  Users, 
  TrendingUp, 
  DollarSign, 
  ArrowUpRight, 
  Package,
  Activity,
  Zap,
  Globe
} from 'lucide-vue-next'

const stats = [
  { name: '总营收', value: '￥1,245,192.00', change: '+12.5%', isPositive: true, icon: DollarSign },
  { name: '活跃身份数', value: '1,240', change: '+18.2%', isPositive: true, icon: Users },
  { name: '待处理订单', value: '450', change: '5 项需注意', isPositive: false, icon: ShoppingBag },
  { name: '库存总量', value: '8,321', change: '12 条预警', isPositive: false, icon: Package },
]

const recentActions = [
  { id: 'ACT-902', type: '结算付款', user: '张三', amount: '￥2,499.00', status: '成功', time: '2 分钟前' },
  { id: 'ACT-856', type: '物流发货', user: '李四', amount: '￥399.00', status: '处理中', time: '12 分钟前' },
  { id: 'ACT-741', type: '创建订单', user: '王五', amount: '￥7,850.00', status: '成功', time: '1 小时前' },
]
</script>

<template>
  <div class="px-8 md:px-12 py-10 transition-all duration-700 animate-spa-reveal">
    <!-- Header Summary -->
    <div class="mb-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div class="space-y-4">
        <p class="label-refined text-gray-400">系统数据概览</p>
        <h1 class="h1-refined text-4xl">控制台数据大盘。</h1>
      </div>
      <button class="spa-button gap-3 bg-white text-black border border-gray-200 hover:bg-gray-50 flex-shrink-0">
        导出核心报表 <ArrowUpRight :size="16" />
      </button>
    </div>

    <!-- Metric Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
      <div v-for="stat in stats" :key="stat.name" class="spa-card p-8 flex flex-col relative overflow-hidden group">
         <!-- Subtle accent shape -->
         <div class="absolute -right-8 -top-8 w-24 h-24 bg-gray-50 rounded-full group-hover:scale-110 transition-transform duration-700"></div>
         
         <div class="relative z-10 flex flex-col h-full justify-between space-y-8">
            <div class="w-12 h-12 rounded-xl bg-gray-50 flex items-center justify-center text-gray-400 group-hover:text-black transition-colors">
               <component :is="stat.icon" :size="20" stroke-width="1.5" />
            </div>
            
            <div class="space-y-2">
               <p class="label-refined text-gray-500">{{ stat.name }}</p>
               <h3 class="text-3xl font-light tracking-tight text-gray-900">{{ stat.value }}</h3>
            </div>
            
            <div class="flex items-center gap-2 text-xs font-semibold" :class="stat.isPositive ? 'text-green-600' : 'text-amber-600'">
               <TrendingUp v-if="stat.isPositive" :size="14" />
               <Activity v-else :size="14" />
               <span>{{ stat.change }}</span>
            </div>
         </div>
      </div>
    </div>

    <!-- Data Insights -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      
      <!-- Chart Placeholder -->
      <div class="lg:col-span-2 spa-card p-8 md:p-10 flex flex-col min-h-[400px]">
         <div class="flex items-center justify-between mb-8">
            <div class="flex items-center gap-3">
               <div class="w-10 h-10 bg-gray-50 rounded-lg flex items-center justify-center text-gray-600">
                  <Activity :size="20" stroke-width="1.5" />
               </div>
               <h3 class="text-xl font-medium tracking-tight">系统运转速率</h3>
            </div>
            <div class="flex bg-gray-50 p-1 rounded-lg">
               <button class="px-4 py-1.5 rounded-md text-xs font-medium text-gray-400 hover:text-gray-900 transition-colors">7 天</button>
               <button class="px-4 py-1.5 bg-white shadow-sm rounded-md text-xs font-semibold text-gray-900">30 天</button>
            </div>
         </div>
         
         <div class="flex-1 w-full bg-[#f8f9fa] border border-gray-100 rounded-2xl flex flex-col items-center justify-center gap-4 group">
            <Zap :size="48" class="text-gray-300 group-hover:text-black transition-colors duration-500" stroke-width="1" />
            <p class="label-refined text-gray-400">数据流双向同步中</p>
         </div>
      </div>

      <!-- Action Feed -->
      <div class="spa-card p-8 md:p-10 flex flex-col">
         <div class="flex items-center gap-3 mb-8">
            <div class="w-10 h-10 bg-gray-50 rounded-lg flex items-center justify-center text-gray-600">
               <Globe :size="20" stroke-width="1.5" />
            </div>
            <h3 class="text-xl font-medium tracking-tight">实时事件流</h3>
         </div>
         
         <div class="space-y-4 flex-1">
            <div v-for="action in recentActions" :key="action.id" class="p-4 rounded-xl hover:bg-gray-50 border border-transparent hover:border-gray-100 transition-all cursor-pointer flex items-center gap-4">
               <div class="w-10 h-10 rounded-full bg-white shadow-sm border border-gray-100 flex items-center justify-center text-gray-400">
                  <Activity :size="16" stroke-width="1.5" />
               </div>
               <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between mb-1">
                     <span class="text-sm font-semibold text-gray-900 truncate">{{ action.type }}</span>
                     <span class="text-[10px] font-bold uppercase tracking-wider" :class="action.status === '成功' ? 'text-green-600' : 'text-amber-600'">{{ action.status }}</span>
                  </div>
                  <div class="flex items-center justify-between text-[11px] font-medium text-gray-500">
                     <span class="truncate">{{ action.user }} • {{ action.amount }}</span>
                     <span class="flex-shrink-0">{{ action.time }}</span>
                  </div>
               </div>
            </div>
         </div>
         
         <button class="mt-6 w-full py-4 text-xs font-bold uppercase tracking-widest text-gray-400 hover:text-black transition-colors">
            查看完整审计流
         </button>
      </div>
      
    </div>
  </div>
</template>

<style scoped>
/* All styles inherited from global spa-design in style.css */
</style>

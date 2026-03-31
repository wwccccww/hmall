<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import {
  ChevronLeft, Ticket, Gift, Clock, CheckCircle2,
  ShoppingBag, Zap, BadgePercent, BookmarkCheck, AlertCircle
} from 'lucide-vue-next'
import { getAvailableCoupons, getMyCoupons, receiveCoupon, getRealtimeStock } from '@/api/coupon'
import { showApiErrorAlert } from '@/utils/apiError'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('available')
const loading = ref(false)
const receivingId = ref(null)

const availableCoupons = ref([])
const myCoupons = ref([])

/** 每秒更新一次的当前时间（用于实时倒计时和过期判断） */
const now = ref(new Date())
let clockTimer = null
let stockTimer = null

// ============================================================
// 工具函数
// ============================================================
const isLoggedIn = () => userStore.isLoggedIn

const pad = n => String(n).padStart(2, '0')

/** 精确到秒的日期时间格式 */
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 动态过期判断（与 now 绑定，响应式） */
const isExpired = (endTime) => {
  if (!endTime) return false
  return new Date(endTime) <= now.value
}

/**
 * 实时倒计时文字。
 * > 1 天 → "X天 HH:mm:ss"
 * ≤ 1 天 → "HH:mm:ss"
 * 已过期 → null（调用方负责处理）
 */
const getCountdown = (endTime) => {
  if (!endTime) return null
  const diff = new Date(endTime) - now.value
  if (diff <= 0) return null
  const s = Math.floor(diff / 1000) % 60
  const m = Math.floor(diff / 60_000) % 60
  const h = Math.floor(diff / 3_600_000) % 24
  const d = Math.floor(diff / 86_400_000)
  if (d > 0) return `${d}天 ${pad(h)}:${pad(m)}:${pad(s)}`
  return `${pad(h)}:${pad(m)}:${pad(s)}`
}

const typeLabel = (type, discountValue, threshold) => {
  if (type === 2) return `${discountValue}折优惠`
  const base = threshold > 0 ? `满¥${threshold / 100}减¥${discountValue / 100}` : `立减¥${discountValue / 100}`
  return base
}

// ============================================================
// 数据加载
// ============================================================
const loadAvailable = async () => {
  loading.value = true
  try {
    const res = await getAvailableCoupons()
    availableCoupons.value = Array.isArray(res) ? res : []
  } catch (e) {
    if (e.response?.status !== 401) {
      availableCoupons.value = []
    }
  } finally {
    loading.value = false
  }
}

const loadMine = async () => {
  if (!isLoggedIn()) {
    router.push('/login?redirect=/coupons')
    return
  }
  loading.value = true
  try {
    const res = await getMyCoupons({ silentError: true })
    myCoupons.value = Array.isArray(res) ? res : []
  } catch (e) {
    showApiErrorAlert(e)
  } finally {
    loading.value = false
  }
}

const onTabChange = (tab) => {
  activeTab.value = tab
  if (tab === 'mine') loadMine()
}

// ============================================================
// 实时库存轮询（每 5 秒从 Redis 拉最新库存）
// ============================================================
const refreshStock = async () => {
  if (activeTab.value !== 'available' || availableCoupons.value.length === 0) return
  const ids = availableCoupons.value.map(c => c.id)
  try {
    const stockMap = await getRealtimeStock(ids, { silentError: true })
    availableCoupons.value = availableCoupons.value.map(c => ({
      ...c,
      stock: stockMap[c.id] !== undefined ? stockMap[c.id] : c.stock
    }))
  } catch { /* 轮询失败静默，不打扰用户 */ }
}

// ============================================================
// 领券
// ============================================================
const handleReceive = async (coupon) => {
  if (!isLoggedIn()) {
    router.push('/login?redirect=/coupons')
    return
  }
  if (receivingId.value === coupon.id) return
  receivingId.value = coupon.id
  try {
    await receiveCoupon(coupon.id, { silentError: true })
    alert(`「${coupon.name}」领取成功！`)
    // 领取后立即刷新库存和列表
    await loadAvailable()
    if (activeTab.value === 'mine') loadMine()
  } catch (e) {
    showApiErrorAlert(e)
  } finally {
    receivingId.value = null
  }
}

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
  stockTimer = setInterval(refreshStock, 5000)
  loadAvailable()
  loadMine()
})

onUnmounted(() => {
  clearInterval(clockTimer)
  clearInterval(stockTimer)
})

const displayList = computed(() =>
  activeTab.value === 'available' ? availableCoupons.value : myCoupons.value
)
</script>

<template>
  <div class="min-h-screen bg-[#F9FAFB] selection:bg-black selection:text-white">

    <!-- 顶栏 -->
    <div class="bg-white border-b border-gray-100 sticky top-0 z-50">
      <div class="max-w-5xl mx-auto px-6 h-16 flex items-center gap-4">
        <button @click="router.back()" class="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors">
          <ChevronLeft :size="20" stroke-width="1.5" />
        </button>
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 bg-black rounded-lg flex items-center justify-center">
            <Ticket :size="16" class="text-white" stroke-width="2" />
          </div>
          <span class="text-[15px] font-bold tracking-tight uppercase">领券中心</span>
        </div>
        <div class="ml-auto">
          <router-link to="/" class="flex items-center gap-2 text-[10px] font-bold uppercase tracking-widest text-gray-400 hover:text-black transition-colors">
            <ShoppingBag :size="14" /> 继续购物
          </router-link>
        </div>
      </div>
    </div>

    <!-- 英雄区 -->
    <div class="bg-black text-white py-16 px-6 text-center relative overflow-hidden">
      <div class="absolute inset-0 bg-gradient-to-br from-gray-900 via-black to-gray-800" />
      <div class="relative z-10 max-w-2xl mx-auto space-y-4">
        <div class="flex items-center justify-center gap-2 text-[10px] font-bold uppercase tracking-[0.2em] text-gray-400">
          <Zap :size="12" /> 限时特权
        </div>
        <h1 class="text-4xl md:text-5xl font-light tracking-tight">专属优惠券</h1>
        <p class="text-gray-400 text-sm leading-relaxed">
          领取优惠券，在结算时自动抵扣。每人限领一张，先到先得。
        </p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="bg-white border-b border-gray-100 sticky top-16 z-40">
      <div class="max-w-5xl mx-auto px-6 flex gap-8">
        <button
          @click="onTabChange('available')"
          class="py-4 text-[11px] font-bold uppercase tracking-widest border-b-2 transition-colors"
          :class="activeTab === 'available' ? 'border-black text-black' : 'border-transparent text-gray-400 hover:text-gray-700'"
        >
          可领取
          <span v-if="availableCoupons.length" class="ml-1.5 px-1.5 py-0.5 bg-black text-white text-[9px] rounded-full">{{ availableCoupons.length }}</span>
        </button>
        <button
          @click="onTabChange('mine')"
          class="py-4 text-[11px] font-bold uppercase tracking-widest border-b-2 transition-colors"
          :class="activeTab === 'mine' ? 'border-black text-black' : 'border-transparent text-gray-400 hover:text-gray-700'"
        >
          我的券
          <span v-if="myCoupons.length" class="ml-1.5 px-1.5 py-0.5 bg-gray-200 text-gray-700 text-[9px] rounded-full">{{ myCoupons.length }}</span>
        </button>
      </div>
    </div>

    <!-- 券列表区 -->
    <div class="max-w-5xl mx-auto px-6 py-12">

      <!-- 加载中 -->
      <div v-if="loading" class="flex justify-center py-24">
        <div class="w-8 h-8 border-2 border-gray-200 border-t-black rounded-full animate-spin" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="displayList.length === 0" class="flex flex-col items-center gap-4 py-24 text-gray-400">
        <Gift :size="40" stroke-width="1" />
        <p class="text-sm font-medium">
          {{ activeTab === 'available' ? '暂无可领取的优惠券' : '还没有领取任何优惠券' }}
        </p>
        <button v-if="activeTab === 'mine'" @click="onTabChange('available')" class="mt-2 px-6 py-2 bg-black text-white text-[11px] font-bold uppercase tracking-widest rounded-full hover:opacity-80 transition-opacity">
          去领券
        </button>
      </div>

      <!-- 券卡片网格 -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div
          v-for="coupon in displayList"
          :key="coupon.id"
          class="bg-white rounded-2xl overflow-hidden border transition-all"
          :class="isExpired(coupon.endTime) ? 'border-gray-100 opacity-60' : 'border-gray-100 hover:border-gray-200 hover:shadow-md'"
        >
          <!-- 券头：黑色区域 -->
          <div class="text-white p-6 relative" :class="isExpired(coupon.endTime) ? 'bg-gray-400' : 'bg-black'">
            <!-- 类型标签 -->
            <div class="flex items-center gap-2 mb-3">
              <BadgePercent v-if="coupon.type === 1" :size="14" class="text-gray-300" />
              <Ticket v-else :size="14" class="text-gray-300" />
              <span class="text-[9px] font-bold uppercase tracking-widest text-gray-400">
                {{ coupon.type === 1 ? '满减券' : '折扣券' }}
              </span>
              <!-- 已过期角标 -->
              <span v-if="isExpired(coupon.endTime)" class="ml-auto flex items-center gap-1 text-[9px] font-bold text-red-300">
                <AlertCircle :size="10" /> 已过期
              </span>
            </div>
            <!-- 优惠金额 -->
            <div class="text-3xl font-light tracking-tight mb-1">
              {{ typeLabel(coupon.type, coupon.discountValue, coupon.threshold) }}
            </div>
            <div class="text-gray-500 text-[11px] font-medium">
              {{ coupon.name }}
            </div>
            <!-- 锯齿分割线装饰 -->
            <div class="absolute -bottom-3 left-0 right-0 flex">
              <div v-for="i in 20" :key="i" class="flex-1 h-3 bg-[#F9FAFB] rounded-t-full" />
            </div>
          </div>

          <!-- 券体 -->
          <div class="px-6 pt-6 pb-5 space-y-4">
            <!-- 精确到秒的时间范围 -->
            <div class="flex items-start gap-1.5 text-[11px] text-gray-400">
              <Clock :size="12" class="mt-0.5 shrink-0" />
              <div class="space-y-0.5">
                <div>{{ formatDateTime(coupon.beginTime) }}</div>
                <div>{{ formatDateTime(coupon.endTime) }}</div>
              </div>
            </div>

            <!-- 实时倒计时 -->
            <div class="flex items-center justify-between">
              <div v-if="!isExpired(coupon.endTime)" class="flex items-center gap-1 text-orange-500 font-bold text-[12px]">
                <Zap :size="12" />
                <span class="font-mono tracking-wider">{{ getCountdown(coupon.endTime) }}</span>
              </div>
              <div v-else class="text-[11px] text-gray-400 font-medium">活动已结束</div>

              <!-- 实时库存（仅可领取 tab） -->
              <div v-if="activeTab === 'available' && coupon.stock !== undefined" class="text-right">
                <span class="text-[10px] text-gray-400">剩余库存 </span>
                <span class="text-[13px] font-bold" :class="coupon.stock <= 10 ? 'text-red-500' : coupon.stock <= 50 ? 'text-orange-500' : 'text-gray-700'">
                  {{ coupon.stock }}
                </span>
              </div>
            </div>

            <!-- 库存进度条（仅可领取 tab 且券有发行总量信息） -->
            <div v-if="activeTab === 'available' && coupon.publishCount && coupon.stock !== undefined" class="space-y-1">
              <div class="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-700"
                  :class="coupon.stock <= 10 ? 'bg-red-500' : coupon.stock <= 50 ? 'bg-orange-400' : 'bg-black'"
                  :style="{ width: `${Math.min(100, (coupon.stock / coupon.publishCount) * 100)}%` }"
                />
              </div>
            </div>

            <!-- 已领取状态（我的券 tab） -->
            <div v-if="activeTab === 'mine'" class="flex items-center gap-1.5 text-[11px] font-medium text-emerald-600">
              <CheckCircle2 :size="14" />
              已领取
            </div>

            <!-- 操作按钮（可领取 tab） -->
            <div v-if="activeTab === 'available'">
              <button
                @click="handleReceive(coupon)"
                :disabled="coupon.stock === 0 || isExpired(coupon.endTime) || receivingId === coupon.id"
                class="w-full h-11 flex items-center justify-center gap-2 rounded-xl text-[11px] font-bold uppercase tracking-widest transition-all active:scale-95"
                :class="coupon.stock === 0 || isExpired(coupon.endTime)
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                  : 'bg-black text-white hover:opacity-80'"
              >
                <span v-if="receivingId === coupon.id">领取中…</span>
                <span v-else-if="isExpired(coupon.endTime)">活动已结束</span>
                <span v-else-if="coupon.stock === 0">已抢完</span>
                <template v-else>
                  <BookmarkCheck :size="14" /> 立即领取
                </template>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

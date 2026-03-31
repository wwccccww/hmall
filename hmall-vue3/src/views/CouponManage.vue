<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Plus, Send, Ticket, BadgePercent, RefreshCw, CheckCircle2, Clock, Users } from 'lucide-vue-next'
import { createCoupon, publishCoupon, getManageCoupons, getCouponReceiveRecords, getItemCategories } from '@/api/coupon'
import { showApiErrorAlert } from '@/utils/apiError'

// ============================================================
// 状态
// ============================================================
const submitting = ref(false)
const publishingId = ref(null)
const coupons = ref([])

/** 展开某券的领取记录面板 */
const recordsOpenId = ref(null)
const recordsPayload = ref({ list: [], total: 0, pages: 0 })
const recordsLoading = ref(false)

const recordStatusLabel = (s) => {
  const map = { 1: '未使用', 2: '已使用', 3: '已过期' }
  return map[s] ?? `状态${s}`
}

const toggleReceiveRecords = async (coupon) => {
  if (recordsOpenId.value === coupon.id) {
    recordsOpenId.value = null
    return
  }
  recordsOpenId.value = coupon.id
  recordsLoading.value = true
  recordsPayload.value = { list: [], total: 0, pages: 0 }
  try {
    const res = await getCouponReceiveRecords(coupon.id, {
      params: { pageNo: 1, pageSize: 50 },
      silentError: true
    })
    recordsPayload.value = {
      list: Array.isArray(res?.list) ? res.list : [],
      total: res?.total ?? 0,
      pages: res?.pages ?? 0
    }
  } catch (e) {
    showApiErrorAlert(e)
    recordsOpenId.value = null
  } finally {
    recordsLoading.value = false
  }
}

/** 每秒更新一次（用于动态判断是否过期） */
const now = ref(new Date())
let clockTimer = null
let pollTimer = null

const form = ref({
  name: '',
  type: 1,
  discountValue: null,
  threshold: 0,
  scopeType: 1,
  categoryNames: [],
  publishCount: null,
  beginTime: '',
  endTime: ''
})

const resetForm = () => {
  form.value = { name: '', type: 1, discountValue: null, threshold: 0, scopeType: 1, categoryNames: [], publishCount: null, beginTime: '', endTime: '' }
}

// ============================================================
// 类目列表（用于指定类目券）
// ============================================================
const categoryOptions = ref([])
const categoryKeyword = ref('')

const loadCategories = async () => {
  try {
    const res = await getItemCategories({ silentError: true })
    const list = Array.isArray(res) ? res : []
    // 后端返回 [{name}]，也兼容 ["手机"] 这种格式
    categoryOptions.value = list
      .map(x => (typeof x === 'string' ? x : x?.name))
      .filter(Boolean)
  } catch (e) {
    // 获取失败不阻塞创建券，仍允许手动输入
    categoryOptions.value = []
  }
}

const filteredCategoryOptions = computed(() => {
  const kw = String(categoryKeyword.value || '').trim()
  if (!kw) return categoryOptions.value
  return categoryOptions.value.filter(n => String(n).includes(kw))
})

// ============================================================
// 格式转换
// ============================================================
const toISOLocal = (datetimeLocal) =>
  datetimeLocal ? datetimeLocal + ':00' : null

const pad = n => String(n).padStart(2, '0')

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ============================================================
// 动态有效状态
// 规则：DB status=2（进行中）但 endTime 已过 → 显示为"已结束"
//       DB status=2 但 beginTime 未到 → 显示为"未开始"
// ============================================================
const effectiveStatus = (coupon) => {
  if (coupon.status !== 2) return coupon.status
  const begin = new Date(coupon.beginTime)
  const end   = new Date(coupon.endTime)
  if (now.value < begin) return 5  // 未开始（扩展状态码，仅前端使用）
  if (now.value >= end)  return 3  // 已结束
  return 2                         // 进行中
}

const statusLabel = (coupon) => {
  const s = effectiveStatus(coupon)
  const map = { 1: '草稿', 2: '进行中', 3: '已结束', 4: '暂停', 5: '未开始' }
  return map[s] || '未知'
}

const statusClass = (coupon) => {
  const s = effectiveStatus(coupon)
  if (s === 2) return 'bg-emerald-50 text-emerald-700'
  if (s === 1) return 'bg-gray-100 text-gray-500'
  if (s === 5) return 'bg-blue-50 text-blue-600'
  return 'bg-red-50 text-red-500'
}

/** 倒计时（距结束）：进行中的券才显示 */
const getCountdown = (coupon) => {
  if (effectiveStatus(coupon) !== 2) return null
  const diff = new Date(coupon.endTime) - now.value
  if (diff <= 0) return null
  const s = Math.floor(diff / 1000) % 60
  const m = Math.floor(diff / 60_000) % 60
  const h = Math.floor(diff / 3_600_000) % 24
  const d = Math.floor(diff / 86_400_000)
  if (d > 0) return `还剩 ${d}天 ${pad(h)}:${pad(m)}:${pad(s)}`
  return `还剩 ${pad(h)}:${pad(m)}:${pad(s)}`
}

// ============================================================
// 加载已发布优惠券（含 Redis 实时库存，由后端 enrichWithRedisStock 注入）
// ============================================================
const loadCoupons = async () => {
  try {
    const res = await getManageCoupons({ silentError: true })
    coupons.value = Array.isArray(res) ? res : []
  } catch (e) {
    showApiErrorAlert(e)
  }
}

// ============================================================
// 创建优惠券
// ============================================================
const handleCreate = async () => {
  if (!form.value.name.trim()) { alert('请填写优惠券名称'); return }
  if (!form.value.discountValue || form.value.discountValue <= 0) { alert('请填写有效的优惠值'); return }
  if (!form.value.publishCount || form.value.publishCount <= 0) { alert('请填写发行总量'); return }
  if (!form.value.beginTime || !form.value.endTime) { alert('请选择活动时间'); return }
  if (form.value.scopeType === 3 && (!Array.isArray(form.value.categoryNames) || form.value.categoryNames.length === 0)) {
    alert('请选择“指定类目”时必须选择至少一个类目')
    return
  }

  submitting.value = true
  try {
    const categoryNames = form.value.scopeType === 3 ? form.value.categoryNames : null
    const payload = {
      ...form.value,
      discountValue: form.value.type === 1 ? Math.round(form.value.discountValue * 100) : form.value.discountValue,
      threshold: form.value.type === 1 ? Math.round((form.value.threshold || 0) * 100) : 0,
      beginTime: toISOLocal(form.value.beginTime),
      endTime: toISOLocal(form.value.endTime)
      ,
      scopeType: form.value.scopeType,
      categoryNames
    }
    const newId = await createCoupon(payload, { silentError: true })
    alert(`优惠券创建成功（ID: ${newId}），请发布以开放领取`)
    resetForm()
    loadCoupons()
  } catch (e) {
    showApiErrorAlert(e)
  } finally {
    submitting.value = false
  }
}

// ============================================================
// 发布优惠券
// ============================================================
const handlePublish = async (coupon) => {
  if (publishingId.value === coupon.id) return
  publishingId.value = coupon.id
  try {
    await publishCoupon(coupon.id, { silentError: true })
    alert(`「${coupon.name}」已发布，用户可开始领取`)
    loadCoupons()
  } catch (e) {
    showApiErrorAlert(e)
  } finally {
    publishingId.value = null
  }
}

const typeLabel = (type, val) => type === 1 ? `满减 ¥${val / 100}` : `${val}折`

// ============================================================
// 生命周期
// ============================================================
onMounted(() => {
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
  // 每 30 秒重新拉一次（含最新 Redis 库存）
  pollTimer = setInterval(loadCoupons, 30_000)
  loadCoupons()
  loadCategories()
})

onUnmounted(() => {
  clearInterval(clockTimer)
  clearInterval(pollTimer)
})
</script>

<template>
  <div class="p-8 space-y-10 animate-spa-reveal">

    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div class="space-y-1">
        <h1 class="text-2xl font-bold tracking-tight">优惠券运营</h1>
        <p class="text-gray-400 text-sm">仅展示当前管理员创建的券；状态与库存实时更新（每 30 秒自动刷新）</p>
      </div>
      <button @click="loadCoupons" class="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors">
        <RefreshCw :size="16" stroke-width="1.5" class="text-gray-400" />
      </button>
    </div>

    <!-- 创建表单 -->
    <div class="bg-white rounded-2xl border border-gray-100 overflow-hidden">
      <div class="flex items-center gap-3 px-8 py-5 border-b border-gray-100">
        <div class="w-8 h-8 bg-black rounded-lg flex items-center justify-center">
          <Plus :size="16" class="text-white" stroke-width="2" />
        </div>
        <span class="text-[14px] font-bold uppercase tracking-wider">创建新优惠券</span>
      </div>

      <div class="px-8 py-8 grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- 名称 -->
        <div class="md:col-span-2 space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">优惠券名称</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="例如：618 限量满减券"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors"
          />
        </div>

        <!-- 优惠类型 -->
        <div class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">优惠类型</label>
          <div class="flex gap-3">
            <button
              @click="form.type = 1"
              class="flex-1 h-11 rounded-xl border text-sm font-medium transition-all"
              :class="form.type === 1 ? 'border-black bg-black text-white' : 'border-gray-200 text-gray-600 hover:border-gray-400'"
            >
              <BadgePercent :size="14" class="inline mr-1.5" />满减
            </button>
            <button
              @click="form.type = 2"
              class="flex-1 h-11 rounded-xl border text-sm font-medium transition-all"
              :class="form.type === 2 ? 'border-black bg-black text-white' : 'border-gray-200 text-gray-600 hover:border-gray-400'"
            >
              <Ticket :size="14" class="inline mr-1.5" />折扣
            </button>
          </div>
        </div>

        <!-- 优惠值 -->
        <div class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">
            {{ form.type === 1 ? '减免金额（元）' : '折扣（如 85 代表 85 折）' }}
          </label>
          <input
            v-model.number="form.discountValue"
            type="number"
            :min="form.type === 2 ? 1 : 0.01"
            :max="form.type === 2 ? 99 : undefined"
            :placeholder="form.type === 1 ? '例如：50（元）' : '例如：85（85 折）'"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors"
          />
        </div>

        <!-- 使用门槛（仅满减） -->
        <div v-if="form.type === 1" class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">使用门槛（元，0 = 无门槛）</label>
          <input
            v-model.number="form.threshold"
            type="number" min="0"
            placeholder="例如：200（即满 200 元可用）"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors"
          />
        </div>

        <!-- 发行总量 -->
        <div :class="form.type === 2 ? 'md:col-span-1' : ''" class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">发行总量（张）</label>
          <input
            v-model.number="form.publishCount"
            type="number" min="1"
            placeholder="例如：100"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors"
          />
        </div>

        <!-- 适用范围 -->
        <div class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">适用范围</label>
          <select
            v-model.number="form.scopeType"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors bg-white"
          >
            <option :value="1">全场通用</option>
            <option :value="3">指定类目</option>
          </select>
        </div>

        <!-- 指定类目（scopeType=3） -->
        <div v-if="form.scopeType === 3" class="space-y-2 md:col-span-2">
          <div class="flex items-center justify-between gap-3">
            <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">指定类目（多选）</label>
            <input
              v-model="categoryKeyword"
              type="text"
              placeholder="搜索类目…"
              class="h-9 px-3 rounded-lg border border-gray-200 text-xs focus:outline-none focus:border-black transition-colors"
            />
          </div>
          <select
            v-model="form.categoryNames"
            multiple
            class="w-full min-h-[120px] px-3 py-2 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors bg-white"
          >
            <option v-for="c in filteredCategoryOptions" :key="c" :value="c">{{ c }}</option>
          </select>
          <p class="text-[10px] text-gray-400">
            类目来源：商品表 `item.category` 去重；请选择与商品类目完全一致的名称。
          </p>
        </div>

        <!-- 活动时间 -->
        <div class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">活动开始时间</label>
          <input
            v-model="form.beginTime"
            type="datetime-local"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors"
          />
        </div>
        <div class="space-y-1.5">
          <label class="text-[11px] font-bold uppercase tracking-widest text-gray-400">活动结束时间</label>
          <input
            v-model="form.endTime"
            type="datetime-local"
            class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors"
          />
        </div>
      </div>

      <div class="px-8 pb-8 flex gap-4">
        <button
          @click="handleCreate"
          :disabled="submitting"
          class="px-8 h-11 bg-black text-white text-[11px] font-bold uppercase tracking-widest rounded-full hover:opacity-80 transition-all disabled:opacity-50 flex items-center gap-2"
        >
          <Plus :size="14" />
          {{ submitting ? '创建中…' : '创建优惠券' }}
        </button>
        <button
          @click="resetForm"
          class="px-6 h-11 border border-gray-200 text-gray-500 text-[11px] font-bold uppercase tracking-widest rounded-full hover:border-gray-400 transition-all"
        >
          重置
        </button>
      </div>
    </div>

    <!-- 优惠券列表 -->
    <div class="bg-white rounded-2xl border border-gray-100 overflow-hidden">
      <div class="flex items-center gap-3 px-8 py-5 border-b border-gray-100">
        <div class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center">
          <Ticket :size="16" class="text-gray-500" stroke-width="1.5" />
        </div>
        <span class="text-[14px] font-bold uppercase tracking-wider">优惠券列表</span>
        <span class="ml-2 px-2 py-0.5 bg-gray-100 text-gray-500 text-[10px] font-bold rounded-full">{{ coupons.length }}</span>
      </div>

      <div v-if="coupons.length === 0" class="flex flex-col items-center gap-3 py-16 text-gray-400">
        <Ticket :size="36" stroke-width="1" />
        <p class="text-sm">暂无优惠券，上方创建后点「发布」即可上线</p>
      </div>

      <div v-else class="divide-y divide-gray-50">
        <div
          v-for="coupon in coupons"
          :key="coupon.id"
          class="hover:bg-gray-50/50 transition-colors"
        >
        <div class="flex items-center gap-6 px-8 py-5">
          <!-- 图标 -->
          <div class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0"
            :class="effectiveStatus(coupon) === 2 ? 'bg-black' : 'bg-gray-200'">
            <BadgePercent v-if="coupon.type === 1" :size="18" :class="effectiveStatus(coupon) === 2 ? 'text-white' : 'text-gray-400'" />
            <Ticket v-else :size="18" :class="effectiveStatus(coupon) === 2 ? 'text-white' : 'text-gray-400'" />
          </div>

          <!-- 信息 -->
          <div class="flex-1 min-w-0 space-y-1.5">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="font-semibold text-[14px] truncate">{{ coupon.name }}</span>
              <span class="shrink-0 px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-wider" :class="statusClass(coupon)">
                {{ statusLabel(coupon) }}
              </span>
            </div>
            <div class="flex items-center gap-4 text-[11px] text-gray-400 flex-wrap">
              <span>{{ typeLabel(coupon.type, coupon.discountValue) }}</span>
              <!-- Redis 实时库存 -->
              <span class="font-medium" :class="coupon.stock <= 10 ? 'text-red-500' : coupon.stock <= 50 ? 'text-orange-500' : 'text-gray-500'">
                剩余库存 {{ coupon.stock }}
              </span>
            </div>
            <!-- 精确到秒的时间 -->
            <div class="flex items-center gap-1.5 text-[10px] text-gray-300">
              <Clock :size="10" />
              {{ formatDateTime(coupon.beginTime) }} ~ {{ formatDateTime(coupon.endTime) }}
            </div>
            <!-- 进行中显示实时倒计时 -->
            <div v-if="getCountdown(coupon)" class="text-[10px] font-mono font-bold text-orange-500">
              {{ getCountdown(coupon) }}
            </div>
          </div>

          <!-- 操作：发布 / 领取记录 -->
          <div class="shrink-0 flex flex-col items-end gap-2">
            <div v-if="coupon.status === 2" class="flex items-center gap-1.5 text-[11px] font-bold text-emerald-600">
              <CheckCircle2 :size="14" /> 已上线
            </div>
            <button
              v-else
              @click="handlePublish(coupon)"
              :disabled="publishingId === coupon.id"
              class="flex items-center gap-2 px-5 h-9 bg-black text-white text-[10px] font-bold uppercase tracking-widest rounded-full hover:opacity-80 transition-all disabled:opacity-50"
            >
              <Send :size="12" />
              {{ publishingId === coupon.id ? '发布中…' : '发布上线' }}
            </button>
            <button
              type="button"
              @click="toggleReceiveRecords(coupon)"
              class="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-gray-500 hover:text-black"
            >
              <Users :size="12" />
              {{ recordsOpenId === coupon.id ? '收起领取记录' : '领取记录' }}
            </button>
          </div>
        </div>

        <!-- 领取记录（user_coupon） -->
        <div
          v-if="recordsOpenId === coupon.id"
          class="px-8 pb-6 border-t border-gray-50 bg-gray-50/40"
        >
          <p v-if="recordsLoading" class="text-xs text-gray-400 py-3">加载中…</p>
          <template v-else>
            <p v-if="recordsPayload.list.length === 0" class="text-xs text-gray-400 py-3">暂无领取记录</p>
            <div v-else class="overflow-x-auto rounded-xl border border-gray-100 bg-white">
              <table class="min-w-full text-left text-[11px]">
                <thead class="bg-gray-50 text-gray-500 font-bold uppercase tracking-wider">
                  <tr>
                    <th class="px-4 py-2">用户 ID</th>
                    <th class="px-4 py-2">领取时间</th>
                    <th class="px-4 py-2">状态</th>
                    <th class="px-4 py-2">过期时间</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-gray-50">
                  <tr v-for="(row, idx) in recordsPayload.list" :key="idx">
                    <td class="px-4 py-2 font-mono">{{ row.userId }}</td>
                    <td class="px-4 py-2">{{ formatDateTime(row.receiveTime) }}</td>
                    <td class="px-4 py-2">{{ recordStatusLabel(row.status) }}</td>
                    <td class="px-4 py-2">{{ formatDateTime(row.expiredAt) }}</td>
                  </tr>
                </tbody>
              </table>
              <p v-if="recordsPayload.total > recordsPayload.list.length" class="px-4 py-2 text-[10px] text-gray-400">
                共 {{ recordsPayload.total }} 条，当前展示前 {{ recordsPayload.list.length }} 条
              </p>
            </div>
          </template>
        </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Send, Ticket, BadgePercent, RefreshCw, CheckCircle2 } from 'lucide-vue-next'
import { createCoupon, publishCoupon, getAvailableCoupons } from '@/api/coupon'
import { showApiErrorAlert } from '@/utils/apiError'

// ============================================================
// 状态
// ============================================================
const submitting = ref(false)
const publishingId = ref(null)
const coupons = ref([])

const form = ref({
  name: '',
  type: 1,
  discountValue: null,
  threshold: 0,
  publishCount: null,
  beginTime: '',
  endTime: ''
})

const resetForm = () => {
  form.value = { name: '', type: 1, discountValue: null, threshold: 0, publishCount: null, beginTime: '', endTime: '' }
}

// ============================================================
// 格式转换：datetime-local 值 → ISO 字符串（LocalDateTime 接受 yyyy-MM-ddTHH:mm:ss）
// ============================================================
const toISOLocal = (datetimeLocal) =>
  datetimeLocal ? datetimeLocal.replace('T', 'T') + ':00' : null

// ============================================================
// 加载已发布优惠券
// ============================================================
const loadCoupons = async () => {
  try {
    const res = await getAvailableCoupons()
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

  submitting.value = true
  try {
    const payload = {
      ...form.value,
      discountValue: form.value.type === 1 ? Math.round(form.value.discountValue * 100) : form.value.discountValue,
      threshold: form.value.type === 1 ? Math.round((form.value.threshold || 0) * 100) : 0,
      beginTime: toISOLocal(form.value.beginTime),
      endTime: toISOLocal(form.value.endTime)
    }
    const newId = await createCoupon(payload)
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
    await publishCoupon(coupon.id)
    alert(`「${coupon.name}」已发布，用户可开始领取`)
    loadCoupons()
  } catch (e) {
    showApiErrorAlert(e)
  } finally {
    publishingId.value = null
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const typeLabel = (type, val) => {
  return type === 1 ? `满减 ¥${val / 100}` : `${val}折`
}

const statusLabel = (status) => {
  const map = { 1: '草稿', 2: '进行中', 3: '已结束', 4: '暂停' }
  return map[status] || '未知'
}

const statusClass = (status) => {
  if (status === 2) return 'bg-emerald-50 text-emerald-700'
  if (status === 1) return 'bg-gray-100 text-gray-500'
  return 'bg-red-50 text-red-500'
}

onMounted(loadCoupons)
</script>

<template>
  <div class="p-8 space-y-10 animate-spa-reveal">

    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <div class="space-y-1">
        <h1 class="text-2xl font-bold tracking-tight">优惠券运营</h1>
        <p class="text-gray-400 text-sm">创建限量优惠券并发布到 Redis，用户可实时秒杀领取</p>
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
          class="flex items-center gap-6 px-8 py-5 hover:bg-gray-50/50 transition-colors"
        >
          <!-- 图标 -->
          <div class="w-10 h-10 bg-black rounded-xl flex items-center justify-center shrink-0">
            <BadgePercent v-if="coupon.type === 1" :size="18" class="text-white" />
            <Ticket v-else :size="18" class="text-white" />
          </div>

          <!-- 信息 -->
          <div class="flex-1 min-w-0 space-y-1">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-[14px] truncate">{{ coupon.name }}</span>
              <span class="shrink-0 px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-wider" :class="statusClass(coupon.status)">
                {{ statusLabel(coupon.status) }}
              </span>
            </div>
            <div class="flex items-center gap-4 text-[11px] text-gray-400">
              <span>{{ typeLabel(coupon.type, coupon.discountValue) }}</span>
              <span>库存 {{ coupon.stock }}</span>
              <span>{{ formatDate(coupon.beginTime) }} ~ {{ formatDate(coupon.endTime) }}</span>
            </div>
          </div>

          <!-- 发布按钮（已发布时显示"已上线"） -->
          <div class="shrink-0">
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
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

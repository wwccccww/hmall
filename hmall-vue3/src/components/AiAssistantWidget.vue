<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { X, Loader2, ChevronDown, ChevronUp, ExternalLink } from 'lucide-vue-next'
import { aiChatStream, aiChatSync } from '@/api/ai'
import { addCartItem } from '@/api/cart'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const open = ref(false)
const sending = ref(false)
const input = ref('')
const sourceDebugOpen = ref(false)
const messages = ref([
  {
    role: 'assistant',
    content:
      '我是 AI 导购助手。你可以问我：\n- 推荐 2000 元左右手机\n- 我有哪些优惠券？\n- 我的地址\n- 订单 123456789 状态？'
  }
])
const sources = ref(null)
/** 后端下发的购物动作；旧版无 actions 时由 sources 里 type=item 推导 */
const shoppingActions = ref([])

const isDev = import.meta.env.DEV

function deriveShoppingActions(srcList) {
  if (!Array.isArray(srcList)) return []
  return srcList
    .filter((x) => x && x.type === 'item' && x.id != null && x.addToCart)
    .map((x) => ({
      type: 'shopping_item',
      itemId: x.id,
      name: x.name,
      image: x.image,
      price: x.price,
      category: x.category,
      brand: x.brand,
      productPath: x.productPath || `/product/${x.id}`,
      productUrl: x.productUrl,
      addToCart: x.addToCart
    }))
}

function applySourcesPayload(payload) {
  if (payload && typeof payload === 'object' && !Array.isArray(payload) && 'sources' in payload) {
    sources.value = payload.sources
    shoppingActions.value =
      Array.isArray(payload.actions) && payload.actions.length > 0
        ? normalizeActionsFromApi(payload.actions)
        : deriveShoppingActions(payload.sources)
  } else {
    sources.value = Array.isArray(payload) ? payload : null
    shoppingActions.value = deriveShoppingActions(sources.value)
  }
}

/** 同步后端 buildShoppingActions 结构，补齐展示用字段 */
function normalizeActionsFromApi(actions) {
  return actions.map((a) => {
    const cart = a.addToCart
    return {
      ...a,
      image: a.image ?? cart?.image,
      price: a.price ?? cart?.price,
      category: a.category ?? cart?.category,
      brand: a.brand ?? cart?.brand
    }
  })
}

function applyChatResponse(resp) {
  const ans = resp?.answer
  if (ans != null) {
    return ans
  }
  return ''
}

const isLoggedIn = computed(() => userStore.isLoggedIn)
const title = computed(() => (isLoggedIn.value ? 'AI 导购（已登录）' : 'AI 导购（未登录）'))

const lastMessageIdx = computed(() => Math.max(0, messages.value.length - 1))

/** 工具类 actions 非商品，不参与推荐卡片 */
const displayShoppingActions = computed(() =>
  shoppingActions.value.filter((a) => a && a.itemId != null && a.addToCart)
)

function isStreamingAssistantSlot(idx, m) {
  return sending.value && idx === lastMessageIdx.value && m.role === 'assistant'
}

const scrollRef = ref(null)
async function scrollToBottom() {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function toggle() {
  open.value = !open.value
  if (open.value) scrollToBottom()
}

function closePanel() {
  open.value = false
}

function formatPriceYuan(action) {
  const raw = action.price ?? action.addToCart?.price
  if (raw == null || raw === '') return null
  const n = Number(raw)
  if (Number.isNaN(n)) return null
  return (n / 100).toFixed(2)
}

function productImageUrl(action) {
  const u = action.image || action.addToCart?.image
  if (u && String(u).trim()) return String(u).trim()
  return null
}

function onProductImgError(e) {
  const el = e?.target
  if (el && el.style) {
    el.style.display = 'none'
  }
}

async function send({ stream = true } = {}) {
  const text = input.value.trim()
  if (!text || sending.value) return
  input.value = ''
  sources.value = null
  shoppingActions.value = []
  sourceDebugOpen.value = false

  messages.value.push({ role: 'user', content: text })
  const assistantMsg = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)
  await scrollToBottom()

  sending.value = true
  try {
    if (stream) {
      await aiChatStream(text, {
        onDelta: (d) => {
          assistantMsg.content += d
          scrollToBottom()
        },
        onSources: (s) => {
          applySourcesPayload(s)
        },
        onResult: (resp) => {
          assistantMsg.content = applyChatResponse(resp)
          applySourcesPayload(
            resp?.actions != null
              ? { sources: resp?.sources || [], actions: resp.actions }
              : resp?.sources || null
          )
        },
        onError: (e) => {
          assistantMsg.content += `\n\n[错误] ${e?.message || e}`
        }
      })
    } else {
      const resp = await aiChatSync(text)
      assistantMsg.content = applyChatResponse(resp)
      applySourcesPayload(
        resp?.actions != null
          ? { sources: resp?.sources || [], actions: resp.actions }
          : resp?.sources || null
      )
    }
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function openAiProduct(action) {
  const path = action.productPath || `/product/${action.itemId}`
  router.push(path)
}

function openAiProductExternal(action) {
  const url = action.productUrl
  if (url && String(url).trim()) {
    window.open(String(url).trim(), '_blank', 'noopener,noreferrer')
  }
}

async function addAiItemToCart(action) {
  const body = action.addToCart
  if (!body) return
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await addCartItem(body)
    router.push('/cart')
  } catch (e) {
    if (e.response?.status === 401) {
      userStore.clearUserInfo()
      router.push({ path: '/login', query: { redirect: route.fullPath } })
    }
  }
}
</script>

<template>
  <div class="fixed right-5 bottom-5 z-[9999]">
    <button
      type="button"
      class="rounded-full shadow-lg bg-black text-white px-4 py-3 text-sm font-medium hover:bg-gray-900 active:bg-gray-950"
      :class="open ? 'ring-2 ring-gray-300' : ''"
      @click="toggle"
    >
      AI 导购
    </button>

    <div
      v-if="open"
      class="mt-3 w-[360px] h-[520px] bg-white rounded-2xl shadow-2xl border border-gray-200 flex flex-col overflow-hidden"
    >
      <div class="px-3 py-2.5 border-b border-gray-100 flex items-center gap-2 bg-white">
        <div class="flex-1 min-w-0">
          <div class="font-semibold text-gray-900 text-sm truncate">{{ title }}</div>
        </div>
        <span
          class="shrink-0 inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium"
          :class="
            sending
              ? 'bg-amber-50 text-amber-800 border border-amber-200'
              : 'bg-gray-100 text-gray-600 border border-gray-200'
          "
        >
          {{ sending ? '回复中' : '就绪' }}
        </span>
        <button
          type="button"
          class="shrink-0 p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 hover:text-gray-800"
          aria-label="关闭"
          @click="closePanel"
        >
          <X class="w-4 h-4" />
        </button>
      </div>

      <div ref="scrollRef" class="flex-1 overflow-auto px-3 py-3 space-y-3 bg-gray-50/90">
        <div
          v-for="(m, idx) in messages"
          :key="idx"
          class="flex"
          :class="m.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[90%] text-sm leading-relaxed px-3 py-2 rounded-2xl"
            :class="
              m.role === 'user'
                ? 'bg-gray-900 text-white rounded-br-md whitespace-pre-wrap'
                : 'bg-white text-gray-900 rounded-bl-md border border-gray-200 shadow-sm'
            "
          >
            <template v-if="m.role === 'assistant'">
              <div v-if="m.content" class="whitespace-pre-wrap">{{ m.content }}</div>
              <div
                v-else-if="isStreamingAssistantSlot(idx, m)"
                class="flex items-center gap-2 text-gray-500 py-0.5"
              >
                <Loader2 class="w-4 h-4 shrink-0 animate-spin text-gray-400" />
                <span class="text-xs">正在回复…</span>
                <span class="inline-block w-2 h-4 ml-0.5 bg-gray-300 rounded-sm animate-pulse" />
              </div>
            </template>
            <template v-else>{{ m.content }}</template>
          </div>
        </div>

        <div v-if="displayShoppingActions.length" class="space-y-2">
          <div class="text-[11px] font-semibold text-gray-500 uppercase tracking-wide">
            推荐商品
          </div>
          <div
            v-for="(a, i) in displayShoppingActions"
            :key="`${a.itemId}-${i}`"
            class="bg-white border border-gray-200 rounded-xl p-2.5 shadow-sm flex gap-3"
          >
            <div
              class="w-20 h-20 shrink-0 rounded-lg bg-gray-100 border border-gray-100 overflow-hidden flex items-center justify-center"
            >
              <img
                v-if="productImageUrl(a)"
                :src="productImageUrl(a)"
                :alt="a.name || ''"
                class="w-full h-full object-cover"
                loading="lazy"
                @error="onProductImgError"
              />
              <span v-else class="text-[10px] text-gray-400 px-1 text-center">暂无图</span>
            </div>
            <div class="flex-1 min-w-0 flex flex-col gap-1.5">
              <div class="font-medium text-gray-900 text-xs leading-snug line-clamp-3">
                {{ a.name || `商品 #${a.itemId}` }}
              </div>
              <div v-if="formatPriceYuan(a)" class="text-sm font-semibold text-gray-900">
                ¥{{ formatPriceYuan(a) }}
              </div>
              <div
                v-if="(a.category && String(a.category).trim()) || (a.brand && String(a.brand).trim())"
                class="flex flex-wrap gap-1"
              >
                <span
                  v-if="a.category && String(a.category).trim()"
                  class="text-[10px] px-1.5 py-0.5 rounded-md bg-gray-100 text-gray-600"
                >
                  {{ a.category }}
                </span>
                <span
                  v-if="a.brand && String(a.brand).trim()"
                  class="text-[10px] px-1.5 py-0.5 rounded-md bg-gray-100 text-gray-600"
                >
                  {{ a.brand }}
                </span>
              </div>
              <div class="flex flex-wrap gap-1.5 mt-auto pt-0.5">
                <button
                  type="button"
                  class="px-2.5 py-1 rounded-lg text-xs bg-white border border-gray-300 text-gray-800 hover:bg-gray-50"
                  @click="openAiProduct(a)"
                >
                  查看商品
                </button>
                <button
                  v-if="a.productUrl"
                  type="button"
                  class="inline-flex items-center gap-0.5 px-2 py-1 rounded-lg text-xs bg-white border border-gray-300 text-gray-700 hover:bg-gray-50"
                  @click="openAiProductExternal(a)"
                >
                  <ExternalLink class="w-3 h-3" />
                  新窗口
                </button>
                <button
                  type="button"
                  class="px-2.5 py-1 rounded-lg text-xs bg-gray-900 text-white hover:bg-gray-800"
                  @click="addAiItemToCart(a)"
                >
                  加入购物车
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="sources" class="rounded-xl border border-gray-200 bg-white overflow-hidden">
          <button
            type="button"
            class="w-full flex items-center justify-between px-3 py-2 text-left text-[11px] text-gray-500 hover:bg-gray-50"
            @click="sourceDebugOpen = !sourceDebugOpen"
          >
            <span>{{ isDev ? 'sources（调试）' : '技术详情' }}</span>
            <component :is="sourceDebugOpen ? ChevronUp : ChevronDown" class="w-3.5 h-3.5 shrink-0" />
          </button>
          <div v-if="sourceDebugOpen" class="px-3 pb-3 pt-0">
            <pre class="text-[10px] text-gray-600 whitespace-pre-wrap break-words max-h-40 overflow-auto bg-gray-50 rounded-lg p-2 border border-gray-100">{{ JSON.stringify(sources, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <div class="p-3 border-t border-gray-100 bg-white">
        <div class="flex gap-2">
          <input
            v-model="input"
            class="flex-1 border border-gray-200 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-black/15 placeholder:text-gray-400"
            placeholder="输入你的问题…"
            @keydown.enter.prevent="send({ stream: true })"
          />
          <button
            type="button"
            class="px-3 py-2 rounded-xl bg-gray-900 text-white text-sm font-medium disabled:opacity-50 hover:bg-gray-800"
            :disabled="sending"
            @click="send({ stream: true })"
          >
            发送
          </button>
        </div>
        <div class="mt-1.5 text-[10px] text-gray-400 flex items-center justify-between">
          <span>回车发送 · 流式 SSE</span>
          <button
            type="button"
            class="text-gray-400 hover:text-gray-600 disabled:opacity-50"
            :disabled="sending"
            @click="send({ stream: false })"
          >
            改用同步
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { aiChatStream, aiChatSync } from '@/api/ai'
import { addCartItem } from '@/api/cart'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const open = ref(false)
const sending = ref(false)
const input = ref('')
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

function deriveShoppingActions(srcList) {
  if (!Array.isArray(srcList)) return []
  return srcList
    .filter((x) => x && x.type === 'item' && x.id != null && x.addToCart)
    .map((x) => ({
      type: 'shopping_item',
      itemId: x.id,
      name: x.name,
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
        ? payload.actions
        : deriveShoppingActions(payload.sources)
  } else {
    sources.value = Array.isArray(payload) ? payload : null
    shoppingActions.value = deriveShoppingActions(sources.value)
  }
}

const isLoggedIn = computed(() => userStore.isLoggedIn)
const title = computed(() => (isLoggedIn.value ? 'AI 导购（已登录）' : 'AI 导购（未登录）'))

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

async function send({ stream = true } = {}) {
  const text = input.value.trim()
  if (!text || sending.value) return
  input.value = ''
  sources.value = null
  shoppingActions.value = []

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
        onError: (e) => {
          assistantMsg.content += `\n\n[错误] ${e?.message || e}`
        }
      })
    } else {
      const resp = await aiChatSync(text)
      assistantMsg.content = resp?.answer || ''
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
      class="rounded-full shadow-lg bg-black text-white px-4 py-3 hover:bg-gray-900 active:bg-gray-950"
      @click="toggle"
    >
      {{ open ? '关闭 AI' : 'AI 导购' }}
    </button>

    <div
      v-if="open"
      class="mt-3 w-[360px] h-[520px] bg-white rounded-2xl shadow-2xl border border-gray-200 flex flex-col overflow-hidden"
    >
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-semibold text-gray-900">{{ title }}</div>
        <div class="text-xs text-gray-500">
          {{ sending ? '思考中…' : '就绪' }}
        </div>
      </div>

      <div ref="scrollRef" class="flex-1 overflow-auto px-4 py-3 space-y-3 bg-gray-50">
        <div
          v-for="(m, idx) in messages"
          :key="idx"
          class="flex"
          :class="m.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[85%] whitespace-pre-wrap text-sm leading-relaxed px-3 py-2 rounded-2xl"
            :class="m.role === 'user' ? 'bg-black text-white rounded-br-md' : 'bg-white text-gray-900 rounded-bl-md border border-gray-200'"
          >
            {{ m.content }}
          </div>
        </div>

        <div v-if="shoppingActions.length" class="space-y-2">
          <div class="text-xs font-semibold text-gray-700">推荐商品（可跳转或加购）</div>
          <div
            v-for="(a, i) in shoppingActions"
            :key="`${a.itemId}-${i}`"
            class="text-xs bg-white border border-gray-200 rounded-xl p-3 flex flex-col gap-2"
          >
            <div class="font-medium text-gray-900 line-clamp-2">{{ a.name || `商品 #${a.itemId}` }}</div>
            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                class="px-2 py-1 rounded-lg bg-white border border-gray-300 text-gray-800 hover:bg-gray-50"
                @click="openAiProduct(a)"
              >
                查看商品
              </button>
              <button
                type="button"
                class="px-2 py-1 rounded-lg bg-black text-white hover:bg-gray-900"
                @click="addAiItemToCart(a)"
              >
                加入购物车
              </button>
            </div>
          </div>
        </div>

        <div v-if="sources" class="text-xs text-gray-600 bg-white border border-gray-200 rounded-xl p-3">
          <div class="font-semibold text-gray-800 mb-2">sources（调试）</div>
          <pre class="whitespace-pre-wrap break-words">{{ JSON.stringify(sources, null, 2) }}</pre>
        </div>
      </div>

      <div class="p-3 border-t border-gray-200 bg-white">
        <div class="flex gap-2">
          <input
            v-model="input"
            class="flex-1 border border-gray-300 rounded-xl px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-black/20"
            placeholder="输入你的问题…"
            @keydown.enter.prevent="send({ stream: true })"
          />
          <button
            class="px-3 py-2 rounded-xl bg-black text-white text-sm disabled:opacity-50"
            :disabled="sending"
            @click="send({ stream: true })"
          >
            发送
          </button>
        </div>
        <div class="mt-2 text-xs text-gray-500 flex items-center justify-between">
          <span>回车发送（SSE）</span>
          <button class="underline" :disabled="sending" @click="send({ stream: false })">改用同步</button>
        </div>
      </div>
    </div>
  </div>
</template>


<script setup>
import { computed, nextTick, ref } from 'vue'
import { aiChatStream, aiChatSync } from '@/api/ai'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

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
          sources.value = s
        },
        onError: (e) => {
          assistantMsg.content += `\n\n[错误] ${e?.message || e}`
        }
      })
    } else {
      const resp = await aiChatSync(text)
      assistantMsg.content = resp?.answer || ''
      sources.value = resp?.sources || null
    }
  } finally {
    sending.value = false
    await scrollToBottom()
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

        <div v-if="sources" class="text-xs text-gray-600 bg-white border border-gray-200 rounded-xl p-3">
          <div class="font-semibold text-gray-800 mb-2">sources</div>
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


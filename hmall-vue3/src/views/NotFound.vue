<script setup>
import { useRouter } from 'vue-router'
import { ShoppingBag, ArrowLeft } from 'lucide-vue-next'
const router = useRouter()
</script>

<template>
  <div class="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center gap-8 px-6 selection:bg-black selection:text-white">
    <div class="text-center space-y-4">
      <div class="w-16 h-16 bg-black rounded-2xl flex items-center justify-center mx-auto mb-6">
        <ShoppingBag :size="28" class="text-white" stroke-width="1.5" />
      </div>
      <p class="text-[10px] font-bold uppercase tracking-[0.3em] text-gray-400">404 Not Found</p>
      <h1 class="text-5xl font-light tracking-tight text-gray-900">页面不存在</h1>
      <p class="text-gray-400 text-sm max-w-sm mx-auto leading-relaxed">
        您访问的页面已移动或不存在，请返回首页重试。
      </p>
    </div>
    <button
      @click="router.push('/')"
      class="flex items-center gap-2 px-8 py-3 bg-black text-white text-[11px] font-bold uppercase tracking-widest rounded-full hover:opacity-80 transition-opacity active:scale-95"
    >
      <ArrowLeft :size="14" /> 返回首页
    </button>
  </div>
</template>

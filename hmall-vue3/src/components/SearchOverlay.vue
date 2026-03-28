<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { Search, X, ArrowRight, Loader2, ShoppingBag, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useSearchStore } from '../store/search'
import { storeToRefs } from 'pinia'

const searchStore = useSearchStore()
const { searchQuery, results, pageNo, totalPages, totalItems, isLoading, showSearch } = storeToRefs(searchStore)

const inputRef = ref(null)
let timer = null

const handleSearch = (query) => {
  if (timer) clearTimeout(timer)
  
  if (!query.trim()) {
    searchStore.results = []
    searchStore.totalPages = 0
    searchStore.totalItems = 0
    return
  }
  
  timer = setTimeout(() => {
    searchStore.setSearchQuery(query)
  }, 300)
}

const visiblePages = computed(() => {
  const pages = []
  const maxPages = 5 // Show at most 5 page buttons
  let start = Math.max(1, pageNo.value - Math.floor(maxPages / 2))
  let end = Math.min(totalPages.value, start + maxPages - 1)
  
  if (end - start + 1 < maxPages) {
    start = Math.max(1, end - maxPages + 1)
  }
  
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

watch(() => showSearch.value, (newVal) => {
  if (newVal) {
    setTimeout(() => {
      inputRef.value?.focus()
    }, 100)
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

const handleKeydown = (e) => {
  if (e.key === 'Escape') searchStore.toggleSearch(false)
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  if (showSearch.value) document.body.style.overflow = 'hidden'
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Transition name="fade">
    <div v-if="showSearch" class="fixed inset-0 z-[200] flex flex-col bg-white">
      <!-- Header -->
      <div class="flex items-center justify-between px-6 md:px-12 py-8 border-b border-gray-50">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-black rounded-lg flex items-center justify-center">
            <ShoppingBag :size="18" class="text-white" stroke-width="2" />
          </div>
          <span class="text-lg font-bold tracking-tight uppercase">HMALL 检索系统</span>
        </div>
        <button 
          @click="searchStore.toggleSearch(false)"
          class="w-12 h-12 flex items-center justify-center rounded-full hover:bg-gray-50 transition-all group"
        >
          <X :size="24" class="text-gray-400 group-hover:text-black transition-colors" />
        </button>
      </div>

      <!-- Search Area -->
      <div class="flex-1 overflow-y-auto results-container">
        <div class="max-w-4xl mx-auto px-6 py-20">
          <div class="relative mb-20">
            <Search 
              class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-300" 
              :size="32" 
              stroke-width="1.5"
            />
            <input 
              ref="inputRef"
              :value="searchQuery"
              @input="handleSearch($event.target.value)"
              type="text" 
              placeholder="搜索数字奢华单品..." 
              class="w-full bg-transparent border-b-2 border-gray-100 pb-6 pl-12 text-3xl md:text-5xl font-light outline-none focus:border-black transition-colors placeholder:text-gray-100"
            />
            <div v-if="isLoading" class="absolute right-0 top-1/2 -translate-y-1/2">
              <Loader2 class="animate-spin text-gray-400" :size="24" />
            </div>
          </div>

          <!-- Results -->
          <div v-if="results.length > 0" class="space-y-12">
            <p class="text-[10px] font-bold text-gray-400 uppercase tracking-[0.3em]">发现相关单品 ({{ totalItems }})</p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
              <router-link 
                v-for="item in results" 
                :key="item.id"
                :to="'/product/' + item.id"
                @click="searchStore.toggleSearch(false)"
                class="flex items-center gap-6 p-6 rounded-3xl border border-transparent hover:border-gray-100 hover:bg-gray-50/50 transition-all group"
              >
                <div class="w-24 h-24 bg-[#F9FAFB] rounded-2xl flex items-center justify-center overflow-hidden flex-shrink-0">
                  <img :src="item.img" class="w-16 h-16 object-contain group-hover:scale-110 transition-transform duration-500">
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">{{ item.brand || item.category }}</p>
                  <h4 class="text-lg font-semibold text-gray-900 truncate">{{ item.name }}</h4>
                  <p class="text-gray-500 mt-1 italic">¥{{ item.price }}</p>
                </div>
                <ArrowRight :size="18" class="text-gray-300 group-hover:text-black group-hover:translate-x-1 transition-all" />
              </router-link>
            </div>

            <!-- Pagination: Selective Page Selection -->
            <div v-if="totalPages > 1" class="flex flex-col md:flex-row items-center justify-between pt-12 border-t border-gray-50 pb-20 gap-8">
              <div class="flex items-center gap-4">
                <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest leading-none">
                  Page <span class="text-black">{{ pageNo }}</span> of {{ totalPages }}
                  <span class="mx-3 text-gray-200">|</span>
                  <span class="text-gray-300">{{ totalItems }} Total Creations</span>
                </p>
              </div>
              
              <div class="flex items-center gap-2">
                <button 
                  @click="searchStore.setPage(pageNo - 1)"
                  :disabled="pageNo === 1"
                  class="w-10 h-10 flex items-center justify-center rounded-full border border-gray-100 hover:bg-black hover:text-white transition-all disabled:opacity-20 disabled:hover:bg-transparent disabled:hover:text-gray-400"
                >
                  <ChevronLeft :size="16" />
                </button>

                <!-- Page Number Buttons -->
                <div class="flex items-center bg-gray-50 p-1 rounded-full border border-gray-100">
                  <button 
                    v-for="num in visiblePages" 
                    :key="num"
                    @click="searchStore.setPage(num)"
                    class="w-8 h-8 rounded-full text-[11px] font-bold transition-all"
                    :class="pageNo === num ? 'bg-black text-white' : 'text-gray-400 hover:text-black'"
                  >
                    {{ num }}
                  </button>
                </div>
                
                <button 
                  @click="searchStore.setPage(pageNo + 1)"
                  :disabled="pageNo === totalPages"
                  class="w-10 h-10 flex items-center justify-center rounded-full border border-gray-100 hover:bg-black hover:text-white transition-all disabled:opacity-20 disabled:hover:bg-transparent disabled:hover:text-gray-400"
                >
                  <ChevronRight :size="16" />
                </button>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else-if="searchQuery && !isLoading" class="py-20 text-center space-y-4">
            <p class="text-5xl font-light text-gray-100 italic">No discoveries found.</p>
            <p class="text-gray-400 text-sm">尝试输入不同的关键词，如 "iPhone", "Audio" 或 "Mac"</p>
          </div>

          <!-- Quick Suggestions -->
          <div v-if="!searchQuery" class="space-y-8 animate-spa-reveal">
            <p class="text-[10px] font-bold text-gray-400 uppercase tracking-[0.3em]">热门搜索建议</p>
            <div class="flex flex-wrap gap-3">
              <button 
                v-for="tag in ['iPhone 15 Pro', 'AirPods Max', 'MacBook Air', 'iPad Pro', 'Apple Watch']" 
                :key="tag"
                @click="searchStore.setSearchQuery(tag)"
                class="px-6 py-2.5 rounded-full bg-gray-50 text-[11px] font-bold uppercase tracking-widest text-gray-500 hover:bg-black hover:text-white transition-all"
              >
                {{ tag }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.animate-spa-reveal {
  animation: spaReveal 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes spaReveal {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

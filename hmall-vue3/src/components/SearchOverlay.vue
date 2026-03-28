<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { Search, X, ArrowRight, Loader2, ShoppingBag, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useSearchStore } from '../store/search'
import { storeToRefs } from 'pinia'

const searchStore = useSearchStore()
const { searchQuery, results, pageNo, totalPages, totalItems, isLoading, showSearch } = storeToRefs(searchStore)

const inputRef = ref(null)
const localQuery = ref(searchQuery.value)
let timer = null

// 当 Store 中的全局搜索词改变时（如通过点击搜索建议），同步更新本地输入框
watch(searchQuery, (newVal) => {
  if (newVal !== localQuery.value) {
    localQuery.value = newVal
  }
})

// 监听本地输入变化，并进行防抖处理
watch(localQuery, (newVal) => {
  if (timer) clearTimeout(timer)
  // 如果本地输入与 Store 一致，说明是 Store 同步过来的，不重复触发搜索
  if (newVal === searchQuery.value) return 

  timer = setTimeout(() => {
    searchStore.setSearchQuery(newVal)
  }, 400)
})

const visiblePages = computed(() => {
  const pages = []
  const maxPages = 5
  if (totalPages.value <= 0) return pages
  let start = Math.max(1, pageNo.value - Math.floor(maxPages / 2))
  let end = Math.min(totalPages.value, start + maxPages - 1)
  if (end - start + 1 < maxPages) start = Math.max(1, end - maxPages + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const clearInput = () => {
  localQuery.value = ''
  searchStore.setSearchQuery('')
  inputRef.value?.focus()
}

watch(showSearch, (newVal) => {
  if (newVal) {
    localQuery.value = searchQuery.value
    setTimeout(() => {
      inputRef.value?.focus()
    }, 150)
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
  if (showSearch.value) {
    localQuery.value = searchQuery.value
    document.body.style.overflow = 'hidden'
  }
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
      <div class="flex-1 overflow-y-auto results-container scrollbar-hide">
        <div class="max-w-4xl mx-auto px-6 py-20">
          
          <!-- Premium Search Bar -->
          <div class="relative mb-20 flex items-center border-b-2 border-gray-100 focus-within:border-black transition-all group">
            <Search 
              class="text-gray-300 mr-6 group-focus-within:text-black transition-colors" 
              :size="36" 
              stroke-width="1.5"
            />
            <input 
              ref="inputRef"
              v-model="localQuery"
              type="text" 
              placeholder="搜索数字奢华单品..." 
              class="flex-1 bg-transparent py-8 text-3xl md:text-5xl font-light outline-none placeholder:text-gray-200"
              spellcheck="false"
              autocomplete="off"
            />
            <div class="flex items-center gap-4">
              <Loader2 v-if="isLoading" class="animate-spin text-gray-400" :size="24" />
              <button 
                v-if="localQuery" 
                @click="clearInput"
                class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-50 text-gray-300 hover:text-black transition-all"
              >
                <X :size="20" stroke-width="2" />
              </button>
            </div>
          </div>

          <!-- Results -->
          <div v-if="results.length > 0" class="space-y-12 pb-32">
            <p class="text-[10px] font-bold text-gray-400 uppercase tracking-[0.3em]">发现相关单品 ({{ totalItems }})</p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
              <router-link 
                v-for="item in results" 
                :key="item.id"
                :to="'/product/' + item.id"
                @click="searchStore.toggleSearch(false)"
                class="flex items-center gap-6 p-6 rounded-3xl border border-transparent hover:border-gray-100 hover:bg-gray-50/50 transition-all group animate-spa-reveal"
              >
                <div class="w-24 h-24 bg-[#F9FAFB] rounded-2xl flex items-center justify-center overflow-hidden flex-shrink-0">
                  <img :src="item.img" class="w-16 h-16 object-contain group-hover:scale-110 transition-transform duration-500" loading="lazy">
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">{{ item.category }}</p>
                  <h4 class="text-lg font-semibold text-gray-900 truncate">{{ item.name }}</h4>
                  <p class="text-gray-500 mt-1 italic font-medium">¥{{ item.price }}</p>
                </div>
                <ArrowRight :size="18" class="text-gray-300 group-hover:text-black group-hover:translate-x-1 transition-all" />
              </router-link>
            </div>

            <!-- Enhanced Pagination -->
            <div v-if="totalPages > 1" class="flex flex-col md:flex-row items-center justify-between pt-12 border-t border-gray-50 gap-8">
              <div class="flex items-center gap-2">
                <p class="text-[11px] font-bold text-gray-400 uppercase tracking-widest">
                  Page <span class="text-black">{{ pageNo }}</span> / {{ totalPages }}
                </p>
              </div>
              
              <div class="flex items-center gap-2">
                <button 
                  @click="searchStore.setPage(pageNo - 1)"
                  :disabled="pageNo === 1"
                  class="w-10 h-10 flex items-center justify-center rounded-full border border-gray-100 hover:bg-black hover:text-white transition-all disabled:opacity-20 disabled:hover:bg-transparent"
                >
                  <ChevronLeft :size="16" />
                </button>

                <div class="flex items-center bg-gray-50 p-1 rounded-full border border-gray-100 shadow-inner">
                  <button 
                    v-for="num in visiblePages" 
                    :key="num"
                    @click="searchStore.setPage(num)"
                    class="w-8 h-8 rounded-full text-[11px] font-bold transition-all"
                    :class="pageNo === num ? 'bg-black text-white shadow-lg' : 'text-gray-400 hover:text-black'"
                  >
                    {{ num }}
                  </button>
                </div>
                
                <button 
                  @click="searchStore.setPage(pageNo + 1)"
                  :disabled="pageNo === totalPages"
                  class="w-10 h-10 flex items-center justify-center rounded-full border border-gray-100 hover:bg-black hover:text-white transition-all disabled:opacity-20 disabled:hover:bg-transparent"
                >
                  <ChevronRight :size="16" />
                </button>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else-if="localQuery && !isLoading" class="py-20 text-center space-y-4 animate-spa-reveal">
            <p class="text-5xl font-light text-gray-100 italic">No discoveries found.</p>
            <p class="text-gray-400 text-sm">尝试输入不同的关键词，如 "iPhone", "Audio" 或 "Mac"</p>
          </div>

          <!-- Quick Suggestions -->
          <div v-if="!localQuery" class="space-y-8 animate-spa-reveal">
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

.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>

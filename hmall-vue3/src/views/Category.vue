<script setup>
import { ref } from 'vue'
import { FolderPlus, Trash2, Edit2, Search, MoreHorizontal, LayoutGrid, ListTree, Smartphone, Laptop, Headphones, Camera, Gamepad, Watch } from 'lucide-vue-next'

const categories = ref([
  { id: 101, name: '移动设备', count: 124, icon: Smartphone, color: 'blue' },
  { id: 102, name: '笔记型电脑', count: 56, icon: Laptop, color: 'purple' },
  { id: 103, name: '影音设备', count: 89, icon: Headphones, color: 'teal' },
  { id: 104, name: '专业相机', count: 34, icon: Camera, color: 'orange' },
  { id: 105, name: '游戏中心', count: 210, icon: Gamepad, color: 'rose' },
  { id: 106, name: '智能穿戴', count: 45, icon: Watch, color: 'emerald' },
])
</script>

<template>
  <div class="px-8 md:px-12 py-10 animate-spa-reveal">
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
      <div class="space-y-4">
        <p class="label-refined text-gray-400">商品分类与集合</p>
        <h1 class="h1-refined text-4xl">类目资源。</h1>
      </div>
      <div class="flex items-center gap-3">
        <button class="w-10 h-10 flex items-center justify-center rounded-lg text-gray-400 hover:bg-gray-50 hover:text-black transition-colors border border-gray-100">
          <ListTree :size="18" stroke-width="1.5" />
        </button>
        <button class="w-10 h-10 flex items-center justify-center bg-gray-50 rounded-lg text-black transition-colors border border-gray-200 shadow-sm">
          <LayoutGrid :size="18" stroke-width="1.5" />
        </button>
        <button class="spa-button gap-2 ml-2">
          <FolderPlus :size="18" stroke-width="2" />
          <span>新建类目</span>
        </button>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="spa-card mb-10 p-4 flex items-center gap-4 border-dashed border-gray-200 bg-transparent shadow-none">
      <div class="flex-1 relative group">
        <Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-black transition-colors" :size="18" stroke-width="1.5" />
        <input 
          type="text" 
          placeholder="搜索系统类目..." 
          class="w-full h-12 bg-white border border-gray-100 rounded-xl pl-12 pr-4 text-gray-900 placeholder-gray-400 focus:outline-none focus:border-gray-300 focus:shadow-[0_0_0_4px_rgba(0,0,0,0.02)] transition-all font-medium"
        >
      </div>
    </div>

    <!-- Category Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div v-for="cat in categories" :key="cat.id" class="spa-card p-8 flex flex-col group cursor-pointer border hover:border-black/5 transition-all">
        <div class="flex items-start justify-between mb-8">
          <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-gray-600 bg-gray-50 border border-gray-100 transition-transform duration-500 group-hover:scale-110 group-hover:bg-black group-hover:text-white group-hover:border-black shadow-sm">
            <component :is="cat.icon" :size="24" stroke-width="1.5" />
          </div>
          <button class="w-8 h-8 flex items-center justify-center hover:bg-gray-50 rounded-lg text-gray-400 hover:text-black transition-colors">
            <MoreHorizontal :size="18" />
          </button>
        </div>
        
        <h3 class="text-xl font-semibold text-gray-900 mb-2 truncate">{{ cat.name }}</h3>
        <p class="body-refined text-sm">{{ cat.count }} 款商品关联</p>
        
        <div class="mt-8 flex items-center justify-between pt-6 border-t border-gray-100/50">
          <div class="flex items-center gap-3">
            <button class="text-xs font-bold text-gray-400 hover:text-black transition-colors uppercase tracking-widest">高级配置</button>
          </div>
          <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
            <button class="w-8 h-8 flex items-center justify-center hover:bg-gray-50 rounded-lg text-gray-400 hover:text-black transition-colors">
              <Edit2 :size="14" stroke-width="2" />
            </button>
            <button class="w-8 h-8 flex items-center justify-center hover:bg-red-50 rounded-lg text-gray-400 hover:text-red-500 transition-colors">
              <Trash2 :size="14" stroke-width="2" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Inherits global spa design */
</style>

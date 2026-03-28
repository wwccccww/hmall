<script setup>
import { 
  LayoutDashboard, 
  Users, 
  ChevronLeft, 
  ChevronRight, 
  ShieldCheck, 
  Settings, 
  FolderTree, 
  Package,
  Layers,
  ArrowUpRight
} from 'lucide-vue-next'
import { ref } from 'vue'

const isCollapsed = ref(false)
const menuItems = [
  { name: '数据大盘', icon: LayoutDashboard, path: '/admin/dashboard' },
  { name: '商品图录', icon: Package, path: '/admin/goods/list' },
  { name: '类目资源', icon: FolderTree, path: '/admin/goods/category' },
  { name: '权限安全', icon: ShieldCheck, path: '/admin/auth/role' },
  { name: '系统设置', icon: Settings, path: '/admin/auth/menu' },
]

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<template>
  <aside 
    class="sidebar h-[96vh] bg-white border border-gray-100 flex flex-col rounded-3xl overflow-hidden m-4 shadow-[0_8px_30px_rgb(0,0,0,0.04)] fixed top-0 left-0 transition-all duration-500 z-50 animate-spa-reveal"
    :class="isCollapsed ? 'w-20' : 'w-72'"
  >
    <!-- Brand Logo Section -->
    <div class="h-20 flex items-center px-6 border-b border-gray-100 space-x-3 shrink-0">
      <div class="w-10 h-10 bg-black flex items-center justify-center rounded-xl cursor-pointer hover:rotate-6 transition-transform duration-300 shrink-0 shadow-sm">
        <Layers :size="18" class="text-white" stroke-width="2" />
      </div>
      <div v-show="!isCollapsed" class="flex flex-col whitespace-nowrap overflow-hidden transition-all">
        <span class="font-bold text-[15px] tracking-tight uppercase text-gray-900">管理控制台</span>
        <span class="text-[9px] font-semibold text-gray-400 uppercase tracking-widest mt-0.5">核心节点 v4.0</span>
      </div>
    </div>

    <!-- Navigation List -->
    <nav class="flex-1 py-8 px-4 space-y-1.5 overflow-y-auto scrollbar-hide">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="group flex items-center h-12 px-4 rounded-xl transition-all duration-300 active:scale-95"
        :class="$route.path === item.path ? 'bg-gray-50' : 'hover:bg-gray-50/50'"
      >
        <component 
          :is="item.icon" 
          :size="18" 
          class="shrink-0 transition-colors duration-300"
          stroke-width="1.5"
          :class="$route.path === item.path ? 'text-black' : 'text-gray-400 group-hover:text-gray-900'" 
        />
        <span v-show="!isCollapsed" class="ml-4 font-medium text-[13px] whitespace-nowrap" :class="$route.path === item.path ? 'text-black font-semibold' : 'text-gray-500 group-hover:text-gray-900'">
          {{ item.name }}
        </span>
      </router-link>
    </nav>

    <!-- Toggle & Profile Area -->
    <div class="p-4 border-t border-gray-100 flex flex-col space-y-4 shrink-0 bg-white">
       <button 
          @click="toggleSidebar"
          class="flex items-center justify-center h-10 w-full bg-gray-50 border-none hover:bg-gray-100 rounded-lg cursor-pointer transition-colors duration-300"
       >
          <ChevronLeft v-if="!isCollapsed" :size="16" class="text-gray-400" stroke-width="2" />
          <ChevronRight v-else :size="16" class="text-gray-400" stroke-width="2" />
       </button>
    </div>
  </aside>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>

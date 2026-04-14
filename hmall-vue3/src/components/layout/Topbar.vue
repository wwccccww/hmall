<script setup>
import { Bell, Search, User, LogOut, ChevronDown, Zap, ShieldCheck } from 'lucide-vue-next'
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useUserStore, GUEST_USER_DISPLAY } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const { userInfo: storeUser } = storeToRefs(userStore)
const showProfileMenu = ref(false)

/** 与 Pinia 同步：未登录时展示默认昵称 */
const userInfo = computed(() => storeUser.value ?? GUEST_USER_DISPLAY)

const isAdmin = computed(() => userStore.isAdmin)

const formatYuanFromFen = (fen) => {
  const n = Number(fen)
  if (!Number.isFinite(n)) return '0.00'
  return (n / 100).toFixed(2)
}

const handleLogout = () => {
  userStore.clearUserInfo()
  router.push('/admin-login')
}
</script>

<template>
  <header class="pr-8 md:pr-12 pl-[320px] h-28 flex items-center justify-between sticky top-0 z-40 bg-white/80 backdrop-blur-md border-b border-gray-100 transition-all duration-700 animate-spa-reveal">
    <!-- Breadcrumb Group -->
    <div class="flex items-center gap-4 bg-gray-50/50 border border-gray-100 rounded-full h-10 px-5 shadow-sm">
      <div class="flex items-center gap-2">
        <Zap :size="14" class="text-black" stroke-width="2" />
        <span class="text-[10px] font-bold uppercase tracking-widest text-gray-500">{{ $route.name || '概览' }}</span>
      </div>
      <div class="h-3 w-px bg-gray-200"></div>
      <div class="text-[11px] font-semibold text-gray-400 tracking-wider flex items-center gap-1">
         终端节点: <span class="text-black">核心系统-102</span>
      </div>
    </div>

    <!-- Actions -->
    <div class="flex items-center gap-6 h-full">
      <!-- Search Input Specialized -->
      <div class="flex items-center gap-3 px-5 h-10 bg-gray-50 border border-transparent rounded-full focus-within:border-gray-200 focus-within:bg-white transition-all group">
        <Search :size="16" class="text-gray-400 group-focus-within:text-black transition-colors" />
        <input type="text" placeholder="搜索任意资源..." class="bg-transparent text-[13px] outline-none text-gray-800 font-medium w-48 placeholder:text-gray-400">
      </div>

      <!-- User Profile Specialized -->
      <div class="relative h-10 flex items-center">
         <div 
           @click="showProfileMenu = !showProfileMenu"
           class="flex items-center gap-3 h-full px-4 rounded-full border border-gray-100 hover:bg-gray-50 transition-colors cursor-pointer group active:scale-95"
         >
           <div class="w-6 h-6 rounded-full bg-black flex items-center justify-center transition-all">
              <span class="text-[10px] font-bold text-white">{{ userInfo.username.charAt(0).toUpperCase() }}</span>
           </div>
           <div class="hidden sm:flex flex-col text-left gap-0.5">
              <span class="text-[11px] font-bold text-gray-900 leading-none tracking-tight">{{ userInfo.username }}</span>
              <span v-if="isAdmin" class="flex items-center gap-1 text-[9px] font-bold uppercase tracking-wider text-indigo-600">
                <ShieldCheck :size="10" stroke-width="2.5" />管理员
              </span>
           </div>
           <ChevronDown :size="14" class="text-gray-400 transition-transform duration-300" :class="{ 'rotate-180': showProfileMenu }" />
         </div>

         <!-- Dropdown menu -->
         <div v-show="showProfileMenu" class="absolute top-[120%] right-0 w-60 bg-white border border-gray-100 rounded-2xl overflow-hidden py-2 z-[60] shadow-[0_20px_40px_-10px_rgba(0,0,0,0.1)] animate-spa-reveal">
            <div class="px-5 py-4 border-b border-gray-50">
               <p class="text-[9px] font-bold text-gray-400 uppercase tracking-widest mb-1.5">已认证身份信息</p>
               <div class="flex items-center gap-2">
                 <p class="text-[13px] font-semibold text-gray-900 tracking-tight">{{ userInfo.username }}</p>
                 <span v-if="isAdmin" class="inline-flex items-center gap-1 px-1.5 py-0.5 bg-indigo-50 text-indigo-600 text-[9px] font-bold uppercase tracking-wider rounded-full border border-indigo-100">
                   <ShieldCheck :size="9" stroke-width="2.5" />Admin
                 </span>
               </div>
               <p v-if="userInfo.balance !== undefined" class="text-[11px] text-gray-500 mt-1 font-medium italic">账户余额: ¥{{ formatYuanFromFen(userInfo.balance) }}</p>
            </div>
            <div class="py-2 px-2">
               <a href="#" class="flex items-center gap-3 px-4 py-2.5 rounded-lg text-[12px] font-medium text-gray-600 hover:bg-gray-50 hover:text-black transition-colors">
                  <User :size="16" stroke-width="1.5" /> 个人认证档案
               </a>
               <a href="#" class="flex items-center gap-3 px-4 py-2.5 rounded-lg text-[12px] font-medium text-gray-600 hover:bg-gray-50 hover:text-black transition-colors">
                  <Bell :size="16" stroke-width="1.5" /> 事件通告
               </a>
               <div class="h-px bg-gray-50 my-1"></div>
               <a @click="handleLogout" class="flex items-center gap-3 px-4 py-2.5 rounded-lg text-[12px] font-medium text-red-500 hover:bg-red-50 hover:text-red-600 transition-colors cursor-pointer">
                  <LogOut :size="16" stroke-width="1.5" /> 登出终端设备
               </a>
            </div>
         </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* Scoped overrides if needed, but primary styles in style.css */
</style>

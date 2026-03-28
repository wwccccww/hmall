<script setup>
import Sidebar from './Sidebar.vue'
import Topbar from './Topbar.vue'
</script>

<template>
  <div class="layout h-screen flex bg-[#fdfdfd]">
    <Sidebar />
    
    <div class="main-content flex-1 flex flex-col min-w-0" style="margin-left: 288px; transition: margin-left 0.3s ease;">
      <Topbar />
      
      <main class="flex-1 overflow-x-hidden overflow-y-auto">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.main-content {

  background: transparent;
}

.min-w-0 { min-width: 0; }
.overflow-x-hidden { overflow-x: hidden; }
.overflow-y-auto { overflow-y: auto; }

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

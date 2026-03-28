<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Shield, User, Lock, ArrowRight, Eye, EyeOff } from 'lucide-vue-next'
import request from '../utils/request'

/**
 * Aesthetic: Swiss Spa Minimalist
 * Focus: High-end clarity, perfect spacing, and airy typography.
 */

interface LoginForm {
  username: string
  password: string
}

const router = useRouter()
const isLoading = ref(false)
const showPassword = ref(false)
const loginForm = ref<LoginForm>({ username: '', password: '' })
const isFocused = ref<string | null>(null)

const handleLogin = async () => {
  if (isLoading.value) return
  isLoading.value = true
  
  try {
    const data = await request.post('/users/login', {
      username: loginForm.value.username,
      password: loginForm.value.password
    })
    
    // data 包含了 token 和其余 user 字段
    const { token, ...user } = data
    sessionStorage.setItem("token", token)
    sessionStorage.setItem("user-info", JSON.stringify(user))
    console.log("user信息:" + user)
    console.log("token信息:" + token)
    isLoading.value = false
    router.push('/')
  } catch (err) {
    alert("登录失败，请检查用户名或密码")
    isLoading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-[#F9FAFB] px-6 py-12">
    
    <!-- Subtle Background Elements -->
    <div class="absolute inset-0 z-0 overflow-hidden pointer-events-none">
      <div class="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-50/50 rounded-full blur-[120px]"></div>
      <div class="absolute bottom-[-10%] left-[-10%] w-[30%] h-[30%] bg-gray-100/50 rounded-full blur-[100px]"></div>
    </div>

    <!-- Login Container -->
    <div class="relative z-10 w-full max-w-[440px] animate-spa-reveal">
      
      <!-- Branding & Header -->
      <div class="text-center mb-12">
        <div class="inline-flex items-center justify-center w-14 h-14 bg-black rounded-2xl mb-6 shadow-sm">
          <Shield class="text-white" :size="24" stroke-width="1.5" />
        </div>
        <h1 class="h1-refined mb-3">欢迎回来</h1>
        <p class="body-refined">请输入账号密码登录商城哦</p>
      </div>

      <!-- Authentication Card -->
      <div class="spa-card p-10 md:p-12 shadow-sm bg-white/80 backdrop-blur-sm">
        <form @submit.prevent="handleLogin" class="space-y-8">
          
          <!-- Username Field -->
          <div class="space-y-2">
            <label class="label-refined block ml-1">身份标识</label>
            <div class="relative group">
              <input 
                v-model="loginForm.username"
                type="text" 
                placeholder="用户名或电子邮箱"
                class="spa-input pl-11"
                @focus="isFocused = 'username'"
                @blur="isFocused = null"
                required
              >
              <User 
                :size="18" 
                class="absolute left-4 top-1/2 -translate-y-1/2 transition-colors duration-300" 
                :class="isFocused === 'username' ? 'text-black' : 'text-gray-400'"
                stroke-width="1.5"
              />
            </div>
          </div>

          <!-- Password Field -->
          <div class="space-y-2">
            <div class="flex items-center justify-between px-1">
              <label class="label-refined">安全密钥</label>
              <a href="#" class="text-[11px] font-medium text-gray-500 hover:text-black transition-colors">忘记密码？</a>
            </div>
            <div class="relative group">
              <input 
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'" 
                placeholder="••••••••"
                class="spa-input pl-11 pr-11"
                @focus="isFocused = 'password'"
                @blur="isFocused = null"
                required
              >
              <Lock 
                :size="18" 
                class="absolute left-4 top-1/2 -translate-y-1/2 transition-colors duration-300"
                :class="isFocused === 'password' ? 'text-black' : 'text-gray-400'"
                stroke-width="1.5"
              />
              <button 
                type="button" 
                @click="showPassword = !showPassword"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-black transition-colors"
              >
                <component :is="showPassword ? EyeOff : Eye" :size="16" stroke-width="1.5" />
              </button>
            </div>
          </div>

          <!-- Action Button -->
          <button 
            type="submit" 
            :disabled="isLoading"
            class="spa-button w-full h-14 group"
          >
            <span v-if="isLoading" class="flex items-center gap-2">
              <span class="w-1.5 h-1.5 bg-white/40 rounded-full animate-pulse"></span>
              <span class="w-1.5 h-1.5 bg-white/70 rounded-full animate-pulse [animation-delay:0.2s]"></span>
              <span class="w-1.5 h-1.5 bg-white rounded-full animate-pulse [animation-delay:0.4s]"></span>
            </span>
            <span v-else class="flex items-center gap-2">
              继续登录 <ArrowRight :size="16" stroke-width="2" class="group-hover:translate-x-1 transition-transform" />
            </span>
          </button>
        </form>

        <div class="mt-10 pt-8 border-t border-gray-100 text-center">
          <p class="body-refined text-[13px]">
            首次使用管理系统？ 
            <a href="#" class="font-semibold text-black hover:underline underline-offset-4">建立账户授权</a>
          </p>
        </div>
      </div>

      <!-- Footer Info -->
      <div class="mt-8 text-center">
        <p class="text-[10px] text-gray-400 uppercase tracking-[0.2em] font-medium">
          安全终端节点接入 • v2.4.0
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Scoped overrides if needed, but primary styles in style.css */
</style>


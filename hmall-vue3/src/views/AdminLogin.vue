<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ShieldCheck, User, Lock, ArrowRight, Eye, EyeOff } from 'lucide-vue-next'
import { login as loginApi } from '@/api/user'
import { useUserStore } from '../store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = reactive({ username: '', password: '' })
const isLoading = ref(false)
const showPassword = ref(false)
const isFocused = ref(null)
const errorMsg = ref('')

const handleLogin = async () => {
  if (isLoading.value) return
  errorMsg.value = ''
  isLoading.value = true

  try {
    const data = await loginApi({ username: loginForm.username, password: loginForm.password })
    const body = data
    const payload =
      body && body.code === 200 && body.data != null && typeof body.data === 'object'
        ? body.data
        : body

    if (!payload || typeof payload !== 'object') {
      errorMsg.value = '登录异常：响应格式不正确'
      isLoading.value = false
      return
    }

    const token = typeof payload.token === 'string' ? payload.token.trim() : ''
    if (!token) {
      errorMsg.value = '登录异常：未获取到有效令牌'
      isLoading.value = false
      return
    }

    // 角色校验：必须是管理员
    if (payload.role !== 1) {
      errorMsg.value = '权限不足：该账号不是管理员'
      isLoading.value = false
      return
    }

    const { token: _t, ...user } = payload
    userStore.setUserInfo(user, token)
    isLoading.value = false

    const redirect = route.query.redirect
    const path = typeof redirect === 'string' ? redirect : ''
    if (path.startsWith('/') && !path.startsWith('//')) {
      router.replace(path)
    } else {
      router.push('/admin/dashboard')
    }
  } catch {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-[#F9FAFB] px-6 py-12">

    <div class="absolute inset-0 z-0 overflow-hidden pointer-events-none">
      <div class="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-50/40 rounded-full blur-[120px]"></div>
      <div class="absolute bottom-[-10%] left-[-10%] w-[30%] h-[30%] bg-gray-100/50 rounded-full blur-[100px]"></div>
    </div>

    <div class="relative z-10 w-full max-w-[440px] animate-spa-reveal">

      <!-- Header -->
      <div class="text-center mb-12">
        <div class="inline-flex items-center justify-center w-14 h-14 bg-black rounded-2xl mb-6 shadow-sm">
          <ShieldCheck class="text-white" :size="24" stroke-width="1.5" />
        </div>
        <p class="text-[10px] font-bold uppercase tracking-[0.3em] text-gray-400 mb-3">管理员控制台</p>
        <h1 class="h1-refined mb-3">后台登录</h1>
        <p class="body-refined">仅限管理员账号登录，权限不足将拒绝访问</p>
      </div>

      <!-- Card -->
      <div class="spa-card p-10 md:p-12 shadow-sm bg-white/80 backdrop-blur-sm">
        <form @submit.prevent="handleLogin" class="space-y-8">

          <!-- Error -->
          <div
            v-if="errorMsg"
            class="flex items-start gap-3 px-4 py-3 bg-red-50 border border-red-100 rounded-xl text-red-600 text-[13px]"
          >
            <span class="mt-0.5 shrink-0">⚠</span>
            <span>{{ errorMsg }}</span>
          </div>

          <!-- Username -->
          <div class="space-y-2">
            <label class="label-refined block ml-1">管理员账号</label>
            <div class="relative group">
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="   请输入管理员用户名"
                class="spa-input pl-11"
                @focus="isFocused = 'username'"
                @blur="isFocused = null"
                required
              />
              <User
                :size="18"
                class="absolute left-4 top-1/2 -translate-y-1/2 transition-colors duration-300"
                :class="isFocused === 'username' ? 'text-black' : 'text-gray-400'"
                stroke-width="1.5"
              />
            </div>
          </div>

          <!-- Password -->
          <div class="space-y-2">
            <label class="label-refined block ml-1">安全密钥</label>
            <div class="relative group">
              <input
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="   请输入密码"
                class="spa-input pl-11 pr-11"
                @focus="isFocused = 'password'"
                @blur="isFocused = null"
                required
              />
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

          <!-- Submit -->
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
              验证身份并进入 <ArrowRight :size="16" stroke-width="2" class="group-hover:translate-x-1 transition-transform" />
            </span>
          </button>
        </form>

        <div class="mt-10 pt-8 border-t border-gray-100 text-center">
          <button
            @click="router.push('/')"
            class="text-[13px] text-gray-400 hover:text-black transition-colors"
          >
            ← 返回商城首页
          </button>
        </div>
      </div>

      <div class="mt-8 text-center">
        <p class="text-[10px] text-gray-400 uppercase tracking-[0.2em] font-medium">
          安全后台节点 • hmall Admin v1.0
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spa-input {
  padding-left: 3rem !important; 
  padding-right: 3rem !important; /* 给右侧的小眼睛图标也留点空间 */
}

/* 确保图标位置在 padding 区域内居中 */
.relative.group .absolute.left-4 {
  left: 1rem; /* 图标距离左边缘 16px */
}

/* 修正 placeholder 的样式，确保它也不会和图标重叠 */
.spa-input::placeholder {
  color: #9CA3AF;
  font-size: 0.875rem;
}
</style>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ShoppingBag, ChevronLeft, Trash2, ArrowRight, Minus, Plus } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()

const cartItems = ref([])

onMounted(async () => {
  try {
    const res = await request.get('/carts')
    if (res && res.length > 0) {
      cartItems.value = res.map(i => ({
        id: i.id,
        itemId: i.itemId,
        name: i.name,
        spec: i.spec && i.spec !== '{}' ? Object.values(JSON.parse(i.spec)).join(', ') : '',
        price: (i.newPrice || i.price) / 100,
        quantity: i.num,
        stock: i.stock,
        img: i.image
      }))
    }
  } catch(e) {
    console.warn("使用本地测试数据，因为后端购物车请求失败")
    cartItems.value = [
      { id: 101, name: 'iPhone 15 Pro (测试数据)', spec: '256GB, 原色钛金属', price: 8999, quantity: 1, stock: 100, img: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=400' },
      { id: 102, name: 'AirPods Max (测试数据)', spec: '银色', price: 3999, quantity: 1, stock: 100, img: 'https://images.unsplash.com/photo-1613040809024-b4ef7ba99bc3?w=400' }
    ]
  }
})

const total = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const updateQuantity = async (item, delta) => {
  const newQ = item.quantity + delta
  if (newQ >= 1 && newQ <= (item.stock || 999)) {
    try {
      await request.put('/carts', { id: item.id, num: newQ })
      item.quantity = newQ
    } catch(e) {
      // 本地测试状态
      item.quantity = newQ
    }
  }
}

const removeItem = async (id) => {
  try {
    await request.delete(`/carts/${id}`)
    cartItems.value = cartItems.value.filter(item => item.id !== id)
  } catch(e) {
    cartItems.value = cartItems.value.filter(item => item.id !== id)
  }
}

const checkout = async () => {
  if(cartItems.value.length === 0) return alert('购物车为空')
  
  try {
    const details = cartItems.value.map(i => ({ itemId: i.itemId || i.id, num: i.quantity }))
    
    // Attempt backend `/orders` creation
    const orderId = await request.post('/orders', {
      details,
      paymentType: 3, // 面板默认余额支付
      addressId: 1 // 替换为数据库中真实存在的地址 ID，通常 1 是默认测试地址
    })
    router.push({ path: '/pay', query: { id: orderId } })
  } catch(e) {
    console.error("生成后端订单失败，启动本地测试链路...", e)
    router.push('/pay')
  }
}
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Navbar -->
    <nav class="h-24 flex items-center px-6 md:px-12 border-b border-gray-100">
      <div class="w-full max-w-5xl mx-auto flex items-center justify-between">
        <button @click="router.push('/')" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors">
          <ChevronLeft :size="20" stroke-width="1.5" />
        </button>
        <div class="flex items-center gap-3">
          <ShoppingBag :size="18" stroke-width="2" />
          <span class="text-sm font-bold tracking-widest uppercase">购物袋</span>
        </div>
        <div class="w-10"></div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-5xl mx-auto px-6 md:px-12 py-16 animate-spa-reveal">
      <div v-if="cartItems.length === 0" class="text-center py-32 space-y-6">
        <p class="text-gray-400 text-lg">您的购物袋是空的。</p>
        <button @click="router.push('/')" class="spa-button">继续选购</button>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-16">
        <!-- Cart Items -->
        <div class="lg:col-span-2 space-y-8">
          <h1 class="text-3xl font-light tracking-tight mb-8">选购商品</h1>
          
          <div class="space-y-6 border-t border-gray-100 pt-6">
             <div v-for="item in cartItems" :key="item.id" class="flex gap-6 group">
                <div class="w-32 h-32 bg-[#f8f9fa] rounded-2xl flex items-center justify-center p-4">
                   <img :src="item.img" class="w-full h-full object-contain mix-blend-multiply transition-transform group-hover:scale-105" alt="product">
                </div>
                
                <div class="flex-1 flex flex-col justify-between py-2">
                   <div class="flex justify-between items-start">
                      <div>
                         <h3 class="text-lg font-semibold text-gray-900">{{ item.name }}</h3>
                         <p class="text-sm text-gray-500 mt-1">{{ item.spec }}</p>
                      </div>
                      <p class="text-lg font-medium">¥{{ item.price.toLocaleString() }}</p>
                   </div>
                   
                   <div class="flex items-center justify-between">
                      <div class="flex items-center gap-4 bg-gray-50 rounded-full px-2 py-1">
                         <button @click="updateQuantity(item, -1)" class="w-8 h-8 flex items-center justify-center text-gray-500 hover:text-black transition-colors rounded-full hover:bg-white shadow-[0_0_0_1px_rgba(0,0,0,0.05)] cursor-pointer"><Minus :size="14" /></button>
                         <span class="text-sm font-semibold w-4 text-center">{{ item.quantity }}</span>
                         <button @click="updateQuantity(item, 1)" class="w-8 h-8 flex items-center justify-center text-gray-500 hover:text-black transition-colors rounded-full hover:bg-white shadow-[0_0_0_1px_rgba(0,0,0,0.05)] cursor-pointer"><Plus :size="14" /></button>
                      </div>
                      
                      <button @click="removeItem(item.id)" class="text-[11px] font-bold uppercase tracking-widest text-gray-400 hover:text-red-500 transition-colors flex items-center gap-1.5">
                         移除
                      </button>
                   </div>
                </div>
             </div>
          </div>
        </div>
        
        <!-- Order Summary -->
        <div class="lg:col-span-1">
          <div class="bg-gray-50/50 p-8 rounded-3xl space-y-8 sticky top-32">
             <h2 class="text-xl font-semibold tracking-tight">订单总计</h2>
             
             <div class="space-y-4 text-sm border-b border-gray-200 pb-6">
                <div class="flex justify-between text-gray-600">
                   <span>小计</span>
                   <span>¥{{ total.toLocaleString() }}</span>
                </div>
                <div class="flex justify-between text-gray-600">
                   <span>预估运费</span>
                   <span>免费</span>
                </div>
             </div>
             
             <div class="flex justify-between items-end">
                <span class="text-sm font-bold uppercase tracking-widest text-gray-500">总计 (CNY)</span>
                <span class="text-3xl font-light tracking-tight">¥{{ total.toLocaleString() }}</span>
             </div>
             
             <button @click="checkout" class="spa-button w-full h-14 flex items-center justify-between px-6 group">
                <span class="font-bold">结算付款</span>
                <ArrowRight :size="18" class="group-hover:translate-x-1 transition-transform" />
             </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

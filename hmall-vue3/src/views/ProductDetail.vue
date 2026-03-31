<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft, ShoppingBag, CreditCard, ChevronDown, Check } from 'lucide-vue-next'
import { useRouter, useRoute } from 'vue-router'
import { getItemById, getCarts, addCartItem, createOrder } from '@/api'
import { useUserStore } from '../store/user'
import { getMyCoupons } from '@/api/coupon'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const product = ref({
  id: route.params.id,
  name: '正在获取商品档案...',
  brand: 'HMALL',
  price: 0,
  desc: '',
  specs: [],
  colors: [],
  img: ''
})

const selectedSpec = ref('')
const selectedColor = ref('')

const quantityInCart = ref(0) // 记录该商品在购物车中的现有数量
const myCoupons = ref([])
const selectedCouponId = ref(null)
const selectedCoupon = computed(() =>
  myCoupons.value.find(c => String(c.id) === String(selectedCouponId.value)) || null
)

onMounted(async () => {
  try {
    const res = await getItemById(route.params.id)
    if (res) {
      // 解析规格信息
      let parsedSpecs = []
      let parsedColors = []
      
      try {
        const specObj = JSON.parse(res.spec || '{}')
        parsedSpecs = specObj['容量'] ? [specObj['容量']] : (specObj['内存'] ? [specObj['内存']] : ['标准配置'])
        parsedColors = specObj['颜色'] ? [specObj['颜色']] : ['默认外观']
      } catch (e) {
        parsedSpecs = [res.spec || '标准配置']
        parsedColors = ['默认外观']
      }

      product.value = {
        id: res.id,
        name: res.name,
        brand: res.brand || 'HMALL',
        price: res.price / 100,
        desc: `${res.name}。这不仅是一款产品，更是数字工艺与未来主义设计的结晶。采用行业领先的材料打造，专为追求卓越体验的专业人士而生。`,
        specs: parsedSpecs,
        colors: parsedColors,
        img: res.image,
        stock: res.stock
      }
      selectedSpec.value = product.value.specs[0]
      selectedColor.value = product.value.colors[0]
    }

    // 获取当前购物车状态，检查该商品是否已存在于购物车
    try {
      const carts = await getCarts({ silentError: true })
      const existing = carts.find(
        c => String(c.itemId) === String(product.value.id)
      )
      if (existing) quantityInCart.value = existing.num
    } catch(e) {
      console.warn("未能获取购物车实时状态，跳过预检")
    }

  } catch (e) {
    console.error('获取商品详情失败:', e)
  }

  // 获取我的优惠券（用于立即支付选择）
  if (userStore.isLoggedIn) {
    try {
      const res = await getMyCoupons({ silentError: true })
      myCoupons.value = Array.isArray(res) ? res : []
    } catch {
      myCoupons.value = []
    }
  }
})

const addToCart = async () => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  // 综合检查：当前已选 + 1 是否超过库存
  const nextQuantity = quantityInCart.value + 1
  if (product.value.stock !== undefined && nextQuantity > product.value.stock) {
     alert(`抱歉，该商品库存有限（当前库存：${product.value.stock}）。您的购物车中已有 ${quantityInCart.value} 件，无法再添加更多。`)
     return
  }

  try {
    await addCartItem({
      // 商品 id 来自后端 Long 序列化成的字符串，勿用 Number()，否则超过 JS 安全整数会精度丢失导致入库失败/500
      itemId: product.value.id,
      num: 1,
      name: product.value.name,
      price: Math.round(Number(product.value.price) * 100),
      image: product.value.img,
      category: '手机',
      brand: product.value.brand,
      spec: JSON.stringify({ 容量: selectedSpec.value, 颜色: selectedColor.value })
    })
    
    // 更新本地缓存的数量，防止连续点击绕过检查
    quantityInCart.value += 1
    
    router.push('/cart')
  } catch (e) {
    console.error('加入购物袋失败', e)
  }
}

const goPay = async () => {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const details = [{ itemId: product.value.id, num: 1 }]
    
    // 向后端 trade-service 申请创建订单
    const orderId = await createOrder({
      details,
      paymentType: 3, // 对应后端的余额支付
      addressId: 1, // 替换为数据库中真实存在的地址 ID
      couponId: selectedCouponId.value || null
    })
    
    // 携带生成的订单 ID 跳转至支付页
    router.push({ 
      path: '/pay', 
      query: { id: orderId } 
    })
  } catch (e) {
    console.error('立即结算失败', e)
  }
}
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Navbar -->
    <nav class="h-24 sticky top-0 z-50 bg-white/80 backdrop-blur-md flex items-center border-b border-gray-100">
      <div class="w-full max-w-7xl mx-auto px-6 md:px-12 flex items-center justify-between">
        <button @click="router.push('/')" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors">
          <ArrowLeft :size="20" stroke-width="1.5" />
        </button>
        <div class="flex items-center gap-3">
          <span class="text-[11px] font-bold uppercase tracking-widest text-gray-500">{{ product.brand }}</span>
        </div>
        <button @click="router.push('/cart')" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors relative">
          <ShoppingBag :size="18" stroke-width="1.5" />
          <span class="absolute top-2 right-2 w-2 h-2 bg-black rounded-full"></span>
        </button>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-6 md:px-12 py-12 md:py-24 animate-spa-reveal">
       <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 lg:gap-24 items-center">
          <!-- Product Visual -->
          <div class="bg-[#F8F9FA] rounded-[40px] p-12 flex items-center justify-center relative overflow-hidden h-[600px]">
             <img :src="product.img" class="w-full h-full object-contain relative z-10 hover:scale-105 transition-transform duration-700" alt="product">
             <div class="absolute inset-0 bg-gradient-to-tr from-gray-100/50 to-transparent pointer-events-none"></div>
          </div>
          
          <!-- Product Configuration -->
          <div class="space-y-12">
             <div class="space-y-4">
                <p class="text-[10px] font-bold tracking-widest uppercase text-gray-400">旗舰级工程设计</p>
                <h1 class="text-4xl md:text-5xl font-light tracking-tight text-gray-900">{{ product.name }}</h1>
                <p class="text-xl md:text-2xl font-semibold">¥{{ product.price.toLocaleString() }}</p>
             </div>
             
             <p class="text-gray-500 leading-relaxed max-w-lg">{{ product.desc }}</p>
             
             <!-- Specs -->
             <div class="space-y-4">
                <span class="text-[11px] font-bold uppercase tracking-widest text-gray-900 block">默认配置</span>
                <div class="flex flex-wrap gap-4">
                   <button 
                     v-for="spec in product.specs" :key="spec"
                     @click="selectedSpec = spec"
                     class="px-6 py-3 rounded-xl border text-sm font-medium transition-all"
                     :class="selectedSpec === spec ? 'border-black text-black bg-white shadow-[0_0_0_1px_black]' : 'border-gray-200 text-gray-500 hover:border-gray-300'"
                   >
                     {{ spec }}
                   </button>
                </div>
             </div>
             
             <!-- Colors -->
             <div class="space-y-4">
                <span class="text-[11px] font-bold uppercase tracking-widest text-gray-900 block">外观颜色 - {{ selectedColor }}</span>
                <div class="flex flex-wrap gap-4">
                   <button 
                     v-for="(color, idx) in product.colors" :key="color"
                     @click="selectedColor = color"
                     class="w-10 h-10 rounded-full border-2 transition-all p-0.5 flex items-center justify-center relative"
                     :class="selectedColor === color ? 'border-gray-400' : 'border-transparent hover:border-gray-200'"
                   >
                     <span class="w-full h-full rounded-full border border-black/5" 
                           :class="['bg-[#C1BEB9]', 'bg-[#2F3A4A]', 'bg-[#F2F1ED]', 'bg-[#313031]'][idx]"></span>
                   </button>
                </div>
             </div>
             
             <!-- Actions -->
             <div class="pt-6 border-t border-gray-100 flex flex-col sm:flex-row gap-4">
                <button @click="addToCart" class="h-14 flex-1 rounded-2xl border border-gray-200 bg-white text-gray-900 font-semibold flex items-center justify-center gap-3 hover:bg-gray-50 hover:border-gray-300 transition-all">
                   <ShoppingBag :size="18" stroke-width="1.5" /> 加入购物袋
                </button>
                <button @click="goPay" class="h-14 flex-1 rounded-2xl bg-black text-white font-semibold flex items-center justify-center gap-3 hover:opacity-90 transition-all shadow-xl shadow-black/10">
                   立即支付 <CreditCard :size="18" stroke-width="1.5" />
                </button>
             </div>

             <div v-if="userStore.isLoggedIn" class="pt-4">
                <p class="text-[11px] font-bold uppercase tracking-widest text-gray-400 mb-2">优惠券（可选）</p>
                <select
                  v-model="selectedCouponId"
                  class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:border-black transition-colors bg-white"
                >
                  <option :value="null">不使用优惠券</option>
                  <option v-for="c in myCoupons" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
                <p v-if="selectedCoupon" class="text-[10px] text-gray-400 mt-2">已选：{{ selectedCoupon.name }}</p>
             </div>
          </div>
       </div>
    </main>
  </div>
</template>

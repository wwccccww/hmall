<script setup>
import { ref, computed, onMounted } from 'vue'
import { ShoppingBag, ChevronLeft, Trash2, ArrowRight, Minus, Plus, MapPin, CheckCircle2, Circle } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()

const cartItems = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)

onMounted(async () => {
  // 获取购物车
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
        img: i.image,
        selected: true // 默认选中
      }))
    }
  } catch(e) {
    console.warn("使用本地测试数据，因为后端购物车请求失败")
    cartItems.value = [
      { id: 101, name: 'iPhone 15 Pro (测试数据)', spec: '256GB, 原色钛金属', price: 8999, quantity: 1, stock: 100, img: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=400', selected: true },
      { id: 102, name: 'AirPods Max (测试数据)', spec: '银色', price: 3999, quantity: 1, stock: 100, img: 'https://images.unsplash.com/photo-1613040809024-b4ef7ba99bc3?w=400', selected: true }
    ]
  }

  // 获取地址
  try {
    const addrRes = await request.get('/addresses')
    addresses.value = addrRes
    const defaultAddr = addrRes.find(a => a.isDefault)
    if (defaultAddr) selectedAddressId.value = defaultAddr.id
    else if (addrRes.length > 0) selectedAddressId.value = addrRes[0].id
  } catch(e) {
    console.warn("地址获取失败", e)
    addresses.value = [{ id: 1, contact: '测试用户', mobile: '13800000000', province: '广东省', city: '深圳市', town: '南山区', street: '腾讯大厦', isDefault: true }]
    selectedAddressId.value = 1
  }
})

const total = computed(() => {
  return cartItems.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const isAllSelected = computed(() => {
  return cartItems.value.length > 0 && cartItems.value.every(i => i.selected)
})

const toggleSelectAll = () => {
  const target = !isAllSelected.value
  cartItems.value.forEach(i => i.selected = target)
}

const updateQuantity = async (item, delta) => {
  const newQ = item.quantity + delta
  
  if (newQ < 1) {
    alert("商品数量不能少于 1 件")
    return
  }
  
  // 检查是否超出库存
  if (item.stock !== undefined && newQ > item.stock) {
    alert(`抱歉，该商品库存仅剩 ${item.stock} 件`)
    return
  }

  try {
    await request.put('/carts', { id: item.id, num: newQ })
    item.quantity = newQ
  } catch(e) {
    console.error("更新数量失败", e)
    // 即使后端失败，我们也更新本地状态以保证交互流畅，或根据业务决定回滚
    item.quantity = newQ
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
  const selectedItems = cartItems.value.filter(i => i.selected)
  if(selectedItems.length === 0) return alert('请选择要结算的商品')
  if(!selectedAddressId.value) return alert('请选择收货地址')
  
  try {
    const details = selectedItems.map(i => ({ itemId: i.itemId || i.id, num: i.quantity }))
    
    const orderId = await request.post('/orders', {
      details,
      paymentType: 3, 
      addressId: selectedAddressId.value
    })
    console.log("收到的订单id是: " , orderId)
    router.push({ path: '/pay', query: { id: orderId } })
  } catch(e) {
    console.error("生成订单失败", e)
    alert("结算失败，请检查网络或库存")
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
    <main class="max-w-5xl mx-auto px-6 md:px-12 py-12 animate-spa-reveal">
      <div v-if="cartItems.length === 0" class="text-center py-32 space-y-6">
        <p class="text-gray-400 text-lg">您的购物袋是空的。</p>
        <button @click="router.push('/')" class="spa-button">继续选购</button>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-16">
        <!-- Cart Content -->
        <div class="lg:col-span-2 space-y-12">
          
          <!-- Selection Controls -->
          <div class="flex items-center justify-between pb-6 border-b border-gray-100">
             <button @click="toggleSelectAll" class="flex items-center gap-3 text-sm font-semibold hover:opacity-70 transition-opacity">
                <CheckCircle2 v-if="isAllSelected" :size="20" class="text-black" />
                <Circle v-else :size="20" class="text-gray-300" />
                全选
             </button>
             <span class="text-[11px] font-bold uppercase tracking-widest text-gray-400">
                已选 {{ cartItems.filter(i => i.selected).length }} 件
             </span>
          </div>

          <!-- Items List -->
          <div class="space-y-10">
             <div v-for="item in cartItems" :key="item.id" class="flex gap-6 group relative">
                <!-- Checkbox -->
                <button @click="item.selected = !item.selected" class="mt-12">
                   <CheckCircle2 v-if="item.selected" :size="20" class="text-black" />
                   <Circle v-else :size="20" class="text-gray-300" />
                </button>

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
                      
                      <button @click="removeItem(item.id)" class="text-[11px] font-bold uppercase tracking-widest text-gray-400 hover:text-red-500 transition-colors">
                         移除
                      </button>
                   </div>
                </div>
             </div>
          </div>

          <!-- Address Selection -->
          <div class="pt-12 border-t border-gray-100">
             <div class="flex items-center gap-2 mb-6">
                <MapPin :size="16" />
                <h2 class="text-sm font-bold uppercase tracking-widest">收货地址</h2>
             </div>
             
             <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div 
                  v-for="addr in addresses" :key="addr.id"
                  @click="selectedAddressId = addr.id"
                  class="p-5 rounded-2xl border transition-all cursor-pointer relative overflow-hidden"
                  :class="selectedAddressId === addr.id ? 'border-black bg-white shadow-lg' : 'border-gray-100 bg-gray-50/50 hover:bg-gray-50'"
                >
                   <div class="flex justify-between items-start mb-2">
                      <span class="text-sm font-bold">{{ addr.contact }}</span>
                      <span v-if="addr.isDefault" class="text-[9px] px-2 py-0.5 bg-black text-white rounded-full font-bold uppercase tracking-tighter">Default</span>
                   </div>
                   <p class="text-[12px] text-gray-500 leading-relaxed">
                      {{ addr.province }} {{ addr.city }} {{ addr.town }}<br>
                      {{ addr.street }}
                   </p>
                   <p class="text-[12px] font-medium mt-2">{{ addr.mobile }}</p>
                </div>
             </div>
          </div>
        </div>
        
        <!-- Order Summary -->
        <div class="lg:col-span-1">
          <div class="bg-gray-50/50 p-8 rounded-3xl space-y-8 sticky top-32 border border-gray-100">
             <h2 class="text-xl font-semibold tracking-tight">订单总计</h2>
             
             <div class="space-y-4 text-sm border-b border-gray-200 pb-6">
                <div class="flex justify-between text-gray-600">
                   <span>已选小计</span>
                   <span>¥{{ total.toLocaleString() }}</span>
                </div>
                <div class="flex justify-between text-gray-600">
                   <span>物流体验</span>
                   <span>尊享免邮</span>
                </div>
             </div>
             
             <div class="flex justify-between items-end">
                <span class="text-sm font-bold uppercase tracking-widest text-gray-500">总计 (CNY)</span>
                <span class="text-3xl font-light tracking-tight">¥{{ total.toLocaleString() }}</span>
             </div>
             
             <button @click="checkout" class="spa-button w-full h-14 flex items-center justify-between px-6 group shadow-xl">
                <span class="font-bold">前往结算</span>
                <ArrowRight :size="18" class="group-hover:translate-x-1 transition-transform" />
             </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

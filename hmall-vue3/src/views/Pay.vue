<script setup>
import { ref, onMounted } from 'vue'
import { Check, ArrowRight, ShieldCheck, QrCode } from 'lucide-vue-next'
import { useRouter, useRoute } from 'vue-router'
import { getOrderById, createPayOrder, payOrder, getCurrentUser } from '@/api'

const router = useRouter()
const route = useRoute()
const isPaid = ref(false)
const isPaying = ref(false)

const amount = ref('----')
const orderId = ref(route.query.id)
const payOrderNo = ref('')

onMounted(async () => {
  if (orderId.value) {
    try {
      const res = await getOrderById(orderId.value)
      console.log("获取到的订单详细信息：", res)
      console.log("订单ID：", orderId.value)
      // 关键修复：兼容数组包装情况
      const order = Array.isArray(res) ? res[0] : res
      if (order) {
        // 兼容蛇形命名和驼峰命名
        const finalFee = order.totalFee || order.total_fee || 0;
        const finalOrderId = order.id || order.id_ || orderId.value;
        
        if (finalFee) {
          amount.value = (finalFee / 100).toLocaleString('en-US', { minimumFractionDigits: 2 })
        }
        
        const pno = await createPayOrder({
          bizOrderNo: finalOrderId,
          amount: finalFee,
          payType: 5,
          orderInfo: '黑马商城商品',
          payChannelCode: 'balance'
        })
        payOrderNo.value = pno
      }
    } catch (e) {
      console.warn('数据加载失败', e)
    }
  }
})

const handlePay = async () => {
  if (isPaying.value) return
  isPaying.value = true
  
  try {
    console.log("PayOrderNo支付: ", payOrderNo.value)
    if (payOrderNo.value) {
      await payOrder(payOrderNo.value, {
        id: payOrderNo.value,
        pw: '123' // 测试密码
      })
      
      // 支付成功后，立即拉取最新的用户信息（包括余额）并同步到缓存
      try {
        const user = await getCurrentUser()
        if (user) {
          sessionStorage.setItem("user-info", JSON.stringify(user))
          // 同时也发送一个全局事件或简单刷新缓存，让 Navbar 感知到
          window.dispatchEvent(new Event('storage')) 
        }
      } catch (userErr) {
        console.warn("余额自动同步失败，请手动刷新", userErr)
      }

      isPaying.value = false
      isPaid.value = true
    } else {
      alert("支付单初始化失败，请刷新页面重试")
      isPaying.value = false
    }
  } catch (e) {
    console.error('支付请求详细错误：', e)
    isPaying.value = false
  }
}

const backToHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen bg-[#f8f9fa] flex flex-col pt-24 px-6 items-center">
    
    <div v-show="!isPaid" class="w-full max-w-md bg-white border border-gray-100 rounded-[32px] p-10 shadow-xl shadow-black-[0.02] animate-spa-reveal">
       <div class="text-center mb-10">
          <p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-2">安全收银台</p>
          <h2 class="text-3xl font-light tracking-tight">确认付款</h2>
          <div class="mt-6 flex items-baseline justify-center gap-2">
             <span class="text-gray-400 font-medium">¥</span>
             <span class="text-5xl font-light tracking-tight text-gray-900">{{ amount }}</span>
          </div>
       </div>
       
       <div class="space-y-4 mb-10">
          <div class="flex items-center justify-between p-4 border border-black/5 rounded-2xl bg-gray-50/50">
             <div class="flex items-center gap-3">
                <QrCode :size="20" class="text-gray-400" />
                <span class="text-sm font-semibold">扫码支付</span>
             </div>
             <div class="w-4 h-4 rounded-full border-[4px] border-black bg-white"></div>
          </div>
       </div>
       
       <button 
         @click="handlePay"
         :disabled="isPaying"
         class="w-full h-14 bg-black text-white rounded-2xl font-semibold shadow-lg shadow-black/10 flex items-center justify-center gap-2 hover:opacity-90 active:scale-95 transition-all"
       >
         <span v-if="isPaying" class="flex items-center gap-2">
           <span class="w-1.5 h-1.5 bg-white/40 rounded-full animate-pulse"></span>
           <span class="w-1.5 h-1.5 bg-white/70 rounded-full animate-pulse [animation-delay:0.2s]"></span>
           <span class="w-1.5 h-1.5 bg-white rounded-full animate-pulse [animation-delay:0.4s]"></span>
         </span>
         <span v-else class="flex items-center gap-2">
           立即支付 <ArrowRight :size="16" />
         </span>
       </button>
       
       <div class="mt-8 flex justify-center items-center gap-2 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
          <ShieldCheck :size="14" /> 端到端加密交易
       </div>
    </div>
    
    <!-- Success State -->
    <div v-show="isPaid" class="w-full max-w-md bg-white border border-gray-100 rounded-[32px] p-12 text-center shadow-xl shadow-black-[0.02] animate-spa-reveal">
       <div class="w-20 h-20 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-8 text-green-600 transition-all duration-700 ease-[cubic-bezier(0.19,1,0.22,1)] scale-in">
          <Check :size="32" stroke-width="3" />
       </div>
       <h2 class="text-3xl font-light tracking-tight mb-4">支付成功</h2>
       <p class="text-gray-500 mb-10 text-sm leading-relaxed">您的订单已通过安全网关结清。我们将在 2 个完整周期内启动极速履约发货流程。</p>
       
       <button @click="backToHome" class="w-full h-12 border border-gray-200 rounded-xl font-semibold text-sm hover:bg-gray-50 transition-colors">
          返回
       </button>
    </div>

  </div>
</template>

<style scoped>
.scale-in {
  animation: pop 0.6s cubic-bezier(0.19, 1, 0.22, 1) forwards;
}
@keyframes pop {
  0% { transform: scale(0.5); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
</style>

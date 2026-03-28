<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSearchStore } from '../store/search'
import { storeToRefs } from 'pinia'
import request from '../utils/request'
import { 
  ShoppingBag, 
  Search, 
  User, 
  Heart, 
  ArrowRight,
  Zap,
  ShieldCheck,
  Globe,
  Star,
  Play,
  ArrowUpRight,
  Menu,
  X
} from 'lucide-vue-next'
import SearchOverlay from '../components/SearchOverlay.vue'

const categories = ['All', 'iPhone', 'Audio', 'iPad', 'Mac', 'Watch']
const activeCategory = ref('All')
const isMenuOpen = ref(false)

const searchStore = useSearchStore()
const { showSearch } = storeToRefs(searchStore)

const featured = ref([])
const fallbackFeatured = [
  { id: 101, name: 'iPhone 15 Pro', price: 999, img: 'https://images.unsplash.com/photo-1696446701796-da61225697cc?w=800&auto=format&fit=crop', badge: 'Titanium' },
  { id: 102, name: 'AirPods Max', price: 549, img: 'https://images.unsplash.com/photo-1613040809024-b4ef7ba99bc3?w=800&auto=format&fit=crop', badge: 'High-Fidelity' },
  { id: 103, name: 'MacBook Air M3', price: 1099, img: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&auto=format&fit=crop', badge: 'Power' }
]

const scrollY = ref(0)
const handleScroll = () => { scrollY.value = window.scrollY }
const userInfo = ref(null)

const router = useRouter()

const logout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user-info')
  userInfo.value = null
  router.push('/login')
}

const loadItems = async () => {
  try {
    const res = await request.get('/items/page', {
      params: { 
        pageNo: 1, 
        pageSize: 12,
        category: activeCategory.value === 'All' ? null : activeCategory.value
      }
    })
    if (res && res.list && res.list.length > 0) {
      featured.value = res.list.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price / 100,
        img: item.image,
        badge: item.category
      }))
      console.log("从后端获取到的商品信息：", featured.value)
    } else {
      featured.value = fallbackFeatured
    }
  } catch (e) {
    console.error("Failed to load products, using fallback", e)
    featured.value = fallbackFeatured
  }
}

watch(activeCategory, () => {
  loadItems()
})

onMounted(() => { 
  window.addEventListener('scroll', handleScroll)
  const info = sessionStorage.getItem('user-info')
  if (info) {
    try {
      userInfo.value = JSON.parse(info)
    } catch(e) {}
  }
  loadItems()
})
onUnmounted(() => { window.removeEventListener('scroll', handleScroll) })
</script>

<template>
  <div class="min-h-screen bg-white selection:bg-black selection:text-white">
    
    <!-- Precision Navigation -->
    <nav 
      class="fixed top-0 left-0 w-full z-[100] transition-all duration-500 border-b"
      :class="scrollY > 20 ? 'bg-white/80 backdrop-blur-xl border-gray-100 py-4' : 'bg-transparent border-transparent py-6'"
    >
      <div class="max-w-7xl mx-auto px-6 md:px-12 flex items-center justify-between">
        <a href="/" class="flex items-center gap-3">
          <div class="w-8 h-8 bg-black rounded-lg flex items-center justify-center">
            <ShoppingBag :size="18" class="text-white" stroke-width="2" />
          </div>
          <span class="text-lg font-bold tracking-tight uppercase">HMALL</span>
        </a>

        <!-- Desktop Menu -->
        <div class="hidden md:flex items-center gap-10">
          <a v-for="link in ['单品专区', '技术探索', '关于我们']" :key="link" href="#" class="text-[11px] font-semibold uppercase tracking-widest text-gray-500 hover:text-black transition-colors">{{ link }}</a>
        </div>

        <div class="flex items-center gap-4">
          <button @click="$router.push('/cart')" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors relative">
            <ShoppingBag :size="18" stroke-width="1.5" />
            <span class="absolute top-2 right-2 w-2 h-2 bg-black rounded-full"></span>
          </button>
          <button 
            @click="searchStore.toggleSearch(true)"
            class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-gray-50 transition-colors"
          >
            <Search :size="18" stroke-width="1.5" />
          </button>
          <template v-if="userInfo">
            <div class="hidden sm:flex items-center gap-3">
              <div class="w-9 h-9 bg-gray-100 rounded-full flex items-center justify-center text-black font-bold text-xs border border-gray-200">
                {{ userInfo.username ? userInfo.username.charAt(0).toUpperCase() : 'U' }}
              </div>
              <div class="flex flex-col">
                <span class="text-[11px] font-bold uppercase tracking-widest text-gray-900 leading-tight">{{ userInfo.username }}</span>
                <span v-if="userInfo.balance !== undefined" class="text-[10px] text-gray-500 font-medium">余额: ¥{{ userInfo.balance / 100 }}</span>
              </div>
              <button @click="logout" class="text-[10px] ml-2 font-bold uppercase tracking-widest text-gray-400 hover:text-red-500 transition-colors">退出</button>
            </div>
          </template>
          <template v-else>
            <router-link to="/login" class="hidden sm:flex items-center gap-2 text-[11px] font-bold uppercase tracking-widest px-6 py-2.5 bg-black text-white rounded-full hover:opacity-80 transition-all">
              进入系统 <ArrowRight :size="14" />
            </router-link>
          </template>
          <button @click="isMenuOpen = !isMenuOpen" class="md:hidden w-10 h-10 flex items-center justify-center">
            <Menu v-if="!isMenuOpen" :size="20" />
            <X v-else :size="20" />
          </button>
        </div>
      </div>
    </nav>

    <main>

      <!-- Category Filter & Grid -->
      <section class="max-w-7xl mx-auto px-6 md:px-12 py-32">
        <div class="flex flex-col md:flex-row md:items-end justify-between gap-8 mb-20 animate-spa-reveal">
          <div class="space-y-4">
            <h2 class="text-4xl md:text-5xl font-light tracking-tight">商品分类</h2>
          </div>
          <div class="flex flex-wrap gap-2 overflow-x-auto pb-4 md:pb-0 scrollbar-hide">
            <button 
              v-for="cat in categories" 
              :key="cat" 
              @click="activeCategory = cat" 
              class="px-6 py-2 rounded-full text-[11px] font-bold uppercase tracking-widest transition-all whitespace-nowrap"
              :class="activeCategory === cat ? 'bg-black text-white' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'"
            >
              {{ cat }}
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-10">
          <div v-for="item in featured" :key="item.id" class="spa-card group overflow-hidden bg-[#F9FAFB] border-none flex flex-col items-center justify-center p-12 text-center animate-spa-reveal">
            <img :src="item.img" class="w-full max-w-[240px] h-[300px] object-contain group-hover:scale-105 transition-transform duration-700 mb-10">
            <div class="space-y-4">
              <span class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{{ item.badge }}</span>
              <h3 class="text-2xl font-semibold tracking-tight">{{ item.name }}</h3>
              <p class="text-gray-400 text-lg">¥{{ item.price }}</p>
              <router-link :to="'/product/' + item.id" class="inline-flex items-center gap-2 mt-8 text-[11px] font-bold uppercase tracking-[0.2em] text-gray-400 group-hover:text-black transition-all">
                探索详情 <ArrowRight :size="14" stroke-width="2.5" class="opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-500" />
              </router-link>
            </div>
          </div>
        </div>
      </section>

      <!-- Values: Precision & Purity -->
      <section class="border-y border-gray-100 bg-white py-40">
        <div class="max-w-7xl mx-auto px-6 md:px-12 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-16">
          <div v-for="(val, idx) in [
            { icon: Globe, title: '全球极速履约', desc: '采用精准定位跟踪技术，确保商品无暇送达全球任何一个节点。' },
            { icon: Zap, title: '闪电出库服务', desc: '重新定义高效体系，您的订单将在极短的流转周期内处理并发货。' },
            { icon: ShieldCheck, title: '金融级安全', desc: '为您提供不妥协的隐私保护与端到端的数据传输安全加密。' },
            { icon: Star, title: '尊贵会员特权', desc: '解锁高级限量专享单品及全天候一对一的专属管家支持。' }
          ]" :key="idx" class="space-y-6 animate-spa-reveal">
            <div class="w-12 h-12 rounded-xl bg-gray-50 flex items-center justify-center text-black">
              <component :is="val.icon" :size="24" stroke-width="1.5" />
            </div>
            <h4 class="text-lg font-bold tracking-tight uppercase">{{ val.title }}</h4>
            <p class="body-refined text-sm leading-relaxed">{{ val.desc }}</p>
          </div>
        </div>
      </section>
    </main>

    <!-- Footer: Clean Symmetry -->
    <footer class="bg-gray-50 pt-32 pb-20 px-6 md:px-12">
      <div class="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-20">
        <div class="col-span-1 md:col-span-1 space-y-8">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 bg-black rounded-lg flex items-center justify-center">
              <ShoppingBag :size="18" class="text-white" stroke-width="2" />
            </div>
            <span class="text-lg font-bold tracking-tight uppercase">HMALL</span>
          </div>
          <p class="body-refined text-sm leading-relaxed">
            重塑现代数字奢华的标准。自 2024 年起，致力于为具备前瞻视野的思想者打造不凡体验。
          </p>
        </div>
        
        <div v-for="menu in [
          { title: '探索发现', links: ['在线商店', '技术革新', '全系产品'] },
          { title: '集团信息', links: ['品牌故事', '服务条款', '联系我们'] }
        ]" :key="menu.title" class="space-y-8">
          <h6 class="label-refined text-black">{{ menu.title }}</h6>
          <ul class="space-y-4">
            <li v-for="link in menu.links" :key="link">
              <a href="#" class="text-sm text-gray-400 hover:text-black transition-colors">{{ link }}</a>
            </li>
          </ul>
        </div>

        <div class="space-y-8">
          <h6 class="label-refined text-black">订阅通讯</h6>
          <p class="text-xs text-gray-400 font-medium">获取早期新品阅览与直达终端的独家内部资讯。</p>
          <div class="flex flex-col gap-3">
            <input type="email" placeholder="邮箱地址" class="spa-input bg-white border-gray-100">
            <button class="spa-button w-full">确认订阅</button>
          </div>
        </div>
      </div>
      
      <div class="mt-32 pt-10 border-t border-gray-100 flex flex-col md:flex-row items-center justify-between gap-6">
        <div class="flex items-center gap-8">
          <span class="text-[10px] font-bold text-gray-300 uppercase tracking-widest">© 2024 HMALL 数字网络</span>
          <a href="#" class="text-[10px] font-bold text-gray-300 hover:text-black uppercase tracking-widest">隐私权政策</a>
        </div>
        <div class="flex gap-4">
           <div v-for="i in 3" :key="i" class="w-8 h-8 rounded-full border border-gray-100 flex items-center justify-center text-gray-300 hover:text-black transition-colors cursor-pointer">
              <Globe :size="14" />
           </div>
        </div>
      </div>
    </footer>

    <SearchOverlay />
  </div>
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


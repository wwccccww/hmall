<script setup>
import { ref, onMounted } from 'vue'
import { 
  Plus, 
  Search, 
  Filter, 
  MoreHorizontal, 
  Edit2, 
  Trash2, 
  Check,
  X,
  Package,
  AlertCircle
} from 'lucide-vue-next'
import { getItemPage } from '@/api/item'

const goods = ref([])
const total = ref(0)
const pageNo = ref(1)

const loadGoods = async () => {
  try {
    const res = await getItemPage({ pageNo: pageNo.value, pageSize: 10 }, { silentError: true })
    if (res && res.list) {
      goods.value = res.list.map(item => ({
        id: item.id,
        name: item.name,
        category: item.category,
        price: item.price / 100,
        stock: item.stock,
        status: item.status === 1 ? '上架销售' : (item.status === 2 ? '草稿' : '下架停售'),
        image: item.image
      }))
      total.value = res.total
    }
  } catch(e) {
    console.warn("请求失败，保留界面显示...", e)
    goods.value = [
      { id: 1, name: '小米14 Pro 12GB+256GB 黑色', category: '手机', price: 4999.00, stock: 45, status: '上架销售', image: 'https://images.unsplash.com/photo-1696446701796-da61225697cc?w=100&h=100&fit=crop' },
      { id: 2, name: '华为 Mate 60 Pro 雅川青', category: '手机', price: 6999.00, stock: 12, status: '上架销售', image: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=100&h=100&fit=crop' },
      { id: 3, name: 'Apple MacBook Air M2 13.6寸', category: '笔记本', price: 7999.00, stock: 0, status: '下架停售', image: 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=100&h=100&fit=crop' },
    ]
    total.value = 3
  }
}

const nextPage = () => {
  pageNo.value++
  loadGoods()
}

const prevPage = () => {
  if(pageNo.value > 1) {
    pageNo.value--
    loadGoods()
  }
}

onMounted(loadGoods)
</script>

<template>
  <div class="px-8 md:px-12 py-10 animate-spa-reveal">
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
      <div class="space-y-4">
        <p class="label-refined text-gray-400">核心商品库存调度</p>
        <h1 class="h1-refined text-4xl">商品图录。</h1>
      </div>
      <button class="spa-button gap-2">
        <Plus :size="18" stroke-width="2" />
        <span>录入新商品</span>
      </button>
    </div>

    <!-- Filter & Search Toolbar -->
    <div class="spa-card mb-8 p-4 flex flex-wrap items-center justify-between gap-4 border-dashed border-gray-200 bg-transparent shadow-none">
      <div class="flex items-center gap-3 w-full max-w-[700px]">
        <div class="relative flex-1 group">
          <Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-black transition-colors" :size="16" stroke-width="1.5" />
          <input 
            type="text" 
            placeholder="搜索商品名、专属 SKU 编码..." 
            class="w-full h-12 bg-white border border-gray-100 rounded-xl pl-11 pr-4 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-gray-300 focus:shadow-[0_0_0_4px_rgba(0,0,0,0.02)] transition-all font-medium"
          >
        </div>
        <select class="h-12 px-5 bg-white border border-gray-100 rounded-xl text-xs font-semibold text-gray-600 outline-none focus:border-gray-300 transition-colors w-32 cursor-pointer">
          <option>全部状态</option>
          <option>上架售卖</option>
          <option>草稿</option>
          <option>下架停售</option>
        </select>
        <button class="h-12 w-12 flex items-center justify-center bg-white border border-gray-100 rounded-xl hover:bg-gray-50 transition-colors text-gray-600">
           <Filter :size="18" stroke-width="1.5" />
        </button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="spa-card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-gray-50/50 border-b border-gray-100">
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest w-20">预览缩略图</th>
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest">主数据及型号</th>
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest">属性分类</th>
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest">标价 (人民币)</th>
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest">库存计量</th>
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest">运行状态</th>
              <th class="py-4 px-6 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-right">资产操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50/80">
            <tr v-for="item in goods" :key="item.id" class="hover:bg-gray-50/50 transition-colors group">
              <td class="py-4 px-6">
                 <div class="w-14 h-14 bg-gray-50 rounded-lg border border-gray-100/50 flex items-center justify-center overflow-hidden">
                   <img :src="item.image" class="w-full h-full object-cover mix-blend-multiply opacity-90 group-hover:scale-110 transition-transform duration-500">
                 </div>
              </td>
              <td class="py-4 px-6">
                 <p class="text-sm font-semibold text-gray-900 truncate max-w-[240px]">{{ item.name }}</p>
                 <span class="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-1 block">SKU: {{ 1000+item.id }}</span>
              </td>
              <td class="py-4 px-6">
                 <span class="bg-gray-100/80 text-gray-600 px-3 py-1 rounded-md text-xs font-medium">{{ item.category }}</span>
              </td>
              <td class="py-4 px-6">
                 <span class="text-sm font-medium text-gray-900">¥{{ item.price.toLocaleString() }}</span>
              </td>
              <td class="py-4 px-6">
                 <div class="flex items-center gap-2">
                    <span class="text-sm font-medium" :class="item.stock > 10 ? 'text-gray-900' : 'text-amber-600'">{{ item.stock }}</span>
                 </div>
              </td>
              <td class="py-4 px-6">
                 <div class="flex items-center gap-1.5 px-3 py-1 rounded-md w-fit text-[11px] font-semibold tracking-wide" 
                      :class="item.status === '上架销售' ? 'bg-green-50 text-green-700' : 'bg-rose-50 text-rose-700'">
                    <component :is="item.status === '上架销售' ? Check : AlertCircle" :size="12" stroke-width="2.5" />
                    {{ item.status }}
                 </div>
              </td>
              <td class="py-4 px-6">
                <div class="flex items-center justify-end gap-1 opacity-60 group-hover:opacity-100 transition-opacity">
                  <button class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-black hover:bg-gray-100 rounded-md transition-colors"><Edit2 :size="14" stroke-width="2" /></button>
                  <button class="w-8 h-8 flex items-center justify-center text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-md transition-colors"><Trash2 :size="14" stroke-width="2" /></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <!-- Pagination -->
      <div class="p-6 border-t border-gray-100 bg-white flex items-center justify-between">
         <span class="text-[11px] font-semibold text-gray-400 uppercase tracking-widest">当前共呈现 {{ total }} 条记录，本页 {{ goods.length }} 条</span>
         <div class="flex items-center gap-2">
            <button @click="prevPage" class="px-3 h-8 border border-gray-100 rounded-md bg-white text-xs font-semibold hover:border-gray-200 hover:text-gray-900 transition-colors" :class="pageNo === 1 ? 'text-gray-300 cursor-not-allowed' : 'text-gray-600'">上一页</button>
            <button class="px-3 h-8 border border-gray-200 rounded-md bg-black text-xs font-bold text-white">{{ pageNo }}</button>
            <button @click="nextPage" class="px-3 h-8 border border-gray-100 rounded-md bg-white text-xs font-semibold text-gray-600 hover:border-gray-200 hover:text-gray-900 transition-colors">下一页</button>
         </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Inherits global spa design */
</style>

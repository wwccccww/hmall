import { defineStore } from 'pinia'
import { getSearchList } from '@/api/search'

export const useSearchStore = defineStore('search', {
  state: () => ({
    searchQuery: '',
    results: [],
    pageNo: 1,
    totalItems: 0,
    totalPages: 0,
    showSearch: false,
    pageSize: 8,
    isLoading: false
  }),
  actions: {
    async fetchResults(page = this.pageNo) {
      if (!this.searchQuery.trim()) {
        this.results = []
        this.totalPages = 0
        this.totalItems = 0
        return
      }
      
      this.isLoading = true
      this.pageNo = page
      try {
        const res = await getSearchList(
          {
            key: this.searchQuery,
            pageNo: this.pageNo,
            pageSize: this.pageSize
          },
          { silentError: true }
        )
        this.results = res.list.map(item => ({
          id: item.id,
          name: item.name,
          price: item.price / 100,
          img: item.image,
          category: item.category
        }))
        this.totalPages = res.pages
        this.totalItems = res.total
      } catch (e) {
        console.error('Search failed', e)
      } finally {
        this.isLoading = false
      }
    },
    setSearchQuery(query) {
      this.searchQuery = query
      this.pageNo = 1
      this.fetchResults(1)
    },
    setPage(page) {
      if (page < 1 || page > this.totalPages) return
      this.pageNo = page
      this.fetchResults(page)
    },
    toggleSearch(show) {
      this.showSearch = show
      if (!show) {
        // Optional: clear on close? Let's keep it to satisfy "stay on page"
      }
    }
  }
})

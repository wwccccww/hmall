import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(sessionStorage.getItem('user-info') || 'null'),
    token: sessionStorage.getItem('token') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 1
  },
  actions: {
    setUserInfo(user, token) {
      this.userInfo = user
      this.token = token
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('user-info', JSON.stringify(user))
    },
    clearUserInfo() {
      this.userInfo = null
      this.token = ''
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user-info')
    },
    async logout() {
      this.clearUserInfo()
    }
  }
})

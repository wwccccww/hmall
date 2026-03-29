import { defineStore } from 'pinia'

/** 未登录时展示用（不含 balance，避免顶栏误显示「余额 ¥0」） */
export const GUEST_USER_DISPLAY = Object.freeze({
  username: '未登录'
})

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

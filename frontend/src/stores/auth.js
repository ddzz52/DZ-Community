import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null,
    inited: false
  }),
  actions: {
    initFromStorage() {
      this.token = localStorage.getItem('token') || ''
      const userStr = localStorage.getItem('user')
      this.user = userStr ? JSON.parse(userStr) : null
      this.inited = true
    },
    persist() {
      localStorage.setItem('token', this.token || '')
      localStorage.setItem('user', this.user ? JSON.stringify(this.user) : '')
    },
    async login(username, password) {
      const data = await http.post('/api/auth/login', { username, password })
      this.token = data.token
      this.user = data.user
      this.persist()
    },
    async register(username, password, nickname) {
      const user = await http.post('/api/auth/register', { username, password, nickname })
      return user
    },
    async registerV2(payload) {
      const user = await http.post('/api/auth/register', payload)
      return user
    },
    async fetchMe() {
      const user = await http.get('/api/users/me')
      this.user = user
      this.persist()
      return user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})

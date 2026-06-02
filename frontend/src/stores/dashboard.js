import { defineStore } from 'pinia'
import http from '../api/http'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    data: null,
    loading: false
  }),
  getters: {
    badges(state) {
      return state.data?.badges || { unreadMessages: 0, unreadNotifications: 0, reminders: 0 }
    }
  },
  actions: {
    async fetch() {
      this.loading = true
      try {
        this.data = await http.get('/api/dashboard')
        return this.data
      } finally {
        this.loading = false
      }
    }
  }
})


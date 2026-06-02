import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body.code !== 'undefined') {
      if (body.code === 0) return body.data
      if (body.code === 40100) {
        const auth = useAuthStore()
        auth.logout()
        if (location.pathname !== '/login') {
          location.href = '/login'
        }
      }
      const err = new Error(body.message || '请求失败')
      err.code = body.code
      throw err
    }
    return body
  },
  (error) => {
    if (error?.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }
    throw error
  }
)

export default http

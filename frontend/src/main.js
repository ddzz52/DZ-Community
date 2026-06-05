import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'

try {
  const t = localStorage.getItem('dz_theme')
  if (t === 'dark') document.documentElement.dataset.theme = 'dark'
} catch (e) {}

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')

// ====== PWA Service Worker（仅生产环境注册） ======
// 删除此行即可回退 PWA 功能
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  navigator.serviceWorker.register('/sw.js').catch(() => {})
}

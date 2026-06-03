import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/app/home' },
  { path: '/home', redirect: '/app/home' },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  {
    path: '/app',
    component: () => import('../views/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'home', component: () => import('../views/Home.vue') },
      { path: 'chat', component: () => import('../modules/chat/ChatPage.vue') },
      { path: 'agent', name: 'Agent', component: () => import('../modules/agent/AgentChat.vue') },
      { path: 'records', component: () => import('../views/Records.vue') },
      { path: 'timeline', component: () => import('../views/Timeline.vue') },
      { path: 'memos', component: () => import('../views/Memos.vue') },
      { path: 'albums', component: () => import('../views/Albums.vue') },
      { path: 'notifications', component: () => import('../views/Notifications.vue') },
      { path: 'partner', component: () => import('../views/Partner.vue') },
      { path: 'me', component: () => import('../views/Me.vue') },
      { path: 'admin', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/app/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.inited) {
    auth.initFromStorage()
  }
  const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)
  if (requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 管理员页面权限检查
  const requiresAdmin = to.matched.some((r) => r.meta.requiresAdmin)
  if (requiresAdmin && auth.user?.role !== 'ADMIN') {
    return { path: '/app/me' }
  }
  if ((to.path === '/login' || to.path === '/register') && auth.token) {
    return { path: '/app/home' }
  }
})

export default router

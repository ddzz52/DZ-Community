<script setup>
import { computed, h, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, House, MagicStick, Picture, User, Plus, EditPen, Money, Notebook, Calendar, ArrowUp } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useDashboardStore } from '../stores/dashboard'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const dashboard = useDashboardStore()
const auth = useAuthStore()

// ====== 底部导航（5 Tab） ======
const tabs = [
  { path: '/app/home', label: '日常', icon: House },
  { path: '/app/chat', label: '聊天', icon: ChatDotRound },
  { path: '/app/agent', label: 'AI管家', icon: MagicStick },
  { path: '/app/albums', label: '相册', icon: Picture },
  { path: '/app/me', label: '我的', icon: User }
]

const activePath = computed(() => {
  const p = route.path
  const hit = tabs.find((t) => p === t.path || p.startsWith(t.path + '/'))
  return hit ? hit.path : '/app/home'
})

const go = (path) => {
  if (path !== activePath.value) router.push(path)
}

// ====== 浮动按钮（FAB） ======
const fabOpen = ref(false)
const showScrollTop = ref(false)

const fabItems = [
  { key: 'diary', label: '写日记', icon: EditPen, route: '/app/records?tab=diaries' },
  { key: 'account', label: '记账', icon: Money, route: '/app/records?tab=accounts' },
  { key: 'memo', label: '备忘', icon: Notebook, route: '/app/memos' },
  { key: 'anniversary', label: '纪念日', icon: Calendar, route: '/app/records?tab=ann' }
]

let fabCloseTimer = 0
const toggleFab = () => { fabOpen.value = !fabOpen.value }
const closeFab = () => {
  if (fabCloseTimer) clearTimeout(fabCloseTimer)
  fabCloseTimer = window.setTimeout(() => { fabOpen.value = false }, 200)
}
const fabAction = (item) => {
  fabOpen.value = false
  router.push(item.route)
}

const onPageScroll = () => {
  const el = document.querySelector('.layout-content')
  if (!el) return
  showScrollTop.value = el.scrollTop > 400
}
const scrollToTop = () => {
  const el = document.querySelector('.layout-content')
  if (el) el.scrollTo({ top: 0, behavior: 'smooth' })
}

// ====== Badge 计算 ======
const badgePulse = ref({ home: false, chat: false, me: false })

const badgeFor = (path) => {
  const b = dashboard.badges
  if (path === '/app/chat') return b.unreadMessages
  if (path === '/app/me') return b.unreadNotifications + b.reminders
  if (path === '/app/home') return b.unreadMessages + b.unreadNotifications + b.reminders
  return 0
}

// ====== WebSocket + 轮询 ======
let timer = 0
let pushTimer = 0
let chatWs = null
let chatWsRetry = 0

const onVisible = () => {
  if (!document.hidden) {
    dashboard.fetch().catch(() => {})
    pollPush().catch(() => {})
  }
}

const safeText = (s) => String(s || '').replace(/\s+/g, ' ').trim()

const maybePopupAnn = async () => {
  const day = new Date().toISOString().slice(0, 10)
  const key = `ann_popup_${day}`
  if (localStorage.getItem(key)) return
  try {
    const list = await http.get('/api/anniversaries/upcoming', { params: { days: 3 } })
    if (!Array.isArray(list) || !list.length) return
    localStorage.setItem(key, '1')
    const lines = list.slice(0, 5).map((it) => {
      const title = safeText(it.title)
      const left = typeof it.daysLeft === 'number' ? it.daysLeft : null
      const date = it.nextDate || it.date
      const ds = date ? new Date(date).toLocaleDateString() : '-'
      const p = it.pinned ? '（重要）' : ''
      if (left === 0) return `${title}${p}：就是今天 · ${ds}`
      if (left != null) return `${title}${p}：还有 ${left} 天 · ${ds}`
      return `${title}${p}：${ds}`
    })
    await ElMessageBox.confirm(
      h('div', { style: 'white-space: pre-wrap; line-height: 1.6;' }, lines.join('\n')),
      '纪念日提醒',
      {
        confirmButtonText: '去看看',
        cancelButtonText: '知道了',
        type: 'info',
        distinguishCancelAndClose: true
      }
    )
    router.push({ path: '/app/records', query: { tab: 'ann' } })
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
  }
}

const pushKey = computed(() => {
  const uid = auth.user?.id
  return uid ? `dz_push_last_${uid}` : null
})

const pollPush = async () => {
  const key = pushKey.value
  if (!key) return
  if (route.path === '/app/notifications') return
  const list = await http.get('/api/notifications', { params: { unreadOnly: true, limit: 10 } })
  if (!Array.isArray(list) || !list.length) return
  const last = Number(localStorage.getItem(key) || '0') || 0
  const fresh = list.filter((x) => x && typeof x.id === 'number' && x.id > last && x.silent !== true)
  if (!fresh.length) {
    const maxId = Math.max(...list.map((x) => (typeof x?.id === 'number' ? x.id : 0)))
    if (maxId > last) localStorage.setItem(key, String(maxId))
    return
  }
  const maxId = Math.max(...fresh.map((x) => x.id))
  localStorage.setItem(key, String(maxId))
  const toShow = fresh.slice(0, 3)
  for (const it of toShow) {
    ElNotification({
      title: String(it.title || '新通知'),
      message: String(it.content || ''),
      duration: 3500
    })
  }
  if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
    navigator.vibrate(60)
  }
}

// WebSocket 消息 → 即时更新 Badge
const updateBadgeFromWs = (eventType) => {
  const prev = { ...dashboard.badges }
  dashboard.fetch().catch(() => {}).then(() => {
    // 新消息到达 → 触发脉冲动画
    if (eventType === 'MESSAGE' && Number(dashboard.badges.unreadMessages) > Number(prev.unreadMessages)) {
      badgePulse.value.chat = true
      setTimeout(() => { badgePulse.value.chat = false }, 500)
    }
    if (['MESSAGE', 'ACCOUNT_CHANGED', 'MEMO_CHANGED'].includes(eventType)) {
      badgePulse.value.home = true
      setTimeout(() => { badgePulse.value.home = false }, 500)
    }
  })
}

const wsBase = computed(() => {
  const api = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
  const base = api || location.origin
  return base.replace(/^http/i, 'ws')
})

const ensureDeviceId = () => {
  const key = 'dz_device_id'
  let id = localStorage.getItem(key)
  if (id && String(id).trim()) return String(id)
  id = Math.random().toString(16).slice(2) + Math.random().toString(16).slice(2)
  localStorage.setItem(key, id)
  return id
}

const openChatWs = () => {
  if (!auth.token) return
  if (chatWs && (chatWs.readyState === WebSocket.OPEN || chatWs.readyState === WebSocket.CONNECTING)) return
  const deviceId = ensureDeviceId()
  const url = `${wsBase.value}/ws/chat?token=${encodeURIComponent(auth.token)}&deviceId=${encodeURIComponent(deviceId)}`
  try {
    chatWs = new WebSocket(url)
  } catch (e) {
    return
  }
  chatWs.onopen = () => {
    chatWsRetry = 0
    window.__dzChatSend = (obj) => {
      try {
        if (!chatWs || chatWs.readyState !== WebSocket.OPEN) return false
        chatWs.send(JSON.stringify(obj))
        return true
      } catch (e) {
        return false
      }
    }
  }
  chatWs.onmessage = (ev) => {
    const text = String(ev?.data || '')
    if (!text || text === 'pong') return
    let payload = null
    try {
      payload = JSON.parse(text)
    } catch (e) {
      return
    }
    if (!payload || !payload.event) return
    if (payload.event === 'MESSAGE' && payload.data) {
      window.dispatchEvent(new CustomEvent('dz_chat_message', { detail: payload.data }))
      updateBadgeFromWs('MESSAGE')
      const m = payload.data
      const uid = auth.user?.id
      if (route.path !== '/app/chat' && uid && m?.senderId && m.senderId !== uid) {
        const text =
          m.type === 'IMAGE' || m.type === 'STICKER' ? '[图片]' : m.type === 'VOICE' ? '[语音]' : String(m.content || '')
        ElNotification({
          title: '新消息',
          message: text,
          duration: 3000
        })
      }
      return
    }
    if (payload.event === 'READ_UP_TO') {
      window.dispatchEvent(new CustomEvent('dz_chat_read_up_to', { detail: payload.data }))
      return
    }
    if (payload.event === 'DELIVERED_UP_TO') {
      window.dispatchEvent(new CustomEvent('dz_chat_delivered_up_to', { detail: payload.data }))
      return
    }
    if (payload.event === 'READ') {
      window.dispatchEvent(new CustomEvent('dz_chat_read', { detail: payload.data }))
      return
    }
    if (payload.event === 'DELIVERED') {
      window.dispatchEvent(new CustomEvent('dz_chat_delivered', { detail: payload.data }))
      return
    }
    if (payload.event === 'RECALL') {
      window.dispatchEvent(new CustomEvent('dz_chat_recall', { detail: payload.data }))
      return
    }
    if (payload.event === 'TYPING') {
      window.dispatchEvent(new CustomEvent('dz_chat_typing', { detail: payload.data }))
      return
    }
    if (payload.event === 'CURSOR') {
      window.dispatchEvent(new CustomEvent('dz_chat_cursor', { detail: payload.data }))
      return
    }
    if (payload.event === 'ACCOUNT_CHANGED') {
      window.dispatchEvent(new CustomEvent('dz_accounts_changed', { detail: payload.data }))
      updateBadgeFromWs('ACCOUNT_CHANGED')
      return
    }
    if (payload.event === 'MEMO_CHANGED') {
      window.dispatchEvent(new CustomEvent('dz_memos_changed', { detail: payload.data }))
      updateBadgeFromWs('MEMO_CHANGED')
      return
    }
    if (payload.event === 'MEMO_CATEGORY_CHANGED') {
      window.dispatchEvent(new CustomEvent('dz_memo_categories_changed', { detail: payload.data }))
      return
    }
    if (payload.event === 'MEMO_EDITING') {
      window.dispatchEvent(new CustomEvent('dz_memo_editing', { detail: payload.data }))
      return
    }
    if (payload.event === 'MEMO_EDIT_DENIED') {
      window.dispatchEvent(new CustomEvent('dz_memo_edit_denied', { detail: payload.data }))
      return
    }
    if (payload.event === 'MEMO_EDIT_OK') {
      window.dispatchEvent(new CustomEvent('dz_memo_edit_ok', { detail: payload.data }))
      return
    }
  }
  chatWs.onclose = () => {
    if (!auth.token) return
    chatWsRetry += 1
    const delay = Math.min(10000, 800 + chatWsRetry * 600)
    window.setTimeout(() => {
      openChatWs()
    }, delay)
  }
}

onMounted(() => {
  dashboard.fetch().catch(() => {})
  maybePopupAnn()
  pollPush().catch(() => {})
  openChatWs()
  document.addEventListener('visibilitychange', onVisible)
  // 降低轮询频率：WebSocket 负责实时 Badge，轮询只做兜底
  timer = window.setInterval(() => {
    dashboard.fetch().catch(() => {})
  }, 60000) // 60s 兜底（原来 15s）
  pushTimer = window.setInterval(() => {
    if (!document.hidden) pollPush().catch(() => {})
  }, 12000) // 12s Push 通知轮询（原来 8s）
  // 滚动监听
  const el = document.querySelector('.layout-content')
  if (el) el.addEventListener('scroll', onPageScroll, { passive: true })
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisible)
  if (timer) window.clearInterval(timer)
  if (pushTimer) window.clearInterval(pushTimer)
  if (fabCloseTimer) window.clearTimeout(fabCloseTimer)
  if (chatWs) {
    try { chatWs.close() } catch (e) {}
    chatWs = null
  }
  if (window.__dzChatSend) delete window.__dzChatSend
  const el = document.querySelector('.layout-content')
  if (el) el.removeEventListener('scroll', onPageScroll)
})
</script>

<template>
  <div class="layout">
    <main class="layout-content">
      <div class="app-max">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- 浮动发布按钮 -->
    <div class="fab-area" @mouseleave="closeFab">
      <transition name="fab-menu">
        <div v-if="fabOpen" class="fab-menu">
          <button
            v-for="item in fabItems" :key="item.key"
            class="fab-menu-item btn-press"
            type="button"
            @click="fabAction(item)"
          >
            <span class="fab-menu-label">{{ item.label }}</span>
            <span class="fab-menu-icon"><el-icon :size="16"><component :is="item.icon" /></el-icon></span>
          </button>
        </div>
      </transition>
      <button class="fab-btn btn-press" type="button" @click="toggleFab">
        <el-icon :size="24"><Plus /></el-icon>
      </button>
    </div>

    <!-- 回顶部 -->
    <transition name="fab-menu">
      <button v-if="showScrollTop" class="scroll-top-btn btn-press" type="button" @click="scrollToTop">
        <el-icon :size="20"><ArrowUp /></el-icon>
      </button>
    </transition>

    <!-- 底部导航（5 Tab） -->
    <nav class="tabbar" :style="{ gridTemplateColumns: `repeat(${tabs.length}, 1fr)` }">
      <button
        v-for="t in tabs"
        :key="t.path"
        class="tab btn-press"
        :class="{ active: activePath === t.path }"
        type="button"
        @click="go(t.path)"
      >
        <span class="tab-icon-wrap" :class="{ 'badge-pulse': badgePulse[t.path === '/app/home' ? 'home' : t.path === '/app/chat' ? 'chat' : t.path === '/app/me' ? 'me' : ''] }">
          <el-badge :value="badgeFor(t.path)" :hidden="badgeFor(t.path) <= 0" :max="99">
            <el-icon :size="20">
              <component :is="t.icon" />
            </el-icon>
          </el-badge>
        </span>
        <span class="label">{{ t.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  background: transparent;
  position: relative;
}
.layout-content {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 14px calc(84px + env(safe-area-inset-bottom));
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

/* ====== 5 Tab 底部导航 ====== */
.tabbar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  height: 64px;
  padding-bottom: env(safe-area-inset-bottom);
  background: rgba(255,255,255,0.42);
  backdrop-filter: blur(22px) saturate(1.1);
  display: grid;
  z-index: 10;
}
.tabbar::before {
  content: '';
  position: absolute; top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(139,92,246,0.22), rgba(255,255,255,0.1));
  pointer-events: none;
}
.tab {
  border: 0; background: transparent;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 3px;
  color: rgba(17,24,39,0.62);
  padding: 6px 0 5px; cursor: pointer;
  transition: color var(--e-normal);
  -webkit-tap-highlight-color: transparent;
}
.tab.active {
  color: var(--c-primary-d);
  animation: tabPop 0.3s ease;
}
.tab:active {
  transform: scale(0.94);
}
@keyframes tabPop {
  0%   { transform: scale(0.92); }
  60%  { transform: scale(1.06); }
  100% { transform: scale(1); }
}
.tab-icon-wrap {
  display: inline-flex;
  line-height: 0;
}
.label {
  font-size: 11px; line-height: 1; font-weight: 600;
}

/* ====== 浮动发布按钮 ====== */
.fab-area {
  position: fixed;
  left: 50%; bottom: 72px;
  transform: translateX(-50%);
  z-index: 15;
  display: flex; flex-direction: column; align-items: center;
}
.fab-btn {
  width: 52px; height: 52px;
  border-radius: var(--r-full); border: none;
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-d));
  color: #fff; cursor: pointer;
  display: grid; place-items: center;
  box-shadow: 0 4px 18px rgba(99,102,241,0.32);
  animation: fabBreathe 3s ease-in-out infinite;
  transition: transform var(--e-fast), filter var(--e-fast);
  -webkit-tap-highlight-color: transparent;
}
.fab-btn:hover {
  transform: scale(1.06);
  filter: brightness(1.08);
}
.fab-btn:active {
  transform: scale(0.93);
}

/* FAB 菜单 */
.fab-menu {
  display: flex; flex-direction: column-reverse;
  gap: 10px; margin-bottom: 14px;
}
.fab-menu-item {
  display: flex; align-items: center; gap: 10px;
  background: rgba(255,255,255,0.86);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(139,92,246,0.14);
  border-radius: var(--r-full);
  padding: 8px 16px 8px 12px;
  cursor: pointer;
  box-shadow: var(--sh-lg);
  transition: all var(--e-fast);
  -webkit-tap-highlight-color: transparent;
}
.fab-menu-item:hover {
  transform: translateX(-4px);
  border-color: rgba(139,92,246,0.32);
  box-shadow: var(--sh-xl);
}
.fab-menu-label {
  font-size: 14px; font-weight: 700; white-space: nowrap;
  color: rgba(17,24,39,0.85);
}
.fab-menu-icon {
  width: 34px; height: 34px;
  border-radius: var(--r-full);
  background: rgba(139,92,246,0.1);
  display: grid; place-items: center;
  color: var(--c-primary);
}

/* FAB 菜单过渡 */
.fab-menu-enter-active {
  transition: all var(--e-bounce);
}
.fab-menu-leave-active {
  transition: all 180ms cubic-bezier(0.4,0,0.2,1);
}
.fab-menu-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.92);
}
.fab-menu-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.94);
}

.fab-menu-enter-from .fab-menu-item {
  opacity: 0;
  transform: translateY(8px);
}

/* ====== Badge ====== */
.tab-icon-wrap :deep(.el-badge__content) {
  border: none;
}

/* ====== 页面过渡 ====== */
.page-enter-active {
  animation: pageIn var(--e-slow) both;
}
.page-leave-active {
  animation: pageOut 180ms cubic-bezier(0.4,0,0.2,1) both;
}
@keyframes pageIn {
  0%   { opacity: 0; transform: translateY(8px) scale(0.985); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes pageOut {
  0%   { opacity: 1; transform: translateY(0) scale(1); }
  100% { opacity: 0; transform: translateY(-6px) scale(0.99); }
}
</style>

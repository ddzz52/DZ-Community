<script setup>
import { computed, h, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Document, House, MagicStick, Picture, User, Notebook } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useDashboardStore } from '../stores/dashboard'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const dashboard = useDashboardStore()
const auth = useAuthStore()

const tabs = [
  { path: '/app/home', label: '主页', icon: House },
  { path: '/app/records', label: '记录', icon: Document },
  { path: '/app/memos', label: '备忘', icon: Notebook },
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

const badgeFor = (path) => {
  const b = dashboard.badges
  if (path === '/app/chat') {
    return b.unreadMessages
  }
  if (path === '/app/me') {
    return b.unreadNotifications + b.reminders
  }
  if (path === '/app/home') {
    return b.unreadMessages + b.unreadNotifications + b.reminders
  }
  return 0
}

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
      dashboard.fetch().catch(() => {})
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
      return
    }
    if (payload.event === 'MEMO_CHANGED') {
      window.dispatchEvent(new CustomEvent('dz_memos_changed', { detail: payload.data }))
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
  timer = window.setInterval(() => {
    dashboard.fetch().catch(() => {})
  }, 15000)
  pushTimer = window.setInterval(() => {
    if (!document.hidden) pollPush().catch(() => {})
  }, 8000)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisible)
  if (timer) window.clearInterval(timer)
  if (pushTimer) window.clearInterval(pushTimer)
  if (chatWs) {
    try {
      chatWs.close()
    } catch (e) {}
    chatWs = null
  }
  if (window.__dzChatSend) delete window.__dzChatSend
})
</script>

<template>
  <div class="layout">
    <main class="content">
      <div class="app-max">
        <router-view />
      </div>
    </main>
    <nav class="tabbar" :style="{ gridTemplateColumns: `repeat(${tabs.length}, 1fr)` }">
      <button
        v-for="t in tabs"
        :key="t.path"
        class="tab"
        :class="{ active: activePath === t.path }"
        type="button"
        @click="go(t.path)"
      >
        <el-badge :value="badgeFor(t.path)" :hidden="badgeFor(t.path) <= 0" class="badge">
          <el-icon :size="18">
            <component :is="t.icon" />
          </el-icon>
        </el-badge>
        <span class="label">{{ t.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  background: transparent;
}
.content {
  padding: 16px 14px calc(84px + env(safe-area-inset-bottom));
}
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 64px;
  padding-bottom: env(safe-area-inset-bottom);
  background: rgba(255, 255, 255, 0.42);
  backdrop-filter: blur(22px) saturate(1.1);
  display: grid;
  z-index: 10;
}
.tabbar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.22), rgba(255, 255, 255, 0.1));
  pointer-events: none;
}
.tab {
  border: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: rgba(17, 24, 39, 0.62);
  padding: 6px 0 5px;
  cursor: pointer;
  transition: color 0.25s ease, transform 0.25s ease;
}
.tab.active {
  color: #4f46e5;
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
.label {
  font-size: 11px;
  line-height: 1;
}
.badge :deep(.el-badge__content) {
  border: none;
}
</style>

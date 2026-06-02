<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { ArrowLeft, Bell, Check, Delete, RefreshRight } from '@element-plus/icons-vue'
import { useDashboardStore } from '../stores/dashboard'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const dashboard = useDashboardStore()
const auth = useAuthStore()
const loading = ref(false)
const items = ref([])
const unreadOnly = ref(false)
const typeFilter = ref('ALL')
let timer = 0

const typeText = (t) => {
  const s = String(t || '')
  if (s === 'ANNIVERSARY_REMINDER') return '纪念日'
  if (s === 'DIARY_COMMENT') return '日记评论'
  if (s === 'PHOTO_LIKE') return '相册点赞'
  if (s === 'SECURITY_PASSWORD_CHANGED') return '安全'
  if (s === 'SECURITY_LOGIN_ANOMALY') return '安全'
  if (s === 'PASSWORD_RESET') return '安全'
  if (s === 'SYSTEM') return '系统'
  return s || '通知'
}

const matchFilter = (it) => {
  const t = String(it?.type || '')
  if (typeFilter.value === 'ALL') return true
  if (typeFilter.value === 'REMINDER') return t === 'ANNIVERSARY_REMINDER'
  if (typeFilter.value === 'INTERACTION') return t === 'DIARY_COMMENT' || t === 'PHOTO_LIKE'
  if (typeFilter.value === 'SECURITY') return t.startsWith('SECURITY_') || t === 'PASSWORD_RESET'
  if (typeFilter.value === 'SYSTEM') return t === 'SYSTEM'
  return true
}

const load = async (silent) => {
  try {
    loading.value = true
    items.value = await http.get('/api/notifications', {
      params: { unreadOnly: unreadOnly.value, limit: 200 }
    })
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const markRead = async (id) => {
  try {
    await http.put(`/api/notifications/${id}/read`)
    const it = items.value.find((x) => x.id === id)
    if (it) it.read = true
    dashboard.fetch().catch(() => {})
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const markAll = async () => {
  try {
    await http.put('/api/notifications/read-all')
    items.value = items.value.map((x) => ({ ...x, read: true }))
    ElMessage.success('已全部标记已读')
    dashboard.fetch().catch(() => {})
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const clearRead = async () => {
  try {
    const n = items.value.filter((x) => x.read).length
    if (!n) return
    await ElMessageBox.confirm(`确认清理已读通知（${n} 条）吗？`, '清理已读', {
      confirmButtonText: '清理',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.put('/api/notifications/clear-read')
    items.value = items.value.filter((x) => !x.read)
    ElMessage.success('已清理')
    dashboard.fetch().catch(() => {})
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '操作失败')
  }
}

const filteredItems = computed(() => items.value.filter(matchFilter))
const unreadCount = computed(() => items.value.filter((x) => !x.read).length)

const logout = async () => {
  auth.logout()
  try {
    await router.replace('/login')
  } catch (e) {}
  try {
    if (location.pathname !== '/login') location.href = '/login'
  } catch (e) {}
}

onMounted(() => {
  load(true)
  timer = window.setInterval(() => {
    if (!document.hidden) load(true)
  }, 8000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="stack">
    <div class="head app-card">
      <div class="head-top">
        <div class="head-left">
          <button class="navbtn" type="button" @click="router.push('/app/home')">
            <el-icon :size="18"><ArrowLeft /></el-icon>
          </button>
        </div>

        <div class="head-mid">
          <div class="brand">
            <span class="brand-ico" aria-hidden="true">
              <el-icon :size="16"><Bell /></el-icon>
            </span>
            <span class="brand-text">提醒中心</span>
            <span v-if="unreadCount > 0" class="unread">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
          <div class="brand-sub app-muted">把重要的事，温柔地提醒你。</div>
        </div>

        <div class="head-right">
          <el-tooltip content="刷新" placement="bottom">
            <button class="navbtn" type="button" :disabled="loading" @click="load()">
              <el-icon :size="18"><RefreshRight /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="全已读" placement="bottom">
            <button class="navbtn" type="button" :disabled="!unreadCount" @click="markAll">
              <el-icon :size="18"><Check /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="清理已读" placement="bottom">
            <button class="navbtn" type="button" :disabled="!items.some((x) => x.read)" @click="clearRead">
              <el-icon :size="18"><Delete /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="退出" placement="bottom">
            <button class="navbtn danger" type="button" @click="logout">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M10 7V6.2C10 4.985 10.985 4 12.2 4h5.6C19.015 4 20 4.985 20 6.2v11.6c0 1.215-.985 2.2-2.2 2.2h-5.6c-1.215 0-2.2-.985-2.2-2.2V17"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                />
                <path d="M4 12h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <path d="M7 9l-3 3 3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </button>
          </el-tooltip>
        </div>
      </div>

      <div class="filters">
        <div class="fleft">
          <el-switch v-model="unreadOnly" :disabled="loading" @change="() => load(true)" />
          <span class="app-muted">只看未读</span>
        </div>
        <el-select v-model="typeFilter" :disabled="loading" size="small" style="width: 140px" @change="() => {}">
          <el-option label="全部" value="ALL" />
          <el-option label="纪念日" value="REMINDER" />
          <el-option label="互动" value="INTERACTION" />
          <el-option label="安全" value="SECURITY" />
          <el-option label="系统" value="SYSTEM" />
        </el-select>
      </div>
    </div>

    <el-card class="app-card" shadow="never">
      <div v-if="!filteredItems.length" class="empty app-muted">还没有提醒</div>
      <div v-else class="list">
        <div v-for="it in filteredItems" :key="it.id" class="item" :class="{ read: it.read }">
          <div class="item-top">
            <div class="item-title">
              <el-tag size="small" effect="light" round>{{ typeText(it.type) }}</el-tag>
              <span class="t">{{ it.title }}</span>
            </div>
            <el-button v-if="!it.read" size="small" text @click="markRead(it.id)">标记已读</el-button>
            <span v-else class="app-muted">已读</span>
          </div>
          <div v-if="it.content" class="item-content">{{ it.content }}</div>
          <div class="item-time app-muted">{{ new Date(it.createdAt).toLocaleString() }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.head {
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.06), rgba(139, 92, 246, 0.06), rgba(255, 255, 255, 0.85));
  border-color: rgba(139, 92, 246, 0.1);
}
.head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.head-left {
  width: 44px;
  display: flex;
  align-items: center;
}
.head-mid {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.head-right {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 168px;
}
.navbtn {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  place-items: center;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.72);
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease, background 0.16s ease, color 0.16s ease;
}
.navbtn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.navbtn.danger {
  color: rgba(239, 68, 68, 0.9);
  border-color: rgba(239, 68, 68, 0.18);
}
@media (hover: hover) {
  .navbtn:hover:not(:disabled) {
    transform: translateY(-1px);
    border-color: rgba(59, 130, 246, 0.22);
    box-shadow: 0 14px 30px rgba(59, 130, 246, 0.12);
    background: rgba(255, 255, 255, 0.86);
    color: rgba(37, 99, 235, 0.92);
  }
  .navbtn.danger:hover {
    border-color: rgba(239, 68, 68, 0.28);
    box-shadow: 0 14px 30px rgba(239, 68, 68, 0.12);
    color: rgba(239, 68, 68, 0.92);
  }
}
.brand {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.brand-ico {
  width: 28px;
  height: 28px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(99, 102, 241, 0.12));
  border: 1px solid rgba(255, 255, 255, 0.7);
  color: rgba(37, 99, 235, 0.92);
}
.brand-text {
  font-weight: 900;
  letter-spacing: 0.2px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.92), rgba(99, 102, 241, 0.9));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.unread {
  min-width: 22px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.92);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.brand-sub {
  font-size: 12px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.filters {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(17, 24, 39, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.fleft {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.empty {
  padding: 14px 0;
  text-align: center;
  font-size: 13px;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.item {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
}
.item.read {
  opacity: 0.75;
}
.item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.item-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.t {
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-content {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}
.item-time {
  margin-top: 8px;
  font-size: 12px;
}
</style>

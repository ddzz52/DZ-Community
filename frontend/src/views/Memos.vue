<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { Delete, Edit, Plus, RefreshRight, Search, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const myName = computed(() => auth.user?.nickname || auth.user?.username || '我')
const myId = computed(() => {
  const v = auth.user?.id
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : null
})

const categories = ref([])
const loadingCategories = ref(false)

const memos = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)

const filters = reactive({
  categoryId: null,
  status: null,
  q: ''
})

const cursor = reactive({
  beforeStatus: null,
  beforeAt: null,
  beforeId: null
})

const EDIT_TTL_MS = 15000
const editingMap = ref(new Map())
const nowMs = ref(Date.now())
let nowTimer = 0

const cleanupExpiredEditing = () => {
  const m = editingMap.value
  if (!m || !m.size) return
  const out = new Map(m)
  let changed = false
  for (const [id, v] of out.entries()) {
    const exp = Number(v?.expireAtMs || 0)
    if (!exp || exp <= nowMs.value) {
      out.delete(id)
      changed = true
    }
  }
  if (changed) editingMap.value = out
}

const editingInfo = (id) => {
  if (id == null) return null
  const v = editingMap.value.get(id)
  if (!v) return null
  const exp = Number(v?.expireAtMs || 0)
  if (exp && exp <= nowMs.value) return null
  return v
}

const isLockedByOther = (m) => {
  const id = m?.id
  return id != null && !!editingInfo(id)
}

const editingCountdown = (id) => {
  const v = editingInfo(id)
  if (!v) return ''
  const exp = Number(v?.expireAtMs || 0)
  if (!exp) return ''
  const left = Math.max(0, Math.ceil((exp - nowMs.value) / 1000))
  return left ? `${left}s` : '即将释放'
}

const sendWs = (obj) => {
  try {
    return typeof window !== 'undefined' && typeof window.__dzChatSend === 'function' ? window.__dzChatSend(obj) : false
  } catch (e) {
    return false
  }
}

const errText = (e, fallback) => {
  const msg = String(e?.message || '').trim()
  if (msg === 'Network Error') {
    return '网络异常：无法连接服务器，请确认后端服务已启动且 /api/health/ping 可访问'
  }
  const status = e?.response?.status
  const body = e?.response?.data
  const bmsg = typeof body?.message === 'string' ? body.message : ''
  if (status) {
    const s = String(status)
    if (bmsg && bmsg.trim()) return `${s}：${bmsg.trim()}`
    if (msg) return `${s}：${msg}`
    return `${s}：${fallback}`
  }
  return msg || fallback
}

const loadCategories = async (silent) => {
  if (loadingCategories.value) return
  loadingCategories.value = true
  try {
    const list = await http.get('/api/memos/categories')
    categories.value = Array.isArray(list) ? list : []
  } catch (e) {
    if (!silent) ElMessage.error(errText(e, '加载分类失败'))
  } finally {
    loadingCategories.value = false
  }
}

const listParams = computed(() => {
  const p = {}
  if (filters.categoryId != null) p.categoryId = filters.categoryId
  if (filters.status != null) p.status = filters.status
  const q = String(filters.q || '').trim()
  if (q) p.q = q
  if (cursor.beforeStatus != null && cursor.beforeAt && cursor.beforeId != null) {
    p.beforeStatus = cursor.beforeStatus
    p.beforeAt = cursor.beforeAt
    p.beforeId = cursor.beforeId
  }
  p.limit = 50
  return p
})

const resetCursor = () => {
  cursor.beforeStatus = null
  cursor.beforeAt = null
  cursor.beforeId = null
}

const loadMemos = async (reset, silent) => {
  if (loading.value) return
  if (reset) {
    resetCursor()
    memos.value = []
    hasMore.value = true
  }
  loading.value = true
  try {
    const list = await http.get('/api/memos', { params: listParams.value })
    const arr = Array.isArray(list) ? list : []
    memos.value = reset ? arr : memos.value.concat(arr)
    hasMore.value = arr.length >= 50
  } catch (e) {
    if (!silent) ElMessage.error(errText(e, '加载备忘录失败'))
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!hasMore.value || loadingMore.value || loading.value) return
  const last = memos.value[memos.value.length - 1]
  if (!last) return
  cursor.beforeStatus = last.status
  cursor.beforeAt = fmtDateTime(last.updatedAt)
  cursor.beforeId = last.id
  loadingMore.value = true
  try {
    await loadMemos(false, true)
  } finally {
    loadingMore.value = false
  }
}

let filterTimer = 0
watch(
  () => [filters.categoryId, filters.status, filters.q],
  () => {
    if (filterTimer) window.clearTimeout(filterTimer)
    filterTimer = window.setTimeout(() => {
      loadMemos(true, true)
    }, 300)
  }
)

const defaultCategoryId = computed(() => {
  const list = categories.value || []
  const other = list.find((c) => c && c.name === '其他')
  return other?.id || list[0]?.id || null
})

const toggleCategoryFilter = (id) => {
  if (id == null) return
  filters.categoryId = filters.categoryId === id ? null : id
}

const memoDialogVisible = ref(false)
const memoSaving = ref(false)
const editingId = ref(null)
const lockedEditingId = ref(null)
let hbTimer = 0

const memoForm = reactive({
  categoryId: null,
  title: '',
  content: '',
  status: 0
})

const stopEditingLock = () => {
  if (hbTimer) window.clearInterval(hbTimer)
  hbTimer = 0
  const id = lockedEditingId.value
  lockedEditingId.value = null
  if (id != null) {
    sendWs({ event: 'MEMO_EDIT_END', id })
  }
}

const startHeartbeat = (id) => {
  if (hbTimer) window.clearInterval(hbTimer)
  hbTimer = window.setInterval(() => {
    sendWs({ event: 'MEMO_EDIT_HEARTBEAT', id })
  }, 5000)
}

const waitForLock = (id) => {
  return new Promise((resolve) => {
    let done = false
    const ok = (ev) => {
      const d = ev?.detail
      if (!d || d.id !== id) return
      cleanup()
      resolve({ ok: true })
    }
    const denied = (ev) => {
      const d = ev?.detail
      if (!d || d.id !== id) return
      cleanup()
      resolve({ ok: false, byNickname: d.byNickname })
    }
    const cleanup = () => {
      if (done) return
      done = true
      window.removeEventListener('dz_memo_edit_ok', ok)
      window.removeEventListener('dz_memo_edit_denied', denied)
      if (t) window.clearTimeout(t)
    }
    window.addEventListener('dz_memo_edit_ok', ok)
    window.addEventListener('dz_memo_edit_denied', denied)
    const sent = sendWs({ event: 'MEMO_EDIT_START', id })
    const t = window.setTimeout(() => {
      cleanup()
      resolve({ ok: !!sent })
    }, 1200)
  })
}

const openCreate = async () => {
  stopEditingLock()
  editingId.value = null
  memoForm.categoryId = defaultCategoryId.value
  memoForm.title = ''
  memoForm.content = ''
  memoForm.status = 0
  memoDialogVisible.value = true
}

const openEdit = async (m) => {
  const id = m?.id
  if (id == null) return
  if (isLockedByOther(m)) {
    const info = editingInfo(id)
    ElMessage.warning(info?.nickname ? `对方正在编辑：${info.nickname}` : '对方正在编辑，请稍后操作')
    return
  }
  stopEditingLock()
  const r = await waitForLock(id)
  if (!r.ok) {
    ElMessage.warning(r.byNickname ? `对方正在编辑：${r.byNickname}` : '对方正在编辑，请稍后操作')
    return
  }
  lockedEditingId.value = id
  startHeartbeat(id)
  editingId.value = id
  memoForm.categoryId = m.categoryId
  memoForm.title = String(m.title || '')
  memoForm.content = String(m.content || '')
  memoForm.status = m.status === 1 ? 1 : 0
  memoDialogVisible.value = true
}

const closeMemoDialog = () => {
  memoDialogVisible.value = false
  editingId.value = null
  memoSaving.value = false
  stopEditingLock()
}

const saveMemo = async () => {
  if (memoSaving.value) return
  const payload = {
    categoryId: memoForm.categoryId,
    title: String(memoForm.title || '').trim(),
    content: String(memoForm.content || '').trim(),
    status: memoForm.status
  }
  memoSaving.value = true
  try {
    if (!payload.categoryId) {
      ElMessage.warning('请选择分类')
      return
    }
    if (!payload.title) {
      ElMessage.warning('请输入标题')
      return
    }
    if (!payload.content) {
      ElMessage.warning('请输入内容')
      return
    }
    if (payload.title.length > 30) {
      ElMessage.warning('标题≤30字')
      return
    }
    if (payload.content.length > 500) {
      ElMessage.warning('内容≤500字')
      return
    }
    if (editingId.value == null) {
      await http.post('/api/memos', payload)
      ElMessage.success('已新增')
      closeMemoDialog()
      await loadMemos(true, true)
      return
    }
    await http.put(`/api/memos/${editingId.value}`, payload)
    ElMessage.success('已保存')
    closeMemoDialog()
    await loadMemos(true, true)
  } catch (e) {
    ElMessage.error(errText(e, '保存失败'))
  } finally {
    memoSaving.value = false
  }
}

const removeMemo = async (m) => {
  const id = m?.id
  if (id == null) return
  if (isLockedByOther(m)) {
    const info = editingInfo(id)
    ElMessage.warning(info?.nickname ? `对方正在编辑：${info.nickname}` : '对方正在编辑，请稍后操作')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除这条备忘录吗？', '提示', { type: 'warning', distinguishCancelAndClose: true })
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
  }
  try {
    await http.delete(`/api/memos/${id}`)
    ElMessage.success('已删除')
    if (editingId.value === id) closeMemoDialog()
    await loadMemos(true, true)
  } catch (e) {
    ElMessage.error(errText(e, '删除失败'))
  }
}

const toggleStatus = async (m) => {
  const id = m?.id
  if (id == null) return
  if (isLockedByOther(m)) {
    const info = editingInfo(id)
    ElMessage.warning(info?.nickname ? `对方正在编辑：${info.nickname}` : '对方正在编辑，请稍后操作')
    return
  }
  const old = m.status === 1 ? 1 : 0
  const next = old === 1 ? 0 : 1
  m.status = next
  try {
    await http.patch(`/api/memos/${id}/status`, { status: next })
  } catch (e) {
    m.status = old
    ElMessage.error(errText(e, '更新状态失败'))
  }
}

function fmtDateTime(d) {
  if (!d) return ''
  const dt = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(dt.getTime())) return ''
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  const hh = String(dt.getHours()).padStart(2, '0')
  const mm = String(dt.getMinutes()).padStart(2, '0')
  const ss = String(dt.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
}

const formatTime = (v) => {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString()
  } catch (e) {
    return String(v)
  }
}

const modifierLabel = (m) => {
  const uid = m?.updatedBy
  if (uid != null && myId.value != null && Number(uid) === Number(myId.value)) return '我'
  return m?.updatedByNickname || '对方'
}

const isMeUserId = (uid) => {
  if (uid == null || myId.value == null) return false
  return Number(uid) === Number(myId.value)
}
const roleText = (uid) => (isMeUserId(uid) ? '我' : 'TA')
const roleNick = (uid, nick) => {
  if (isMeUserId(uid)) return myName.value
  const t = String(nick || '').trim()
  return t ? t : 'TA'
}

const creatorLabel = (m) => {
  const uid = m?.createdBy
  if (uid != null && myId.value != null && Number(uid) === Number(myId.value)) return '我'
  return m?.createdByNickname || '对方'
}

const editingLabel = (id) => {
  const v = editingInfo(id)
  const nick = v?.nickname ? String(v.nickname) : ''
  if (!nick) return ''
  const left = editingCountdown(id)
  return left ? `${nick}（${left}）` : nick
}

const addCategory = async () => {
  let name = ''
  try {
    const r = await ElMessageBox.prompt('请输入分类名称（≤20字）', '新增分类', {
      confirmButtonText: '新增',
      cancelButtonText: '取消',
      inputValue: '',
      inputValidator: (v) => {
        const t = String(v || '').trim()
        if (!t) return '请输入分类名称'
        if (t.length > 20) return '分类名称≤20字'
        return true
      }
    })
    name = String(r?.value || '').trim()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
  }
  if (!name) return
  try {
    await http.post('/api/memos/categories', { name })
    ElMessage.success('已新增分类')
    await loadCategories(true)
  } catch (e) {
    ElMessage.error(errText(e, '新增分类失败'))
  }
}

const renameCategory = async (c) => {
  if (!c || c.systemFlag === 1) return
  let name = ''
  try {
    const r = await ElMessageBox.prompt('请输入新的分类名称（≤20字）', '重命名分类', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: String(c.name || ''),
      inputValidator: (v) => {
        const t = String(v || '').trim()
        if (!t) return '请输入分类名称'
        if (t.length > 20) return '分类名称≤20字'
        return true
      }
    })
    name = String(r?.value || '').trim()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
  }
  if (!name || c.id == null) return
  try {
    await http.put(`/api/memos/categories/${c.id}`, { name })
    ElMessage.success('已保存')
    await loadCategories(true)
  } catch (e) {
    ElMessage.error(errText(e, '保存失败'))
  }
}

const deleteCategory = async (c) => {
  if (!c || c.systemFlag === 1 || c.id == null) return
  try {
    await ElMessageBox.confirm(`确定删除分类“${String(c.name || '')}”吗？`, '提示', {
      type: 'warning',
      distinguishCancelAndClose: true
    })
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
  }
  try {
    await http.delete(`/api/memos/categories/${c.id}`)
    ElMessage.success('已删除')
    if (filters.categoryId === c.id) filters.categoryId = null
    await loadCategories(true)
    await loadMemos(true, true)
  } catch (e) {
    ElMessage.error(errText(e, '删除失败'))
  }
}

const onWsMemosChanged = () => {
  loadMemos(true, true)
}
const onWsCategoriesChanged = () => {
  loadCategories(true)
  loadMemos(true, true)
}
const onWsEditing = (ev) => {
  const d = ev?.detail
  if (!d || d.id == null) return
  if (d.editing !== true && d.editing !== false) return
  const byUserId = d.byUserId
  if (byUserId != null && myId.value != null && Number(byUserId) === Number(myId.value)) {
    if (d.editing === false) {
      const m = new Map(editingMap.value)
      m.delete(d.id)
      editingMap.value = m
    }
    return
  }
  const m = new Map(editingMap.value)
  if (d.editing) {
    const at = typeof d.at === 'number' ? d.at : Date.now()
    m.set(d.id, { nickname: d.byNickname || '对方', expireAtMs: at + EDIT_TTL_MS })
  } else {
    m.delete(d.id)
  }
  editingMap.value = m
}

onMounted(async () => {
  await loadCategories(true)
  await loadMemos(true, true)
  nowMs.value = Date.now()
  nowTimer = window.setInterval(() => {
    nowMs.value = Date.now()
    cleanupExpiredEditing()
  }, 1000)
  window.addEventListener('dz_memos_changed', onWsMemosChanged)
  window.addEventListener('dz_memo_categories_changed', onWsCategoriesChanged)
  window.addEventListener('dz_memo_editing', onWsEditing)
})

onBeforeUnmount(() => {
  stopEditingLock()
  window.removeEventListener('dz_memos_changed', onWsMemosChanged)
  window.removeEventListener('dz_memo_categories_changed', onWsCategoriesChanged)
  window.removeEventListener('dz_memo_editing', onWsEditing)
  if (filterTimer) window.clearTimeout(filterTimer)
  if (nowTimer) window.clearInterval(nowTimer)
  nowTimer = 0
})
</script>

<template>
  <div class="stack">
    <div class="top app-card">
      <div class="topbar">
        <div class="head-mid">
          <div class="title">共享备忘录</div>
          <div class="sub app-muted">未完成优先 · 按更新时间倒序</div>
        </div>
        <div class="head-right">
          <el-tooltip content="刷新" placement="bottom">
            <button
              class="iconbtn"
              type="button"
              :disabled="loading || loadingCategories"
              @click="() => { loadCategories(true); loadMemos(true, true) }"
            >
              <el-icon :size="18"><RefreshRight /></el-icon>
            </button>
          </el-tooltip>
          <el-button class="primarybtn" size="small" type="primary" @click="openCreate">
            <el-icon :size="16"><Plus /></el-icon>
            <span>新增</span>
          </el-button>
        </div>
      </div>
    </div>

    <div class="filters-card app-card">
      <div class="filters">
        <el-select v-model="filters.categoryId" class="pill" clearable placeholder="分类" style="width: 140px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filters.status" class="pill" clearable placeholder="状态" style="width: 140px">
          <el-option label="待办" :value="0" />
          <el-option label="已完成" :value="1" />
        </el-select>
        <el-input v-model="filters.q" class="pill" clearable placeholder="搜索标题 / 内容">
          <template #prefix>
            <el-icon :size="16"><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <el-card class="app-card" shadow="never">
      <div v-if="!memos.length && !loading" class="empty">
        <div class="empty-ill">
          <svg viewBox="0 0 240 160" width="220" height="140" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M56 34c0-8.8 7.2-16 16-16h88c8.8 0 16 7.2 16 16v92c0 8.8-7.2 16-16 16H72c-8.8 0-16-7.2-16-16V34Z"
              stroke="rgba(17,24,39,0.28)"
              stroke-width="3"
            />
            <path d="M80 54h72" stroke="rgba(17,24,39,0.26)" stroke-width="3" stroke-linecap="round" />
            <path d="M80 74h60" stroke="rgba(17,24,39,0.22)" stroke-width="3" stroke-linecap="round" />
            <path d="M80 94h66" stroke="rgba(17,24,39,0.22)" stroke-width="3" stroke-linecap="round" />
            <path
              d="M92 120c8 10 18 14 28 14s20-4 28-14"
              stroke="rgba(99,102,241,0.45)"
              stroke-width="3"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <div class="empty-text">
          <div class="empty-title">暂无备忘录</div>
          <div class="empty-sub app-muted">和 TA 一起记录你们的小计划吧</div>
          <div class="empty-actions">
            <el-button class="primarybtn" size="small" type="primary" @click="openCreate">新增第一条</el-button>
          </div>
        </div>
      </div>
      <transition-group name="memo" tag="div" class="memolist">
        <div v-for="m in memos" :key="m.id" class="memo" :class="{ locked: isLockedByOther(m), doneCard: m.status === 1 }">
          <div class="memo-left">
            <el-checkbox :model-value="m.status === 1" :disabled="isLockedByOther(m)" @change="() => toggleStatus(m)">
              <span class="memo-title" :class="{ done: m.status === 1 }">{{ m.title }}</span>
            </el-checkbox>
            <div class="memo-content app-muted" :class="{ done: m.status === 1 }">{{ m.content }}</div>
            <div class="memo-meta app-muted">
              <el-tag size="small" effect="light" round>{{ m.categoryName || '未分类' }}</el-tag>
              <span class="meta-split">·</span>
            <span class="metaitem">
              <span class="metalabel">创建</span>
              <span class="rolepill" :class="isMeUserId(m.createdBy) ? 'me' : 'ta'">{{ roleText(m.createdBy) }}</span>
              <span class="metanick">{{ roleNick(m.createdBy, m.createdByNickname) }}</span>
            </span>
            <span class="meta-split">·</span>
            <span class="metaitem">
              <span class="metalabel">修改</span>
              <span class="rolepill" :class="isMeUserId(m.updatedBy) ? 'me' : 'ta'">{{ roleText(m.updatedBy) }}</span>
              <span class="metanick">{{ roleNick(m.updatedBy, m.updatedByNickname) }}</span>
            </span>
              <span class="meta-split">·</span>
              <span>{{ formatTime(m.updatedAt) }}</span>
              <template v-if="editingLabel(m.id)">
                <span class="meta-split">·</span>
                <el-tag size="small" type="warning" effect="light" round>编辑中 · {{ editingLabel(m.id) }}</el-tag>
              </template>
            </div>
          </div>
          <div class="memo-actions">
            <el-tooltip :content="isLockedByOther(m) ? '对方正在编辑，已锁定' : '编辑'" placement="top">
              <button class="iconbtn" type="button" :disabled="isLockedByOther(m)" @click="() => openEdit(m)">
                <el-icon :size="18"><Edit /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip :content="isLockedByOther(m) ? '对方正在编辑，已锁定' : '删除'" placement="top">
              <button class="iconbtn danger" type="button" :disabled="isLockedByOther(m)" @click="() => removeMemo(m)">
                <el-icon :size="18"><Delete /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </div>
      </transition-group>

      <div v-if="hasMore && memos.length" class="more">
        <el-button size="small" :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>

      <div class="cats">
        <div class="cats-head">
          <div class="cats-title">分类</div>
          <div class="cats-actions">
            <el-button class="pillbtn" size="small" @click="addCategory">新增分类</el-button>
            <el-tooltip content="点击分类可筛选；自定义分类悬停可重命名/删除" placement="top">
              <button class="iconbtn" type="button">
                <el-icon :size="18"><InfoFilled /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </div>
        <div class="cats-list">
          <div
            v-for="c in categories"
            :key="c.id"
            class="catpill"
            :class="{ active: filters.categoryId === c.id }"
          >
            <button class="catbtn" type="button" @click="() => toggleCategoryFilter(c.id)">
              <span class="catname">{{ c.name }}</span>
            </button>
            <div v-if="c.systemFlag !== 1" class="catactions">
              <el-tooltip content="重命名" placement="top">
                <button class="caticobtn" type="button" @click.stop="() => renameCategory(c)">
                  <el-icon :size="16"><Edit /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <button class="caticobtn danger" type="button" @click.stop="() => deleteCategory(c)">
                  <el-icon :size="16"><Delete /></el-icon>
                </button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="memoDialogVisible" class="memo-dialog" :title="editingId == null ? '新增备忘录' : '编辑备忘录'" width="520px" @close="closeMemoDialog">
      <div class="form">
        <el-form label-width="70px">
          <el-form-item label="分类">
            <el-select v-model="memoForm.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="memoForm.status" style="width: 100%">
              <el-option label="待办" :value="0" />
              <el-option label="已完成" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="memoForm.title" maxlength="30" show-word-limit placeholder="≤30字" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="memoForm.content" type="textarea" :rows="6" maxlength="500" show-word-limit placeholder="≤500字" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="closeMemoDialog">取消</el-button>
        <el-button type="primary" :loading="memoSaving" @click="saveMemo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.top {
  padding: 14px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.06), rgba(139, 92, 246, 0.06), rgba(255, 255, 255, 0.85));
  border-color: rgba(139, 92, 246, 0.1);
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.head-mid {
  min-width: 0;
}
.title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.2px;
  line-height: 1.2;
}
.sub {
  margin-top: 6px;
  font-size: 12px;
}
.head-right {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.iconbtn {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  padding: 8px 10px;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.7);
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}
.iconbtn:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.1);
}
.iconbtn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.primarybtn {
  border-radius: 999px;
  padding: 0 14px;
  height: 34px;
  transition: transform 0.15s ease, filter 0.15s ease;
}
.primarybtn:hover {
  transform: translateY(-1px);
  filter: brightness(1.03);
}
.filters-card {
  padding: 12px 14px;
}
.filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.pill :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: none;
}
.pill :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.35);
}
.pill :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(64, 158, 255, 0.55);
}
.empty {
  padding: 26px 10px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.empty-ill {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: radial-gradient(420px 180px at 50% 70%, rgba(99, 102, 241, 0.12), transparent 65%),
    radial-gradient(420px 220px at 55% 0%, rgba(99, 102, 241, 0.1), transparent 62%);
  border: 1px solid rgba(255, 255, 255, 0.7);
}
.empty-text {
  text-align: center;
}
.empty-title {
  font-weight: 700;
  letter-spacing: 0.2px;
}
.empty-actions {
  padding-top: 10px;
}
.memolist {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.memo {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.06);
  transition: transform 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease, border-color 0.15s ease;
}
.memo:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 34px rgba(17, 24, 39, 0.1);
  border-color: rgba(99, 102, 241, 0.22);
}
.memo.locked {
  background: var(--el-color-warning-light-9);
}
.memo.doneCard {
  background: rgba(17, 24, 39, 0.04);
  border-color: rgba(17, 24, 39, 0.06);
  box-shadow: none;
}
.memo.doneCard:hover {
  transform: none;
}
.memo-left {
  flex: 1;
  min-width: 0;
}
.memo-title {
  font-weight: 600;
}
.memo-content {
  margin-left: 24px;
  padding-top: 6px;
  white-space: pre-wrap;
  word-break: break-word;
}
.memo-meta {
  margin-left: 24px;
  padding-top: 8px;
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}
.meta-split {
  opacity: 0.8;
}
.metaitem {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.metalabel {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.62);
}
.metanick {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.62);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rolepill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 18px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.2px;
  color: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08);
  flex: 0 0 auto;
}
.rolepill.me {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.92), rgba(139, 92, 246, 0.92));
}
.rolepill.ta {
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.92), rgba(139, 92, 246, 0.92));
}
.memo-actions {
  display: flex;
  gap: 8px;
}
.memo-actions button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.done {
  text-decoration: line-through;
  opacity: 0.7;
}
.more {
  padding-top: 10px;
  text-align: center;
}
.cats {
  padding-top: 14px;
}
.cats-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
}
.cats-title {
  font-weight: 800;
  letter-spacing: 0.2px;
}
.cats-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.pillbtn {
  border-radius: 999px;
}
.cats-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.catpill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.7);
  transition: border-color 0.15s ease, background-color 0.15s ease;
}
.catpill.active {
  border-color: rgba(64, 158, 255, 0.5);
  background: rgba(64, 158, 255, 0.12);
}
.catbtn {
  border: 0;
  background: transparent;
  padding: 4px 8px;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.8);
}
.catactions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
}
.catpill:hover .catactions {
  opacity: 1;
  pointer-events: auto;
}
.caticobtn {
  border: 0;
  background: transparent;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 999px;
  color: rgba(17, 24, 39, 0.62);
  transition: background-color 0.15s ease, color 0.15s ease;
}
.caticobtn:hover {
  background: rgba(99, 102, 241, 0.1);
  color: rgba(17, 24, 39, 0.82);
}
.caticobtn.danger:hover {
  background: rgba(239, 68, 68, 0.1);
  color: rgba(239, 68, 68, 0.9);
}

.memo-enter-active,
.memo-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.memo-enter-from,
.memo-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
.memo-move {
  transition: transform 0.18s ease;
}

@keyframes dzDialogIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.memo-dialog :deep(.el-dialog) {
  animation: dzDialogIn 0.18s ease;
}
</style>

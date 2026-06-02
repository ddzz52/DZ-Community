<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Microphone, Picture, RefreshRight, Delete, Edit, Close, CopyDocument, CircleCheck, CircleCheckFilled, Select } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useDashboardStore } from '../../stores/dashboard'
import http from '../../api/http'

const router = useRouter()
const auth = useAuthStore()
const dashboard = useDashboardStore()

const apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const ensureDeviceId = () => {
  const key = 'dz_device_id'
  let id = localStorage.getItem(key)
  if (id && String(id).trim()) return String(id)
  id = Math.random().toString(16).slice(2) + Math.random().toString(16).slice(2)
  localStorage.setItem(key, id)
  return id
}
const deviceId = ensureDeviceId()
const assetUrl = (u) => {
  const s = String(u || '')
  if (!s) return ''
  if (/^(https?:)?\/\//.test(s) || s.startsWith('blob:') || s.startsWith('data:')) return s
  if (s.startsWith('/') && apiBase) return apiBase + s
  return s
}
const openUrl = (u) => {
  const url = assetUrl(u)
  if (!url) return
  try {
    window.open(url, '_blank')
  } catch (e) {}
}

const myId = computed(() => auth.user?.id)
const profile = ref(null)
const meUser = computed(() => profile.value?.me || auth.user || null)
const partnerUser = computed(() => profile.value?.partner || null)
const initials = (u) => {
  const n = String(u?.nickname || u?.username || '').trim()
  return n ? n.slice(0, 1).toUpperCase() : 'U'
}
const avatarUrlFor = (u) => {
  const url = String(u?.avatarUrl || '').trim()
  return url ? assetUrl(url) : ''
}
const loadProfile = async () => {
  try {
    profile.value = await http.get('/api/profile')
  } catch (e) {}
}

const list = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const input = ref('')
const inputRef = ref(null)
const draftStickerUrl = ref('')
const inputLen = computed(() => String(input.value || '').length)
const canSend = computed(() => {
  if (sending.value) return false
  return !!String(input.value || '').trim() || !!String(draftStickerUrl.value || '').trim()
})
const sending = ref(false)
const msgType = ref('TEXT')
const partnerTyping = ref(false)
let typingTimer = 0
let typingStopTimer = 0
const cursorLastReadId = ref(null)
const recording = ref(false)
const recordSec = ref(0)
let recordTimer = 0
let recordStream = null
let mediaRecorder = null
let recordChunks = []
let recordCanceled = false
const recordCanceling = ref(false)
let recordStartY = 0
const voicePlayers = new Map()
const canRecord = computed(() => {
  try {
    return !!(navigator?.mediaDevices?.getUserMedia && window.MediaRecorder)
  } catch (e) {
    return false
  }
})

// ===== 删除 & 编辑模式 =====
const editMode = ref(false)
const selectedIds = ref([])
const ctxMenu = ref({ visible: false, x: 0, y: 0, msg: null })
let longPressTimer = 0
const LONG_PRESS_MS = 500

const enterEditMode = () => { editMode.value = true; selectedIds.value = [] }
const exitEditMode = () => { editMode.value = false; selectedIds.value = [] }
const toggleSelect = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}
const selectAll = () => { selectedIds.value = list.value.map(m => m.id).filter(Boolean) }
const deselectAll = () => { selectedIds.value = [] }
const selectedCount = computed(() => selectedIds.value.length)

const showContextMenu = (e, m) => {
  e.preventDefault()
  if (editMode.value) return
  const clientX = e.touches?.[0]?.clientX ?? e.clientX
  const clientY = e.touches?.[0]?.clientY ?? e.clientY
  const menuW = 150; const menuH = 120
  ctxMenu.value = {
    visible: true,
    x: Math.min(clientX, window.innerWidth - menuW - 8),
    y: Math.min(clientY, window.innerHeight - menuH - 8),
    msg: m
  }
}
const hideContextMenu = () => { ctxMenu.value = { visible: false, x: 0, y: 0, msg: null } }
const onTouchStartCtx = (e, m) => {
  longPressTimer = window.setTimeout(() => showContextMenu(e, m), LONG_PRESS_MS)
}
const onTouchEndCtx = () => { if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = 0 } }
const onTouchMoveCtx = () => { if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = 0 } }

const copyMessageText = () => {
  const m = ctxMenu.value.msg
  if (!m) return
  const text = String(m.content || '')
  navigator.clipboard?.writeText(text).then(() => ElMessage.success('已复制')).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = text; document.body.appendChild(ta); ta.select()
    document.execCommand('copy'); document.body.removeChild(ta)
    ElMessage.success('已复制')
  })
  hideContextMenu()
}

const deleteSingle = async () => {
  const m = ctxMenu.value.msg
  hideContextMenu()
  if (!m?.id) return
  try {
    await ElMessageBox.confirm('确定删除这条消息吗？', '提示', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await http.delete(`/api/messages/${m.id}`)
    list.value = list.value.filter(x => x?.id !== m.id)
    ElMessage.success('已删除')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const batchDelete = async () => {
  if (!selectedIds.value.length) { ElMessage.info('请至少选择一条消息'); return }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条消息吗？`, '删除确认', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await http.post('/api/messages/batch-delete', selectedIds.value)
    const set = new Set(selectedIds.value)
    list.value = list.value.filter(x => !set.has(x.id))
    exitEditMode()
    ElMessage.success(`已删除 ${selectedIds.value.length} 条消息`)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '操作失败')
  }
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空全部聊天记录吗？此操作不可恢复！', '清空聊天记录', { confirmButtonText: '清空全部', cancelButtonText: '取消', type: 'warning' })
    await http.delete('/api/messages')
    list.value = []
    ElMessage.success('聊天记录已清空')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '操作失败')
  }
}

const onWsDelete = (e) => {
  const id = e?.detail
  if (!id) return
  list.value = list.value.filter(x => x?.id !== id)
}
const onWsDeleteBatch = (e) => {
  const ids = e?.detail
  if (!Array.isArray(ids) || !ids.length) return
  const set = new Set(ids)
  list.value = list.value.filter(x => !set.has(x.id))
}
const onWsDeleteAll = () => { list.value = [] }

// ===== 时间分隔线 =====
const showTimeDivider = (idx) => {
  if (idx === 0) return true
  const prev = list.value[idx - 1]
  const cur = list.value[idx]
  if (!prev || !cur) return true
  const pt = prev.createdAt ? new Date(prev.createdAt) : null
  const ct = cur.createdAt ? new Date(cur.createdAt) : null
  if (!pt || !ct) return true
  return pt.getFullYear() !== ct.getFullYear() || pt.getMonth() !== ct.getMonth() || pt.getDate() !== ct.getDate()
}
const fmtTime = (t) => {
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

const emojiOptions = ['😀', '🥰', '😘', '😌', '😎', '🥺', '😭', '🤗', '🤭', '😴', '❤️', '💞', '💐', '🎉', '🎂', '🌙', '✨', '🍀', '🧸', '📌']
const quickOptions = ['想你了', '在干嘛', '辛苦啦', '今天也很喜欢你', '我爱你', '抱抱', '晚安', '早安', '路上小心', '我到啦']
const stickers = ref([])
const stickerLoading = ref(false)
const stickerUploading = ref(false)
const imgInputRef = ref(null)
const voiceInputRef = ref(null)
const previewing = ref(false)
const previewSrc = ref('')

const firstId = computed(() => (list.value.length ? list.value[0].id : null))

const scrollToBottom = async () => {
  await nextTick()
  window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'smooth' })
}

const msgTime = (m) => {
  const t = m?.createdAt
  const n = t ? new Date(t).getTime() : 0
  if (Number.isFinite(n) && n > 0) return n
  const id = typeof m?.id === 'number' ? m.id : Number(m?.id || 0)
  return Number.isFinite(id) ? id : 0
}

const mergeMessage = (m) => {
  if (!m) return
  if (!m.id) return
  const idx = list.value.findIndex((x) => x?.id === m.id)
  if (idx >= 0) {
    list.value[idx] = { ...list.value[idx], ...m }
    return
  }
  list.value = [...list.value, m].sort((a, b) => {
    const ta = msgTime(a)
    const tb = msgTime(b)
    if (ta !== tb) return ta - tb
    const ia = a?.id != null ? Number(a.id) : 0
    const ib = b?.id != null ? Number(b.id) : 0
    return ia - ib
  })
}

const openPreview = (u) => {
  const url = assetUrl(u)
  if (!url) return
  previewSrc.value = url
  previewing.value = true
}

const closePreview = () => {
  previewing.value = false
  previewSrc.value = ''
}

const load = async (silent) => {
  try {
    loading.value = true
    const res = await http.get('/api/messages', { params: { limit: 40 } })
    list.value = Array.isArray(res) ? res : []
    hasMore.value = (list.value?.length || 0) >= 40
    if (!silent) ElMessage.success('已刷新')
    await ackVisibleBatch()
    await dashboard.fetch()
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!hasMore.value || loadingMore.value) return
  try {
    loadingMore.value = true
    const res = await http.get('/api/messages', { params: { beforeId: firstId.value || undefined, limit: 40 } })
    const older = Array.isArray(res) ? res : []
    hasMore.value = older.length >= 40
    const existing = new Set(list.value.map((x) => x.id))
    const merged = [...older.filter((x) => x?.id && !existing.has(x.id)), ...list.value].sort((a, b) => msgTime(a) - msgTime(b))
    list.value = merged
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loadingMore.value = false
  }
}

const getTextareaEl = () => {
  const r = inputRef.value
  const el = r?.textarea || r?.input || r?.$el?.querySelector?.('textarea') || r?.$el?.querySelector?.('input')
  return el || null
}

const insertToInput = async (text, focus) => {
  const t = String(text || '')
  if (!t) return
  msgType.value = 'TEXT'
  const el = getTextareaEl()
  const s = String(input.value || '')
  if (!el || typeof el.selectionStart !== 'number' || typeof el.selectionEnd !== 'number') {
    input.value = s + t
    if (focus !== false) await nextTick(() => focusInput())
    return
  }
  const start = el.selectionStart
  const end = el.selectionEnd
  input.value = s.slice(0, start) + t + s.slice(end)
  await nextTick()
  const el2 = getTextareaEl()
  if (focus !== false) focusInput()
  try {
    el2?.setSelectionRange?.(start + t.length, start + t.length)
  } catch (e) {}
}

const pickEmoji = (e) => {
  insertToInput(String(e || ''), false)
}

const pickQuick = (t) => {
  const s = String(input.value || '')
  const piece = String(t || '')
  const add = s && !/\s$/.test(s) ? ' ' + piece : piece
  insertToInput(add, true)
}

const pickSticker = (s) => {
  if (!s?.url) return
  msgType.value = 'TEXT'
  draftStickerUrl.value = s.url
  nextTick(() => focusInput())
}

const sendOne = async (type, content) => {
  const localId = `local_${Date.now()}_${Math.random().toString(16).slice(2)}`
  const pending = {
    _localId: localId,
    senderId: myId.value,
    type,
    content,
    createdAt: new Date().toISOString(),
    delivered: false,
    read: false,
    recalled: false,
    sending: true,
    sendFailed: false
  }
  list.value = [...list.value, pending].sort((a, b) => msgTime(a) - msgTime(b))
  scrollToBottom()
  try {
    const m = await http.post('/api/messages', { type, content })
    list.value = list.value.filter((x) => x?._localId !== localId)
    mergeMessage(m)
    scrollToBottom()
    dashboard.fetch().catch(() => {})
    sendTyping(false)
    return true
  } catch (e) {
    list.value = list.value.map((x) => {
      if (x?._localId === localId) return { ...x, sending: false, sendFailed: true }
      return x
    })
    ElMessage.error(e?.message || '发送失败')
    return false
  }
}

const send = async () => {
  if (sending.value) return
  const raw = String(input.value || '')
  const text = raw.trim()
  const typeNow = String(msgType.value || 'TEXT')

  if (typeNow !== 'TEXT') {
    if (!text) return
    try {
      sending.value = true
      await sendOne(typeNow, text)
    } finally {
      input.value = ''
      msgType.value = 'TEXT'
      sending.value = false
    }
    return
  }

  const queue = []
  if (text) queue.push({ type: 'TEXT', content: text })
  if (String(draftStickerUrl.value || '').trim()) queue.push({ type: 'STICKER', content: String(draftStickerUrl.value || '').trim() })
  if (!queue.length) return

  try {
    sending.value = true
    for (const it of queue) {
      const ok = await sendOne(it.type, it.content)
      if (!ok) return
    }
    input.value = ''
    draftStickerUrl.value = ''
    msgType.value = 'TEXT'
  } finally {
    sending.value = false
  }
}

const retrySend = async (m) => {
  if (!m?.sendFailed || !m?._localId) return
  const localId = m._localId
  try {
    list.value = list.value.map((x) => {
      if (x?._localId === localId) return { ...x, sending: true, sendFailed: false }
      return x
    })
    const res = await http.post('/api/messages', { type: m.type, content: m.content })
    list.value = list.value.filter((x) => x?._localId !== localId)
    mergeMessage(res)
    scrollToBottom()
    dashboard.fetch().catch(() => {})
  } catch (e) {
    list.value = list.value.map((x) => {
      if (x?._localId === localId) return { ...x, sending: false, sendFailed: true }
      return x
    })
    ElMessage.error(e?.message || '重试失败')
  }
}

const onWsMessage = async (e) => {
  const m = e?.detail
  mergeMessage(m)
  if (m?.senderId && myId.value && m.senderId !== myId.value) {
    if (m.id) {
      await ackDelivered([m.id])
      await ackRead([m.id])
    }
  }
  scrollToBottom()
}

const markMyRead = (ids) => {
  const uid = myId.value
  if (!uid) return
  const set = new Set((ids || []).filter(Boolean))
  if (!set.size) return
  list.value = list.value.map((x) => {
    if (x?.senderId === uid && x?.id && set.has(x.id)) {
      return { ...x, read: true }
    }
    return x
  })
}

const markMyDelivered = (ids) => {
  const uid = myId.value
  if (!uid) return
  const set = new Set((ids || []).filter(Boolean))
  if (!set.size) return
  list.value = list.value.map((x) => {
    if (x?.senderId === uid && x?.id && set.has(x.id)) {
      return { ...x, delivered: true }
    }
    return x
  })
}

const onWsRead = (e) => {
  const data = e?.detail
  const ids = Array.isArray(data) ? data : data ? [data] : []
  markMyRead(ids)
}

const onWsDelivered = (e) => {
  const data = e?.detail
  const ids = Array.isArray(data) ? data : data ? [data] : []
  markMyDelivered(ids)
}

const onWsRecall = (e) => {
  const id = e?.detail
  if (!id) return
  list.value = list.value.map((x) => {
    if (x?.id === id) {
      return { ...x, recalled: true, content: '' }
    }
    return x
  })
}

const onWsReadUpTo = (e) => {
  const upTo = Number(e?.detail || 0) || 0
  if (!upTo) return
  const uid = myId.value
  if (!uid) return
  list.value = list.value.map((x) => {
    if (x?.senderId === uid && x?.id && x.id <= upTo) {
      return { ...x, read: true }
    }
    return x
  })
}

const onWsDeliveredUpTo = (e) => {
  const upTo = Number(e?.detail || 0) || 0
  if (!upTo) return
  const uid = myId.value
  if (!uid) return
  list.value = list.value.map((x) => {
    if (x?.senderId === uid && x?.id && x.id <= upTo) {
      return { ...x, delivered: true }
    }
    return x
  })
}

const onWsCursor = (e) => {
  const data = e?.detail
  if (!data?.deviceId || data.deviceId !== deviceId) return
  const id = Number(data.lastReadId || 0) || 0
  if (!id) return
  cursorLastReadId.value = id
}

const onWsTyping = (e) => {
  const data = e?.detail
  const uid = myId.value
  if (!uid) return
  if (data?.from && data.from === uid) return
  if (data?.typing === true) {
    partnerTyping.value = true
    if (typingTimer) window.clearTimeout(typingTimer)
    typingTimer = window.setTimeout(() => {
      partnerTyping.value = false
    }, 2200)
  } else {
    partnerTyping.value = false
  }
}

const canRecall = (m) => {
  if (!m?.id) return false
  if (m.senderId !== myId.value) return false
  if (m.recalled) return false
  const t = m.createdAt ? new Date(m.createdAt).getTime() : 0
  if (!t) return false
  return Date.now() - t <= 2 * 60 * 1000
}

const recall = async (m) => {
  if (!m?.id) return
  try {
    await ElMessageBox.confirm('确认撤回这条消息吗？', '撤回消息', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.put(`/api/messages/${m.id}/recall`)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '操作失败')
  }
}

const sendTyping = (typing) => {
  try {
    if (typeof window.__dzChatSend !== 'function') return
    window.__dzChatSend({ event: 'TYPING', typing: typing === true })
  } catch (e) {}
}

const onInput = () => {
  if (typingStopTimer) window.clearTimeout(typingStopTimer)
  sendTyping(true)
  typingStopTimer = window.setTimeout(() => {
    sendTyping(false)
  }, 900)
}

const focusInput = () => {
  try {
    inputRef.value?.focus?.()
  } catch (e) {}
}

const loadStickers = async () => {
  try {
    stickerLoading.value = true
    const res = await http.get('/api/stickers', { params: { limit: 200 } })
    stickers.value = Array.isArray(res) ? res : []
  } catch (e) {
    stickers.value = []
  } finally {
    stickerLoading.value = false
  }
}

const uploadChatImage = async (file) => {
  const fd = new FormData()
  fd.append('files', file)
  const res = await http.post('/api/uploads/images', fd, { params: { biz: 'chat' } })
  const one = Array.isArray(res) ? res[0] : null
  return one?.url || ''
}

const uploadChatAudio = async (file) => {
  const fd = new FormData()
  fd.append('files', file)
  const res = await http.post('/api/uploads/audios', fd, { params: { biz: 'chat' } })
  const one = Array.isArray(res) ? res[0] : null
  return one?.url || ''
}

const wsSend = (obj) => {
  try {
    if (typeof window.__dzChatSend !== 'function') return false
    return window.__dzChatSend(obj)
  } catch (e) {
    return false
  }
}

const ackDelivered = async (ids) => {
  const arr = (ids || []).filter(Boolean)
  if (!arr.length) return
  if (wsSend({ event: arr.length === 1 ? 'DELIVERED' : 'DELIVERED_BATCH', id: arr[0], ids: arr })) {
    return
  }
  const maxId = Math.max(...arr)
  await http.put(`/api/messages/delivered-up-to/${maxId}`)
  dashboard.fetch().catch(() => {})
}

const ackRead = async (ids) => {
  const arr = (ids || []).filter(Boolean)
  if (!arr.length) return
  if (wsSend({ event: arr.length === 1 ? 'READ' : 'READ_BATCH', id: arr[0], ids: arr, deviceId })) {
    dashboard.fetch().catch(() => {})
    return
  }
  const maxId = Math.max(...arr)
  await http.put(`/api/messages/read-up-to/${maxId}`)
  await http.put(`/api/messages/cursor/${maxId}`, null, { params: { deviceId } }).catch(() => {})
  dashboard.fetch().catch(() => {})
}

const ackVisibleBatch = async () => {
  const uid = myId.value
  if (!uid) return
  const incoming = list.value.filter((m) => m && m.senderId && m.senderId !== uid && !m.recalled)
  if (!incoming.length) return
  const ids = incoming.map((m) => m.id).filter(Boolean)
  const unreadIds = incoming.filter((m) => m.read !== true).map((m) => m.id).filter(Boolean)
  await ackDelivered(ids).catch(() => {})
  if (unreadIds.length) await ackRead(unreadIds).catch(() => {})
}

const restoreCursorScroll = async () => {
  try {
    const lastReadId = await http.get('/api/messages/cursor', { params: { deviceId } })
    if (!lastReadId || !list.value.length) return
    cursorLastReadId.value = lastReadId
    const el = document.getElementById(`msg_${lastReadId}`)
    if (el) {
      el.scrollIntoView({ block: 'center' })
      return true
    }
  } catch (e) {}
  return false
}

const chooseImage = async (ev) => {
  const f = ev?.target?.files?.[0]
  ev.target.value = ''
  if (!f) return
  try {
    stickerUploading.value = true
    const url = await uploadChatImage(f)
    if (!url) throw new Error('上传失败')
    msgType.value = 'IMAGE'
    input.value = url
    await send()
  } catch (e) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    stickerUploading.value = false
  }
}

const chooseVoice = async (ev) => {
  const f = ev?.target?.files?.[0]
  ev.target.value = ''
  if (!f) return
  try {
    stickerUploading.value = true
    const url = await uploadChatAudio(f)
    if (!url) throw new Error('上传失败')
    msgType.value = 'VOICE'
    input.value = url
    await send()
  } catch (e) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    stickerUploading.value = false
  }
}

const triggerChooseImage = () => {
  try {
    imgInputRef.value?.click?.()
  } catch (e) {}
}

const triggerChooseVoice = () => {
  try {
    voiceInputRef.value?.click?.()
  } catch (e) {}
}

const addSticker = async (ev) => {
  const f = ev?.target?.files?.[0]
  ev.target.value = ''
  if (!f) return
  try {
    stickerUploading.value = true
    const url = await uploadChatImage(f)
    if (!url) throw new Error('上传失败')
    await http.post('/api/stickers', { url })
    await loadStickers()
    ElMessage.success('已添加')
  } catch (e) {
    ElMessage.error(e?.message || '添加失败')
  } finally {
    stickerUploading.value = false
  }
}

const removeSticker = async (it) => {
  if (!it?.id) return
  try {
    await http.delete(`/api/stickers/${it.id}`)
    stickers.value = stickers.value.filter((x) => x.id !== it.id)
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const statusText = (m) => {
  if (!m || m.senderId !== myId.value) return ''
  if (m.recalled) return ''
  if (m.read) return '已读'
  if (m.delivered) return '已送达'
  return '未送达'
}

const toggleVoice = async (m) => {
  const id = m?.id
  if (!id) return
  try {
    const url = assetUrl(m.content)
    if (!url) return
    for (const [k, p] of voicePlayers.entries()) {
      if (k !== id && p && typeof p.pause === 'function') p.pause()
    }
    let p = voicePlayers.get(id)
    if (!p) {
      p = new Audio(url)
      p.preload = 'none'
      voicePlayers.set(id, p)
    }
    if (p.paused) {
      await p.play()
    } else {
      p.pause()
    }
  } catch (e) {
    ElMessage.error('无法播放，已为你打开文件')
    openUrl(m?.content)
  }
}

const pickRecordMime = () => {
  const list = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/ogg']
  for (const t of list) {
    try {
      if (window.MediaRecorder && typeof MediaRecorder.isTypeSupported === 'function' && MediaRecorder.isTypeSupported(t)) return t
    } catch (e) {}
  }
  return ''
}

const startRecord = async (ev) => {
  if (recording.value) return
  ev?.preventDefault?.()
  if (!canRecord.value) {
    ElMessage.error('当前浏览器不支持按住录音，可使用“语音”上传')
    return
  }
  try {
    recordCanceled = false
    recordCanceling.value = false
    recordStartY = typeof ev?.clientY === 'number' ? ev.clientY : 0
    recordChunks = []
    recordSec.value = 0
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recordStream = stream
    const mimeType = pickRecordMime()
    mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e?.data && e.data.size > 0) recordChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      const chunks = recordChunks
      recordChunks = []
      const type = mediaRecorder?.mimeType || (chunks[0] && chunks[0].type) || 'audio/webm'
      const blob = new Blob(chunks, { type })
      try {
        if (recordCanceled) return
        if (blob.size < 600) {
          ElMessage.info('录音太短')
          return
        }
        const ext = type.includes('ogg') ? 'ogg' : 'webm'
        const file = new File([blob], `voice_${Date.now()}.${ext}`, { type })
        const url = await uploadChatAudio(file)
        if (!url) throw new Error('上传失败')
        msgType.value = 'VOICE'
        input.value = url
        await send()
      } catch (e) {
        ElMessage.error(e?.message || '发送失败')
      }
    }
    mediaRecorder.start()
    recording.value = true
    if (recordTimer) window.clearInterval(recordTimer)
    recordTimer = window.setInterval(() => {
      recordSec.value += 1
    }, 1000)
  } catch (e) {
    ElMessage.error(e?.message || '无法开始录音')
    stopRecord(true).catch(() => {})
  }
}

const onRecordMove = (ev) => {
  if (!recording.value) return
  const y = typeof ev?.clientY === 'number' ? ev.clientY : 0
  if (!recordStartY || !y) return
  recordCanceling.value = recordStartY - y > 60
}

const endRecord = async () => {
  await stopRecord(recordCanceling.value)
  recordCanceling.value = false
  recordStartY = 0
}

const stopRecord = async (cancel) => {
  if (recordTimer) {
    window.clearInterval(recordTimer)
    recordTimer = 0
  }
  const wasRecording = recording.value
  recording.value = false
  recordCanceled = cancel === true
  recordCanceling.value = false
  try {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
    }
  } catch (e) {}
  if (cancel) {
    recordChunks = []
  }
  try {
    if (recordStream) {
      recordStream.getTracks().forEach((t) => t.stop())
      recordStream = null
    }
  } catch (e) {}
  mediaRecorder = null
  if (wasRecording && cancel) {
    ElMessage.info('已取消')
  }
}

const onChatVisible = () => {
  if (!document.hidden) {
    ackVisibleBatch().catch(() => {})
    dashboard.fetch().catch(() => {})
  }
}

onMounted(async () => {
  if (!auth.user) {
    await auth.fetchMe().catch(() => {})
  }
  await loadProfile()
  load(true).then(async () => {
    const ok = await restoreCursorScroll()
    if (!ok) scrollToBottom()
  })
  loadStickers()
  document.addEventListener('visibilitychange', onChatVisible)
  window.addEventListener('dz_chat_message', onWsMessage)
  window.addEventListener('dz_chat_read', onWsRead)
  window.addEventListener('dz_chat_delivered', onWsDelivered)
  window.addEventListener('dz_chat_read_up_to', onWsReadUpTo)
  window.addEventListener('dz_chat_delivered_up_to', onWsDeliveredUpTo)
  window.addEventListener('dz_chat_recall', onWsRecall)
  window.addEventListener('dz_chat_typing', onWsTyping)
  window.addEventListener('dz_chat_cursor', onWsCursor)
  window.addEventListener('dz_chat_delete', onWsDelete)
  window.addEventListener('dz_chat_delete_batch', onWsDeleteBatch)
  window.addEventListener('dz_chat_delete_all', onWsDeleteAll)
  document.addEventListener('click', hideContextMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', onChatVisible)
  window.removeEventListener('dz_chat_message', onWsMessage)
  window.removeEventListener('dz_chat_read', onWsRead)
  window.removeEventListener('dz_chat_delivered', onWsDelivered)
  window.removeEventListener('dz_chat_read_up_to', onWsReadUpTo)
  window.removeEventListener('dz_chat_delivered_up_to', onWsDeliveredUpTo)
  window.removeEventListener('dz_chat_recall', onWsRecall)
  window.removeEventListener('dz_chat_typing', onWsTyping)
  window.removeEventListener('dz_chat_cursor', onWsCursor)
  window.removeEventListener('dz_chat_delete', onWsDelete)
  window.removeEventListener('dz_chat_delete_batch', onWsDeleteBatch)
  window.removeEventListener('dz_chat_delete_all', onWsDeleteAll)
  document.removeEventListener('click', hideContextMenu)
  sendTyping(false)
  if (typingTimer) window.clearTimeout(typingTimer)
  if (typingStopTimer) window.clearTimeout(typingStopTimer)
  stopRecord(true).catch(() => {})
  for (const p of voicePlayers.values()) {
    try {
      if (p && typeof p.pause === 'function') p.pause()
    } catch (e) {}
  }
  voicePlayers.clear()
})
</script>

<template>
  <div class="stack">
    <div class="head">
      <div class="head-row">
        <button class="navbtn" type="button" @click="router.push('/app/home')">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </button>
        <div class="brand">
          <span class="brand-ico">
            <svg viewBox="0 0 24 24" focusable="false"><path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41.81 4.5 2.09C12.09 4.81 13.76 4 15.5 4 18 4 20 6 20 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
          </span>
          <span class="brand-text">私密聊天</span>
          <span v-if="Number(dashboard.badges.unreadMessages || 0) > 0" class="unread">{{ Number(dashboard.badges.unreadMessages || 0) > 99 ? '99+' : Number(dashboard.badges.unreadMessages || 0) }}</span>
        </div>
        <div class="head-actions">
          <button class="actbtn" type="button" :disabled="loading" @click="load()" title="刷新">
            <el-icon :size="16"><RefreshRight /></el-icon>
          </button>
          <template v-if="!editMode">
            <button class="actbtn" type="button" @click="enterEditMode" title="批量管理">
              <el-icon :size="16"><Edit /></el-icon>
            </button>
            <button class="actbtn del" type="button" @click="clearAll" title="清空记录">
              <el-icon :size="16"><Delete /></el-icon>
            </button>
          </template>
          <template v-else>
            <button class="actbtn warn" type="button" @click="exitEditMode" title="取消">
              <el-icon :size="16"><Close /></el-icon>
            </button>
          </template>
        </div>
      </div>
    </div>

    <el-card class="app-card" shadow="never">
      <template #header>
        <div class="dz-cardhead">
          <div class="dz-cardtitle">聊天记录</div>
        </div>
      </template>

      <div v-if="!list.length" class="empty app-muted">还没有消息，发一句吧</div>
      <div v-else class="msgs" @click="hideContextMenu">
        <button v-if="hasMore" class="loadmore" type="button" :disabled="loadingMore" @click="loadMore">
          <el-icon v-if="loadingMore" class="spin" :size="16"><RefreshRight /></el-icon>
          <span>{{ loadingMore ? '加载中…' : '加载更早消息' }}</span>
        </button>

        <template v-for="(m, idx) in list" :key="m?._localId || m.id">
          <div v-if="showTimeDivider(idx)" class="time-divider">
            <span>{{ fmtTime(m.createdAt) }}</span>
          </div>

          <div
            :id="m?.id ? `msg_${m.id}` : null"
            class="msg"
            :class="{ mine: m.senderId === myId, 'edit-mode': editMode }"
            @contextmenu="(e) => showContextMenu(e, m)"
            @touchstart="(e) => onTouchStartCtx(e, m)"
            @touchend="onTouchEndCtx"
            @touchmove="onTouchMoveCtx"
          >
            <div v-if="editMode" class="msg-check" @click="m.id && toggleSelect(m.id)">
              <el-icon :size="20" :color="m.id && selectedIds.includes(m.id) ? '#6366f1' : '#cbd5e1'">
                <CircleCheckFilled v-if="m.id && selectedIds.includes(m.id)" />
                <CircleCheck v-else />
              </el-icon>
            </div>

            <div v-if="cursorLastReadId && m?.id === cursorLastReadId" class="cursor app-muted">上次看到这里</div>
            <div v-if="m.recalled" class="recalled">{{ m.senderId === myId ? '你撤回了一条消息' : '对方撤回了一条消息' }}</div>
            <div v-else class="msgrow">
              <div class="avatar" :class="{ mine: m.senderId === myId }">
                <img v-if="(m.senderId === myId ? avatarUrlFor(meUser) : avatarUrlFor(partnerUser))" :src="m.senderId === myId ? avatarUrlFor(meUser) : avatarUrlFor(partnerUser)" alt="" />
                <span v-else>{{ m.senderId === myId ? initials(meUser) : initials(partnerUser) }}</span>
              </div>

              <div class="body" :class="{ mine: m.senderId === myId }">
                <div class="sender-name app-muted">{{ m.senderId === myId ? (meUser?.nickname || '我') : (partnerUser?.nickname || 'TA') }}</div>
                <div class="meta app-muted">
                  <span>{{ m.createdAt ? new Date(m.createdAt).toLocaleString() : '' }}</span>
                  <span v-if="m.senderId === myId && !m.recalled" class="ticks" :class="{ on: m.read || m.delivered }">
                    <span v-if="m.read">✓✓</span>
                    <span v-else>✓</span>
                  </span>
                  <button v-if="canRecall(m)" class="recall" type="button" @click="recall(m)">撤回</button>
                </div>
                <div class="bubble" :class="[m.senderId === myId ? 'mine' : 'other', { emoji: m.type === 'EMOJI', media: m.type === 'IMAGE' || m.type === 'STICKER' || m.type === 'VOICE' }]">
                  <template v-if="m.type === 'IMAGE' || m.type === 'STICKER'">
                    <button class="imgwrap" type="button" @click="openPreview(m.content)">
                      <img class="img" :src="assetUrl(m.content)" alt="" />
                      <span class="img-zoom" aria-hidden="true">⤢</span>
                    </button>
                    <el-tooltip v-if="m.senderId === myId && m.sendFailed" content="发送失败，点击重试" placement="top">
                      <button class="fail" type="button" @click.stop="retrySend(m)">!</button>
                    </el-tooltip>
                  </template>
                  <template v-else-if="m.type === 'VOICE'">
                    <button class="voice" type="button" @click="toggleVoice(m)">
                      <span class="voice-ico">▶</span>
                      <span class="voice-text">点击播放</span>
                    </button>
                    <el-tooltip v-if="m.senderId === myId && m.sendFailed" content="发送失败，点击重试" placement="top">
                      <button class="fail" type="button" @click.stop="retrySend(m)">!</button>
                    </el-tooltip>
                  </template>
                  <template v-else>
                    {{ m.content }}
                    <el-tooltip v-if="m.senderId === myId && m.sendFailed" content="发送失败，点击重试" placement="top">
                      <button class="fail" type="button" @click.stop="retrySend(m)">!</button>
                    </el-tooltip>
                  </template>
                </div>

              </div>
            </div>
          </div>
        </template>
      </div>
    </el-card>

    <!-- 编辑模式底部操作栏 -->
    <Teleport to="body">
      <div v-if="editMode" class="edit-bar">
        <div class="edit-bar-inner">
          <button class="ebtn" type="button" @click="selectAll">
            <el-icon :size="14"><Select /></el-icon>
            <span>全选</span>
          </button>
          <button class="ebtn" type="button" @click="deselectAll">
            <span>取消</span>
          </button>
          <span class="ebadge">{{ selectedCount }} 条已选</span>
          <button class="ebtn danger" type="button" :disabled="selectedCount === 0" @click="batchDelete">
            <el-icon :size="14"><Delete /></el-icon>
            <span>删除</span>
          </button>
        </div>
      </div>
    </Teleport>

    <!-- 上下文菜单 -->
    <Teleport to="body">
      <div v-if="ctxMenu.visible" class="ctx-overlay" @click="hideContextMenu">
        <div class="ctx-menu" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }" @click.stop>
          <div class="ctx-item" @click="copyMessageText">
            <el-icon :size="14"><CopyDocument /></el-icon>
            <span>复制文本</span>
          </div>
          <div class="ctx-divider"></div>
          <div class="ctx-item danger" @click="deleteSingle">
            <el-icon :size="14"><Delete /></el-icon>
            <span>删除本条</span>
          </div>
        </div>
      </div>
    </Teleport>

    <el-dialog v-model="previewing" width="96%" top="3vh" :close-on-click-modal="true" @closed="closePreview">
      <img v-if="previewSrc" class="preview-img" :src="previewSrc" alt="" />
    </el-dialog>

    <div class="composer app-card">
      <div v-if="partnerTyping" class="typing app-muted">对方正在输入…</div>
      <div class="toolbar">
        <div class="tools">
          <el-tooltip content="文字" placement="top">
            <button class="toolbtn" type="button" @click="msgType = 'TEXT'; focusInput()">
              <span class="toolico">Aa</span>
            </button>
          </el-tooltip>

          <el-dropdown trigger="click" :hide-on-click="false">
            <button class="toolbtn" type="button">
              <el-tooltip content="表情" placement="top">
                <span class="toolico">😊</span>
              </el-tooltip>
            </button>
            <template #dropdown>
              <div class="emojis">
                <button v-for="e in emojiOptions" :key="e" class="emoji" type="button" @click="pickEmoji(e)">{{ e }}</button>
              </div>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click">
            <button class="toolbtn" type="button">
              <el-tooltip content="快捷语" placement="top">
                <span class="toolico">💬</span>
              </el-tooltip>
            </button>
            <template #dropdown>
              <div class="quick">
                <div class="quick-hint app-muted">点击插入到输入框</div>
                <button v-for="t in quickOptions" :key="t" class="q" type="button" @click="pickQuick(t)">{{ t }}</button>
              </div>
            </template>
          </el-dropdown>

          <el-dropdown trigger="click">
            <button class="toolbtn" type="button" :disabled="stickerLoading">
              <el-tooltip content="表情包" placement="top">
                <span class="toolico">🧸</span>
              </el-tooltip>
            </button>
            <template #dropdown>
              <div class="stickers">
                <div class="stick-top">
                  <label class="add">
                    <input class="file" type="file" accept="image/*" @change="addSticker" />
                    <span>添加</span>
                  </label>
                </div>
                <div v-if="!stickers.length" class="app-muted">还没有表情包</div>
                <div v-else class="stick-grid">
                  <button v-for="s in stickers" :key="s.id" class="stick" type="button" @click="pickSticker(s)">
                    <img :src="assetUrl(s.url)" alt="" />
                    <button class="stick-del" type="button" @click.stop="removeSticker(s)">×</button>
                  </button>
                </div>
              </div>
            </template>
          </el-dropdown>

          <input ref="imgInputRef" class="file" type="file" accept="image/*" @change="chooseImage" />
          <el-tooltip content="图片" placement="top">
            <button class="toolbtn" type="button" :disabled="stickerUploading" @click="triggerChooseImage">
              <el-icon :size="18"><Picture /></el-icon>
            </button>
          </el-tooltip>

          <input ref="voiceInputRef" class="file" type="file" accept="audio/*" @change="chooseVoice" />
          <el-tooltip content="语音" placement="top">
            <button class="toolbtn" type="button" :disabled="stickerUploading" @click="triggerChooseVoice">
              <el-icon :size="18"><Microphone /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
      <div class="recordbar">
        <button
          v-if="canRecord"
          class="hold"
          type="button"
          :class="{ recording, canceling: recordCanceling }"
          @pointerdown="startRecord"
          @pointermove="onRecordMove"
          @pointerup="endRecord"
          @pointercancel="() => stopRecord(true)"
          @pointerleave="() => stopRecord(true)"
        >
          <span v-if="recording && recordCanceling" class="hold-text">松开取消</span>
          <span v-else-if="recording" class="hold-text">录音中 {{ recordSec }}s · 松开发送</span>
          <span v-else class="hold-text">按住 说话</span>
          <span v-if="recording" class="waves" aria-hidden="true">
            <span class="w w1" />
            <span class="w w2" />
            <span class="w w3" />
          </span>
        </button>
        <div v-else class="app-muted" style="font-size: 12px">当前浏览器不支持按住录音，可使用“语音”上传</div>
      </div>
      <div class="inputbar">
        <div v-if="draftStickerUrl" class="draftrow">
          <div class="draftpill">
            <img class="draftimg" :src="assetUrl(draftStickerUrl)" alt="" />
            <span class="drafttext">已选表情包</span>
            <button class="draftx" type="button" @click="draftStickerUrl = ''">×</button>
          </div>
        </div>
        <el-input
          ref="inputRef"
          v-model="input"
          class="chatinput"
          type="textarea"
          :rows="2"
          maxlength="500"
          placeholder="和 TA 说点悄悄话吧～"
          @input="onInput"
          @blur="() => sendTyping(false)"
          @keyup.enter.exact.prevent="send"
        />
        <div class="sendcol">
          <button class="sendbtn" type="button" :disabled="!canSend" :class="{ sending }" @click="send">
            <span v-if="sending" class="sendspin" aria-hidden="true" />
            <span v-else class="sendheart" aria-hidden="true">
              <svg viewBox="0 0 24 24" focusable="false">
                <path
                  fill="currentColor"
                  d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 6 4 4 6.5 4c1.74 0 3.41.81 4.5 2.09C12.09 4.81 13.76 4 15.5 4 18 4 20 6 20 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                />
              </svg>
            </span>
            <span class="sendtext">发送</span>
          </button>
          <div class="count app-muted">{{ inputLen }}/500</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.head {
  padding: 10px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08), rgba(139, 92, 246, 0.08), rgba(255, 255, 255, 0.88));
  border: 1px solid rgba(139, 92, 246, 0.1);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}
.head-row {
  display: flex;
  align-items: center;
  gap: 10px;
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
  flex: none;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}
.navbtn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
@media (hover: hover) {
  .navbtn:hover:not(:disabled) {
    transform: translateY(-1px);
    border-color: rgba(99, 102, 241, 0.22);
    box-shadow: 0 14px 30px rgba(99, 102, 241, 0.12);
    background: rgba(255, 255, 255, 0.86);
  }
}
.brand {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(99, 102, 241, 0.14);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  box-shadow: 0 14px 30px rgba(99, 102, 241, 0.08);
  min-width: 0;
}
.head-actions {
  display: inline-flex;
  gap: 6px;
  flex: none;
}
.actbtn {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  background: rgba(255, 255, 255, 0.62);
  display: grid;
  place-items: center;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.5);
  transition: all 0.16s ease;
}
.actbtn:disabled { opacity: 0.5; cursor: not-allowed; }
.actbtn:hover:not(:disabled) {
  transform: translateY(-1px);
  color: rgba(99, 102, 241, 0.88);
  border-color: rgba(99, 102, 241, 0.2);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.1);
}
.actbtn.del:hover:not(:disabled) {
  color: rgba(239, 68, 68, 0.88);
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.04);
  box-shadow: 0 8px 18px rgba(239, 68, 68, 0.1);
}
.actbtn.warn {
  color: rgba(245, 158, 11, 0.78);
  border-color: rgba(245, 158, 11, 0.16);
  background: rgba(245, 158, 11, 0.06);
}
.brand-ico {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  color: rgba(99, 102, 241, 0.92);
}
.brand-ico svg {
  width: 18px;
  height: 18px;
  display: block;
}
.brand-text {
  font-weight: 950;
  letter-spacing: 0.2px;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.95), rgba(139, 92, 246, 0.95));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
  -webkit-text-stroke: 0.35px rgba(255, 255, 255, 0.38);
}
.unread {
  margin-left: 2px;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 900;
  color: #fff;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.96), rgba(99, 102, 241, 0.96));
  box-shadow: 0 10px 18px rgba(239, 68, 68, 0.2);
}
.empty {
  padding: 14px 0;
  text-align: center;
  font-size: 13px;
}
.msgs {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.loadmore {
  position: sticky;
  top: 10px;
  align-self: center;
  z-index: 2;
  border: 1px solid rgba(99, 102, 241, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: rgba(17, 24, 39, 0.82);
  border-radius: 999px;
  padding: 8px 14px;
  font-weight: 900;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  backdrop-filter: blur(14px);
  box-shadow: 0 14px 30px rgba(99, 102, 241, 0.08);
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}
.loadmore:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
@media (hover: hover) {
  .loadmore:hover:not(:disabled) {
    transform: translateY(-1px);
    background: rgba(255, 255, 255, 0.9);
    border-color: rgba(99, 102, 241, 0.22);
    box-shadow: 0 16px 34px rgba(99, 102, 241, 0.12);
  }
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.msg {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.msgrow {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}
.msg.mine .msgrow {
  flex-direction: row-reverse;
}
.bubble {
  max-width: 72%;
  padding: 12px 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.78);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.5;
  font-weight: 800;
  position: relative;
}
.bubble.mine {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.22), rgba(139, 92, 246, 0.18));
  border-color: rgba(99, 102, 241, 0.18);
  border-radius: 16px 16px 6px 16px;
}
.bubble.other {
  background: linear-gradient(135deg, #f0f1f8, #e8eaf2);
  border: 1px solid #d8dae4;
  border-radius: 16px 16px 16px 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.bubble.media {
  padding: 8px;
}
.bubble.emoji {
  font-size: 28px;
  line-height: 1.2;
  padding: 8px 10px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  overflow: hidden;
  flex: none;
  display: grid;
  place-items: center;
  font-weight: 950;
  font-size: 13px;
  color: rgba(190, 24, 93, 0.92);
  background: rgba(255, 255, 255, 0.78);
  border: 2px solid rgba(99, 102, 241, 0.22);
  box-shadow: 0 14px 30px rgba(99, 102, 241, 0.08);
  position: relative;
}
.avatar.mine {
  border-color: rgba(139, 92, 246, 0.26);
}
.avatar::after {
  content: '❤';
  position: absolute;
  right: -2px;
  bottom: -6px;
  font-size: 10px;
  color: rgba(99, 102, 241, 0.75);
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  flex: 1;
}
.body.mine {
  align-items: flex-end;
}
.sender-name {
  margin-bottom: 4px;
  font-size: 11px;
  font-weight: 800;
  padding: 0 6px;
  color: rgba(17, 24, 39, 0.45);
}
.body.mine .sender-name {
  color: rgba(99, 102, 241, 0.55);
}
.meta {
  margin-bottom: 6px;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  align-self: center;
}
.ticks {
  font-size: 12px;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.35);
}
.ticks.on {
  color: rgba(17, 24, 39, 0.5);
}
.recalled {
  align-self: center;
  padding: 6px 0;
  font-size: 12px;
  font-style: italic;
  color: rgba(17, 24, 39, 0.45);
}
.composer {
  padding: 12px;
}
.recordbar {
  margin: 10px 0;
}
.hold {
  width: 100%;
  height: 44px;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.28), rgba(139, 92, 246, 0.22));
  font-weight: 900;
  cursor: pointer;
  user-select: none;
  touch-action: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  position: relative;
  overflow: hidden;
}
.hold.recording {
  border-color: rgba(99, 102, 241, 0.26);
  box-shadow: 0 16px 34px rgba(99, 102, 241, 0.14);
}
.hold.canceling {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.2);
  color: rgba(239, 68, 68, 0.92);
}
.hold-text {
  position: relative;
  z-index: 1;
}
.waves {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  opacity: 0.9;
}
.w {
  width: 3px;
  border-radius: 999px;
  background: rgba(190, 24, 93, 0.65);
  height: 8px;
  animation: wave 0.9s ease-in-out infinite;
}
.w2 {
  animation-delay: 0.12s;
}
.w3 {
  animation-delay: 0.24s;
}
@keyframes wave {
  0%,
  100% {
    transform: scaleY(0.8);
    opacity: 0.65;
  }
  50% {
    transform: scaleY(1.9);
    opacity: 1;
  }
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}
.tools {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}
.toolbtn {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  place-items: center;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.7);
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease, color 0.16s ease, background 0.16s ease;
}
.toolbtn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.toolico {
  font-weight: 950;
  font-size: 13px;
}
@media (hover: hover) {
  .toolbtn:hover:not(:disabled) {
    transform: translateY(-1px) scale(1.02);
    border-color: rgba(99, 102, 241, 0.22);
    color: rgba(99, 102, 241, 0.92);
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 14px 30px rgba(99, 102, 241, 0.12);
  }
}
.inputbar {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.chatinput {
  flex: 1;
}
:deep(.chatinput .el-textarea__inner) {
  border-radius: 14px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
  font-weight: 800;
}
:deep(.chatinput .el-textarea__inner:focus) {
  border-color: rgba(99, 102, 241, 0.22);
  box-shadow: 0 16px 34px rgba(99, 102, 241, 0.12);
}
.sendcol {
  width: 92px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.sendbtn {
  width: 92px;
  height: 44px;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.92), rgba(139, 92, 246, 0.92));
  color: #fff;
  font-weight: 950;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
}
.sendbtn:disabled {
  filter: grayscale(0.35);
  opacity: 0.55;
  cursor: not-allowed;
}
@media (hover: hover) {
  .sendbtn:hover:not(:disabled) {
    transform: translateY(-1px) scale(1.02);
    box-shadow: 0 18px 38px rgba(99, 102, 241, 0.22);
  }
}

.draftrow {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}
.draftpill {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.16);
  background: rgba(99, 102, 241, 0.06);
}
.draftimg {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.8);
}
.drafttext {
  font-size: 12px;
  font-weight: 900;
  color: rgba(99, 102, 241, 0.92);
}
.draftx {
  margin-left: 2px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: rgba(255, 255, 255, 0.68);
  color: rgba(99, 102, 241, 0.92);
  cursor: pointer;
  display: grid;
  place-items: center;
  font-weight: 900;
  line-height: 1;
  transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
}
@media (hover: hover) {
  .draftx:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(99, 102, 241, 0.12);
    background: rgba(255, 255, 255, 0.86);
  }
}
.sendheart {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
}
.sendheart svg {
  width: 18px;
  height: 18px;
  display: block;
}
.sendspin {
  width: 16px;
  height: 16px;
  border-radius: 999px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: rgba(255, 255, 255, 1);
  animation: spin 0.9s linear infinite;
}
.sendtext {
  font-size: 13px;
}
.count {
  font-size: 11px;
}
.emojis {
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
}
.emoji {
  width: 34px;
  height: 34px;
  border: 0;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  cursor: pointer;
  font-size: 18px;
}
.quick {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.quick-hint {
  font-size: 12px;
  font-weight: 900;
}
.q {
  border: 0;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  text-align: left;
  font-weight: 700;
}
.typing {
  margin-bottom: 10px;
  font-size: 12px;
}
.cursor {
  padding: 6px 0;
  font-size: 12px;
  text-align: center;
}
.imgwrap {
  position: relative;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  display: inline-block;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
}
.imgwrap::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 14px;
  pointer-events: none;
}
.img {
  display: block;
  width: 220px;
  max-width: 62vw;
  height: auto;
}
.img-zoom {
  position: absolute;
  right: 8px;
  top: 8px;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(17, 24, 39, 0.45);
  color: #fff;
  font-weight: 900;
  opacity: 0;
  transition: opacity 0.16s ease;
  pointer-events: none;
}
@media (hover: hover) {
  .imgwrap:hover .img-zoom {
    opacity: 1;
  }
}
.fail {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 0;
  background: rgba(239, 68, 68, 0.95);
  color: #fff;
  font-weight: 950;
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 12px 20px rgba(239, 68, 68, 0.2);
}
.voice {
  width: 240px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.78);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 0 12px;
  font-weight: 900;
}
.voice-ico {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(99, 102, 241, 0.12);
  color: rgba(190, 24, 93, 0.92);
  font-size: 12px;
  flex: none;
}
.voice-text {
  font-size: 13px;
}
.recall {
  margin-left: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: rgba(239, 68, 68, 0.9);
  font-weight: 800;
}
.preview-img {
  width: 100%;
  max-height: 82vh;
  object-fit: contain;
  display: block;
  border-radius: 14px;
}
.file {
  display: none;
}
/* 时间分隔线 */
.time-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px 0;
}
.time-divider::before,
.time-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.12), transparent);
}
.time-divider span {
  font-size: 11px;
  font-weight: 800;
  color: rgba(99, 102, 241, 0.6);
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08), rgba(139, 92, 246, 0.06));
  border: 1px solid rgba(99, 102, 241, 0.1);
  padding: 5px 16px;
  border-radius: 999px;
  letter-spacing: 0.03em;
}
/* 编辑模式底部悬浮栏 */
.edit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 500;
  padding: 12px 16px;
  padding-bottom: max(12px, env(safe-area-inset-bottom));
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.94) 30%);
}
.edit-bar-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(99, 102, 241, 0.14);
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(99, 102, 241, 0.12), 0 2px 8px rgba(0,0,0,0.04);
}
.ebtn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  font-weight: 800;
  color: rgba(17, 24, 39, 0.7);
  cursor: pointer;
  transition: all 0.16s ease;
}
.ebtn:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.22);
  background: #fff;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.1);
}
.ebtn:disabled { opacity: 0.45; cursor: not-allowed; }
.ebtn.danger {
  color: rgba(239, 68, 68, 0.78);
  border-color: rgba(239, 68, 68, 0.14);
  background: rgba(239, 68, 68, 0.04);
}
.ebtn.danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.22);
  box-shadow: 0 8px 20px rgba(239, 68, 68, 0.1);
}
.ebadge {
  flex: 1;
  text-align: center;
  font-size: 13px;
  font-weight: 800;
  color: rgba(99, 102, 241, 0.7);
}
.msg.edit-mode {
  cursor: pointer;
}
.msg-check {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-top: 24px;
  cursor: pointer;
}
/* 上下文菜单 */
.ctx-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0,0,0,0.08);
  animation: ctxFadeIn 0.12s ease-out;
}
@keyframes ctxFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.ctx-menu {
  position: fixed;
  z-index: 10000;
  min-width: 150px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(99, 102, 241, 0.1);
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(99, 102, 241, 0.12), 0 2px 8px rgba(0,0,0,0.06);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  animation: ctxPopIn 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes ctxPopIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}
.ctx-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.75);
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
}
.ctx-item:hover {
  background: rgba(99, 102, 241, 0.06);
  color: #6366f1;
}
.ctx-item.danger {
  color: rgba(239, 68, 68, 0.75);
}
.ctx-item.danger:hover {
  background: rgba(239, 68, 68, 0.05);
  color: #ef4444;
}
.ctx-divider {
  height: 1px;
  background: rgba(99, 102, 241, 0.06);
  margin: 4px 8px;
}
.uploader {
  display: inline-flex;
}
.stickers {
  padding: 10px;
  width: 280px;
}
.stick-top {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 10px;
}
.add {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.7);
  font-weight: 800;
}
.stick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
.stick {
  position: relative;
  border: 0;
  background: transparent;
  padding: 0;
  width: 62px;
  height: 62px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
}
.stick img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.stick-del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: 0;
  background: rgba(17, 24, 39, 0.55);
  color: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  padding: 0;
  line-height: 18px;
  font-weight: 900;
}
</style>

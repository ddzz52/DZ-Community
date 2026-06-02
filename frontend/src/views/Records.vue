<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { ArrowLeft, ArrowUpBold, Calendar, Delete, Edit, Notebook, Plus, RefreshRight, Star, StarFilled, UploadFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import AccountsPanel from '../modules/accounts/AccountsPanel.vue'
import PeriodCarePanel from '../modules/period/PeriodCarePanel.vue'
import WishScratchPanel from '../modules/wish/WishScratchPanel.vue'
import WishListPanel from '../modules/wishlist/WishListPanel.vue'

const apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const assetUrl = (u) => {
  const s = String(u || '')
  if (!s) return ''
  if (/^(https?:)?\/\//.test(s) || s.startsWith('blob:') || s.startsWith('data:')) return s
  if (s.startsWith('/') && apiBase) return apiBase + s
  return s
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const partner = ref(null)
const myId = computed(() => {
  const v = auth.user?.id
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : null
})
const myName = computed(() => auth.user?.nickname || auth.user?.username || '我')
const partnerName = computed(() => partner.value?.nickname || partner.value?.username || '对方')
const authorLabel = (authorId) => {
  const v = typeof authorId === 'number' ? authorId : Number(authorId)
  if (myId.value != null && Number.isFinite(v) && v === myId.value) return myName.value
  return partnerName.value
}
const isMine = (it) => {
  const v = it?.authorId
  const n = typeof v === 'number' ? v : Number(v)
  if (!Number.isFinite(n) || myId.value == null) return false
  return n === myId.value
}

const loadPartner = async () => {
  try {
    const p = await http.get('/api/profile')
    partner.value = p?.partner || null
  } catch (e) {
    partner.value = null
  }
}

const normalizeTab = (v) => {
  if (v === 'diary') return 'diary'
  if (v === 'accounts') return 'accounts'
  if (v === 'period') return 'period'
  if (v === 'wish') return 'wish'
  if (v === 'wishlist') return 'wishlist'
  return 'ann'
}
const tab = ref(normalizeTab(route.query.tab))
const pendingOpen = ref({ kind: '', id: null })
const lastOpenKey = ref('')
watch(
  () => route.query.tab,
  (v) => {
    tab.value = normalizeTab(v)
  }
)

const setTab = (v) => {
  tab.value = v
  router.replace({ path: '/app/records', query: { tab: v } })
}

const annLoading = ref(false)
const anniversaries = ref([])

const loadAnn = async (silent) => {
  try {
    annLoading.value = true
    anniversaries.value = await http.get('/api/anniversaries')
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    annLoading.value = false
    tryOpenFromQuery()
  }
}

const annDialog = ref(false)
const annSaving = ref(false)
const editingId = ref(null)
const annForm = reactive({
  title: '',
  date: '',
  calendarType: 'SOLAR',
  lunarMonth: 1,
  lunarDay: 1,
  lunarLeap: false,
  type: '',
  icon: '',
  themeColor: '',
  coverUrl: '',
  coverThumbUrl: '',
  note: '',
  reminderEnabled: true,
  reminderDaysBefore: 3,
  reminderOnDay: true
})
const annCoverFiles = ref([])
const annTypeOptions = ['生日', '周年', '旅行', '第一次', '春节', '元宵', '端午', '七夕', '中秋', '重阳', '其他']
const annIconPresets = ['🎂', '💍', '✈️', '🌟', '🎁', '🎉', '📌', '💞', '🌙', '🏖️']
const lunarMonthOptions = Array.from({ length: 12 }, (_, i) => i + 1)
const lunarDayOptions = Array.from({ length: 30 }, (_, i) => i + 1)

const annThemeDefault = (type) => {
  const t = String(type || '')
  if (t.includes('生日')) return '#F59E0B'
  if (t.includes('周年') || t.includes('纪念')) return '#EC4899'
  if (t.includes('旅行')) return '#10B981'
  if (t.includes('第一次')) return '#8B5CF6'
  return '#6366F1'
}

const annIconDefault = (type) => {
  const t = String(type || '')
  if (t.includes('生日')) return '🎂'
  if (t.includes('周年') || t.includes('纪念')) return '💍'
  if (t.includes('旅行')) return '✈️'
  if (t.includes('第一次')) return '🌟'
  return '✨'
}

const annStyle = (it) => {
  const color = String(it?.themeColor || '').trim() || '#6366F1'
  const cover = assetUrl(it?.coverThumbUrl || it?.coverUrl)
  return {
    '--ann-color': color,
    '--ann-cover': cover ? `url(${cover})` : 'none'
  }
}

const openAdd = () => {
  editingId.value = null
  annForm.title = ''
  annForm.date = ''
  annForm.calendarType = 'SOLAR'
  annForm.lunarMonth = 1
  annForm.lunarDay = 1
  annForm.lunarLeap = false
  annForm.type = ''
  annForm.icon = ''
  annForm.themeColor = ''
  annForm.coverUrl = ''
  annForm.coverThumbUrl = ''
  annForm.note = ''
  annForm.reminderEnabled = true
  annForm.reminderDaysBefore = 3
  annForm.reminderOnDay = true
  annCoverFiles.value = []
  annDialog.value = true
}

const openEdit = (it) => {
  editingId.value = it.id
  annForm.title = it.title || ''
  annForm.date = it.date ? new Date(it.date).toISOString().slice(0, 10) : ''
  annForm.calendarType = it.calendarType === 'LUNAR' ? 'LUNAR' : 'SOLAR'
  annForm.lunarMonth = typeof it.lunarMonth === 'number' ? it.lunarMonth : 1
  annForm.lunarDay = typeof it.lunarDay === 'number' ? it.lunarDay : 1
  annForm.lunarLeap = it.lunarLeap === true
  annForm.type = it.type || ''
  annForm.icon = it.icon || ''
  annForm.themeColor = it.themeColor || ''
  annForm.coverUrl = it.coverUrl || ''
  annForm.coverThumbUrl = it.coverThumbUrl || ''
  annForm.note = it.note || ''
  annForm.reminderEnabled = it.reminderEnabled !== false
  annForm.reminderDaysBefore = typeof it.reminderDaysBefore === 'number' ? it.reminderDaysBefore : 3
  annForm.reminderOnDay = it.reminderOnDay !== false
  const cover = assetUrl(it.coverThumbUrl || it.coverUrl)
  annCoverFiles.value = cover ? [{ name: 'cover', url: cover }] : []
  annDialog.value = true
}

const uploadAnnCover = async () => {
  const files = annCoverFiles.value || []
  const raw = files.find((x) => x?.raw)?.raw
  if (!raw) return { coverUrl: annForm.coverUrl || null, coverThumbUrl: annForm.coverThumbUrl || null }
  const fd = new FormData()
  fd.append('files', raw)
  const res = await http.post('/api/uploads/images', fd)
  const first = Array.isArray(res) ? res[0] : null
  return {
    coverUrl: first?.url || null,
    coverThumbUrl: first?.thumbUrl || first?.url || null
  }
}

const saveAnn = async () => {
  if (!annForm.title.trim()) {
    ElMessage.error('请输入名称')
    return
  }
  if (annForm.calendarType !== 'LUNAR' && !annForm.date) {
    ElMessage.error('请选择日期')
    return
  }
  if (annForm.calendarType === 'LUNAR') {
    if (!Number.isFinite(Number(annForm.lunarMonth)) || Number(annForm.lunarMonth) < 1 || Number(annForm.lunarMonth) > 12) {
      ElMessage.error('请选择农历月份')
      return
    }
    if (!Number.isFinite(Number(annForm.lunarDay)) || Number(annForm.lunarDay) < 1 || Number(annForm.lunarDay) > 30) {
      ElMessage.error('请选择农历日期')
      return
    }
  }
  try {
    annSaving.value = true
    const cover = await uploadAnnCover()
    const payload = {
      title: annForm.title,
      date: annForm.date,
      calendarType: annForm.calendarType,
      lunarMonth: annForm.calendarType === 'LUNAR' ? Number(annForm.lunarMonth) : null,
      lunarDay: annForm.calendarType === 'LUNAR' ? Number(annForm.lunarDay) : null,
      lunarLeap: annForm.calendarType === 'LUNAR' ? annForm.lunarLeap === true : false,
      type: annForm.type || null,
      icon: annForm.icon || null,
      themeColor: annForm.themeColor || (annForm.type ? annThemeDefault(annForm.type) : null),
      coverUrl: cover.coverUrl,
      coverThumbUrl: cover.coverThumbUrl,
      note: annForm.note,
      reminderEnabled: annForm.reminderEnabled,
      reminderDaysBefore: annForm.reminderDaysBefore,
      reminderOnDay: annForm.reminderOnDay
    }
    if (editingId.value) {
      await http.put(`/api/anniversaries/${editingId.value}`, payload)
      ElMessage.success('已更新')
    } else {
      await http.post('/api/anniversaries', payload)
      ElMessage.success('已添加')
    }
    annDialog.value = false
    await loadAnn(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    annSaving.value = false
  }
}

const togglePin = async (it) => {
  try {
    await http.put(`/api/anniversaries/${it.id}/pin`)
    await loadAnn(true)
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const removeAnn = async (it) => {
  try {
    await ElMessageBox.confirm(`确认删除“${it.title}”吗？`, '删除纪念日', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/anniversaries/${it.id}`)
    ElMessage.success('已删除')
    await loadAnn(true)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const annList = computed(() => {
  const arr = Array.isArray(anniversaries.value) ? [...anniversaries.value] : []
  arr.sort((a, b) => {
    const ap = a.pinned ? 0 : 1
    const bp = b.pinned ? 0 : 1
    if (ap !== bp) return ap - bp
    const ad = typeof a.daysLeft === 'number' ? a.daysLeft : 999999
    const bd = typeof b.daysLeft === 'number' ? b.daysLeft : 999999
    if (ad !== bd) return ad - bd
    return String(a.title || '').localeCompare(String(b.title || ''))
  })
  return arr
})

const dateText = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
}

onMounted(() => {
  loadPartner()
  if (tab.value === 'ann') loadAnn(true)
})

watch(
  () => tab.value,
  (v) => {
    if (v === 'ann') loadAnn(true)
    if (v === 'diary') loadDiaries(true, { reset: true })
  }
)

const diaryLoading = ref(false)
const diaryLoadingMore = ref(false)
const diaries = ref([])
const keyword = ref('')
const diaryLimit = ref(20)
const diaryHasMore = ref(true)
const diaryFilters = ref([])
const diaryDialog = ref(false)
const diarySaving = ref(false)
const diaryEditingId = ref(null)
const diaryForm = reactive({
  content: '',
  mood: '',
  privateFlag: false
})
const diaryFiles = ref([])
const diaryViewerOpen = ref(false)
const diaryViewerIndex = ref(0)
const diaryViewerUrls = ref([])
const diaryExpanded = reactive({})
const diaryBaseline = ref({ content: '', mood: '', privateFlag: false })

const diaryDraftSavedAt = ref(null)
const diaryDraftKey = computed(() => {
  const uid = myId.value
  if (uid == null) return null
  const scope = diaryEditingId.value ? `edit_${diaryEditingId.value}` : 'new'
  return `dz_diary_draft_${uid}_${scope}`
})
const diaryDraftLabel = computed(() => {
  if (!diaryDraftSavedAt.value) return ''
  return new Date(diaryDraftSavedAt.value).toLocaleString()
})
const isDiaryDirty = () => {
  const base = diaryBaseline.value || { content: '', mood: '', privateFlag: false }
  const cur = {
    content: String(diaryForm.content || ''),
    mood: String(diaryForm.mood || ''),
    privateFlag: diaryForm.privateFlag === true
  }
  return cur.content !== String(base.content || '') || cur.mood !== String(base.mood || '') || cur.privateFlag !== (base.privateFlag === true)
}
const saveDiaryDraftNow = () => {
  const key = diaryDraftKey.value
  if (!key) return
  if (!isDiaryDirty()) {
    localStorage.removeItem(key)
    diaryDraftSavedAt.value = null
    return
  }
  const payload = {
    content: diaryForm.content,
    mood: diaryForm.mood,
    privateFlag: diaryForm.privateFlag,
    savedAt: Date.now()
  }
  const empty = !String(payload.content || '').trim() && !String(payload.mood || '').trim() && payload.privateFlag !== true
  if (empty) {
    localStorage.removeItem(key)
    diaryDraftSavedAt.value = null
    return
  }
  localStorage.setItem(key, JSON.stringify(payload))
  diaryDraftSavedAt.value = payload.savedAt
}
const clearDiaryDraft = () => {
  const key = diaryDraftKey.value
  if (!key) return
  localStorage.removeItem(key)
  diaryDraftSavedAt.value = null
}
const tryRestoreDiaryDraft = async () => {
  const key = diaryDraftKey.value
  if (!key) return
  const raw = localStorage.getItem(key)
  if (!raw) return
  let draft = null
  try {
    draft = JSON.parse(raw)
  } catch (e) {
    localStorage.removeItem(key)
    return
  }
  if (!draft) return
  diaryDraftSavedAt.value = draft.savedAt || null
  const differs =
    String(draft.content || '') !== String(diaryForm.content || '') ||
    String(draft.mood || '') !== String(diaryForm.mood || '') ||
    Boolean(draft.privateFlag) !== Boolean(diaryForm.privateFlag)
  if (!differs) return
  try {
    await ElMessageBox.confirm('检测到未保存草稿，是否恢复？', '草稿', {
      confirmButtonText: '恢复',
      cancelButtonText: '丢弃',
      type: 'info'
    })
    diaryForm.content = String(draft.content || '')
    diaryForm.mood = String(draft.mood || '')
    diaryForm.privateFlag = draft.privateFlag === true
    diaryDraftSavedAt.value = draft.savedAt || null
  } catch (e) {
    if (e === 'cancel' || e === 'close') {
      localStorage.removeItem(key)
      diaryDraftSavedAt.value = null
    }
  }
}

let diaryDraftTimer = null
watch(
  [() => diaryDialog.value, () => diaryEditingId.value, () => diaryForm.content, () => diaryForm.mood, () => diaryForm.privateFlag],
  () => {
    if (!diaryDialog.value) return
    if (diaryDraftTimer) clearTimeout(diaryDraftTimer)
    diaryDraftTimer = setTimeout(() => {
      saveDiaryDraftNow()
    }, 600)
  }
)

const diaryBeforeUnload = (e) => {
  if (!diaryDialog.value) return
  if (!isDiaryDirty()) return
  e.preventDefault()
  e.returnValue = ''
}
watch(
  () => diaryDialog.value,
  (v) => {
    if (v) window.addEventListener('beforeunload', diaryBeforeUnload)
    else window.removeEventListener('beforeunload', diaryBeforeUnload)
  }
)

const diaryImageList = (it) => {
  const arr = Array.isArray(it?.images) ? it.images : []
  return arr
    .map((x) => {
      if (!x) return null
      if (typeof x === 'string') return { url: x, thumbUrl: x }
      if (typeof x === 'object') return { url: x.url || '', thumbUrl: x.thumbUrl || x.url || '' }
      return null
    })
    .filter((x) => x?.url)
}

const openDiaryViewer = (it, idx) => {
  const imgs = diaryImageList(it)
  diaryViewerUrls.value = imgs.map((x) => assetUrl(x.url)).filter(Boolean)
  diaryViewerIndex.value = Math.max(0, idx || 0)
  diaryViewerOpen.value = true
}

const diaryDetailDialog = ref(false)
const diaryDetailItem = ref(null)
const openDiaryDetail = (it) => {
  diaryDetailItem.value = it || null
  if (diaryDetailItem.value) {
    diaryViewerUrls.value = diaryImageList(diaryDetailItem.value)
      .map((x) => assetUrl(x.url))
      .filter(Boolean)
    diaryViewerIndex.value = 0
  }
  diaryDetailDialog.value = true
}

const annDetailDialog = ref(false)
const annDetailItem = ref(null)
const openAnnDetail = (it) => {
  annDetailItem.value = it || null
  annDetailDialog.value = true
}

const clearOpenQuery = () => {
  const q = { ...route.query }
  delete q.open
  delete q.id
  router.replace({ path: route.path, query: q })
}

const tryOpenFromQuery = () => {
  const kind = pendingOpen.value.kind
  const id = pendingOpen.value.id
  if (!kind || id == null) return
  const key = `${kind}_${id}`
  if (lastOpenKey.value === key) return
  if (kind === 'ann') {
    const it = annList.value.find((x) => x?.id === id)
    if (!it) return
    lastOpenKey.value = key
    pendingOpen.value = { kind: '', id: null }
    openAnnDetail(it)
    clearOpenQuery()
    return
  }
  if (kind === 'diary') {
    const it = diaries.value.find((x) => x?.id === id)
    if (!it) return
    lastOpenKey.value = key
    pendingOpen.value = { kind: '', id: null }
    openDiaryDetail(it)
    clearOpenQuery()
    return
  }
}

const groupKey = (d) => {
  if (!d) return 'unknown'
  const dt = new Date(d)
  if (Number.isNaN(dt.getTime())) return 'unknown'
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const dd = String(dt.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const groupLabel = (key) => {
  if (!key) return ''
  if (key === 'unknown') return '未知日期'
  const [y, m, d] = key.split('-')
  if (!y || !m || !d) return key
  return `${y}年${Number(m)}月${Number(d)}日`
}

const pinnedDiaries = computed(() => (Array.isArray(diaries.value) ? diaries.value.filter((x) => x?.pinned) : []))
const normalDiaries = computed(() => (Array.isArray(diaries.value) ? diaries.value.filter((x) => !x?.pinned) : []))
const diaryGroups = computed(() => {
  const arr = normalDiaries.value || []
  const out = []
  const map = new Map()
  for (const it of arr) {
    const k = groupKey(it?.createdAt)
    if (!map.has(k)) {
      const g = { key: k, items: [] }
      map.set(k, g)
      out.push(g)
    }
    map.get(k).items.push(it)
  }
  return out
})

const filteredDiaries = computed(() => {
  let arr = Array.isArray(diaries.value) ? diaries.value : []
  const fs = Array.isArray(diaryFilters.value) ? diaryFilters.value : []
  if (fs.includes('private')) arr = arr.filter((x) => x?.privateFlag === true)
  if (fs.includes('liked')) arr = arr.filter((x) => x?.liked === true)
  if (fs.includes('favorited')) arr = arr.filter((x) => x?.favorited === true)
  return arr
})
const pinnedDiariesFiltered = computed(() => (Array.isArray(filteredDiaries.value) ? filteredDiaries.value.filter((x) => x?.pinned) : []))
const normalDiariesFiltered = computed(() => (Array.isArray(filteredDiaries.value) ? filteredDiaries.value.filter((x) => !x?.pinned) : []))
const diaryGroupsFiltered = computed(() => {
  const arr = normalDiariesFiltered.value || []
  const out = []
  const map = new Map()
  for (const it of arr) {
    const k = groupKey(it?.createdAt)
    if (!map.has(k)) {
      const g = { key: k, items: [] }
      map.set(k, g)
      out.push(g)
    }
    map.get(k).items.push(it)
  }
  return out
})

const loadDiaries = async (silent, opts = {}) => {
  const reset = opts?.reset === true
  try {
    if (reset) {
      diaryLimit.value = 20
      diaryHasMore.value = true
    }
    if (opts?.more === true) diaryLoadingMore.value = true
    else diaryLoading.value = true
    const res = await http.get('/api/diaries', {
      params: { keyword: keyword.value || undefined, limit: diaryLimit.value }
    })
    const list = Array.isArray(res) ? res : []
    diaryHasMore.value = list.length === diaryLimit.value
    const prev = Array.isArray(diaries.value) ? diaries.value : []
    const prevById = new Map(prev.map((x) => [x?.id, x]))
    diaries.value = list.map((x) => (x?.id && prevById.has(x.id) ? { ...prevById.get(x.id), ...x } : x))
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    diaryLoading.value = false
    diaryLoadingMore.value = false
    tryOpenFromQuery()
  }
}

const openDiaryAdd = () => {
  diaryEditingId.value = null
  diaryForm.content = ''
  diaryForm.mood = ''
  diaryForm.privateFlag = false
  diaryBaseline.value = { content: '', mood: '', privateFlag: false }
  diaryFiles.value = []
  diaryDialog.value = true
  nextTick(() => {
    tryRestoreDiaryDraft()
  })
}

const openDiaryEdit = (it) => {
  if (!isMine(it)) {
    ElMessage.error('只有作者本人可以编辑')
    return
  }
  diaryEditingId.value = it.id
  diaryForm.content = it.content || ''
  diaryForm.mood = it.mood || ''
  diaryForm.privateFlag = it.privateFlag === true
  diaryBaseline.value = { content: diaryForm.content, mood: diaryForm.mood, privateFlag: diaryForm.privateFlag }
  diaryFiles.value = []
  diaryDialog.value = true
  nextTick(() => {
    tryRestoreDiaryDraft()
  })
}

const uploadImages = async () => {
  const files = diaryFiles.value || []
  if (!files.length) return []
  const fd = new FormData()
  files.slice(0, 5).forEach((f) => fd.append('files', f.raw || f))
  const res = await http.post('/api/uploads/images', fd)
  return Array.isArray(res) ? res.map((x) => x.url).filter(Boolean) : []
}

const saveDiary = async () => {
  if (!diaryForm.content.trim()) {
    ElMessage.error('请输入内容')
    return
  }
  try {
    diarySaving.value = true
    const images = await uploadImages()
    const payload = {
      content: diaryForm.content,
      mood: diaryForm.mood || null,
      privateFlag: diaryForm.privateFlag,
      images
    }
    if (diaryEditingId.value) {
      await http.put(`/api/diaries/${diaryEditingId.value}`, payload)
      ElMessage.success('已更新')
    } else {
      await http.post('/api/diaries', payload)
      ElMessage.success('已发布')
    }
    clearDiaryDraft()
    diaryDialog.value = false
    await loadDiaries(true, { reset: true })
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    diarySaving.value = false
  }
}

const removeDiary = async (it) => {
  try {
    await ElMessageBox.confirm('确认删除这条记录吗？', '删除记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/diaries/${it.id}`)
    ElMessage.success('已删除')
    await loadDiaries(true)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const toggleLike = async (it) => {
  try {
    const updated = await http.put(`/api/diaries/${it.id}/like`)
    diaries.value = diaries.value.map((d) => (d.id === it.id ? updated : d))
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const moodText = (m) => (m ? String(m).trim() : '')
const moodPresets = ['开心', '想你', '小确幸', '感动', '期待', '疲惫', '委屈', '生气']
const moodTagType = (m) => {
  const s = moodText(m)
  if (!s) return 'info'
  if (/开心|甜|快乐|小确幸|感动|幸福/.test(s)) return 'success'
  if (/期待|想你|思念|想念|爱/.test(s)) return 'primary'
  if (/委屈|难过|失落|疲惫|累/.test(s)) return 'warning'
  if (/生气|愤怒|崩溃|烦/.test(s)) return 'danger'
  return 'info'
}

const onSearch = () => {
  loadDiaries(false, { reset: true })
}

const loadMoreDiaries = async () => {
  if (diaryLoading.value || diaryLoadingMore.value || !diaryHasMore.value) return
  diaryLimit.value += 20
  await loadDiaries(true, { more: true })
}

const diaryIsLong = (it) => {
  const s = String(it?.content || '')
  if (!s) return false
  if (s.length > 260) return true
  return s.split('\n').length > 8
}
const diaryDisplayContent = (it) => {
  const s = String(it?.content || '')
  if (!s) return ''
  if (diaryExpanded[it?.id] || !diaryIsLong(it)) return s
  const trimmed = s.trim()
  const cut = trimmed.slice(0, 240)
  return cut + (trimmed.length > cut.length ? '…' : '')
}
const toggleDiaryExpand = (it) => {
  if (!it?.id) return
  diaryExpanded[it.id] = !diaryExpanded[it.id]
}

let diaryScrollTicking = false
const onDiaryScroll = () => {
  if (diaryScrollTicking) return
  diaryScrollTicking = true
  requestAnimationFrame(() => {
    diaryScrollTicking = false
    if (tab.value !== 'diary') return
    if (diaryLoading.value || diaryLoadingMore.value || !diaryHasMore.value) return
    const el = document.documentElement
    const remaining = el.scrollHeight - (window.scrollY + window.innerHeight)
    if (remaining < 320) loadMoreDiaries()
  })
}

watch(
  () => tab.value,
  (v) => {
    if (v === 'diary') window.addEventListener('scroll', onDiaryScroll, { passive: true })
    else window.removeEventListener('scroll', onDiaryScroll)
  },
  { immediate: true }
)

watch(
  () => [route.query.open, route.query.id],
  () => {
    const open = String(route.query.open || '')
    const id = Number(route.query.id)
    if (!Number.isFinite(id)) return
    if (open === 'ann') {
      pendingOpen.value = { kind: 'ann', id }
      setTab('ann')
      return
    }
    if (open === 'diary') {
      pendingOpen.value = { kind: 'diary', id }
      diaryLimit.value = Math.max(diaryLimit.value, 200)
      setTab('diary')
      return
    }
  },
  { immediate: true }
)

watch(
  () => annList.value,
  () => {
    tryOpenFromQuery()
  }
)

watch(
  () => diaries.value,
  () => {
    tryOpenFromQuery()
  }
)

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onDiaryScroll)
  window.removeEventListener('beforeunload', diaryBeforeUnload)
  if (diaryDraftTimer) clearTimeout(diaryDraftTimer)
})

const toggleFavorite = async (it) => {
  if (!it?.id) return
  try {
    const updated = await http.put(`/api/diaries/${it.id}/favorite`)
    diaries.value = diaries.value.map((x) => (x.id === it.id ? { ...x, ...updated } : x))
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const togglePinDiary = async (it) => {
  if (!it?.id) return
  try {
    const updated = await http.put(`/api/diaries/${it.id}/pin`)
    diaries.value = diaries.value.map((x) => (x.id === it.id ? { ...x, ...updated } : x))
    await loadDiaries(true)
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const commentDialog = ref(false)
const commentLoading = ref(false)
const commentSending = ref(false)
const commentItems = ref([])
const commentDiary = ref(null)
const commentText = ref('')
const isMyAuthorId = (authorId) => {
  const v = typeof authorId === 'number' ? authorId : Number(authorId)
  if (myId.value == null || !Number.isFinite(v)) return false
  return v === myId.value
}
const initialForName = (name) => {
  const s = String(name || '').trim()
  return s ? s.slice(0, 1).toUpperCase() : 'U'
}
const commentDiaryTitle = computed(() => {
  const s = String(commentDiary.value?.content || '').trim()
  if (!s) return '日记评论'
  return s.length > 24 ? s.slice(0, 24) + '…' : s
})

const loadComments = async (silent) => {
  const d = commentDiary.value
  if (!d?.id) return
  try {
    commentLoading.value = true
    commentItems.value = await http.get(`/api/diaries/${d.id}/comments`, { params: { limit: 100 } })
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    commentLoading.value = false
  }
}

const openComments = (it) => {
  commentDiary.value = it
  commentText.value = ''
  commentItems.value = []
  commentDialog.value = true
  loadComments(true)
}

const sendComment = async () => {
  const d = commentDiary.value
  if (!d?.id) return
  const txt = commentText.value.trim()
  if (!txt) {
    ElMessage.error('请输入评论')
    return
  }
  if (txt.length > 100) {
    ElMessage.error('评论长度需≤100')
    return
  }
  try {
    commentSending.value = true
    const created = await http.post(`/api/diaries/${d.id}/comments`, { content: txt })
    if (created) {
      commentItems.value = [...commentItems.value, created]
      diaries.value = diaries.value.map((x) =>
        x.id === d.id ? { ...x, commentCount: (x.commentCount || 0) + 1 } : x
      )
    } else {
      await loadComments(true)
    }
    commentText.value = ''
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
  } finally {
    commentSending.value = false
  }
}

const removeComment = async (c) => {
  const d = commentDiary.value
  if (!d?.id || !c?.id) return
  try {
    await ElMessageBox.confirm('确认删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/diaries/comments/${c.id}`)
    commentItems.value = commentItems.value.filter((x) => x.id !== c.id)
    diaries.value = diaries.value.map((x) =>
      x.id === d.id ? { ...x, commentCount: Math.max(0, (x.commentCount || 0) - 1) } : x
    )
    ElMessage.success('已删除')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}
</script>

<template>
  <div class="stack">
    <div class="top app-card">
      <div class="top-left">
        <div class="top-title-row">
          <button type="button" class="backbtn" aria-label="返回" @click="router.push('/app/home')">
            <el-icon :size="18"><ArrowLeft /></el-icon>
          </button>
          <div class="title">记录</div>
        </div>
        <div class="tabs">
          <button class="tabbtn" :class="{ active: tab === 'ann' }" type="button" @click="setTab('ann')">
            <el-icon :size="16"><Calendar /></el-icon>
            <span>纪念日</span>
          </button>
          <button class="tabbtn" :class="{ active: tab === 'diary' }" type="button" @click="setTab('diary')">
            <el-icon :size="16"><Notebook /></el-icon>
            <span>日记/心情</span>
          </button>
          <button class="tabbtn" :class="{ active: tab === 'accounts' }" type="button" @click="setTab('accounts')">
            <el-icon :size="16">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M7 8.5h10c1.657 0 3 1.343 3 3v4c0 1.657-1.343 3-3 3H7c-1.657 0-3-1.343-3-3v-4c0-1.657 1.343-3 3-3Z"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linejoin="round"
                />
                <path
                  d="M6 8.5V7.2c0-1.215.985-2.2 2.2-2.2h7.6c1.215 0 2.2.985 2.2 2.2v1.3"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linecap="round"
                />
                <path d="M15.7 13.5h1.8" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" />
              </svg>
            </el-icon>
            <span>记账</span>
          </button>
          <button class="tabbtn" :class="{ active: tab === 'period' }" type="button" @click="setTab('period')">
            <el-icon :size="16">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M12 4c4.418 0 8 3.582 8 8 0 3.866-2.744 7.092-6.4 7.84"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linecap="round"
                />
                <path
                  d="M12 4c-4.418 0-8 3.582-8 8 0 4.418 3.582 8 8 8"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linecap="round"
                  opacity="0.6"
                />
                <path d="M12 12v8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
                <path d="M9.2 17h5.6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
              </svg>
            </el-icon>
            <span>关怀</span>
          </button>
          <button class="tabbtn" :class="{ active: tab === 'wish' }" type="button" @click="setTab('wish')">
            <el-icon :size="16">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M5 8.6c0-1.436 1.164-2.6 2.6-2.6h8.8c1.436 0 2.6 1.164 2.6 2.6v6.8c0 1.436-1.164 2.6-2.6 2.6H7.6c-1.436 0-2.6-1.164-2.6-2.6V8.6Z"
                  stroke="currentColor"
                  stroke-width="1.7"
                  stroke-linejoin="round"
                />
                <path d="M7.2 10.2h9.6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" opacity="0.55" />
                <path d="M8.2 14.2c1.2 1.6 2.5 2.4 3.8 2.4 1.3 0 2.6-.8 3.8-2.4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
              </svg>
            </el-icon>
            <span>心愿</span>
          </button>
          <button class="tabbtn" :class="{ active: tab === 'wishlist' }" type="button" @click="setTab('wishlist')">
            <el-icon :size="16"><Notebook /></el-icon>
            <span>清单</span>
          </button>
        </div>
      </div>
      <div class="top-actions">
        <el-button v-if="tab === 'ann'" size="small" :loading="annLoading" @click="loadAnn()">
          <el-icon :size="16"><RefreshRight /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button v-if="tab === 'ann'" size="small" class="pinkbtn" @click="openAdd">
          <el-icon :size="16"><Plus /></el-icon>
          <span>新增</span>
        </el-button>
      </div>
    </div>

    <el-card v-if="tab === 'ann'" class="app-card" shadow="never">
      <template #header>
        <div class="row">
          <div class="h">纪念日列表</div>
          <div class="sub app-muted">按距离今天由近到远排序</div>
        </div>
      </template>

      <div v-if="!annList.length" class="empty app-muted">
        <el-empty description="还没有纪念日">
          <template #image>
            <el-icon :size="46"><Calendar /></el-icon>
          </template>
        </el-empty>
        <div class="tips app-muted">新增一个纪念日，马上提升“情侣站”氛围。</div>
      </div>

      <div v-else class="list">
        <div v-for="it in annList" :key="it.id" class="ann" :class="{ pinned: it.pinned }" :style="annStyle(it)">
          <div class="ann-cover" />
          <div class="ann-main">
            <div class="ann-top">
              <div class="ann-title">
                <span class="ann-icon">{{ it.icon || annIconDefault(it.type) }}</span>
                <el-tag v-if="it.type" effect="light" round size="small" class="anntype">{{ it.type }}</el-tag>
                <el-tag v-if="it.pinned" effect="light" round size="small" class="annpinned">重要</el-tag>
                <span class="t">{{ it.title }}</span>
              </div>
              <div class="ann-days" :class="{ today: it.daysLeft === 0 }">
                <span v-if="it.daysLeft === 0">今天</span>
                <span v-else>{{ it.daysLeft }} 天</span>
              </div>
            </div>
            <div class="ann-sub app-muted">
              <span>{{ dateText(it.nextDate || it.date) }}</span>
              <template v-if="it.reminderEnabled">
                <span> · 提醒：提前 {{ typeof it.reminderDaysBefore === 'number' ? it.reminderDaysBefore : 3 }} 天</span>
                <span v-if="it.reminderOnDay !== false"> + 当天</span>
              </template>
              <span v-else> · 提醒关闭</span>
            </div>
            <div v-if="it.note" class="ann-note">{{ it.note }}</div>
          </div>
          <div class="ann-actions">
            <el-button size="small" text @click="togglePin(it)">
              <el-icon :size="16"><component :is="it.pinned ? StarFilled : Star" /></el-icon>
              <span>{{ it.pinned ? '取消重要' : '设为重要' }}</span>
            </el-button>
            <el-button size="small" text @click="openEdit(it)">
              <el-icon :size="16"><Edit /></el-icon>
              <span>编辑</span>
            </el-button>
            <el-button size="small" text type="danger" @click="removeAnn(it)">
              <el-icon :size="16"><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card v-else-if="tab === 'diary'" class="app-card" shadow="never">
      <template #header>
        <div class="row">
          <div class="h">日记/心情</div>
          <div class="top-actions">
            <el-button size="small" :loading="diaryLoading" @click="loadDiaries()">
              <el-icon :size="16"><RefreshRight /></el-icon>
              <span>刷新</span>
            </el-button>
            <el-button size="small" class="pinkbtn" @click="openDiaryAdd">
              <el-icon :size="16"><Plus /></el-icon>
              <span>发布</span>
            </el-button>
          </div>
        </div>
      </template>

      <div class="search">
        <el-input v-model="keyword" placeholder="搜索内容" clearable @clear="onSearch" @keyup.enter="onSearch" />
        <el-select v-model="diaryFilters" multiple collapse-tags collapse-tags-tooltip placeholder="筛选" style="width: 180px">
          <el-option label="私密" value="private" />
          <el-option label="已点赞" value="liked" />
          <el-option label="已收藏" value="favorited" />
        </el-select>
        <el-button @click="onSearch">搜索</el-button>
      </div>

      <div v-if="!filteredDiaries.length" class="wrap">
        <el-empty description="还没有日记/心情">
          <template #image>
            <el-icon :size="46"><Notebook /></el-icon>
          </template>
        </el-empty>
        <div class="tips app-muted">发布一条记录，让日常更有迹可循。</div>
      </div>

      <div v-else class="dlist">
        <div class="dwrap">
          <div v-if="pinnedDiariesFiltered.length" class="dpinned">
            <div class="dpinned-title">
              <el-icon :size="16"><ArrowUpBold /></el-icon>
              <span>置顶</span>
            </div>
            <div class="dlist">
              <div v-for="it in pinnedDiariesFiltered" :key="it.id" class="ditem">
                <div class="dtop">
                  <div class="dmeta app-muted">
                    <el-tag effect="light" round size="small" class="dtag">置顶</el-tag>
                    <el-tag v-if="it.favorited" type="warning" effect="light" round size="small" class="dtag">已收藏</el-tag>
                    <span class="dauthor">{{ authorLabel(it.authorId) }}</span>
                    <span> · </span>
                    <el-tag
                      v-if="moodText(it.mood)"
                      class="moodtag"
                      round
                      size="small"
                      effect="light"
                      :type="moodTagType(it.mood)"
                    >
                      {{ moodText(it.mood) }}
                    </el-tag>
                    <span v-if="moodText(it.mood)"> · </span>
                    <span>{{ new Date(it.createdAt).toLocaleString() }}</span>
                    <span v-if="it.privateFlag"> · 私密</span>
                  </div>
                  <div class="dops">
                    <el-button size="small" text @click="togglePinDiary(it)">
                      <el-icon :size="16"><ArrowUpBold /></el-icon>
                      <span>{{ it.pinned ? '取消置顶' : '置顶' }}</span>
                    </el-button>
                    <el-button size="small" text @click="toggleFavorite(it)">
                      <el-icon :size="16"><component :is="it.favorited ? StarFilled : Star" /></el-icon>
                      <span>{{ it.favorited ? '已收藏' : '收藏' }}</span>
                    </el-button>
                    <template v-if="isMine(it)">
                      <el-button size="small" text @click="openDiaryEdit(it)">
                        <el-icon :size="16"><Edit /></el-icon>
                        <span>编辑</span>
                      </el-button>
                      <el-button size="small" text type="danger" @click="removeDiary(it)">
                        <el-icon :size="16"><Delete /></el-icon>
                        <span>删除</span>
                      </el-button>
                    </template>
                  </div>
                </div>
                <div class="dcontent">
                  <div>{{ diaryDisplayContent(it) }}</div>
                  <button v-if="diaryIsLong(it)" class="dmore" type="button" @click="toggleDiaryExpand(it)">
                    {{ diaryExpanded[it.id] ? '收起' : '展开全文' }}
                  </button>
                </div>
                <div v-if="diaryImageList(it).length" class="dimgs">
                  <button
                    v-for="(img, idx) in diaryImageList(it).slice(0, 9)"
                    :key="img.url"
                    class="dimg dimgbtn"
                    type="button"
                    @click="openDiaryViewer(it, idx)"
                  >
                    <img :src="assetUrl(img.thumbUrl || img.url)" alt="" />
                  </button>
                </div>
                <div class="dactions">
                  <el-button size="small" text @click="toggleLike(it)">
                    <span>{{ it.liked ? '已赞' : '点赞' }}</span>
                    <span class="app-muted">({{ it.likeCount || 0 }})</span>
                  </el-button>
                  <el-button size="small" text @click="openComments(it)">
                    <span>评论</span>
                    <span class="app-muted">({{ it.commentCount || 0 }})</span>
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <el-timeline v-if="diaryGroupsFiltered.length" class="dtimeline">
            <el-timeline-item v-for="g in diaryGroupsFiltered" :key="g.key" :timestamp="groupLabel(g.key)" placement="top">
              <div class="dlist">
                <div v-for="it in g.items" :key="it.id" class="ditem">
                  <div class="dtop">
                    <div class="dmeta app-muted">
                      <el-tag v-if="it.favorited" type="warning" effect="light" round size="small" class="dtag">已收藏</el-tag>
                      <span class="dauthor">{{ authorLabel(it.authorId) }}</span>
                      <span> · </span>
                      <el-tag
                        v-if="moodText(it.mood)"
                        class="moodtag"
                        round
                        size="small"
                        effect="light"
                        :type="moodTagType(it.mood)"
                      >
                        {{ moodText(it.mood) }}
                      </el-tag>
                      <span v-if="moodText(it.mood)"> · </span>
                      <span>{{ new Date(it.createdAt).toLocaleString() }}</span>
                      <span v-if="it.privateFlag"> · 私密</span>
                    </div>
                    <div class="dops">
                      <el-button size="small" text @click="togglePinDiary(it)">
                        <el-icon :size="16"><ArrowUpBold /></el-icon>
                        <span>{{ it.pinned ? '取消置顶' : '置顶' }}</span>
                      </el-button>
                      <el-button size="small" text @click="toggleFavorite(it)">
                        <el-icon :size="16"><component :is="it.favorited ? StarFilled : Star" /></el-icon>
                        <span>{{ it.favorited ? '已收藏' : '收藏' }}</span>
                      </el-button>
                      <template v-if="isMine(it)">
                        <el-button size="small" text @click="openDiaryEdit(it)">
                          <el-icon :size="16"><Edit /></el-icon>
                          <span>编辑</span>
                        </el-button>
                        <el-button size="small" text type="danger" @click="removeDiary(it)">
                          <el-icon :size="16"><Delete /></el-icon>
                          <span>删除</span>
                        </el-button>
                      </template>
                    </div>
                  </div>
                  <div class="dcontent">
                    <div>{{ diaryDisplayContent(it) }}</div>
                    <button v-if="diaryIsLong(it)" class="dmore" type="button" @click="toggleDiaryExpand(it)">
                      {{ diaryExpanded[it.id] ? '收起' : '展开全文' }}
                    </button>
                  </div>
                  <div v-if="diaryImageList(it).length" class="dimgs">
                    <button
                      v-for="(img, idx) in diaryImageList(it).slice(0, 9)"
                      :key="img.url"
                      class="dimg dimgbtn"
                      type="button"
                      @click="openDiaryViewer(it, idx)"
                    >
                      <img :src="assetUrl(img.thumbUrl || img.url)" alt="" />
                    </button>
                  </div>
                  <div class="dactions">
                    <el-button size="small" text @click="toggleLike(it)">
                      <span>{{ it.liked ? '已赞' : '点赞' }}</span>
                      <span class="app-muted">({{ it.likeCount || 0 }})</span>
                    </el-button>
                    <el-button size="small" text @click="openComments(it)">
                      <span>评论</span>
                      <span class="app-muted">({{ it.commentCount || 0 }})</span>
                    </el-button>
                  </div>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>

          <div class="dmorebox">
            <el-button v-if="diaryHasMore" size="small" :loading="diaryLoadingMore" @click="loadMoreDiaries">加载更多</el-button>
            <div v-else class="app-muted">没有更多了</div>
          </div>
        </div>
      </div>
    </el-card>

    <WishScratchPanel v-else-if="tab === 'wish'" />

    <WishListPanel v-else-if="tab === 'wishlist'" />

    <PeriodCarePanel v-else-if="tab === 'period'" />

    <AccountsPanel v-else />
  </div>

  <el-dialog v-model="annDialog" :title="editingId ? '编辑纪念日' : '新增纪念日'" width="92%">
    <el-form label-position="top">
      <el-form-item label="名称">
        <el-input v-model="annForm.title" maxlength="64" />
      </el-form-item>
      <el-form-item label="日期类型">
        <el-select v-model="annForm.calendarType" style="width: 100%">
          <el-option label="公历" value="SOLAR" />
          <el-option label="农历" value="LUNAR" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="annForm.calendarType !== 'LUNAR'" label="日期">
        <el-date-picker v-model="annForm.date" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item v-else label="农历日期">
        <div class="lunarrow">
          <el-select v-model="annForm.lunarMonth" style="width: 110px">
            <el-option v-for="m in lunarMonthOptions" :key="m" :label="`${m}月`" :value="m" />
          </el-select>
          <el-select v-model="annForm.lunarDay" style="width: 110px">
            <el-option v-for="d in lunarDayOptions" :key="d" :label="`${d}日`" :value="d" />
          </el-select>
          <div class="lunarleap">
            <span class="app-muted">闰月</span>
            <el-switch v-model="annForm.lunarLeap" />
          </div>
        </div>
      </el-form-item>
      <el-form-item label="类型（可选）">
        <el-select v-model="annForm.type" clearable placeholder="选择类型" style="width: 100%">
          <el-option v-for="t in annTypeOptions" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="图标（可选）">
        <div class="anniconbox">
          <el-input v-model="annForm.icon" maxlength="16" placeholder="比如：🎂 / 💍 / ✈️" />
          <div class="annicons">
            <el-tag
              v-for="ic in annIconPresets"
              :key="ic"
              class="anniconpick"
              round
              effect="light"
              size="small"
              @click="annForm.icon = ic"
            >
              {{ ic }}
            </el-tag>
            <el-tag
              v-if="annForm.type && !annForm.icon"
              class="anniconpick"
              round
              effect="light"
              size="small"
              @click="annForm.icon = annIconDefault(annForm.type)"
            >
              默认：{{ annIconDefault(annForm.type) }}
            </el-tag>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="主题色（可选）">
        <div class="anncolor">
          <el-color-picker v-model="annForm.themeColor" />
          <el-button size="small" @click="annForm.themeColor = annThemeDefault(annForm.type)">
            使用默认
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="照片封面（可选）">
        <el-upload v-model:file-list="annCoverFiles" list-type="picture-card" :auto-upload="false" accept="image/*" :limit="1">
          <el-icon :size="18"><UploadFilled /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="备注（可选）">
        <el-input v-model="annForm.note" type="textarea" maxlength="200" show-word-limit :autosize="{ minRows: 2, maxRows: 4 }" />
      </el-form-item>
      <el-form-item label="提醒">
        <div class="annrem">
          <el-switch v-model="annForm.reminderEnabled" />
          <span class="app-muted">开启后</span>
          <span class="app-muted">提前</span>
          <el-input-number v-model="annForm.reminderDaysBefore" :min="0" :max="30" />
          <span class="app-muted">天提醒</span>
          <span class="app-muted">+ 当天</span>
          <el-switch v-model="annForm.reminderOnDay" />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="annSaving" @click="annDialog = false">取消</el-button>
      <el-button type="primary" :loading="annSaving" @click="saveAnn">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="diaryDialog" :title="diaryEditingId ? '编辑日记/心情' : '发布日记/心情'" width="92%">
    <el-form label-position="top">
      <el-form-item v-if="diaryDraftLabel" label="草稿">
        <div class="draftline">
          <span class="app-muted">已自动保存：{{ diaryDraftLabel }}</span>
          <el-button size="small" text @click="clearDiaryDraft">清空草稿</el-button>
        </div>
      </el-form-item>
      <el-form-item label="心情（可选）">
        <div class="moodbox">
          <el-input v-model="diaryForm.mood" maxlength="16" placeholder="比如：开心 / 想你 / 小确幸" />
          <div class="moods">
            <el-tag
              v-for="m in moodPresets"
              :key="m"
              class="moodpick"
              round
              effect="light"
              size="small"
              :type="moodTagType(m)"
              @click="diaryForm.mood = m"
            >
              {{ m }}
            </el-tag>
            <el-tag
              v-if="moodText(diaryForm.mood)"
              class="moodcur"
              round
              effect="dark"
              size="small"
              :type="moodTagType(diaryForm.mood)"
              @click="diaryForm.mood = ''"
            >
              当前：{{ moodText(diaryForm.mood) }}
            </el-tag>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="内容">
        <el-input
          v-model="diaryForm.content"
          type="textarea"
          maxlength="2000"
          show-word-limit
          :autosize="{ minRows: 4, maxRows: 10 }"
        />
      </el-form-item>
      <el-form-item label="配图（最多5张）">
        <el-upload
          v-model:file-list="diaryFiles"
          list-type="picture-card"
          :auto-upload="false"
          accept="image/*"
          :limit="5"
        >
          <el-icon :size="18"><UploadFilled /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="私密（仅自己可见）">
        <el-switch v-model="diaryForm.privateFlag" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="diarySaving" @click="diaryDialog = false">取消</el-button>
      <el-button type="primary" :loading="diarySaving" @click="saveDiary">保存</el-button>
    </template>
  </el-dialog>

  <el-image-viewer
    v-if="diaryViewerOpen"
    :url-list="diaryViewerUrls"
    :initial-index="diaryViewerIndex"
    @close="diaryViewerOpen = false"
  />

  <el-dialog v-model="diaryDetailDialog" title="日记详情" width="92%">
    <div v-if="diaryDetailItem" class="dview">
      <div class="dview-meta app-muted">
        <span>{{ authorLabel(diaryDetailItem.authorId) }}</span>
        <span> · </span>
        <span>{{ diaryDetailItem.createdAt ? new Date(diaryDetailItem.createdAt).toLocaleString() : '-' }}</span>
        <span v-if="diaryDetailItem.privateFlag"> · 私密</span>
      </div>
      <div v-if="moodText(diaryDetailItem.mood)" class="dview-mood">
        <el-tag round size="small" effect="light" :type="moodTagType(diaryDetailItem.mood)">{{ moodText(diaryDetailItem.mood) }}</el-tag>
      </div>
      <div class="dview-content">{{ diaryDetailItem.content || '—' }}</div>
      <div v-if="diaryViewerUrls.length" class="dview-images">
        <button
          v-for="(url, idx) in diaryViewerUrls"
          :key="url + idx"
          class="dimg dimgbtn"
          type="button"
          @click="openDiaryViewer(diaryDetailItem, idx)"
        >
          <img :src="url" alt="" />
        </button>
      </div>
      <div class="dview-actions">
        <el-button size="small" @click="openComments(diaryDetailItem)">评论</el-button>
        <el-button v-if="isMine(diaryDetailItem)" size="small" @click="openDiaryEdit(diaryDetailItem)">编辑</el-button>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="annDetailDialog" title="纪念日详情" width="92%">
    <div v-if="annDetailItem" class="annview" :style="annStyle(annDetailItem)">
      <div class="annview-top">
        <div class="annview-title">
          <span class="annview-icon">{{ annDetailItem.icon || annIconDefault(annDetailItem.type) }}</span>
          <span class="annview-name">{{ annDetailItem.title }}</span>
        </div>
        <div class="annview-days">
          <span v-if="annDetailItem.daysLeft === 0">今天</span>
          <span v-else>{{ annDetailItem.daysLeft }} 天</span>
        </div>
      </div>
      <div class="annview-sub app-muted">
        <span>{{ dateText(annDetailItem.nextDate || annDetailItem.date) }}</span>
        <template v-if="annDetailItem.reminderEnabled">
          <span> · 提醒：提前 {{ typeof annDetailItem.reminderDaysBefore === 'number' ? annDetailItem.reminderDaysBefore : 3 }} 天</span>
          <span v-if="annDetailItem.reminderOnDay !== false"> + 当天</span>
        </template>
        <span v-else> · 提醒关闭</span>
      </div>
      <div v-if="annDetailItem.note" class="annview-note">{{ annDetailItem.note }}</div>
      <div class="annview-actions">
        <el-button size="small" @click="openEdit(annDetailItem)">编辑</el-button>
        <el-button size="small" text @click="togglePin(annDetailItem)">{{ annDetailItem.pinned ? '取消重要' : '设为重要' }}</el-button>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="commentDialog" title="评论" width="92%">
    <div class="cbox">
      <div class="chead">
        <div class="chead-top">
          <div class="chead-title">{{ commentDiaryTitle }}</div>
          <el-button size="small" :loading="commentLoading" @click="loadComments()">
            <el-icon :size="16"><RefreshRight /></el-icon>
            <span>刷新</span>
          </el-button>
        </div>
        <div class="chead-meta app-muted">
          <span>{{ authorLabel(commentDiary?.authorId) }}</span>
          <span> · </span>
          <span>{{ commentDiary?.createdAt ? new Date(commentDiary.createdAt).toLocaleString() : '-' }}</span>
          <span v-if="commentDiary?.privateFlag"> · 私密</span>
        </div>
      </div>
      <div v-if="!commentItems.length" class="cempty app-muted">还没有评论</div>
      <div v-else class="clist">
        <div v-for="c in commentItems" :key="c.id" class="citem" :class="{ mine: isMyAuthorId(c.authorId) }">
          <div class="cavatar" :class="{ mine: isMyAuthorId(c.authorId) }">
            <span>{{ initialForName(authorLabel(c.authorId)) }}</span>
          </div>
          <div class="cbody">
            <div class="crow">
              <div class="cmeta app-muted">
                <span>{{ authorLabel(c.authorId) }}</span>
                <span> · </span>
                <span>{{ new Date(c.createdAt).toLocaleString() }}</span>
              </div>
              <el-button v-if="isMyAuthorId(c.authorId)" size="small" text type="danger" @click="removeComment(c)">
                删除
              </el-button>
            </div>
            <div class="cbubble" :class="{ mine: isMyAuthorId(c.authorId) }">{{ c.content }}</div>
          </div>
        </div>
      </div>
      <div class="cinput">
        <el-input
          v-model="commentText"
          type="textarea"
          maxlength="100"
          show-word-limit
          :autosize="{ minRows: 2, maxRows: 4 }"
          placeholder="写点什么…"
        />
        <div class="csubmit">
          <el-button type="primary" :loading="commentSending" @click="sendComment">发送</el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.top {
  padding: 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.07), rgba(139, 92, 246, 0.06), rgba(255, 255, 255, 0.85));
  border-color: rgba(139, 92, 246, 0.1);
}
.top-left {
  min-width: 0;
  flex: 1;
}
.top-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.backbtn {
  border: 0;
  background: transparent;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(17, 24, 39, 0.45);
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s;
}
.backbtn:hover {
  background: rgba(99, 102, 241, 0.08);
  color: rgba(190, 24, 93, 0.9);
}
.title {
  font-weight: 900;
  font-size: 18px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.88), rgba(139, 92, 246, 0.88));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.tabs {
  margin-top: 12px;
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tabbtn {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.7);
  font-weight: 900;
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
}
.tabbtn.active {
  color: rgba(17, 24, 39, 0.92);
  border-color: rgba(99, 102, 241, 0.28);
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.14), rgba(139, 92, 246, 0.12), rgba(255, 255, 255, 0.62));
}
.tabbtn:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.22);
  background: rgba(99, 102, 241, 0.06);
  box-shadow: 0 14px 30px rgba(99, 102, 241, 0.06);
}
.top-actions {
  display: inline-flex;
  gap: 8px;
}
.pinkbtn {
  border-radius: 999px !important;
  background: linear-gradient(135deg, #6366f1, #4f46e5) !important;
  border: 0 !important;
  color: #fff !important;
  font-weight: 700;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.22);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;
}
.pinkbtn:hover {
  transform: translateY(-1px);
  filter: brightness(1.06);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.32);
}
.row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.h {
  font-weight: 800;
}
.sub {
  font-size: 12px;
}
.empty {
  padding: 8px 0 6px;
}
.wrap {
  padding: 8px 0 6px;
}
.search {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.dlist {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ditem {
  position: relative;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  overflow: hidden;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}
.ditem::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 99, 132, 0.1), rgba(99, 102, 241, 0.08));
  opacity: 0.6;
  pointer-events: none;
}
.ditem > * {
  position: relative;
  z-index: 1;
}
@media (hover: hover) {
  .ditem:hover {
    transform: translateY(-2px);
    border-color: rgba(99, 102, 241, 0.32);
    box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  }
}
.dtop {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.dmeta {
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.dauthor {
  font-weight: 800;
  color: rgba(17, 24, 39, 0.8);
}
.dwrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.dpinned-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 900;
  margin: 2px 0 8px;
  color: rgba(17, 24, 39, 0.86);
}
.dtimeline {
  margin-top: 2px;
}
.dtag {
  border-color: rgba(99, 102, 241, 0.18);
}
.moodtag {
  border-color: rgba(99, 102, 241, 0.18);
}
.dops {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}
.dcontent {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
.dmore {
  margin-top: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  color: rgba(99, 102, 241, 0.92);
  font-size: 12px;
  font-weight: 900;
}
.dmore:focus-visible {
  outline: 2px solid rgba(99, 102, 241, 0.55);
  outline-offset: 2px;
  border-radius: 8px;
}
.dmorebox {
  margin-top: 4px;
  display: flex;
  justify-content: center;
}
.draftline {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.dimgs {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.dview {
  display: grid;
  gap: 10px;
}
.dview-meta {
  font-size: 12px;
}
.dview-mood {
  margin-top: 2px;
}
.dview-content {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.dview-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.dview-actions {
  display: inline-flex;
  gap: 8px;
}
.annview {
  --ann-color: #6366f1;
  --ann-cover: none;
  position: relative;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  overflow: hidden;
}
.annview::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, color-mix(in srgb, var(--ann-color) 18%, transparent), rgba(255, 255, 255, 0));
  opacity: 0.9;
  pointer-events: none;
}
.annview > * {
  position: relative;
  z-index: 1;
}
.annview-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.annview-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.annview-icon {
  width: 26px;
  height: 26px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.66);
  flex: 0 0 auto;
}
.annview-name {
  font-weight: 900;
}
.annview-days {
  font-weight: 900;
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.75);
  white-space: nowrap;
}
.annview-sub {
  margin-top: 8px;
  font-size: 12px;
}
.annview-note {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-word;
}
.annview-actions {
  margin-top: 10px;
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}
.dimg {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.62);
}
.dimgbtn {
  cursor: pointer;
  padding: 0;
}
.dimgbtn:focus-visible {
  outline: 2px solid rgba(99, 102, 241, 0.55);
  outline-offset: 2px;
}
.dimg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.dactions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
}
.moodbox {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.moods {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.moodpick {
  cursor: pointer;
}
.moodcur {
  cursor: pointer;
}
.cbox {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chead {
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.68));
}
.chead-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.chead-title {
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}
.chead-meta {
  margin-top: 8px;
  font-size: 12px;
}
.cempty {
  padding: 10px 4px;
  text-align: center;
  font-size: 12px;
}
.clist {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 46vh;
  overflow: auto;
  padding-right: 4px;
}
.citem {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.citem.mine {
  flex-direction: row-reverse;
}
.cavatar {
  width: 30px;
  height: 30px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 900;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: linear-gradient(135deg, rgba(255, 99, 132, 0.18), rgba(99, 102, 241, 0.14));
  color: rgba(88, 28, 135, 0.92);
  flex: 0 0 auto;
}
.cavatar.mine {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.22), rgba(16, 185, 129, 0.14));
  color: rgba(17, 24, 39, 0.9);
}
.cbody {
  flex: 1;
  min-width: 0;
}
.crow {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.cmeta {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cbubble {
  margin-top: 6px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}
.cbubble.mine {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(16, 185, 129, 0.12));
  border-color: rgba(99, 102, 241, 0.28);
}
.cinput {
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: sticky;
  bottom: 0;
  padding-top: 6px;
  background: rgba(255, 255, 255, 0);
}
.csubmit {
  display: flex;
  justify-content: flex-end;
}
.tips {
  text-align: center;
  font-size: 12px;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ann {
  --ann-color: #6366f1;
  --ann-cover: none;
  position: relative;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  overflow: hidden;
}
.ann::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, color-mix(in srgb, var(--ann-color) 18%, transparent), rgba(255, 255, 255, 0));
  opacity: 0.9;
  pointer-events: none;
}
.ann-cover {
  position: absolute;
  inset: 0;
  background-image: var(--ann-cover);
  background-size: cover;
  background-position: center;
  opacity: 0.22;
  filter: saturate(1.02);
  pointer-events: none;
}
.ann > * {
  position: relative;
  z-index: 1;
}
.ann.pinned {
  border-color: color-mix(in srgb, var(--ann-color) 38%, rgba(255, 255, 255, 0.7));
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}
.ann-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.ann-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.ann-icon {
  width: 26px;
  height: 26px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.66);
  flex: 0 0 auto;
}
.anntype {
  border-color: color-mix(in srgb, var(--ann-color) 28%, rgba(255, 255, 255, 0.6));
}
.annpinned {
  border-color: color-mix(in srgb, var(--ann-color) 40%, rgba(255, 255, 255, 0.6));
}
.t {
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ann-days {
  font-weight: 900;
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.75);
  white-space: nowrap;
}
.ann-days.today {
  color: rgba(220, 38, 38, 0.95);
  border-color: rgba(220, 38, 38, 0.28);
  background: rgba(220, 38, 38, 0.08);
}
.ann-sub {
  margin-top: 8px;
  font-size: 12px;
}
.ann-note {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-word;
}
.ann-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.anniconbox {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.annicons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.anniconpick {
  cursor: pointer;
}
.anncolor {
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.annrem {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.lunarrow {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.lunarleap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>

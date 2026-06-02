<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { ArrowLeft, Delete, Plus, Picture as PictureIcon, RefreshRight, UploadFilled, Edit, Star, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const assetUrl = (u) => {
  const s = String(u || '')
  if (!s) return ''
  if (/^(https?:)?\/\//.test(s) || s.startsWith('blob:') || s.startsWith('data:')) return s
  if (s.startsWith('/') && apiBase) return apiBase + s
  return s
}

const router = useRouter()
const route = useRoute()
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
const isMine = (p) => {
  const v = p?.uploaderId
  const n = typeof v === 'number' ? v : Number(v)
  if (!Number.isFinite(n) || myId.value == null) return false
  return n === myId.value
}

const logout = async () => {
  auth.logout()
  try {
    await router.replace('/login')
  } catch (e) {}
  try {
    if (location.pathname !== '/login') location.href = '/login'
  } catch (e) {}
}

const albums = ref([])
const albumLoading = ref(false)
const activeAlbumId = ref(null)
const viewMode = ref('grid')
const selecting = ref(false)
const selected = ref(new Set())

const allAlbumId = computed(() => albums.value?.find((a) => a.name === '全部照片')?.id || null)
const normalizeAlbumId = (id) => {
  if (id == null) return null
  if (allAlbumId.value != null && id === allAlbumId.value) return null
  return id
}

const selectedIds = computed(() => Array.from(selected.value))
const selectedCount = computed(() => selected.value.size)
const isSelected = (id) => (id != null ? selected.value.has(id) : false)
const clearSelection = () => {
  selected.value = new Set()
}
const toggleSelected = (id) => {
  if (id == null) return
  const s = new Set(selected.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  selected.value = s
}
const toggleSelecting = () => {
  selecting.value = !selecting.value
  if (!selecting.value) clearSelection()
}
const selectAllVisible = () => {
  const arr = Array.isArray(photos.value) ? photos.value : []
  const s = new Set(arr.map((p) => p?.id).filter(Boolean))
  selected.value = s
}

const loadAlbums = async (silent) => {
  try {
    albumLoading.value = true
    albums.value = await http.get('/api/albums')
    const all = albums.value?.find((a) => a.name === '全部照片')
    if (activeAlbumId.value == null && all?.id) activeAlbumId.value = all.id
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    albumLoading.value = false
  }
}

const photos = ref([])
const photoLoading = ref(false)
const range = reactive({ from: '', to: '' })
const sortOrder = ref('desc')
const photoLimit = ref(120)
const pendingPhotoOpen = ref(null)
const lastOpenKey = ref('')

const listParams = computed(() => {
  const all = albums.value?.find((a) => a.name === '全部照片')
  const isAll = all?.id && activeAlbumId.value === all.id
  return {
    albumId: isAll ? undefined : normalizeAlbumId(activeAlbumId.value),
    deletedOnly: albums.value?.find((a) => a.name === '最近删除')?.id === activeAlbumId.value ? true : undefined,
    order: sortOrder.value,
    from: range.from || undefined,
    to: range.to || undefined,
    limit: photoLimit.value
  }
})

const loadPhotos = async (silent) => {
  try {
    photoLoading.value = true
    photos.value = await http.get('/api/photos', { params: listParams.value })
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    photoLoading.value = false
    tryOpenPhotoFromQuery()
  }
}

watch(
  () => [activeAlbumId.value, range.from, range.to, sortOrder.value],
  () => {
    if (activeAlbumId.value != null) loadPhotos(true)
    if (selecting.value) clearSelection()
  }
)

const timeText = (p) => {
  const t = p?.shotAt || p?.createdAt
  if (!t) return ''
  try {
    const d = new Date(t)
    if (Number.isNaN(d.getTime())) return ''
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${dd} ${hh}:${mm}`
  } catch {
    return ''
  }
}

const clearOpenQuery = () => {
  const q = { ...route.query }
  delete q.open
  delete q.id
  router.replace({ path: route.path, query: q })
}

const tryOpenPhotoFromQuery = () => {
  if (!pendingPhotoOpen.value) return
  const pid = pendingPhotoOpen.value
  const idx = Array.isArray(photos.value) ? photos.value.findIndex((p) => p?.id === pid) : -1
  if (idx < 0) return
  const key = `photo_${pid}`
  if (lastOpenKey.value === key) return
  lastOpenKey.value = key
  pendingPhotoOpen.value = null
  openViewer(idx)
  clearOpenQuery()
}

watch(
  () => [route.query.open, route.query.id],
  () => {
    const open = String(route.query.open || '')
    const id = Number(route.query.id)
    if (open === 'photo' && Number.isFinite(id)) {
      pendingPhotoOpen.value = id
      photoLimit.value = Math.max(photoLimit.value, 300)
      if (allAlbumId.value) activeAlbumId.value = allAlbumId.value
    }
  },
  { immediate: true }
)

watch(
  () => photos.value,
  () => {
    tryOpenPhotoFromQuery()
  }
)

onMounted(async () => {
  try {
    const p = await http.get('/api/profile')
    partner.value = p?.partner || null
    if (p?.me && !auth.user) {
      auth.user = p.me
      auth.persist()
    }
  } catch {
    partner.value = null
  }
  await loadAlbums(true)
  if (pendingPhotoOpen.value && allAlbumId.value) {
    activeAlbumId.value = allAlbumId.value
  }
  if (activeAlbumId.value != null) await loadPhotos(true)
})

const albumDialog = ref(false)
const albumSaving = ref(false)
const albumEditingId = ref(null)
const albumForm = reactive({ name: '' })

const openAlbumAdd = () => {
  albumEditingId.value = null
  albumForm.name = ''
  albumDialog.value = true
}

const openAlbumEdit = (a) => {
  if (!a?.deletable) return
  albumEditingId.value = a.id
  albumForm.name = a.name || ''
  albumDialog.value = true
}

const saveAlbum = async () => {
  if (!albumForm.name.trim()) {
    ElMessage.error('请输入分类名')
    return
  }
  try {
    albumSaving.value = true
    if (albumEditingId.value) {
      await http.put(`/api/albums/${albumEditingId.value}`, { name: albumForm.name, sortNo: 100 })
      ElMessage.success('已更新')
    } else {
      const created = await http.post('/api/albums', { name: albumForm.name })
      ElMessage.success('已添加')
      if (created?.id) activeAlbumId.value = created.id
    }
    albumDialog.value = false
    await loadAlbums(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    albumSaving.value = false
  }
}

const removeAlbum = async (a) => {
  if (!a?.deletable) return
  try {
    await ElMessageBox.confirm(`确认删除分类“${a.name}”吗？分类下的照片会移动到“全部照片”。`, '删除分类', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/albums/${a.id}`)
    ElMessage.success('已删除')
    activeAlbumId.value = albums.value?.find((x) => x.name === '全部照片')?.id || null
    await loadAlbums(true)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const uploadDialog = ref(false)
const uploadSaving = ref(false)
const uploadPercent = ref(0)
const uploadDonePulse = ref(false)
const uploadFiles = ref([])
const uploadAlbumId = ref(null)
const isTrash = computed(() => albums.value?.find((a) => a.name === '最近删除')?.id === activeAlbumId.value)

const openUpload = () => {
  if (isTrash.value) return
  uploadAlbumId.value = normalizeAlbumId(activeAlbumId.value)
  uploadFiles.value = []
  uploadPercent.value = 0
  uploadDonePulse.value = false
  uploadDialog.value = true
}

const clearFilters = () => {
  range.from = ''
  range.to = ''
  sortOrder.value = 'desc'
}

const hasFilter = computed(() => {
  if (String(range.from || '').trim()) return true
  if (String(range.to || '').trim()) return true
  if (String(sortOrder.value || 'desc') !== 'desc') return true
  return false
})

const uploadImages = async () => {
  const files = uploadFiles.value || []
  if (!files.length) {
    ElMessage.error('请选择图片')
    return
  }
  try {
    uploadSaving.value = true
    uploadPercent.value = 0
    uploadDonePulse.value = false
    const targetAlbumId = normalizeAlbumId(uploadAlbumId.value)
    const fd = new FormData()
    files.slice(0, 10).forEach((f) => fd.append('files', f.raw || f))
    const res = await http.post('/api/uploads/images', fd, {
      params: { biz: 'photo', albumId: targetAlbumId || undefined },
      onUploadProgress: (evt) => {
        try {
          const t = Number(evt?.total || 0)
          const l = Number(evt?.loaded || 0)
          if (t > 0) uploadPercent.value = Math.max(0, Math.min(99, Math.round((l / t) * 100)))
        } catch (e) {}
      }
    })
    const reqs = (Array.isArray(res) ? res : [])
      .map((x) => ({ url: x.url, thumbUrl: x.thumbUrl || null, albumId: targetAlbumId || null }))
      .filter((x) => x.url)
    await http.post('/api/photos', reqs)
    uploadPercent.value = 100
    uploadDonePulse.value = true
    ElMessage.success('已上传')
    uploadDialog.value = false
    await loadAlbums(true)
    await loadPhotos(true)
  } catch (e) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    uploadSaving.value = false
  }
}

const batchMoveDialog = ref(false)
const batchMoveSaving = ref(false)
const batchMoveAlbumId = ref(null)
const openBatchMove = () => {
  if (!selectedCount.value) return
  if (isTrash.value) return
  batchMoveAlbumId.value = activeAlbumId.value
  batchMoveDialog.value = true
}
const doBatchMove = async () => {
  if (!selectedCount.value) return
  try {
    batchMoveSaving.value = true
    await http.post('/api/photos/batch/move', { ids: selectedIds.value, albumId: batchMoveAlbumId.value || null })
    ElMessage.success('已移动')
    batchMoveDialog.value = false
    clearSelection()
    selecting.value = false
    await loadAlbums(true)
    await loadPhotos(true)
  } catch (e) {
    ElMessage.error(e?.message || '移动失败')
  } finally {
    batchMoveSaving.value = false
  }
}

const batchDeleteSaving = ref(false)
const batchDelete = async () => {
  if (!selectedCount.value) return
  try {
    const n = selectedCount.value
    await ElMessageBox.confirm(`确认删除已选的 ${n} 张照片吗？`, '批量删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    batchDeleteSaving.value = true
    if (isTrash.value) {
      for (const id of selectedIds.value) {
        await http.delete(`/api/photos/${id}/purge`)
      }
    } else {
      await http.post('/api/photos/batch/delete', { ids: selectedIds.value })
    }
    ElMessage.success('已删除')
    clearSelection()
    selecting.value = false
    await loadAlbums(true)
    await loadPhotos(true)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  } finally {
    batchDeleteSaving.value = false
  }
}

const batchDownloadSaving = ref(false)
const batchDownload = async () => {
  if (!selectedCount.value) return
  try {
    batchDownloadSaving.value = true
    const blob = await http.get('/api/photos/download', {
      params: { ids: selectedIds.value.join(',') },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'photos.zip'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.message || '下载失败')
  } finally {
    batchDownloadSaving.value = false
  }
}

const photoDialog = ref(false)
const photoIndex = ref(0)
const currentPhoto = computed(() => (Array.isArray(photos.value) ? photos.value[photoIndex.value] : null))
const viewerQuality = ref('original')
const viewerSrc = computed(() => {
  const p = currentPhoto.value
  if (!p) return ''
  const u = viewerQuality.value === 'thumb' ? p.thumbUrl || p.url : p.url
  return assetUrl(u)
})
const slideshowPlaying = ref(false)
const slideshowDelay = ref(2500)
let slideshowTimer = null
const stopSlideshow = () => {
  slideshowPlaying.value = false
  if (slideshowTimer) {
    clearInterval(slideshowTimer)
    slideshowTimer = null
  }
}
const startSlideshow = () => {
  if (slideshowPlaying.value) return
  slideshowPlaying.value = true
  if (slideshowTimer) clearInterval(slideshowTimer)
  slideshowTimer = setInterval(() => {
    nextPhoto()
  }, Math.max(1000, Number(slideshowDelay.value) || 2500))
}
const toggleSlideshow = () => {
  if (slideshowPlaying.value) stopSlideshow()
  else startSlideshow()
}
watch(
  () => photoDialog.value,
  (v) => {
    if (!v) stopSlideshow()
  }
)
const openViewer = (idx) => {
  photoIndex.value = Math.max(0, idx || 0)
  photoDialog.value = true
  viewerQuality.value = 'original'
  loadPhotoComments(true)
}
const prevPhoto = () => {
  if (!photos.value?.length) return
  photoIndex.value = (photoIndex.value - 1 + photos.value.length) % photos.value.length
  loadPhotoComments(true)
}
const nextPhoto = () => {
  if (!photos.value?.length) return
  photoIndex.value = (photoIndex.value + 1) % photos.value.length
  loadPhotoComments(true)
}

const tagsList = (p) => {
  const s = String(p?.tags || '').trim()
  if (!s) return []
  return s
    .split(/[,\s]+/)
    .map((x) => x.trim())
    .filter(Boolean)
    .slice(0, 12)
}

const coverStyle = computed(() => {
  const p = currentPhoto.value
  if (!p) return {}
  const u = assetUrl(p.thumbUrl || p.url)
  return { '--pbg': u ? `url(${u})` : 'none' }
})

const toggleCover = async () => {
  const p = currentPhoto.value
  if (!p?.id) return
  try {
    const updated = await http.put(`/api/photos/${p.id}/cover`)
    photos.value = photos.value.map((x) => (x.id === p.id ? { ...x, ...updated } : { ...x, cover: false }))
    ElMessage.success(updated?.cover ? '已设为封面' : '已取消封面')
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const toggleLike = async () => {
  const p = currentPhoto.value
  if (!p?.id) return
  try {
    const updated = await http.put(`/api/photos/${p.id}/like`)
    photos.value = photos.value.map((x) => (x.id === p.id ? { ...x, ...updated } : x))
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const metaDialog = ref(false)
const metaSaving = ref(false)
const metaForm = reactive({ title: '', location: '', mood: '', tags: '', shotAt: '' })
const openMeta = () => {
  const p = currentPhoto.value
  if (!p) return
  metaForm.title = p.title || ''
  metaForm.location = p.location || ''
  metaForm.mood = p.mood || ''
  metaForm.tags = p.tags || ''
  metaForm.shotAt = p.shotAt ? new Date(p.shotAt).toISOString().slice(0, 19).replace('T', ' ') : ''
  metaDialog.value = true
}
const saveMeta = async () => {
  const p = currentPhoto.value
  if (!p?.id) return
  try {
    metaSaving.value = true
    const payload = {
      title: metaForm.title || null,
      location: metaForm.location || null,
      mood: metaForm.mood || null,
      tags: metaForm.tags || null,
      shotAt: metaForm.shotAt || null
    }
    const updated = await http.put(`/api/photos/${p.id}`, payload)
    photos.value = photos.value.map((x) => (x.id === p.id ? { ...x, ...updated } : x))
    ElMessage.success('已保存')
    metaDialog.value = false
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    metaSaving.value = false
  }
}

const commentDialog = ref(false)
const commentLoading = ref(false)
const commentSending = ref(false)
const commentItems = ref([])
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
const loadPhotoComments = async (silent) => {
  const p = currentPhoto.value
  if (!p?.id) return
  try {
    commentLoading.value = true
    commentItems.value = await http.get(`/api/photos/${p.id}/comments`, { params: { limit: 200 } })
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    commentLoading.value = false
  }
}
const openComments = () => {
  commentText.value = ''
  commentItems.value = []
  commentDialog.value = true
  loadPhotoComments(true)
}
const sendComment = async () => {
  const p = currentPhoto.value
  if (!p?.id) return
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
    const created = await http.post(`/api/photos/${p.id}/comments`, { content: txt })
    if (created) {
      commentItems.value = [...commentItems.value, created]
      photos.value = photos.value.map((x) =>
        x.id === p.id ? { ...x, commentCount: (x.commentCount || 0) + 1 } : x
      )
    } else {
      await loadPhotoComments(true)
    }
    commentText.value = ''
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
  } finally {
    commentSending.value = false
  }
}
const removeComment = async (c) => {
  if (!c?.id) return
  try {
    await ElMessageBox.confirm('确认删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/photos/comments/${c.id}`)
    commentItems.value = commentItems.value.filter((x) => x.id !== c.id)
    const p = currentPhoto.value
    if (p?.id) {
      photos.value = photos.value.map((x) =>
        x.id === p.id ? { ...x, commentCount: Math.max(0, (x.commentCount || 0) - 1) } : x
      )
    }
    ElMessage.success('已删除')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const monthKey = (p) => {
  const t = p?.shotAt || p?.createdAt
  if (!t) return 'unknown'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return 'unknown'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}
const monthLabel = (key) => {
  if (key === 'unknown') return '未知月份'
  const [y, m] = String(key).split('-')
  return y && m ? `${y}年${Number(m)}月` : key
}
const albumNameOf = (albumId) => albums.value?.find((a) => a.id === albumId)?.name || '未分类'
const capsuleGroups = computed(() => {
  const arr = Array.isArray(photos.value) ? photos.value : []
  const map = new Map()
  for (const p of arr) {
    const mk = monthKey(p)
    if (!map.has(mk)) map.set(mk, [])
    map.get(mk).push(p)
  }
  const keys = Array.from(map.keys()).sort((a, b) => String(b).localeCompare(String(a)))
  return keys.map((k) => {
    const items = map.get(k) || []
    const sub = new Map()
    for (const p of items) {
      const name = albumNameOf(p.albumId)
      if (!sub.has(name)) sub.set(name, [])
      sub.get(name).push(p)
    }
    const subs = Array.from(sub.entries()).map(([name, list]) => ({ name, items: list }))
    subs.sort((a, b) => b.items.length - a.items.length)
    return { key: k, label: monthLabel(k), items, subs }
  })
})

const removePhoto = async (p) => {
  try {
    await ElMessageBox.confirm('确认删除这张照片吗？删除后可在“最近删除”中恢复。', '删除照片', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/photos/${p.id}`)
    ElMessage.success('已移入最近删除')
    await loadAlbums(true)
    await loadPhotos(true)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const restorePhoto = async (p) => {
  try {
    await http.put(`/api/photos/${p.id}/restore`)
    ElMessage.success('已恢复')
    await loadAlbums(true)
    await loadPhotos(true)
  } catch (e) {
    ElMessage.error(e?.message || '恢复失败')
  }
}

const purgePhoto = async (p) => {
  try {
    await ElMessageBox.confirm('确认彻底删除这张照片吗？此操作不可恢复。', '彻底删除', {
      confirmButtonText: '彻底删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/photos/${p.id}/purge`)
    ElMessage.success('已彻底删除')
    await loadAlbums(true)
    await loadPhotos(true)
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}
</script>

<template>
  <div class="stack">
    <div class="top app-card">
      <div class="topbar">
        <div class="head-left">
          <el-tooltip content="返回" placement="bottom">
            <button class="iconbtn" type="button" @click="router.push('/app/home')">
              <el-icon :size="18"><ArrowLeft /></el-icon>
            </button>
          </el-tooltip>
        </div>
        <div class="head-mid">
          <div class="title">相册</div>
          <div class="sub app-muted">上传的照片会按时间倒序展示</div>
        </div>
        <div class="head-right">
          <el-tooltip content="刷新" placement="bottom">
            <button class="iconbtn" type="button" :disabled="albumLoading || photoLoading" @click="() => { loadAlbums(); loadPhotos(true) }">
              <el-icon :size="18"><RefreshRight /></el-icon>
            </button>
          </el-tooltip>

          <el-tooltip :content="viewMode === 'grid' ? '封存回忆，到纪念日解锁' : '回到照片网格'" placement="bottom">
            <button class="iconbtn capsulebtn" type="button" @click="viewMode = viewMode === 'grid' ? 'capsule' : 'grid'">
              <span class="ico-tt">{{ viewMode === 'grid' ? '⏳' : '🧩' }}</span>
            </button>
          </el-tooltip>

          <el-tooltip :content="selecting ? '取消批量' : '批量'" placement="bottom">
            <button class="iconbtn" type="button" @click="toggleSelecting">
              <span class="ico-tt">{{ selecting ? '✕' : '✓' }}</span>
            </button>
          </el-tooltip>

          <el-button class="uploadbtn" size="small" type="primary" :disabled="isTrash" @click="openUpload">
            <el-icon :size="16"><UploadFilled /></el-icon>
            <span>上传</span>
          </el-button>

          <el-tooltip content="退出" placement="bottom">
            <button class="iconbtn danger" type="button" @click="logout">
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

      <div v-if="selecting" class="batchbar">
        <div class="batchleft">
          <el-tag effect="light" round>已选 {{ selectedCount }}</el-tag>
        </div>
        <div class="batchactions">
          <el-tooltip content="全选" placement="top">
            <button class="iconbtn" type="button" :disabled="!photos.length" @click="selectAllVisible">
              <span class="ico-tt">全</span>
            </button>
          </el-tooltip>
          <el-tooltip content="清空" placement="top">
            <button class="iconbtn" type="button" :disabled="!selectedCount" @click="clearSelection">
              <span class="ico-tt">空</span>
            </button>
          </el-tooltip>
        </div>
      </div>
    </div>

    <el-card class="app-card" shadow="never">
      <template #header>
        <div class="row">
          <div class="h">分类</div>
          <el-tooltip content="新增相册" placement="top">
            <button class="iconbtn" type="button" @click="openAlbumAdd">
              <el-icon :size="18"><Plus /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </template>
      <div class="chips">
        <button
          v-for="a in albums"
          :key="a.id"
          class="chip"
          :class="{ active: activeAlbumId === a.id, trash: a.name === '最近删除' }"
          type="button"
          @click="activeAlbumId = a.id"
        >
          <span class="name">{{ a.name }}</span>
          <el-tooltip :content="`共 ${a.photoCount || 0} 张`" placement="top">
            <span class="dot" aria-hidden="true" />
          </el-tooltip>
          <span v-if="a.deletable" class="ops">
            <el-button size="small" text @click.stop="openAlbumEdit(a)">
              <el-icon :size="14"><Edit /></el-icon>
            </el-button>
            <el-button size="small" text type="danger" @click.stop="removeAlbum(a)">
              <el-icon :size="14"><Delete /></el-icon>
            </el-button>
          </span>
        </button>
      </div>
    </el-card>

    <el-card v-if="viewMode === 'grid'" class="app-card filtercard" shadow="never">
      <template #header>
        <div class="row">
          <div class="h">筛选</div>
          <div class="filters">
            <el-select v-model="sortOrder" class="ctl" style="width: 120px">
              <el-option label="最新优先" value="desc" />
              <el-option label="最早优先" value="asc" />
            </el-select>
            <el-date-picker v-model="range.from" class="ctl" type="date" value-format="YYYY-MM-DD" placeholder="开始" />
            <el-date-picker v-model="range.to" class="ctl" type="date" value-format="YYYY-MM-DD" placeholder="结束" />
            <el-button size="small" class="clearbtn" :class="{ on: hasFilter }" @click="clearFilters">清空</el-button>
          </div>
        </div>
      </template>

      <div v-if="!photos.length" class="wrap">
        <div class="emptybox">
          <div class="illus" aria-hidden="true">
            <svg viewBox="0 0 220 140" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="dzAlbGlow" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="rgba(99, 102, 241, 0.22)" />
                  <stop offset="1" stop-color="rgba(59, 130, 246, 0.18)" />
                </linearGradient>
              </defs>
              <circle cx="110" cy="70" r="62" fill="url(#dzAlbGlow)" />
              <path
                d="M63 88c0-22 17-39 39-39h16c22 0 39 17 39 39v12c0 8-6 14-14 14H77c-8 0-14-6-14-14V88z"
                fill="rgba(255,255,255,0.72)"
                stroke="rgba(17,24,39,0.10)"
                stroke-width="2"
              />
              <path
                d="M88 69l9-10c2-2 6-2 8 0l5 6 5-6c2-2 6-2 8 0l9 10"
                fill="none"
                stroke="rgba(17,24,39,0.25)"
                stroke-width="3"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M84 100l20-22c2-2 6-2 8 0l10 11 6-7c2-2 6-2 8 0l16 18"
                fill="none"
                stroke="rgba(17,24,39,0.20)"
                stroke-width="3"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M110 34c7-10 19-10 26-2 7 8 3 20-5 27-7 6-18 15-21 18-3-3-14-12-21-18-8-7-12-19-5-27 7-8 19-8 26 2z"
                fill="rgba(236,72,153,0.10)"
                stroke="rgba(236,72,153,0.22)"
                stroke-width="2"
              />
            </svg>
          </div>
          <div class="empty-title">这里还没有你们的回忆</div>
          <div class="empty-sub app-muted">点击右上角「上传」，留下你们的每一个瞬间 ❤️</div>
          <button v-if="!isTrash" class="empty-cta" type="button" @click="openUpload">立即上传</button>
        </div>
      </div>

      <transition-group v-else name="fade" tag="div" class="grid">
        <div v-for="(p, idx) in photos" :key="p.id" class="cell">
          <button class="imgbtn" type="button" @click="selecting ? toggleSelected(p.id) : openViewer(idx)">
            <img :src="assetUrl(p.thumbUrl || p.url)" alt="" />
          </button>
          <button v-if="selecting" class="pick" type="button" :class="{ on: isSelected(p.id) }" @click.stop="toggleSelected(p.id)">
            <span v-if="isSelected(p.id)">✓</span>
          </button>
          <div class="hovermeta">
            <div v-if="timeText(p)" class="htext">{{ timeText(p) }}</div>
            <div v-if="p.location" class="hsub">{{ p.location }}</div>
          </div>
          <div v-if="p.cover" class="cover">封面</div>
          <template v-if="!selecting && isTrash">
            <el-button class="del" size="small" text @click="restorePhoto(p)">恢复</el-button>
            <el-button class="del2" size="small" text type="danger" @click="purgePhoto(p)">彻底删除</el-button>
          </template>
          <el-button v-else-if="!selecting" class="del" size="small" text type="danger" @click="removePhoto(p)">
            <el-icon :size="16"><Delete /></el-icon>
          </el-button>
        </div>
      </transition-group>
    </el-card>

    <el-card v-else class="app-card" shadow="never">
      <template #header>
        <div class="row">
          <div class="h">回忆胶囊</div>
          <div class="sub app-muted">按月份 + 分类自动聚合</div>
        </div>
      </template>
      <div v-if="!photos.length" class="wrap">
        <div class="emptybox">
          <div class="illus" aria-hidden="true">
            <svg viewBox="0 0 220 140" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="dzAlbGlow2" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="rgba(99, 102, 241, 0.18)" />
                  <stop offset="1" stop-color="rgba(139, 92, 246, 0.18)" />
                </linearGradient>
              </defs>
              <rect x="44" y="38" width="132" height="78" rx="22" fill="rgba(255,255,255,0.72)" stroke="rgba(17,24,39,0.10)" stroke-width="2" />
              <path d="M76 73h68" stroke="rgba(17,24,39,0.18)" stroke-width="3" stroke-linecap="round" />
              <path d="M76 90h54" stroke="rgba(17,24,39,0.14)" stroke-width="3" stroke-linecap="round" />
              <circle cx="110" cy="74" r="56" fill="url(#dzAlbGlow2)" />
              <path
                d="M110 40c8-11 20-11 27-3 8 9 3 22-6 29-7 6-18 15-21 18-3-3-14-12-21-18-9-7-14-20-6-29 7-8 19-8 27 3z"
                fill="rgba(236,72,153,0.10)"
                stroke="rgba(236,72,153,0.22)"
                stroke-width="2"
              />
            </svg>
          </div>
          <div class="empty-title">这里还没有你们的回忆</div>
          <div class="empty-sub app-muted">点击右上角「上传」，留下你们的每一个瞬间 ❤️</div>
          <button v-if="!isTrash" class="empty-cta" type="button" @click="openUpload">立即上传</button>
        </div>
      </div>
      <div v-else class="capsules">
        <div v-for="g in capsuleGroups" :key="g.key" class="cap">
          <div class="cap-title">{{ g.label }}</div>
          <div v-for="sg in g.subs" :key="sg.name" class="cap-sub">
            <div class="cap-subtitle">
              <span class="cap-name">{{ sg.name }}</span>
              <span class="cap-count app-muted">{{ sg.items.length }} 张</span>
            </div>
            <div class="cap-strip">
              <button
                v-for="p in sg.items.slice(0, 18)"
                :key="p.id"
                class="cap-img"
                type="button"
                @click="selecting ? toggleSelected(p.id) : openViewer(photos.findIndex((x) => x.id === p.id))"
              >
                <img :src="assetUrl(p.thumbUrl || p.url)" alt="" />
                <span v-if="selecting" class="cap-pick" :class="{ on: isSelected(p.id) }">
                  <span v-if="isSelected(p.id)">✓</span>
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>

  <el-dialog v-model="albumDialog" :title="albumEditingId ? '编辑分类' : '新增分类'" width="92%">
    <el-form label-position="top">
      <el-form-item label="分类名">
        <el-input v-model="albumForm.name" maxlength="20" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="albumSaving" @click="albumDialog = false">取消</el-button>
      <el-button type="primary" :loading="albumSaving" @click="saveAlbum">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="uploadDialog" title="上传照片" width="92%">
    <el-form label-position="top">
      <el-form-item label="上传到分类">
        <el-select v-model="uploadAlbumId" style="width: 100%">
          <el-option v-for="a in albums" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择照片（最多10张，单张≤10MB）">
        <el-upload
          v-model:file-list="uploadFiles"
          list-type="picture-card"
          :auto-upload="false"
          accept="image/*"
          :limit="10"
        >
          <el-icon :size="18"><UploadFilled /></el-icon>
        </el-upload>
      </el-form-item>
      <div v-if="uploadSaving || uploadPercent > 0" class="uprogress" :class="{ done: uploadDonePulse }">
        <el-progress :percentage="uploadPercent" :stroke-width="10" :show-text="false" />
        <div class="uprogress-text app-muted">{{ uploadPercent >= 100 ? '上传完成' : `上传中… ${uploadPercent}%` }}</div>
      </div>
    </el-form>
    <template #footer>
      <el-button :disabled="uploadSaving" @click="uploadDialog = false">取消</el-button>
      <el-button type="primary" :loading="uploadSaving" @click="uploadImages">开始上传</el-button>
    </template>
  </el-dialog>

  <div v-if="selecting" class="selbar">
    <div class="sel-left">
      <span class="sel-pill">已选 {{ selectedCount }}</span>
    </div>
    <div class="sel-actions">
      <el-tooltip content="清空" placement="top">
        <button class="selbtn" type="button" :disabled="!selectedCount" @click="clearSelection">清空</button>
      </el-tooltip>
      <el-tooltip content="移动" placement="top">
        <button class="selbtn" type="button" :disabled="isTrash || !selectedCount" @click="openBatchMove">移动</button>
      </el-tooltip>
      <el-tooltip content="删除" placement="top">
        <button class="selbtn danger" type="button" :disabled="!selectedCount || batchDeleteSaving" @click="batchDelete">删除</button>
      </el-tooltip>
      <el-tooltip content="下载" placement="top">
        <button class="selbtn" type="button" :disabled="!selectedCount || batchDownloadSaving" @click="batchDownload">下载</button>
      </el-tooltip>
    </div>
  </div>

  <el-dialog v-model="batchMoveDialog" title="批量移动" width="92%">
    <el-form label-position="top">
      <el-form-item label="移动到分类">
        <el-select v-model="batchMoveAlbumId" style="width: 100%">
          <el-option v-for="a in albums" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
      </el-form-item>
      <div class="app-muted">已选 {{ selectedCount }} 张</div>
    </el-form>
    <template #footer>
      <el-button :disabled="batchMoveSaving" @click="batchMoveDialog = false">取消</el-button>
      <el-button type="primary" :loading="batchMoveSaving" @click="doBatchMove">移动</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="photoDialog" title="一起看" width="96%" top="3vh">
    <div v-if="currentPhoto" class="pbox" :style="coverStyle">
      <div class="pimgwrap">
        <img class="pimg" :src="viewerSrc" alt="" />
        <div class="pmeta">
          <div class="pmeta-top">
            <div class="ptitle">
              <span v-if="currentPhoto.title">{{ currentPhoto.title }}</span>
              <span v-else class="app-muted">未设置标题</span>
            </div>
            <div class="pmeta-sub app-muted">
              <span>{{ authorLabel(currentPhoto.uploaderId) }}</span>
              <span v-if="timeText(currentPhoto)"> · {{ timeText(currentPhoto) }}</span>
            </div>
          </div>
          <div class="pmeta-tags">
            <el-tag v-if="currentPhoto.location" effect="light" round size="small">{{ currentPhoto.location }}</el-tag>
            <el-tag v-if="currentPhoto.mood" effect="light" round size="small">{{ currentPhoto.mood }}</el-tag>
            <el-tag v-for="t in tagsList(currentPhoto)" :key="t" effect="light" round size="small">{{ t }}</el-tag>
          </div>
          <div class="pactions">
            <el-button size="small" @click="prevPhoto">上一张</el-button>
            <el-button size="small" @click="nextPhoto">下一张</el-button>
            <el-button size="small" @click="toggleSlideshow">{{ slideshowPlaying ? '暂停' : '幻灯片' }}</el-button>
            <el-button size="small" @click="viewerQuality = viewerQuality === 'original' ? 'thumb' : 'original'">
              {{ viewerQuality === 'original' ? '原图' : '缩略' }}
            </el-button>
            <el-button size="small" :type="currentPhoto.cover ? 'primary' : 'default'" @click="toggleCover">设为封面</el-button>
            <el-button size="small" text @click="toggleLike">
              <el-icon :size="16"><component :is="currentPhoto.liked ? StarFilled : Star" /></el-icon>
              <span>{{ currentPhoto.liked ? '已赞' : '点赞' }}</span>
              <span class="app-muted">({{ currentPhoto.likeCount || 0 }})</span>
            </el-button>
            <el-button size="small" text @click="openComments">
              <el-icon :size="16"><ChatDotRound /></el-icon>
              <span>评论</span>
              <span class="app-muted">({{ currentPhoto.commentCount || 0 }})</span>
            </el-button>
            <el-button v-if="isMine(currentPhoto)" size="small" text @click="openMeta">
              <el-icon :size="16"><Edit /></el-icon>
              <span>编辑信息</span>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="metaDialog" title="编辑照片信息" width="92%">
    <el-form label-position="top">
      <el-form-item label="标题（可选）">
        <el-input v-model="metaForm.title" maxlength="64" />
      </el-form-item>
      <el-form-item label="地点（可选）">
        <el-input v-model="metaForm.location" maxlength="64" placeholder="比如：杭州/西湖" />
      </el-form-item>
      <el-form-item label="心情（可选）">
        <el-input v-model="metaForm.mood" maxlength="16" placeholder="比如：开心/想你" />
      </el-form-item>
      <el-form-item label="标签（可选，逗号分隔）">
        <el-input v-model="metaForm.tags" maxlength="255" placeholder="旅行, 夕阳, 海边" />
      </el-form-item>
      <el-form-item label="拍摄时间（可选）">
        <el-input v-model="metaForm.shotAt" placeholder="yyyy-MM-dd HH:mm:ss" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="metaSaving" @click="metaDialog = false">取消</el-button>
      <el-button type="primary" :loading="metaSaving" @click="saveMeta">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="commentDialog" title="照片评论" width="92%">
    <div class="cbox">
      <div class="chead">
        <div class="chead-top">
          <div class="chead-title">一起看</div>
          <el-button size="small" :loading="commentLoading" @click="loadPhotoComments()">
            <el-icon :size="16"><RefreshRight /></el-icon>
            <span>刷新</span>
          </el-button>
        </div>
        <div class="chead-meta app-muted">
          <span>{{ authorLabel(currentPhoto?.uploaderId) }}</span>
          <span v-if="timeText(currentPhoto)"> · {{ timeText(currentPhoto) }}</span>
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
              <el-button v-if="isMyAuthorId(c.authorId)" size="small" text type="danger" @click="removeComment(c)">删除</el-button>
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
  padding: 12px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.06), rgba(139, 92, 246, 0.06), rgba(255, 255, 255, 0.85));
  border-color: rgba(139, 92, 246, 0.1);
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 8px 10px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
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
  text-align: center;
}
.head-right {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  flex: 0 0 auto;
}
.title {
  font-weight: 600;
  letter-spacing: 0.2px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.92), rgba(99, 102, 241, 0.86));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 0.5px rgba(17, 24, 39, 0.15);
}
.sub {
  margin-top: 6px;
  font-size: 12px;
}
.iconbtn {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  place-items: center;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.72);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, background 160ms ease, color 160ms ease;
}
.iconbtn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.iconbtn.danger {
  color: rgba(239, 68, 68, 0.9);
  border-color: rgba(239, 68, 68, 0.18);
}
.ico-tt {
  font-weight: 900;
  font-size: 13px;
  line-height: 1;
}
.uploadbtn {
  border-radius: 999px !important;
  padding: 8px 14px !important;
  font-weight: 800;
}
:deep(.uploadbtn.el-button--primary) {
  background: #3b82f6;
  border-color: #3b82f6;
}
@media (hover: hover) {
  .iconbtn:hover:not(:disabled) {
    transform: translateY(-1px);
    border-color: rgba(99, 102, 241, 0.22);
    color: rgba(99, 102, 241, 0.92);
    box-shadow: 0 14px 30px rgba(99, 102, 241, 0.12);
    background: rgba(255, 255, 255, 0.9);
  }
  .iconbtn.danger:hover:not(:disabled) {
    border-color: rgba(239, 68, 68, 0.28);
    color: rgba(239, 68, 68, 0.92);
    box-shadow: 0 14px 30px rgba(239, 68, 68, 0.12);
  }
  :deep(.uploadbtn.el-button--primary:hover) {
    transform: translateY(-1px);
    filter: brightness(0.9);
    box-shadow: 0 16px 34px rgba(59, 130, 246, 0.24);
  }
}

.batchbar {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 8px 6px;
}
.batchactions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.h {
  font-weight: 800;
}
.chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.chip {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  padding: 8px 12px;
  padding-right: 56px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.86);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, background 160ms ease, color 160ms ease;
}
.chip.active {
  border-color: rgba(99, 102, 241, 0.32);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.92), rgba(139, 92, 246, 0.92));
  color: #fff;
}
.chip.trash {
  background: rgba(17, 24, 39, 0.06);
  border-color: rgba(17, 24, 39, 0.08);
}
.name {
  font-weight: 800;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.22);
}
.chip.active .dot {
  background: rgba(255, 255, 255, 0.78);
}
.ops {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%) translateX(-2px);
  display: inline-flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 160ms ease, transform 160ms ease;
}
@media (hover: hover) {
  .chip:hover:not(.active) {
    border-color: rgba(99, 102, 241, 0.22);
    background: rgba(99, 102, 241, 0.05);
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(99, 102, 241, 0.08);
  }
  .chip.active:hover {
    transform: translateY(-1px);
    box-shadow: 0 14px 30px rgba(99, 102, 241, 0.12);
  }
  .chip:hover .ops {
    opacity: 1;
    transform: translateX(0);
  }
}
.filters {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.filtercard :deep(.el-card__header) {
  position: relative;
  padding-bottom: 12px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.04);
}
.ctl {
  --ctl-bd: rgba(17, 24, 39, 0.10);
}
.ctl :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--ctl-bd);
  border-radius: 14px;
  box-shadow: none;
  transition: border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}
.ctl :deep(.el-input__prefix),
.ctl :deep(.el-input__suffix) {
  color: rgba(17, 24, 39, 0.42);
}
.ctl :deep(.el-input__icon) {
  color: rgba(17, 24, 39, 0.42);
}
.ctl :deep(.el-input__inner) {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.86);
}
.ctl :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.32);
}
.ctl :deep(.el-input__wrapper.is-focus),
.ctl :deep(.el-input__wrapper:focus-within) {
  border-color: rgba(99, 102, 241, 0.38);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.10);
}
.clearbtn {
  border-radius: 999px !important;
  background: rgba(255, 255, 255, 0.72) !important;
  border: 1px solid rgba(17, 24, 39, 0.10) !important;
  color: rgba(17, 24, 39, 0.72) !important;
  font-weight: 700;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, color 160ms ease, background 160ms ease;
}
.clearbtn.on {
  border-color: rgba(99, 102, 241, 0.34) !important;
  color: rgba(99, 102, 241, 0.92) !important;
  background: rgba(99, 102, 241, 0.06) !important;
}
@media (hover: hover) {
  .clearbtn:hover {
    transform: translateY(-1px);
    box-shadow: 0 14px 30px rgba(99, 102, 241, 0.10);
  }
}
.wrap {
  padding: 8px 0 6px;
}
.emptybox {
  padding: 10px 6px 12px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.62));
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.illus {
  width: min(320px, 86%);
  margin: 2px auto 8px;
  filter: drop-shadow(0 18px 38px rgba(99, 102, 241, 0.10));
}
.empty-title {
  font-weight: 800;
  color: rgba(17, 24, 39, 0.86);
}
.empty-sub {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
}
.empty-cta {
  margin-top: 10px;
  border: 1px solid rgba(99, 102, 241, 0.20);
  background: rgba(99, 102, 241, 0.08);
  color: rgba(99, 102, 241, 0.92);
  border-radius: 999px;
  padding: 9px 14px;
  font-weight: 900;
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, background 160ms ease;
}
@media (hover: hover) {
  .empty-cta:hover {
    transform: translateY(-1px);
    border-color: rgba(99, 102, 241, 0.28);
    box-shadow: 0 18px 40px rgba(99, 102, 241, 0.14);
    background: rgba(99, 102, 241, 0.12);
  }
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
@media (max-width: 980px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 520px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.cell {
  position: relative;
}
.imgbtn {
  width: 100%;
  aspect-ratio: 1 / 1;
  border: 0;
  padding: 0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.62);
  border: 2px solid rgba(17, 24, 39, 0.08);
  position: relative;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}
.imgbtn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.04), rgba(59, 130, 246, 0.03));
  pointer-events: none;
}
.imgbtn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(1.02) brightness(1.02);
}
.hovermeta {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 8px;
  padding: 7px 8px;
  border-radius: 12px;
  background: rgba(17, 24, 39, 0.42);
  color: rgba(255, 255, 255, 0.92);
  pointer-events: none;
  opacity: 1;
  transform: translateY(0);
  transition: opacity 160ms ease, transform 160ms ease;
}
.htext {
  font-size: 12px;
  line-height: 1.2;
  font-weight: 900;
}
.hsub {
  margin-top: 3px;
  font-size: 11px;
  opacity: 0.9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (hover: hover) {
  .hovermeta {
    opacity: 0;
    transform: translateY(6px);
  }
  .cell:hover .hovermeta {
    opacity: 1;
    transform: translateY(0);
  }
  .cell:hover .imgbtn {
    transform: scale(1.02);
    border-color: rgba(99, 102, 241, 0.18);
    box-shadow: 0 18px 38px rgba(17, 24, 39, 0.10);
  }
}
.pick {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: rgba(17, 24, 39, 0.35);
  color: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  cursor: pointer;
  padding: 0;
  backdrop-filter: blur(10px);
  transition: transform 160ms ease, background 160ms ease, border-color 160ms ease;
}
.pick.on {
  background: rgba(99, 102, 241, 0.92);
  border-color: rgba(99, 102, 241, 0.92);
}
.pick span {
  transform: translateY(-0.5px);
}
.cell .del {
  opacity: 0;
  transform: translateY(-2px);
  transition: opacity 160ms ease, transform 160ms ease;
}
@media (hover: hover) {
  .cell:hover .del {
    opacity: 1;
    transform: translateY(0);
  }
}
.del {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
}
.del2 {
  position: absolute;
  bottom: 6px;
  right: 6px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
}
.cover {
  position: absolute;
  left: 8px;
  top: 8px;
  padding: 4px 6px;
  font-size: 11px;
  line-height: 1;
  color: rgba(17, 24, 39, 0.85);
  background: rgba(255, 255, 255, 0.86);
  border-radius: 10px;
  pointer-events: none;
}
.capsules {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.cap-title {
  font-weight: 900;
  margin-bottom: 6px;
}
.cap-sub {
  padding: 10px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  margin-bottom: 10px;
}
.cap-subtitle {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.cap-name {
  font-weight: 800;
}
.cap-strip {
  display: flex;
  gap: 10px;
  overflow: auto;
  padding-bottom: 2px;
}
.cap-img {
  position: relative;
  width: 84px;
  height: 84px;
  border: 0;
  padding: 0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.62);
  border: 2px solid rgba(17, 24, 39, 0.08);
  flex: 0 0 auto;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}
.cap-img::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.04), rgba(59, 130, 246, 0.03));
  pointer-events: none;
}
.cap-pick {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: rgba(17, 24, 39, 0.35);
  color: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 12px;
}
.cap-pick.on {
  background: rgba(99, 102, 241, 0.92);
  border-color: rgba(99, 102, 241, 0.92);
}
.cap-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(1.02) brightness(1.02);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.selbar {
  position: fixed;
  left: 14px;
  right: 14px;
  bottom: calc(64px + env(safe-area-inset-bottom) + 12px);
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 42px rgba(17, 24, 39, 0.10);
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  z-index: 9;
}
.sel-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
  color: rgba(99, 102, 241, 0.92);
  font-weight: 900;
  font-size: 12px;
}
.sel-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.selbtn {
  border: 1px solid rgba(17, 24, 39, 0.10);
  background: rgba(255, 255, 255, 0.76);
  color: rgba(17, 24, 39, 0.78);
  border-radius: 999px;
  padding: 7px 12px;
  font-weight: 900;
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, background 160ms ease, color 160ms ease;
}
.selbtn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.selbtn.danger {
  border-color: rgba(239, 68, 68, 0.18);
  color: rgba(239, 68, 68, 0.92);
}
@media (hover: hover) {
  .selbtn:hover:not(:disabled) {
    transform: translateY(-1px);
    border-color: rgba(99, 102, 241, 0.22);
    box-shadow: 0 14px 30px rgba(99, 102, 241, 0.10);
  }
}
.uprogress {
  margin-top: 10px;
  padding: 10px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.62);
}
.uprogress-text {
  margin-top: 8px;
  font-size: 12px;
  text-align: center;
}
.uprogress.done {
  animation: upPulse 520ms ease 1;
}
@keyframes upPulse {
  0% {
    transform: scale(1);
  }
  60% {
    transform: scale(1.01);
  }
  100% {
    transform: scale(1);
  }
}
.capsulebtn .ico-tt {
  display: inline-block;
  transition: transform 200ms ease;
}
@media (hover: hover) {
  .capsulebtn:hover .ico-tt {
    transform: rotate(-12deg);
  }
}
.pbox {
  --pbg: none;
}
.pimgwrap {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
}
.pimgwrap::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--pbg);
  background-size: cover;
  background-position: center;
  opacity: 0.18;
  filter: blur(10px);
  transform: scale(1.08);
  pointer-events: none;
}
.pimg {
  position: relative;
  z-index: 1;
  width: 100%;
  max-height: 58vh;
  object-fit: contain;
  display: block;
  background: rgba(0, 0, 0, 0.08);
}
.pmeta {
  position: relative;
  z-index: 1;
  padding: 12px;
}
.ptitle {
  font-weight: 900;
}
.pmeta-sub {
  margin-top: 6px;
  font-size: 12px;
}
.pmeta-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pactions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
</style>

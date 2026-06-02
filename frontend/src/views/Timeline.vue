<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Calendar, Picture, Notebook, Star, Clock, RefreshRight } from '@element-plus/icons-vue'
import http from '../api/http'

const apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const assetUrl = (u) => {
  const s = String(u || '')
  if (!s) return ''
  if (/^(https?:)?\/\//.test(s) || s.startsWith('blob:') || s.startsWith('data:')) return s
  if (s.startsWith('/') && apiBase) return apiBase + s
  return s
}

const router = useRouter()

const loading = ref(false)
const items = ref([])
const hasMore = ref(false)

const range = reactive({ from: '', to: '' })
const types = reactive({
  DIARY: true,
  PHOTO: true,
  ANNIVERSARY: true,
  WISH_DONE: true,
  SCRATCH_DONE: true
})

const fmtDateTime = (d, end) => {
  if (!d) return ''
  const dt = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(dt.getTime())) return ''
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const dd = String(dt.getDate()).padStart(2, '0')
  const hh = end ? '23' : '00'
  const mm = end ? '59' : '00'
  const ss = end ? '59' : '00'
  return `${y}-${m}-${dd} ${hh}:${mm}:${ss}`
}

const fmtDateTimeFull = (d) => {
  if (!d) return ''
  const dt = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(dt.getTime())) return ''
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const dd = String(dt.getDate()).padStart(2, '0')
  const hh = String(dt.getHours()).padStart(2, '0')
  const mm = String(dt.getMinutes()).padStart(2, '0')
  const ss = String(dt.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${dd} ${hh}:${mm}:${ss}`
}

const pickTypesParam = () => {
  const arr = Object.entries(types)
    .filter(([, v]) => !!v)
    .map(([k]) => k)
  return arr.length === 5 ? undefined : arr.join(',')
}

const buildParams = (cursor) => {
  const params = { limit: 30 }
  const t = pickTypesParam()
  if (t) params.types = t
  if (range.from) params.from = fmtDateTime(range.from, false)
  if (range.to) params.to = fmtDateTime(range.to, true)
  if (cursor?.eventAt && cursor?.refId != null && cursor?.typeRank != null) {
    params.beforeAt = fmtDateTimeFull(new Date(cursor.eventAt))
    params.beforeId = cursor.refId
    params.beforeTypeRank = cursor.typeRank
  }
  return params
}

const load = async (silent) => {
  try {
    loading.value = true
    const list = (await http.get('/api/timeline', { params: buildParams(null) })) || []
    items.value = Array.isArray(list) ? list : []
    hasMore.value = items.value.length >= 30
    if (!silent) ElMessage.success({ message: '已刷新', customClass: 'dz-soft-success' })
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  const cur = items.value?.[items.value.length - 1]
  if (!cur?.eventAt || cur?.refId == null || cur?.typeRank == null) return
  try {
    loading.value = true
    const more = (await http.get('/api/timeline', { params: buildParams(cur) })) || []
    const arr = Array.isArray(more) ? more : []
    if (!arr.length) {
      hasMore.value = false
      return
    }
    const keyOf = (x) => `${x?.type || ''}_${x?.refId || ''}_${x?.eventAt || ''}`
    const seen = new Set(items.value.map(keyOf))
    const merged = items.value.slice()
    for (const it of arr) {
      const k = keyOf(it)
      if (!k || seen.has(k)) continue
      merged.push(it)
      seen.add(k)
    }
    items.value = merged
    hasMore.value = arr.length >= 30
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => load(true))

watch(
  () => [range.from, range.to, types.DIARY, types.PHOTO, types.ANNIVERSARY, types.WISH_DONE, types.SCRATCH_DONE],
  () => load(true)
)

const typeMeta = (t) => {
  if (t === 'DIARY') return { name: '日记', icon: Notebook, tone: 'violet', go: () => router.push({ path: '/app/records', query: { tab: 'diary' } }) }
  if (t === 'PHOTO') return { name: '相册', icon: Picture, tone: 'green', go: () => router.push('/app/albums') }
  if (t === 'ANNIVERSARY') return { name: '纪念日', icon: Calendar, tone: 'amber', go: () => router.push({ path: '/app/records', query: { tab: 'ann' } }) }
  if (t === 'WISH_DONE') return { name: '心愿', icon: Star, tone: 'pink', go: () => router.push({ path: '/app/records', query: { tab: 'wishlist' } }) }
  if (t === 'SCRATCH_DONE') return { name: '刮刮乐', icon: Clock, tone: 'blue', go: () => router.push({ path: '/app/records', query: { tab: 'wish' } }) }
  return { name: '动态', icon: Clock, tone: 'gray', go: () => router.push('/app/home') }
}

const timeText = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${dd} ${hh}:${mm}`
}

const openDetail = (it) => {
  const type = String(it?.type || '')
  const id = it?.refId
  if (type === 'DIARY' && id != null) {
    router.push({ path: '/app/records', query: { tab: 'diary', open: 'diary', id } })
    return
  }
  if (type === 'PHOTO' && id != null) {
    router.push({ path: '/app/albums', query: { open: 'photo', id } })
    return
  }
  if (type === 'ANNIVERSARY' && id != null) {
    router.push({ path: '/app/records', query: { tab: 'ann', open: 'ann', id } })
    return
  }
  if (type === 'WISH_DONE' && id != null) {
    router.push({ path: '/app/records', query: { tab: 'wishlist', open: 'wishdone', id } })
    return
  }
  if (type === 'SCRATCH_DONE' && id != null) {
    router.push({ path: '/app/records', query: { tab: 'wish', open: 'scratch', id } })
    return
  }
  typeMeta(type).go()
}
</script>

<template>
  <div class="wrap">
    <div class="head app-card">
      <div class="left">
        <button type="button" class="backbtn" aria-label="返回" @click="router.push('/app/home')">
          <el-icon :size="18"><ArrowLeft /></el-icon>
        </button>
        <div>
          <div class="h">恋爱时间轴</div>
          <div class="sub app-muted">把你们的点滴，按时间串起来。</div>
        </div>
      </div>
      <div class="actions">
        <el-tooltip content="刷新" placement="bottom">
          <button class="iconbtn" type="button" :disabled="loading" @click="load()">
            <el-icon :size="18"><RefreshRight /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>

    <div class="filters app-card">
      <div class="frow">
        <div class="pills">
          <button class="pill" :class="{ active: types.DIARY }" type="button" @click="types.DIARY = !types.DIARY">日记</button>
          <button class="pill" :class="{ active: types.PHOTO }" type="button" @click="types.PHOTO = !types.PHOTO">相册</button>
          <button class="pill" :class="{ active: types.ANNIVERSARY }" type="button" @click="types.ANNIVERSARY = !types.ANNIVERSARY">纪念日</button>
          <button class="pill" :class="{ active: types.WISH_DONE }" type="button" @click="types.WISH_DONE = !types.WISH_DONE">心愿</button>
          <button class="pill" :class="{ active: types.SCRATCH_DONE }" type="button" @click="types.SCRATCH_DONE = !types.SCRATCH_DONE">刮刮乐</button>
        </div>
        <el-date-picker v-model="range.from" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" placeholder="开始日期" class="date" />
        <el-date-picker v-model="range.to" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" placeholder="结束日期" class="date" />
      </div>
      <div class="hint app-muted">排序：时间倒序。支持按模块与时间范围筛选。</div>
    </div>

    <div class="list app-card">
      <div v-if="loading && !items.length" class="empty app-muted">正在加载…</div>
      <div v-else-if="!items.length" class="empty app-muted">暂无时间轴数据</div>
      <div v-else class="tline">
        <el-timeline>
          <el-timeline-item v-for="it in items" :key="`${it.type}_${it.refId}_${it.eventAt}`" :timestamp="timeText(it.eventAt)">
            <button class="item" type="button" @click="openDetail(it)">
              <div class="icon" :class="typeMeta(it.type).tone">
                <el-icon :size="18"><component :is="typeMeta(it.type).icon" /></el-icon>
              </div>
              <div class="main">
                <div class="title">
                  <span class="t">{{ it.title || typeMeta(it.type).name }}</span>
                  <span class="tag">{{ typeMeta(it.type).name }}</span>
                </div>
                <div class="content app-muted">{{ it.content || '—' }}</div>
              </div>
              <div v-if="it.thumbUrl" class="thumb">
                <img :src="assetUrl(it.thumbUrl)" alt="" />
              </div>
            </button>
          </el-timeline-item>
        </el-timeline>
        <div class="more">
          <el-button v-if="hasMore" size="small" :loading="loading" @click="loadMore">加载更多</el-button>
          <div v-else class="app-muted">没有更多了</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  display: grid;
  gap: 12px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(139, 92, 246, 0.12), rgba(59, 130, 246, 0.08)),
    rgba(255, 255, 255, 0.62);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}
.left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.backbtn {
  border: 0;
  background: transparent;
  width: 34px;
  height: 34px;
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
.h {
  font-size: 16px;
  font-weight: 950;
}
.sub {
  margin-top: 6px;
  font-size: 12px;
}
.actions {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.iconbtn {
  appearance: none;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}
.iconbtn:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.1);
}
.iconbtn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.filters {
  padding: 12px;
  border-radius: 16px;
}
.frow {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.pills {
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}
.pill {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.7);
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}
.pill.active {
  color: rgba(17, 24, 39, 0.92);
  border-color: rgba(99, 102, 241, 0.28);
  background: rgba(139, 92, 246, 0.12);
}
.pill:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.28);
  background: rgba(99, 102, 241, 0.08);
}
.date {
  width: 138px;
}
.hint {
  margin-top: 10px;
  font-size: 12px;
}
.list {
  padding: 12px;
  border-radius: 16px;
}
.empty {
  padding: 18px 14px;
  text-align: center;
}
.item {
  width: 100%;
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: 10px;
  align-items: center;
  text-align: left;
  padding: 10px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.62);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}
.item:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.26);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06);
}
.icon {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.8);
}
.icon.violet {
  background: rgba(139, 92, 246, 0.14);
  color: rgba(124, 58, 237, 0.9);
}
.icon.green {
  background: rgba(52, 211, 153, 0.12);
  color: rgba(5, 150, 105, 0.9);
}
.icon.amber {
  background: rgba(251, 146, 60, 0.12);
  color: rgba(234, 88, 12, 0.9);
}
.icon.pink {
  background: rgba(139, 92, 246, 0.12);
  color: rgba(99, 102, 241, 0.92);
}
.icon.blue {
  background: rgba(96, 165, 250, 0.12);
  color: rgba(37, 99, 235, 0.9);
}
.icon.gray {
  background: rgba(148, 163, 184, 0.12);
  color: rgba(71, 85, 105, 0.9);
}
.title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.t {
  font-weight: 950;
  color: rgba(17, 24, 39, 0.92);
}
.tag {
  font-size: 12px;
  font-weight: 900;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.68);
  color: rgba(17, 24, 39, 0.55);
}
.content {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
}
.thumb {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.85);
  flex: 0 0 auto;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.more {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}
</style>

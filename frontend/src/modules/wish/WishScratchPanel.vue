<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Plus, RefreshRight } from '@element-plus/icons-vue'
import http from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import ScratchCard from './ScratchCard.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const list = ref([])
const tab = ref('pending')

const dialog = ref(false)
const saving = ref(false)
const form = reactive({ content: '', revealMode: 0 })
const createdTip = ref(false)
let createdTimer = 0
let pollTimer = 0

const localRevealed = reactive({})
const fx = reactive({ id: null, hearts: [], stars: [] })
let fxTimer = 0

const makeFx = (id) => {
  const hearts = Array.from({ length: 11 }).map(() => ({
    x: Math.round(Math.random() * 90) + 5,
    d: Math.round(Math.random() * 420),
    dur: Math.round(720 + Math.random() * 520),
    s: Math.round(10 + Math.random() * 10),
    o: +(0.45 + Math.random() * 0.4).toFixed(2)
  }))
  const stars = Array.from({ length: 9 }).map(() => ({
    x: Math.round(Math.random() * 90) + 5,
    y: Math.round(Math.random() * 68) + 10,
    d: Math.round(Math.random() * 360),
    dur: Math.round(520 + Math.random() * 460),
    s: Math.round(8 + Math.random() * 10),
    o: +(0.35 + Math.random() * 0.45).toFixed(2)
  }))
  fx.id = id
  fx.hearts = hearts
  fx.stars = stars
  if (fxTimer) window.clearTimeout(fxTimer)
  fxTimer = window.setTimeout(() => {
    fx.id = null
    fx.hearts = []
    fx.stars = []
    fxTimer = 0
  }, 1200)
}

const myId = computed(() => auth?.user?.id)
const isMyScratched = (it) => {
  const uid = myId.value
  if (!uid) return false
  return it?.scratchedBy1 === uid || it?.scratchedBy2 === uid
}

const load = async (silent) => {
  try {
    loading.value = true
    list.value = await http.get('/api/wishes/scratch')
    if (!silent) ElMessage.success({ message: '已刷新', customClass: 'dz-soft-success' })
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
    tryOpenFromQuery()
  }
}

onMounted(() => {
  load(true)
  pollTimer = window.setInterval(() => {
    if (document.visibilityState === 'visible') load(true)
  }, 5000)
})

watch(
  () => [route.query.open, route.query.id],
  () => {
    const open = String(route.query.open || '')
    const id = Number(route.query.id)
    if (open === 'scratch' && Number.isFinite(id)) {
      pendingOpen.value = id
      tryOpenFromQuery()
    }
  },
  { immediate: true }
)

watch(
  () => list.value,
  () => {
    tryOpenFromQuery()
  }
)
onBeforeUnmount(() => {
  if (createdTimer) window.clearTimeout(createdTimer)
  createdTimer = 0
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = 0
  if (fxTimer) window.clearTimeout(fxTimer)
  fxTimer = 0
})

const pending = computed(() => (Array.isArray(list.value) ? list.value.filter((x) => (x?.status ?? 0) === 0) : []))
const done = computed(() => (Array.isArray(list.value) ? list.value.filter((x) => (x?.status ?? 0) === 1) : []))

const detailDialog = ref(false)
const detailItem = ref(null)
const openDetail = (it) => {
  detailItem.value = it || null
  detailDialog.value = true
}

const pendingOpen = ref(null)
const lastOpenKey = ref('')
const clearOpenQuery = () => {
  const q = { ...route.query }
  delete q.open
  delete q.id
  router.replace({ path: route.path, query: q })
}
const tryOpenFromQuery = () => {
  if (!pendingOpen.value) return
  const id = pendingOpen.value
  const it = done.value.find((x) => x?.id === id) || pending.value.find((x) => x?.id === id)
  if (!it) return
  const key = `scratch_${id}`
  if (lastOpenKey.value === key) return
  lastOpenKey.value = key
  pendingOpen.value = null
  tab.value = (it?.status ?? 0) === 1 ? 'done' : 'pending'
  openDetail(it)
  clearOpenQuery()
}

const openCreate = () => {
  form.content = ''
  form.revealMode = 0
  dialog.value = true
}

const create = async () => {
  const content = String(form.content || '').trim()
  if (!content) {
    ElMessage.error('请输入心愿内容')
    return
  }
  if (content.length > 50) {
    ElMessage.error('心愿内容长度需≤50字')
    return
  }
  try {
    saving.value = true
    await http.post('/api/wishes/scratch', { content, revealMode: form.revealMode })
    createdTip.value = true
    if (createdTimer) window.clearTimeout(createdTimer)
    createdTimer = window.setTimeout(() => {
      createdTip.value = false
      createdTimer = 0
    }, 4200)
    dialog.value = false
    await load(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const scratchingId = ref(null)

const scratch = async (it) => {
  if (!it?.id) return
  if ((it.status ?? 0) === 1) return
  if (scratchingId.value) return
  if (isMyScratched(it)) return
  try {
    scratchingId.value = it.id
    localRevealed[it.id] = true
    makeFx(it.id)
    const updated = await http.post(`/api/wishes/scratch/${it.id}/scratch`)
    list.value = (Array.isArray(list.value) ? list.value : []).map((x) => (x?.id === it.id ? { ...x, ...updated } : x))
    const unlocked = (updated?.status ?? 0) === 1
    if (unlocked) {
      ElMessage.success({ message: '解锁成功', customClass: 'dz-soft-success' })
    } else {
      ElMessage.success({ message: '你已刮开，等 TA 一起解锁', customClass: 'dz-soft-success' })
    }
  } catch (e) {
    ElMessage.error(e?.message || '刮开失败')
    await load(true)
  } finally {
    scratchingId.value = null
  }
}

const saveImage = async (it) => {
  if (!it?.id || (it.status ?? 0) !== 1) return
  const width = 1080
  const height = 720
  const canvas = document.createElement('canvas')
  canvas.width = width * 2
  canvas.height = height * 2
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.scale(2, 2)

  const bg = ctx.createLinearGradient(0, 0, width, height)
  bg.addColorStop(0, 'rgba(139, 92, 246, 0.16)')
  bg.addColorStop(0.55, 'rgba(139, 92, 246, 0.16)')
  bg.addColorStop(1, 'rgba(59, 130, 246, 0.12)')
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, width, height)

  ctx.fillStyle = 'rgba(255, 255, 255, 0.88)'
  roundRect(ctx, 60, 70, width - 120, height - 140, 34)
  ctx.fill()

  ctx.fillStyle = 'rgba(17, 24, 39, 0.7)'
  ctx.font = '900 34px system-ui, -apple-system, Segoe UI, Roboto, PingFang SC, Microsoft YaHei'
  ctx.textAlign = 'center'
  ctx.fillText('我们的心愿', width / 2, 160)

  const content = String(it.content || '').trim()
  ctx.fillStyle = 'rgba(17, 24, 39, 0.88)'
  ctx.font = '950 54px system-ui, -apple-system, Segoe UI, Roboto, PingFang SC, Microsoft YaHei'
  drawWrapText(ctx, content, width / 2, 300, width - 220, 70, 3)

  ctx.font = '900 24px system-ui, -apple-system, Segoe UI, Roboto, PingFang SC, Microsoft YaHei'
  ctx.fillStyle = 'rgba(17, 24, 39, 0.56)'
  const at = it.scratchedAt ? new Date(it.scratchedAt).toLocaleString() : ''
  ctx.fillText(at ? `刮开时间：${at}` : '刮开时间：-', width / 2, height - 180)

  ctx.fillStyle = 'rgba(99, 102, 241, 0.75)'
  ctx.font = '900 22px system-ui, -apple-system, Segoe UI, Roboto, PingFang SC, Microsoft YaHei'
  ctx.fillText('DZ 情侣站 · 心愿刮刮乐', width / 2, height - 120)

  const url = canvas.toDataURL('image/png')
  const a = document.createElement('a')
  a.href = url
  a.download = `wish_${it.id}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

const addToWishList = async (it) => {
  if (!it?.id || (it.status ?? 0) !== 1) return
  try {
    const content = String(it.content || '').trim()
    if (!content) return
    await http.post('/api/wish-list', {
      content,
      expectedAt: todayStr(),
      priority: 1,
      remark: '来自心愿刮刮乐',
      sourceType: 'SCRATCH_FROM',
      sourceId: it.id
    })
    ElMessage.success({ message: '已加入心愿清单', customClass: 'dz-soft-success' })
  } catch (e) {
    ElMessage.error(e?.message || '加入失败')
  }
}

const todayStr = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const roundRect = (ctx, x, y, w, h, r) => {
  const rr = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + rr, y)
  ctx.arcTo(x + w, y, x + w, y + h, rr)
  ctx.arcTo(x + w, y + h, x, y + h, rr)
  ctx.arcTo(x, y + h, x, y, rr)
  ctx.arcTo(x, y, x + w, y, rr)
  ctx.closePath()
}

const drawWrapText = (ctx, text, cx, y, maxWidth, lineHeight, maxLines) => {
  const chars = Array.from(String(text || ''))
  const lines = []
  let cur = ''
  for (const ch of chars) {
    const next = cur + ch
    if (ctx.measureText(next).width > maxWidth && cur) {
      lines.push(cur)
      cur = ch
      if (lines.length >= maxLines) break
    } else {
      cur = next
    }
  }
  if (lines.length < maxLines && cur) lines.push(cur)
  const use = lines.slice(0, maxLines)
  const start = y - ((use.length - 1) * lineHeight) / 2
  use.forEach((ln, i) => ctx.fillText(ln, cx, start + i * lineHeight))
}
</script>

<template>
  <div class="wrap">
    <div class="head app-card">
      <div class="left">
        <div class="h">
          <span class="heart">❤</span>
          <span>心愿刮刮乐</span>
        </div>
        <div class="sub app-muted">和 TA 一起，刮开属于你们的小惊喜❤️</div>
      </div>
      <div class="actions">
        <el-tooltip v-if="createdTip" content="已生成一张新的心愿刮刮卡" placement="bottom">
          <div class="created">
            <span class="ck" />
            <span>已生成</span>
          </div>
        </el-tooltip>
        <el-tooltip content="刷新" placement="bottom">
          <button class="iconbtn" type="button" :disabled="loading" @click="load()">
            <el-icon :size="18"><RefreshRight /></el-icon>
          </button>
        </el-tooltip>
        <el-button class="primary" type="primary" round @click="openCreate">
          <el-icon :size="16"><Plus /></el-icon>
          <span>新建</span>
        </el-button>
      </div>
    </div>

    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'pending' }" type="button" @click="tab = 'pending'">未刮</button>
      <button class="tab" :class="{ active: tab === 'done' }" type="button" @click="tab = 'done'">已刮</button>
    </div>

    <div v-if="tab === 'pending'" class="list">
      <div v-if="!pending.length" class="empty app-card app-muted">还没有未刮开的刮刮卡</div>
      <div v-else class="cards">
        <div v-for="it in pending" :key="it.id" class="card app-card">
          <div class="meta app-muted">
            <span>未解锁</span>
            <span> · </span>
            <span>{{ it.createdAt ? new Date(it.createdAt).toLocaleString() : '-' }}</span>
            <span v-if="(it.revealMode ?? 0) === 1" class="mode">双人解锁</span>
          </div>
          <div v-if="fx.id === it.id" class="fx">
            <span
              v-for="(h, i) in fx.hearts"
              :key="`h_${it.id}_${i}`"
              class="fxh"
              :style="{ '--x': h.x + '%', '--d': h.d + 'ms', '--dur': h.dur + 'ms', '--s': h.s + 'px', '--o': h.o }"
            />
            <span
              v-for="(s, i) in fx.stars"
              :key="`s_${it.id}_${i}`"
              class="fxs"
              :style="{ '--x': s.x + '%', '--y': s.y + '%', '--d': s.d + 'ms', '--dur': s.dur + 'ms', '--s': s.s + 'px', '--o': s.o }"
            />
          </div>
          <ScratchCard
            :content="
              (it.revealMode ?? 0) === 1 && (it.status ?? 0) === 0
                ? isMyScratched(it)
                  ? '你已经刮开啦，等 TA 一起解锁❤️'
                  : '双人刮开才会揭晓❤️'
                : it.content || '一个小心愿'
            "
            :disabled="scratchingId === it.id || isMyScratched(it)"
            :revealed="!!localRevealed[it.id] || isMyScratched(it)"
            @done="scratch(it)"
          />
          <div class="tip app-muted">刮开面积达到 80% 会自动完全显示</div>
        </div>
      </div>
    </div>

    <div v-else class="list">
      <div v-if="!done.length" class="empty app-card app-muted">还没有已刮开的刮刮卡</div>
      <div v-else class="cards">
        <div v-for="it in done" :key="it.id" class="card app-card done" @click="openDetail(it)">
          <div class="meta app-muted">
            <span>已刮开</span>
            <span v-if="it.scratchedAt"> · {{ new Date(it.scratchedAt).toLocaleString() }}</span>
            <span v-if="(it.revealMode ?? 0) === 1" class="mode">双人解锁</span>
          </div>
          <div v-if="fx.id === it.id" class="fx">
            <span
              v-for="(h, i) in fx.hearts"
              :key="`h2_${it.id}_${i}`"
              class="fxh"
              :style="{ '--x': h.x + '%', '--d': h.d + 'ms', '--dur': h.dur + 'ms', '--s': h.s + 'px', '--o': h.o }"
            />
            <span
              v-for="(s, i) in fx.stars"
              :key="`s2_${it.id}_${i}`"
              class="fxs"
              :style="{ '--x': s.x + '%', '--y': s.y + '%', '--d': s.d + 'ms', '--dur': s.dur + 'ms', '--s': s.s + 'px', '--o': s.o }"
            />
          </div>
          <div class="revealed">
            <div class="rt">心愿</div>
            <div class="rc">{{ it.content }}</div>
          </div>
          <div class="row">
            <div class="app-muted">{{ it.scratchedByNickname ? `由 ${it.scratchedByNickname} 刮开` : '' }}</div>
            <div class="rightbtns">
              <el-button size="small" round @click.stop="addToWishList(it)">
                <span>加入清单</span>
              </el-button>
              <el-button size="small" round @click.stop="saveImage(it)">
                <el-icon :size="16"><Download /></el-icon>
                <span>保存图片</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialog" title="新建心愿刮刮卡" width="360px" align-center>
      <el-form label-position="top">
        <el-form-item label="心愿内容（≤50字）">
          <el-input v-model="form.content" type="textarea" maxlength="50" show-word-limit :autosize="{ minRows: 3, maxRows: 5 }" />
        </el-form-item>
        <el-form-item label="刮开模式">
          <div class="modepick">
            <button class="pill" :class="{ active: form.revealMode === 0 }" type="button" @click="form.revealMode = 0">一方刮开</button>
            <button class="pill" :class="{ active: form.revealMode === 1 }" type="button" @click="form.revealMode = 1">双人解锁</button>
          </div>
          <div class="app-muted hint">
            {{ form.revealMode === 1 ? '双方都刮开后才会揭晓心愿内容。' : '任意一方刮开后，双方都能看到结果。' }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="刮刮乐详情" width="92%">
      <div v-if="detailItem" class="dview">
        <div class="meta app-muted">
          <span>{{ (detailItem.status ?? 0) === 1 ? '已刮开' : '未解锁' }}</span>
          <span v-if="detailItem.scratchedAt"> · {{ new Date(detailItem.scratchedAt).toLocaleString() }}</span>
          <span v-if="(detailItem.revealMode ?? 0) === 1" class="mode">双人解锁</span>
        </div>
        <div class="revealed">
          <div class="rt">心愿</div>
          <div class="rc">{{ detailItem.content }}</div>
        </div>
        <div class="row">
          <div class="app-muted">{{ detailItem.scratchedByNickname ? `由 ${detailItem.scratchedByNickname} 刮开` : '' }}</div>
          <div class="rightbtns">
            <el-button size="small" round @click="addToWishList(detailItem)">
              <span>加入清单</span>
            </el-button>
            <el-button size="small" round @click="saveImage(detailItem)">
              <el-icon :size="16"><Download /></el-icon>
              <span>保存图片</span>
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
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
  min-width: 0;
}
.h {
  font-size: 16px;
  font-weight: 950;
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.heart {
  width: 22px;
  height: 22px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(139, 92, 246, 0.14);
  border: 1px solid rgba(139, 92, 246, 0.2);
  color: rgba(99, 102, 241, 0.9);
  flex: 0 0 auto;
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
.created {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.22);
  color: rgba(5, 150, 105, 0.92);
  font-weight: 900;
  font-size: 12px;
  user-select: none;
}
.ck {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.9);
  position: relative;
  flex: 0 0 auto;
}
.ck::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 3px;
  width: 6px;
  height: 4px;
  border-left: 2px solid rgba(255, 255, 255, 0.95);
  border-bottom: 2px solid rgba(255, 255, 255, 0.95);
  transform: rotate(-45deg);
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
.primary {
  border-radius: 999px;
}
.tabs {
  display: inline-flex;
  gap: 10px;
}
.tab {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.7);
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}
.tab.active {
  color: rgba(17, 24, 39, 0.92);
  border-color: rgba(99, 102, 241, 0.28);
  background: rgba(139, 92, 246, 0.12);
}
.tab:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.28);
  background: rgba(99, 102, 241, 0.08);
}
.list {
  display: grid;
  gap: 12px;
}
.cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
@media (max-width: 860px) {
  .cards {
    grid-template-columns: 1fr;
  }
}
.card {
  padding: 12px;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}
.card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(520px 200px at 10% 0%, rgba(139, 92, 246, 0.12), transparent 60%),
    radial-gradient(480px 220px at 90% 0%, rgba(139, 92, 246, 0.12), transparent 62%);
  opacity: 0.8;
  pointer-events: none;
}
.card > * {
  position: relative;
  z-index: 1;
}
@media (hover: hover) {
  .card:hover {
    transform: translateY(-2px);
    border-color: rgba(99, 102, 241, 0.26);
    box-shadow: 0 18px 38px rgba(99, 102, 241, 0.12);
  }
}
.card.done::after {
  content: '❤';
  position: absolute;
  right: 10px;
  top: 10px;
  font-size: 12px;
  color: rgba(99, 102, 241, 0.42);
  pointer-events: none;
}
.meta {
  font-size: 12px;
  margin-bottom: 10px;
}
.mode {
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  background: rgba(139, 92, 246, 0.1);
  color: rgba(99, 102, 241, 0.85);
  font-weight: 900;
}
.tip {
  margin-top: 10px;
  font-size: 12px;
}
.modepick {
  display: inline-flex;
  gap: 6px;
  padding: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.pill {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.7);
  cursor: pointer;
  transition: transform 0.15s ease, background-color 0.15s ease;
}
.pill:hover {
  transform: translateY(-1px);
  background: rgba(99, 102, 241, 0.08);
}
.pill.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.95), rgba(139, 92, 246, 0.92));
  color: rgba(255, 255, 255, 0.96);
}
.hint {
  margin-top: 8px;
  font-size: 12px;
}
.fx {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 5;
}
.fxh {
  position: absolute;
  left: var(--x);
  top: -10px;
  width: var(--s);
  height: var(--s);
  opacity: var(--o);
  transform: translateX(-50%);
  animation: fxfall var(--dur) ease-in both;
  animation-delay: var(--d);
  filter: drop-shadow(0 10px 16px rgba(99, 102, 241, 0.18));
}
.fxh::before {
  content: '❤';
  font-size: var(--s);
  line-height: 1;
  color: rgba(99, 102, 241, 0.78);
}
.fxs {
  position: absolute;
  left: var(--x);
  top: var(--y);
  width: var(--s);
  height: var(--s);
  opacity: var(--o);
  transform: translate(-50%, -50%) scale(0.7);
  animation: fxtw var(--dur) ease-in-out both;
  animation-delay: var(--d);
  filter: drop-shadow(0 12px 18px rgba(139, 92, 246, 0.18));
}
.fxs::before {
  content: '✦';
  font-size: var(--s);
  line-height: 1;
  color: rgba(139, 92, 246, 0.82);
}
@keyframes fxfall {
  0% {
    transform: translate(-50%, 0) rotate(0deg) scale(0.9);
    opacity: 0;
  }
  10% {
    opacity: var(--o);
  }
  100% {
    transform: translate(-50%, 520px) rotate(18deg) scale(1);
    opacity: 0;
  }
}
@keyframes fxtw {
  0% {
    transform: translate(-50%, -50%) scale(0.7);
    opacity: 0;
  }
  30% {
    opacity: var(--o);
  }
  60% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: var(--o);
  }
  100% {
    transform: translate(-50%, -50%) scale(0.75);
    opacity: 0;
  }
}
.empty {
  padding: 18px 14px;
  border-radius: 16px;
  text-align: center;
}
.revealed {
  border-radius: 18px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(139, 92, 246, 0.12), rgba(59, 130, 246, 0.08)),
    rgba(255, 255, 255, 0.62);
}
.dview {
  display: grid;
  gap: 10px;
}
.rt {
  font-size: 12px;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.58);
}
.rc {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 950;
  line-height: 1.35;
  color: rgba(17, 24, 39, 0.88);
  word-break: break-word;
}
.row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.rightbtns {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
</style>

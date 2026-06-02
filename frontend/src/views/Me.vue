<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useDashboardStore } from '../stores/dashboard'
import http from '../api/http'
import { Bell, Calendar, Edit, Lock, Notebook, Picture, RefreshRight, UploadFilled, UserFilled } from '@element-plus/icons-vue'

const apiBase = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const assetUrl = (u) => {
  const s = String(u || '')
  if (!s) return ''
  if (/^(https?:)?\/\//.test(s) || s.startsWith('blob:') || s.startsWith('data:')) return s
  if (s.startsWith('/') && apiBase) return apiBase + s
  return s
}

const router = useRouter()
const auth = useAuthStore()
const dashboard = useDashboardStore()
const profile = ref(null)
const loading = ref(false)
const nowTick = ref(Date.now())
let sigTickTimer = 0
const headIn = ref(false)

const theme = ref('light')
const applyTheme = () => {
  document.documentElement.dataset.theme = theme.value === 'dark' ? 'dark' : ''
}
const initTheme = () => {
  try {
    const t = localStorage.getItem('dz_theme')
    theme.value = t === 'dark' ? 'dark' : 'light'
  } catch (e) {
    theme.value = 'light'
  }
  applyTheme()
}
const toggleTheme = () => {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  try {
    localStorage.setItem('dz_theme', theme.value)
  } catch (e) {}
  applyTheme()
  ElMessage.success(theme.value === 'dark' ? '已切换为深色主题' : '已切换为浅色主题')
}

const meUser = computed(() => profile.value?.me || auth.user || null)
const myTempExpireAt = computed(() => {
  const t = meUser.value?.signatureExpireTime
  if (!t) return null
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? null : d
})
const myTempActive = computed(() => {
  const u = meUser.value
  const temp = String(u?.tempSignature || '').trim()
  if (!temp) return false
  const exp = myTempExpireAt.value
  if (!exp) return true
  return exp.getTime() > nowTick.value
})
const mySignature = computed(() => {
  const u = meUser.value
  if (!u) return ''
  return String(u.signature || '').trim()
})
const myHomeSignature = computed(() => {
  const u = meUser.value
  if (!u) return ''
  if (myTempActive.value) return String(u.tempSignature || '').trim()
  return String(u.signature || '').trim()
})

const initialFor = (u) => {
  const n = u?.nickname || u?.username || ''
  return n ? n.trim().slice(0, 1).toUpperCase() : 'U'
}

const refresh = async (silent) => {
  try {
    loading.value = true
    const data = await http.get('/api/profile')
    profile.value = data
    if (data?.me) {
      auth.user = data.me
      auth.persist()
    }
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '刷新失败')
  } finally {
    loading.value = false
  }
}

const openPartner = () => {
  if (!profile.value?.partner) return
  router.push('/app/partner')
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

// ==================== 关于我们 — 暖心收尾栏 ====================
const aboutDefault = '从相遇那天起，每一天都值得记录。\n\n这里收藏着属于你们的每一个瞬间——第一次牵手、第一次旅行、一起看过的电影、一起做过的饭。\n\n无论时光如何流转，愿你们始终记得最初的心动。\n\n💕'
const aboutText = ref('')
const aboutEditing = ref(false)
const aboutEl = ref(null)
const aboutVisible = ref(false)

const loadAbout = async () => {
  try {
    const res = await http.get('/api/couple/about')
    aboutText.value = res?.text ? res.text : aboutDefault
  } catch (e) {
    aboutText.value = aboutDefault
  }
}

const saveAbout = async () => {
  try {
    await http.put('/api/couple/about', { text: aboutText.value })
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const startEditAbout = () => {
  aboutEditing.value = true
}

const aboutExpanded = ref(false)
const toggleAboutExpand = () => {
  aboutExpanded.value = !aboutExpanded.value
}

const finishEditAbout = () => {
  aboutEditing.value = false
  saveAbout()
}

// IntersectionObserver: 滚动到可见区域时触发淡入
let aboutObserver = null
onMounted(() => {
  loadAbout()
  if (aboutEl.value) {
    aboutObserver = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          aboutVisible.value = true
          aboutObserver?.disconnect()
        }
      },
      { threshold: 0.2 }
    )
    aboutObserver.observe(aboutEl.value)
  }
})

onBeforeUnmount(() => {
  aboutObserver?.disconnect()
})
	const sigDialog = ref(false)
const sigForm = reactive({ signature: '' })
const openSignature = () => {
  sigForm.signature = profile.value?.signature || ''
  sigDialog.value = true
}
const saveSignature = async () => {
  try {
    await http.put('/api/profile/signature', { signature: sigForm.signature })
    ElMessage.success('已保存')
    sigDialog.value = false
    await refresh(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const editDialog = ref(false)
const editForm = reactive({
  nickname: '',
  avatarUrl: '',
  gender: null,
  loveDate: null,
  zodiac: ''
})

const zodiacList = [
  '白羊座', '金牛座', '双子座', '巨蟹座',
  '狮子座', '处女座', '天秤座', '天蝎座',
  '射手座', '摩羯座', '水瓶座', '双鱼座'
]
const avatarFiles = ref([])
const openEdit = () => {
  const me = profile.value?.me || auth.user || {}
  editForm.nickname = me.nickname || ''
  editForm.avatarUrl = me.avatarUrl || ''
  editForm.gender = typeof me.gender === 'number' ? me.gender : null
  editForm.loveDate = me.loveDate ? new Date(me.loveDate) : null
  editForm.zodiac = me.zodiac || ''
  const u = assetUrl(me.avatarUrl)
  avatarFiles.value = u ? [{ name: 'avatar', url: u }] : []
  editDialog.value = true
}
const uploadAvatar = async () => {
  const files = avatarFiles.value || []
  const raw = files.find((x) => x?.raw)?.raw
  if (!raw) return editForm.avatarUrl || null
  const fd = new FormData()
  fd.append('files', raw)
  const res = await http.post('/api/uploads/images', fd)
  const first = Array.isArray(res) ? res[0] : null
  return first?.url || null
}
const saveEdit = async () => {
  try {
    const avatarUrl = await uploadAvatar()
    const me = await http.put('/api/profile/me', {
      nickname: editForm.nickname,
      avatarUrl,
      gender: editForm.gender,
      loveDate: editForm.loveDate,
      zodiac: editForm.zodiac || null
    })
    auth.user = me
    auth.persist()
    ElMessage.success('已更新')
    editDialog.value = false
    await refresh(true)
  } catch (e) {
    ElMessage.error(e?.message || '更新失败')
  }
}

const pwdDialog = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const openPwd = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialog.value = true
}
const savePwd = async () => {
  try {
    await http.put('/api/profile/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    pwdDialog.value = false
    logout()
  } catch (e) {
    ElMessage.error(e?.message || '修改失败')
  }
}

const settings = ref(null)
const settingsFetching = ref(false)
const settingsSaving = ref(false)
const loadSettings = async () => {
  try {
    settingsFetching.value = true
    settings.value = await http.get('/api/profile/settings')
  } finally {
    settingsFetching.value = false
  }
}
const updateSettings = async (patch) => {
  const prev = settings.value ? { ...settings.value } : null
  try {
    settingsSaving.value = true
    if (settings.value) {
      settings.value = { ...settings.value, ...patch }
    }
    settings.value = await http.put('/api/profile/settings', patch)
    ElMessage.success('已保存')
  } catch (e) {
    settings.value = prev
    ElMessage.error(e?.message || '保存失败')
  } finally {
    settingsSaving.value = false
  }
}

const DND_DEFAULT_START = '22:00'
const DND_DEFAULT_END = '08:00'

const patchReminderTime = (key, value) => ({ [key]: value })
const patchDndTime = (key, value) => {
  const s = settings.value || {}
  if (!s.dndEnabled) {
    return { [key]: value }
  }
  if (key === 'dndStart') {
    return {
      dndStart: value,
      dndEnd: value != null && value !== '' ? (s.dndEnd || DND_DEFAULT_END) : s.dndEnd
    }
  }
  return {
    dndEnd: value,
    dndStart: value != null && value !== '' ? (s.dndStart || DND_DEFAULT_START) : s.dndStart
  }
}

const applySettingsPatchLocal = (patch) => {
  if (!settings.value || !patch) return
  const next = { ...settings.value }
  for (const [k, v] of Object.entries(patch)) {
    if (v !== undefined) next[k] = v
  }
  settings.value = next
}

const onReminderTimeModel = (key, v) => applySettingsPatchLocal(patchReminderTime(key, v))
const onReminderTimeSave = (key, v) => updateSettings(patchReminderTime(key, v))
const onDndTimeModel = (key, v) => applySettingsPatchLocal(patchDndTime(key, v))
const onDndTimeSave = (key, v) => updateSettings(patchDndTime(key, v))

const onDndEnabledChange = (enabled) => {
  if (enabled) {
    const s = settings.value
    updateSettings({
      dndEnabled: true,
      dndStart: s?.dndStart || DND_DEFAULT_START,
      dndEnd: s?.dndEnd || DND_DEFAULT_END
    })
  } else {
    updateSettings({ dndEnabled: false })
  }
}

const stats = computed(() => profile.value?.stats || { anniversaries: 0, diaries: 0, photos: 0 })
const loveDays = computed(() => profile.value?.loveDays || 0)
const loveText = computed(() => profile.value?.loveAnniversaryText || '')
const importantAnn = computed(() => dashboard.data?.anniversary || null)
const coverPhoto = computed(() => dashboard.data?.coverPhoto || null)
const importantAnnStyle = computed(() => {
  const a = importantAnn.value
  if (!a) return {}
  const color = String(a.themeColor || '').trim() || '#6366F1'
  const cover = assetUrl(a.coverThumbUrl || a.coverUrl)
  return {
    '--ann-color': color,
    '--ann-cover': cover ? `url(${cover})` : 'none'
  }
})
const profileStyle = computed(() => {
  const p = coverPhoto.value
  const cover = p ? assetUrl(p.thumbUrl || p.url) : ''
  return { '--pcover': cover ? `url(${cover})` : 'none' }
})

const loveDaysShow = ref(0)
const photosShow = ref(0)
const diariesShow = ref(0)
const anniversariesShow = ref(0)
const createCounter = (outRef) => {
  let raf = 0
  let token = 0
  const start = (to) => {
    const target = Math.max(0, Math.floor(Number(to || 0) || 0))
    const from = Math.max(0, Math.floor(Number(outRef.value || 0) || 0))
    if (from === target) return
    const cur = ++token
    if (raf) cancelAnimationFrame(raf)
    const startAt = performance.now()
    const duration = 720
    const easeOut = (t) => 1 - Math.pow(1 - t, 3)
    const step = (now) => {
      if (cur !== token) return
      const p = Math.min(1, (now - startAt) / duration)
      outRef.value = Math.round(from + (target - from) * easeOut(p))
      if (p < 1) raf = requestAnimationFrame(step)
    }
    raf = requestAnimationFrame(step)
  }
  const stop = () => {
    token++
    if (raf) cancelAnimationFrame(raf)
    raf = 0
  }
  return { start, stop }
}

const loveCounter = createCounter(loveDaysShow)
const photosCounter = createCounter(photosShow)
const diariesCounter = createCounter(diariesShow)
const annCounter = createCounter(anniversariesShow)

watch(
  () => [loveDays.value, stats.value?.photos, stats.value?.diaries, stats.value?.anniversaries],
  ([ld, ph, di, an]) => {
    loveCounter.start(ld)
    photosCounter.start(ph)
    diariesCounter.start(di)
    annCounter.start(an)
  },
  { immediate: true }
)

const mySigDialog = ref(false)
const mySigForm = reactive({ signature: '' })
const openMySignature = () => {
  mySigForm.signature = meUser.value?.signature || ''
  mySigDialog.value = true
}
const saveMySignature = async () => {
  try {
    await http.put('/api/profile/my-signature', { signature: mySigForm.signature })
    ElMessage.success('已保存')
    mySigDialog.value = false
    await refresh(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const tempSigDialog = ref(false)
const tempSigForm = reactive({ tempSignature: '', expireDays: 1 })
const openTempSignature = () => {
  tempSigForm.tempSignature = meUser.value?.tempSignature || ''
  const exp = myTempExpireAt.value
  if (!tempSigForm.tempSignature) {
    tempSigForm.expireDays = 1
  } else if (!exp) {
    tempSigForm.expireDays = 0
  } else {
    const ms = exp.getTime() - Date.now()
    const days = Math.max(0, Math.ceil(ms / (24 * 3600 * 1000)))
    tempSigForm.expireDays = days <= 1 ? 1 : days <= 3 ? 3 : 7
  }
  tempSigDialog.value = true
}
const saveTempSignature = async () => {
  try {
    const days = Number(tempSigForm.expireDays || 0) || 0
    await http.put('/api/profile/temp-signature', {
      tempSignature: tempSigForm.tempSignature,
      expireDays: days === 0 ? null : days
    })
    ElMessage.success('已保存')
    tempSigDialog.value = false
    await refresh(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

onMounted(async () => {
  initTheme()
  requestAnimationFrame(() => {
    headIn.value = true
  })
  await refresh(true)
  try {
    await dashboard.fetch()
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  }
  await loadSettings()
  sigTickTimer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 30000)
})

onBeforeUnmount(() => {
  if (sigTickTimer) window.clearInterval(sigTickTimer)
  loveCounter.stop()
  photosCounter.stop()
  diariesCounter.stop()
  annCounter.stop()
})
</script>

<template>
  <div class="stack">
    <div class="mehead app-card" :class="{ in: headIn }" :style="profileStyle">
      <div class="mehead-bg" />
      <div class="mebar">
        <div class="brand">
          <span class="logo" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M12 20.4s-7.5-4.6-9.6-9.2C.8 7.9 2.8 5 6 5c1.7 0 3.2.8 4 2.1C10.8 5.8 12.3 5 14 5c3.2 0 5.2 2.9 3.6 6.2-2.1 4.6-9.6 9.2-9.6 9.2Z"
                fill="url(#g)"
              />
              <defs>
                <linearGradient id="g" x1="4" y1="6" x2="20" y2="20" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#EC4899" />
                  <stop offset="1" stop-color="#8B5CF6" />
                </linearGradient>
              </defs>
            </svg>
          </span>
          <div class="brandtext">
            <div class="brandtitle">我的</div>
            <div class="brandsub">把心动写进日常，把爱藏进细节。</div>
          </div>
        </div>
        <div class="bar-actions">
          <el-button size="small" text class="ghostbtn" @click="toggleTheme">切换主题</el-button>
          <el-button size="small" text class="ghostbtn" @click="openEdit">编辑资料</el-button>
        </div>
      </div>

      <div class="bond">
        <div class="person left">
          <div class="pavatar">
            <img v-if="meUser?.avatarUrl" :src="assetUrl(meUser.avatarUrl)" alt="" />
            <span v-else>{{ initialFor(meUser) }}</span>
          </div>
          <div class="pmeta">
            <div class="pname">{{ meUser?.nickname || '-' }}</div>
            <div class="pid app-muted">ID：{{ meUser?.id || '-' }}</div>
            <div v-if="loveDays" class="plove">
              <span class="plovedays">相恋第 {{ loveDays }} 天</span>
              <span v-if="loveText" class="plovetext app-muted">· {{ loveText }}</span>
            </div>
            <div class="psig" :class="{ muted: !myHomeSignature }">
              <span v-if="myHomeSignature">{{ myHomeSignature }}</span>
              <span v-else class="app-muted">写一句只属于你的情话吧</span>
            </div>
            <div v-if="meUser?.zodiac" class="pzodiac">
              <span class="zodiac-icon">♈</span>
              <span>{{ meUser.zodiac }}</span>
            </div>
          </div>
        </div>

        <div class="bondmid" aria-hidden="true">
          <div class="bondline" />
          <div class="bondheart">💗</div>
          <div class="bondline" />
        </div>

        <div class="person right" :class="{ clickable: !!profile?.partner }" role="button" tabindex="0" @click="openPartner">
          <div class="pavatar">
            <img v-if="profile?.partner?.avatarUrl" :src="assetUrl(profile.partner.avatarUrl)" alt="" />
            <span v-else>{{ initialFor(profile?.partner) }}</span>
          </div>
          <div class="pmeta">
            <div class="pname">
              <span>{{ profile?.partner?.nickname || (profile?.partner ? '-' : '等待你的另一半') }}</span>
              <el-tag v-if="profile?.partner" effect="light" round size="small" class="ptag">我的另一半</el-tag>
            </div>
            <div v-if="profile?.partner" class="pid app-muted">ID：{{ profile.partner.id || '-' }}</div>
            <div v-else class="pid app-muted">还没有绑定第二个账号</div>
          </div>
        </div>
      </div>
    </div>

    <div class="statcards" :class="{ in: headIn }">
      <div
        class="statcard"
        role="button"
        tabindex="0"
        style="--tone: #6366f1"
        @click="router.push('/app/home')"
        @keydown.enter="router.push('/app/home')"
        @keydown.space.prevent="router.push('/app/home')"
      >
        <div class="sicon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M12 20.4s-7.5-4.6-9.6-9.2C.8 7.9 2.8 5 6 5c1.7 0 3.2.8 4 2.1C10.8 5.8 12.3 5 14 5c3.2 0 5.2 2.9 3.6 6.2-2.1 4.6-9.6 9.2-9.6 9.2Z"
              fill="currentColor"
              opacity="0.95"
            />
          </svg>
        </div>
        <div class="smeta">
          <div class="svalue">{{ loveDaysShow }}</div>
          <div class="stext">相恋天数</div>
        </div>
      </div>

      <div
        class="statcard"
        role="button"
        tabindex="0"
        style="--tone: #10b981"
        @click="router.push('/app/albums')"
        @keydown.enter="router.push('/app/albums')"
        @keydown.space.prevent="router.push('/app/albums')"
      >
        <div class="sicon" aria-hidden="true">
          <el-icon :size="18"><Picture /></el-icon>
        </div>
        <div class="smeta">
          <div class="svalue">{{ photosShow }}</div>
          <div class="stext">共同照片</div>
        </div>
      </div>

      <div
        class="statcard"
        role="button"
        tabindex="0"
        style="--tone: #6366f1"
        @click="router.push({ path: '/app/records', query: { tab: 'diary' } })"
        @keydown.enter="router.push({ path: '/app/records', query: { tab: 'diary' } })"
        @keydown.space.prevent="router.push({ path: '/app/records', query: { tab: 'diary' } })"
      >
        <div class="sicon" aria-hidden="true">
          <el-icon :size="18"><Notebook /></el-icon>
        </div>
        <div class="smeta">
          <div class="svalue">{{ diariesShow }}</div>
          <div class="stext">日记记录</div>
        </div>
      </div>

      <div
        class="statcard"
        role="button"
        tabindex="0"
        style="--tone: #f59e0b"
        @click="router.push({ path: '/app/records', query: { tab: 'ann' } })"
        @keydown.enter="router.push({ path: '/app/records', query: { tab: 'ann' } })"
        @keydown.space.prevent="router.push({ path: '/app/records', query: { tab: 'ann' } })"
      >
        <div class="sicon" aria-hidden="true">
          <el-icon :size="18"><Calendar /></el-icon>
        </div>
        <div class="smeta">
          <div class="svalue">{{ anniversariesShow }}</div>
          <div class="stext">纪念日</div>
        </div>
      </div>
    </div>

    <div class="midgrid" :class="{ in: headIn }">
      <div class="panel">
        <div class="panelhead">
          <div class="paneltitle">安全中心</div>
        </div>
        <div class="panelbody">
          <button class="setrow" type="button" @click="openPwd">
            <div class="setleft">
              <div class="seticon" aria-hidden="true"><el-icon :size="16"><Lock /></el-icon></div>
              <div class="settexts">
                <div class="setname">修改密码</div>
                <div class="setdesc app-muted">更换密码后会自动退出，需要重新登录</div>
              </div>
            </div>
            <div class="setright" aria-hidden="true">
              <span class="arr">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
          </button>

          <button class="setrow" type="button" @click="logout">
            <div class="setleft">
              <div class="seticon seticon-danger" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M10 7V6.2C10 4.985 10.985 4 12.2 4h5.6C19.015 4 20 4.985 20 6.2v11.6c0 1.215-.985 2.2-2.2 2.2h-5.6c-1.215 0-2.2-.985-2.2-2.2V17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                  <path d="M4 12h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                  <path d="M7 9l-3 3 3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <div class="settexts">
                <div class="setname setname-danger">退出登录</div>
                <div class="setdesc app-muted">退出后返回登录页面</div>
              </div>
            </div>
            <div class="setright" aria-hidden="true">
              <span class="arr arr-danger">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
          </button>
        </div>
      </div>

      <div class="panel">
        <div class="panelhead">
          <div class="paneltitle">
            <span>消息提醒</span>
            <span v-if="settingsFetching" class="pilltag">加载中</span>
            <span v-else-if="settingsSaving" class="pilltag">保存中</span>
          </div>
        </div>
        <div class="panelbody">
            <button class="setrow" type="button" @click="router.push('/app/notifications')">
              <div class="setleft">
                <div class="seticon" aria-hidden="true"><el-icon :size="16"><Bell /></el-icon></div>
                <div class="settexts">
                  <div class="setname">提醒中心</div>
                  <div class="setdesc app-muted">查看提醒、通知与未读</div>
                </div>
              </div>
              <div class="setright" aria-hidden="true">
                <span class="arr">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </span>
              </div>
            </button>

            <div class="setrow switchrow">
              <div class="setleft">
                <div class="seticon" aria-hidden="true"><el-icon :size="16"><Bell /></el-icon></div>
                <div class="settexts">
                  <div class="setname">消息提醒</div>
                  <div class="setdesc app-muted">聊天消息与站内通知；关闭后不推送心愿轮盘等通知</div>
                </div>
              </div>
              <div class="setright">
                <span class="swstate app-muted">{{ (settings?.messageEnabled ?? true) ? '已开启' : '已关闭' }}</span>
                <el-switch
                  :model-value="settings?.messageEnabled ?? true"
                  :disabled="settingsFetching"
                  :loading="settingsSaving"
                  @change="(v) => updateSettings({ messageEnabled: v })"
                />
              </div>
            </div>

            <div class="setrow switchrow">
              <div class="setleft">
                <div class="seticon" aria-hidden="true"><el-icon :size="16"><Calendar /></el-icon></div>
                <div class="settexts">
                  <div class="setname">纪念日/提醒</div>
                  <div class="setdesc app-muted">开启后可设通知时段，开始与结束可只填其一</div>
                </div>
              </div>
              <div class="setright">
                <span class="swstate app-muted">{{ (settings?.reminderEnabled ?? true) ? '已开启' : '已关闭' }}</span>
                <el-switch
                  :model-value="settings?.reminderEnabled ?? true"
                  :disabled="settingsFetching"
                  :loading="settingsSaving"
                  @change="(v) => updateSettings({ reminderEnabled: v })"
                />
              </div>
            </div>

            <div v-if="settings?.reminderEnabled" class="subbox">
              <div class="subrow">
                <div class="sublabel app-muted">提醒开始</div>
                <el-time-picker
                  :model-value="settings?.reminderWindowStart ?? undefined"
                  format="HH:mm"
                  value-format="HH:mm"
                  style="width: 100%"
                  @update:model-value="(v) => onReminderTimeModel('reminderWindowStart', v)"
                  @change="(v) => onReminderTimeSave('reminderWindowStart', v)"
                />
              </div>
              <div class="subrow">
                <div class="sublabel app-muted">提醒结束</div>
                <el-time-picker
                  :model-value="settings?.reminderWindowEnd ?? undefined"
                  format="HH:mm"
                  value-format="HH:mm"
                  style="width: 100%"
                  @update:model-value="(v) => onReminderTimeModel('reminderWindowEnd', v)"
                  @change="(v) => onReminderTimeSave('reminderWindowEnd', v)"
                />
              </div>
            </div>

            <div class="setrow switchrow">
              <div class="setleft">
                <div class="seticon" aria-hidden="true">🌙</div>
                <div class="settexts">
                  <div class="setname">免打扰</div>
                  <div class="setdesc app-muted">免打扰会影响站内提醒展示</div>
                </div>
              </div>
              <div class="setright">
                <span class="swstate app-muted">{{ (settings?.dndEnabled ?? false) ? '已开启' : '已关闭' }}</span>
                <el-switch
                  :model-value="settings?.dndEnabled ?? false"
                  :disabled="settingsFetching"
                  :loading="settingsSaving"
                  @change="onDndEnabledChange"
                />
              </div>
            </div>

            <div v-if="settings" class="subbox">
              <div class="subrow">
                <div class="sublabel app-muted">开始</div>
                <el-time-picker
                  :model-value="settings?.dndStart ?? undefined"
                  format="HH:mm"
                  value-format="HH:mm"
                  style="width: 100%"
                  @update:model-value="(v) => onDndTimeModel('dndStart', v)"
                  @change="(v) => onDndTimeSave('dndStart', v)"
                />
              </div>
              <div class="subrow">
                <div class="sublabel app-muted">结束</div>
                <el-time-picker
                  :model-value="settings?.dndEnd ?? undefined"
                  format="HH:mm"
                  value-format="HH:mm"
                  style="width: 100%"
                  @update:model-value="(v) => onDndTimeModel('dndEnd', v)"
                  @change="(v) => onDndTimeSave('dndEnd', v)"
                />
              </div>
            </div>
          </div>
        </div>

      <div class="panel">
        <div class="panelhead">
          <div class="paneltitle">个性化设置</div>
        </div>
        <div class="panelbody">
          <button class="setrow" type="button" @click="openEdit">
            <div class="setleft">
              <div class="seticon" aria-hidden="true"><el-icon :size="16"><Edit /></el-icon></div>
              <div class="settexts">
                <div class="setname">编辑资料</div>
                <div class="setdesc app-muted">昵称、头像、相恋日期</div>
              </div>
            </div>
            <div class="setright" aria-hidden="true">
              <span class="arr">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
          </button>

          <button class="setrow" type="button" @click="openSignature">
            <div class="setleft">
              <div class="seticon" aria-hidden="true">💞</div>
              <div class="settexts">
                <div class="setname">情侣签名</div>
                <div class="setdesc app-muted">{{ profile?.signature || '未设置' }}</div>
              </div>
            </div>
            <div class="setright" aria-hidden="true">
              <span class="arr">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
          </button>

          <button class="setrow" type="button" @click="openMySignature">
            <div class="setleft">
              <div class="seticon" aria-hidden="true">✨</div>
              <div class="settexts">
                <div class="setname">个人签名</div>
                <div class="setdesc app-muted">{{ mySignature || '未设置' }}</div>
              </div>
            </div>
            <div class="setright" aria-hidden="true">
              <span class="arr">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
          </button>

          <button class="setrow" type="button" @click="openTempSignature">
            <div class="setleft">
              <div class="seticon" aria-hidden="true">🫶</div>
              <div class="settexts">
                <div class="setname">
                  <span>临时签名</span>
                  <span v-if="myTempActive" class="pilltag">生效中</span>
                </div>
                <div class="setdesc app-muted">
                  <span v-if="meUser?.tempSignature">{{ meUser.tempSignature }}</span>
                  <span v-else>未设置</span>
                </div>
              </div>
            </div>
            <div class="setright" aria-hidden="true">
              <span class="arr">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
          </button>
        </div>
      </div>

      <div class="panel annpanel" :style="importantAnnStyle">
        <div class="panelhead">
          <div class="paneltitle">我们的纪念日</div>
        </div>
        <div class="panelbody">
          <button class="anncard" type="button" @click="router.push({ path: '/app/records', query: { tab: 'ann' } })">
            <div class="annbg" />
            <div class="anntop">
              <div class="annh">{{ importantAnn?.pinned ? '最近一个重要纪念日' : '最近纪念日' }}</div>
              <span class="arr" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
            </div>
            <div v-if="importantAnn" class="annmain">
              <div class="anndays">
                <span v-if="importantAnn.daysLeft === 0">今天</span>
                <span v-else>{{ importantAnn.daysLeft }} 天</span>
              </div>
              <div class="annsub app-muted">
                <span v-if="importantAnn.icon">{{ importantAnn.icon }} </span>
                <span v-if="importantAnn.tagline">{{ importantAnn.tagline }}</span>
                <span v-else>{{ importantAnn.title }}</span>
              </div>
            </div>
            <div v-else class="annempty app-muted">还没有纪念日，去添加一个吧</div>
          </button>
        </div>
      </div>
    </div>

    <!-- ====== 关于我们 — 暖心收尾 ====== -->
    <div ref="aboutEl" class="about-wrap" :class="{ visible: aboutVisible }">
      <!-- 情侣插画 -->
      <div class="about-art">
        <svg viewBox="0 0 120 64" width="100" height="56" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="28" cy="28" r="22" fill="rgba(244,114,182,0.08)" stroke="rgba(244,114,182,0.2)" stroke-width="1" />
          <circle cx="92" cy="28" r="22" fill="rgba(167,139,250,0.08)" stroke="rgba(167,139,250,0.2)" stroke-width="1" />
          <path d="M48 18c4-6 12-8 18-2s4 14-2 18" stroke="rgba(236,72,153,0.35)" stroke-width="1.2" fill="none" stroke-linecap="round"/>
          <path d="M72 18c-4-6-12-8-18-2s-4 14 2 18" stroke="rgba(167,139,250,0.35)" stroke-width="1.2" fill="none" stroke-linecap="round"/>
          <circle cx="40" cy="22" r="1.6" fill="rgba(236,72,153,0.5)" />
          <circle cx="80" cy="22" r="1.6" fill="rgba(167,139,250,0.5)" />
          <path d="M56 34c2 3 6 3 8 0" stroke="rgba(236,72,153,0.3)" stroke-width="0.8" fill="none" stroke-linecap="round"/>
        </svg>
      </div>

      <!-- 可编辑文案：双击进入编辑，失焦保存 -->
      <div class="about-text-area" :class="{ expanded: aboutExpanded }" @dblclick="startEditAbout">
        <textarea
          v-if="aboutEditing"
          ref="aboutTextarea"
          v-model="aboutText"
          class="about-textarea"
          maxlength="10000"
          :rows="6"
          @blur="finishEditAbout"
        ></textarea>
        <div v-else class="about-text" :class="{ expanded: aboutExpanded }">{{ aboutText }}</div>
        <button v-if="!aboutEditing" class="about-toggle" @click="toggleAboutExpand">
          <span>{{ aboutExpanded ? '收起' : '展开' }}</span>
          <span class="about-arrow" :class="{ up: aboutExpanded }">›</span>
        </button>
      </div>
      <div class="about-brand">关于我们</div>
    </div>
  </div>

  <el-dialog v-model="sigDialog" title="编辑情侣签名" width="92%">
    <el-form label-position="top">
      <el-form-item label="签名（≤50字）">
        <el-input v-model="sigForm.signature" maxlength="50" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="sigDialog = false">取消</el-button>
      <el-button type="primary" @click="saveSignature">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="mySigDialog" title="编辑个人签名" width="92%">
    <el-form label-position="top">
      <el-form-item label="签名（≤50字）">
        <el-input v-model="mySigForm.signature" maxlength="50" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="mySigDialog = false">取消</el-button>
      <el-button type="primary" @click="saveMySignature">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="tempSigDialog" title="编辑临时签名" width="92%">
    <el-form label-position="top">
      <el-form-item label="临时签名（≤50字）">
        <el-input v-model="tempSigForm.tempSignature" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="有效期">
        <el-select v-model="tempSigForm.expireDays" style="width: 100%">
          <el-option :value="1" label="1 天" />
          <el-option :value="3" label="3 天" />
          <el-option :value="7" label="7 天" />
          <el-option :value="0" label="永久" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="tempSigDialog = false">取消</el-button>
      <el-button type="primary" @click="saveTempSignature">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editDialog" title="编辑我的资料" width="92%">
    <el-form label-position="top">
      <el-form-item label="昵称">
        <el-input v-model="editForm.nickname" maxlength="32" />
      </el-form-item>
      <el-form-item label="相恋日期">
        <el-date-picker v-model="editForm.loveDate" type="date" style="width: 100%" />
      </el-form-item>
      <el-form-item label="性别（可选）">
        <el-select v-model="editForm.gender" placeholder="不填也可以" style="width: 100%">
          <el-option :value="1" label="男" />
          <el-option :value="2" label="女" />
          <el-option :value="0" label="其他/保密" />
        </el-select>
      </el-form-item>
      <el-form-item label="星座（可选）">
        <el-select v-model="editForm.zodiac" placeholder="选择你的星座" clearable style="width: 100%">
          <el-option v-for="z in zodiacList" :key="z" :label="z" :value="z" />
        </el-select>
      </el-form-item>
      <el-form-item label="头像（可选）">
        <el-upload v-model:file-list="avatarFiles" list-type="picture-card" :auto-upload="false" accept="image/*" :limit="1">
          <el-icon :size="18"><UploadFilled /></el-icon>
        </el-upload>
        <div class="app-muted avtip">保存时会自动上传并更新头像</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editDialog = false">取消</el-button>
      <el-button type="primary" @click="saveEdit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pwdDialog" title="修改密码" width="92%">
    <el-form label-position="top">
      <el-form-item label="原密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialog = false">取消</el-button>
      <el-button type="primary" @click="savePwd">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.title {
  font-weight: 700;
}
.mehead {
  --pcover: none;
  position: relative;
  overflow: hidden;
  padding: 14px;
  border-radius: 20px;
  border: 1px solid transparent;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0.5)) padding-box,
    linear-gradient(135deg, rgba(99, 102, 241, 0.55), rgba(139, 92, 246, 0.52)) border-box;
  backdrop-filter: blur(18px);
  transform: translateY(-10px);
  opacity: 0;
}
.mehead.in {
  animation: meheadIn 520ms ease forwards;
}
.mehead-bg {
  position: absolute;
  inset: 0;
  background-image: var(--pcover);
  background-size: cover;
  background-position: center;
  opacity: 0.16;
  pointer-events: none;
}
.mehead::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(900px 320px at 15% 15%, rgba(99, 102, 241, 0.22), transparent 55%),
    radial-gradient(820px 320px at 85% 25%, rgba(139, 92, 246, 0.2), transparent 55%),
    radial-gradient(860px 420px at 50% 120%, rgba(14, 165, 233, 0.12), transparent 60%);
  pointer-events: none;
}
.mehead > * {
  position: relative;
  z-index: 1;
}
.mebar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.62);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.16), rgba(139, 92, 246, 0.12));
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.logo {
  width: 34px;
  height: 34px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.08);
}
.brandtext {
  min-width: 0;
}
.brandtitle {
  font-weight: 600;
  letter-spacing: 0.2px;
}
.brandsub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}
:deep(.ghostbtn.el-button) {
  border-radius: 999px;
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.6);
  color: var(--app-text);
}
:deep(.ghostbtn.el-button:hover) {
  background: rgba(255, 255, 255, 0.82);
  color: var(--app-text);
}

.bond {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr 132px 1fr;
  gap: 10px;
  align-items: stretch;
}
.person {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.68);
  min-width: 0;
}
.person.right.clickable {
  cursor: pointer;
}
.pavatar {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  font-weight: 600;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(139, 92, 246, 0.16));
  color: rgba(88, 28, 135, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.72);
  overflow: hidden;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.07);
}
.pavatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
@media (hover: hover) {
  .pavatar:hover {
    transform: translateY(-3px);
    border-color: rgba(99, 102, 241, 0.36);
    box-shadow: 0 16px 36px rgba(99, 102, 241, 0.14), 0 16px 36px rgba(139, 92, 246, 0.1);
  }
}
.pmeta {
  min-width: 0;
}
.pname {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ptag {
  flex: 0 0 auto;
}
.pid {
  margin-top: 2px;
  font-size: 12px;
}
.plove {
  margin-top: 4px;
  font-size: 12px;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: baseline;
}
.plovedays {
  font-weight: 600;
  color: rgba(99, 102, 241, 0.92);
}
.psig {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.9);
  word-break: break-word;
}
.pzodiac {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(139, 92, 246, 0.08);
  border: 1px solid rgba(139, 92, 246, 0.16);
  font-size: 12px;
  font-weight: 700;
  color: rgba(139, 92, 246, 0.88);
}
.zodiac-icon {
  font-size: 14px;
  line-height: 1;
}
.psig.muted {
  font-weight: 400;
}
.bondmid {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px 0;
}
.bondline {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.55), rgba(139, 92, 246, 0.52), transparent);
}
.bondheart {
  width: 56px;
  height: 56px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.18));
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 16px 36px rgba(99, 102, 241, 0.12), 0 16px 36px rgba(139, 92, 246, 0.1);
  animation: bondBreath 4.8s ease-in-out infinite;
  transform-origin: center;
}

.row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.avatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-weight: 800;
  background: linear-gradient(135deg, rgba(255, 99, 132, 0.18), rgba(99, 102, 241, 0.16));
  color: rgba(88, 28, 135, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.7);
  overflow: hidden;
}
.avatar.small {
  width: 40px;
  height: 40px;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avtip {
  margin-top: 8px;
  font-size: 12px;
}
.meta {
  flex: 1;
  min-width: 0;
}
.name {
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  margin-top: 2px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mini {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  flex-wrap: wrap;
}
.mini-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.statcards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.statcard {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.06);
  cursor: pointer;
  transition: transform 170ms ease, box-shadow 170ms ease, border-color 170ms ease;
}
.sicon {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: var(--tone);
  background: color-mix(in srgb, var(--tone) 14%, #ffffff);
  border: 1px solid color-mix(in srgb, var(--tone) 18%, transparent);
  transition: transform 170ms ease, filter 170ms ease;
  flex: 0 0 auto;
}
.smeta {
  min-width: 0;
}
.svalue {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: rgba(17, 24, 39, 0.92);
  line-height: 1.05;
}
.stext {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(17, 24, 39, 0.52);
}
@media (hover: hover) {
  .statcard:hover {
    transform: translateY(-3px);
    box-shadow: 0 14px 32px rgba(17, 24, 39, 0.085);
    border-color: color-mix(in srgb, var(--tone) 30%, rgba(17, 24, 39, 0.06));
  }
  .statcard:hover .sicon {
    transform: scale(1.06);
    filter: saturate(1.08);
  }
  .statcard:hover .svalue {
    color: color-mix(in srgb, var(--tone) 70%, rgba(17, 24, 39, 0.92));
  }
}
.midgrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  background: color-mix(in srgb, var(--app-card) 90%, #ffffff 10%);
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.06);
  overflow: hidden;
}
.panelhead {
  padding: 14px 14px 10px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  min-height: 48px;
}
.paneltitle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}
.panelbody {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.setrow {
  width: 100%;
  appearance: none;
  border: 1px solid rgba(17, 24, 39, 0.06);
  background: rgba(255, 255, 255, 0.7);
  border-radius: 14px;
  padding: 12px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease, border-color 160ms ease;
}
.switchrow {
  cursor: default;
}
.setleft {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.seticon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.1));
  border: 1px solid rgba(99, 102, 241, 0.14);
  color: rgba(17, 24, 39, 0.82);
  flex: 0 0 auto;
}
.settexts {
  min-width: 0;
}
.setname {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.setdesc {
  margin-top: 3px;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.seticon-danger {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.16);
  color: #ef4444;
}
.setname-danger {
  color: #ef4444 !important;
}
.arr-danger {
  color: #ef4444;
}
.setright {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  color: rgba(17, 24, 39, 0.55);
  flex: 0 0 auto;
  gap: 10px;
}
.arr {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 160ms ease, color 160ms ease;
}
.swstate {
  font-size: 12px;
  white-space: nowrap;
  opacity: 0.75;
  width: 48px;
  text-align: right;
}

.pilltag {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(99, 102, 241, 0.92);
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.16);
}

.annpanel {
  border-color: rgba(99, 102, 241, 0.18);
}
.anncard {
  width: 100%;
  appearance: none;
  border: 1px solid rgba(99, 102, 241, 0.14);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.16), rgba(255, 255, 255, 0.72));
  border-radius: 16px;
  padding: 12px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  text-align: left;
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
  min-height: 100%;
}
.annbg {
  position: absolute;
  inset: 0;
  background-image: var(--ann-cover);
  background-size: cover;
  background-position: center;
  opacity: 0.16;
  pointer-events: none;
}
.anncard > * {
  position: relative;
  z-index: 1;
}
.anntop {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.annh {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.92);
}
.annmain {
  margin-top: 10px;
}
.anndays {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: rgba(99, 102, 241, 0.92);
}
.annsub {
  margin-top: 6px;
  font-size: 12px;
}
.annempty {
  margin-top: 10px;
  font-size: 12px;
}
.subbox {
  border-radius: 14px;
  border: 1px solid rgba(17, 24, 39, 0.06);
  background: rgba(255, 255, 255, 0.62);
  padding: 10px 12px;
  display: grid;
  gap: 10px;
}
.subrow {
  display: grid;
  grid-template-columns: 52px 1fr;
  align-items: center;
  gap: 10px;
}
.sublabel {
  font-size: 12px;
}
@media (hover: hover) {
  .setrow:hover {
    transform: translateY(-2px);
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.08));
    box-shadow: 0 14px 32px rgba(17, 24, 39, 0.085);
    border-color: rgba(99, 102, 241, 0.22);
  }
  .setrow:hover .arr {
    transform: translateX(3px);
    color: rgba(99, 102, 241, 0.9);
  }
  .anncard:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px rgba(99, 102, 241, 0.12), 0 14px 30px rgba(17, 24, 39, 0.07);
    border-color: rgba(99, 102, 241, 0.26);
  }
  .anncard:hover .arr {
    transform: translateX(3px);
    color: rgba(99, 102, 241, 0.9);
  }
  .subbox :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.45) inset;
  }
}

.statcards.in .statcard {
  animation: cardIn 520ms ease both;
}
.statcards.in .statcard:nth-child(1) {
  animation-delay: 40ms;
}
.statcards.in .statcard:nth-child(2) {
  animation-delay: 90ms;
}
.statcards.in .statcard:nth-child(3) {
  animation-delay: 140ms;
}
.statcards.in .statcard:nth-child(4) {
  animation-delay: 190ms;
}
.midgrid.in .panel {
  animation: cardIn 540ms ease both;
}
.midgrid.in .midcol:nth-child(1) .panel:nth-child(1) {
  animation-delay: 140ms;
}
.midgrid.in .midcol:nth-child(1) .panel:nth-child(2) {
  animation-delay: 200ms;
}
.midgrid.in .midcol:nth-child(2) .panel:nth-child(1) {
  animation-delay: 170ms;
}
.midgrid.in .midcol:nth-child(2) .panel:nth-child(2) {
  animation-delay: 230ms;
}

@keyframes cardIn {
  from {
    transform: translateY(10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

:deep(.el-switch__core) {
  transition: background-color 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}
:deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.95), rgba(139, 92, 246, 0.92));
  border-color: rgba(99, 102, 241, 0.58);
  box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.35) inset;
}
:deep(.el-switch:not(.is-checked) .el-switch__core) {
  border-color: rgba(17, 24, 39, 0.18);
}
.empty {
  margin-top: 10px;
  font-size: 12px;
  text-align: center;
}
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.hint {
  margin-top: 10px;
  font-size: 12px;
}
.notify {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.notify-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
}
.notify-row:last-child {
  border-bottom: 0;
}
.notify-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}
.dnd {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 8px;
}
.dnd-row {
  display: grid;
  grid-template-columns: 52px 1fr;
  gap: 10px;
  align-items: center;
}
.dnd-label {
  font-size: 12px;
}

@keyframes meheadIn {
  from {
    transform: translateY(-10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
@keyframes bondBreath {
  0%,
  100% {
    transform: scale(1);
    filter: saturate(1);
  }
  50% {
    transform: scale(1.08);
    filter: saturate(1.06);
  }
}

@media (max-width: 820px) {
  .mebar {
    flex-direction: column;
    align-items: flex-start;
  }
  .brandsub {
    white-space: normal;
  }
  .bond {
    grid-template-columns: 1fr;
  }
  .bondmid {
    flex-direction: row;
    padding: 6px 0;
  }
  .bondline {
    flex: 1;
    width: auto;
  }
  .statcards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .midgrid {
    grid-template-columns: 1fr;
  }
}

/* ==================== 关于我们 — 暖心收尾栏 ==================== */
.about-wrap {
  margin-top: 14px;
  padding: 32px 24px 24px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(244,114,182,0.04), rgba(167,139,250,0.04), rgba(255,255,255,0.5));
  border: 1px solid rgba(244,114,182,0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  opacity: 0;
  transform: translateY(16px);
  transition: opacity 0.7s ease, transform 0.7s ease;
}
.about-wrap.visible {
  opacity: 1;
  transform: translateY(0);
}

.about-art {
  margin-bottom: 18px;
  opacity: 0.55;
}

.about-text-area {
  width: 100%;
  max-width: 380px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.about-text {
  font-size: 13px;
  line-height: 1.8;
  color: rgba(17,24,39,0.55);
  white-space: pre-wrap;
  word-break: break-word;
  cursor: default;
  user-select: none;
  max-height: 100px;
  overflow: hidden;
  transition: max-height 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  -webkit-mask-image: linear-gradient(to bottom, black 68%, transparent 100%);
  mask-image: linear-gradient(to bottom, black 68%, transparent 100%);
}

.about-text.expanded {
  max-height: none;
  overflow: visible;
  -webkit-mask-image: none;
  mask-image: none;
}

/* 展开/收起按钮 */
.about-toggle {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 16px;
  border: 1px solid rgba(244,114,182,0.14);
  border-radius: 999px;
  background: rgba(255,255,255,0.6);
  color: rgba(244,114,182,0.6);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.about-toggle:hover {
  border-color: rgba(244,114,182,0.3);
  color: rgba(236,72,153,0.8);
  background: rgba(244,114,182,0.04);
  box-shadow: 0 6px 18px rgba(236,72,153,0.08);
}
.about-arrow {
  display: inline-block;
  font-size: 16px;
  line-height: 1;
  transform: rotate(90deg);
  transition: transform 0.3s ease;
}
.about-arrow.up {
  transform: rotate(-90deg);
}

.about-textarea {
  width: 100%;
  max-width: 380px;
  padding: 12px 16px;
  border: 1px dashed rgba(244,114,182,0.2);
  border-radius: 14px;
  background: rgba(255,255,255,0.6);
  font-size: 13px;
  line-height: 1.8;
  color: rgba(17,24,39,0.75);
  resize: vertical;
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s;
}
.about-textarea:focus {
  border-color: rgba(244,114,182,0.4);
  background: rgba(255,255,255,0.85);
  box-shadow: 0 0 0 3px rgba(244,114,182,0.06);
}

.about-brand {
  margin-top: 18px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(17,24,39,0.22);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
</style>

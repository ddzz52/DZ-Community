<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDashboardStore } from '../stores/dashboard'
import { Calendar, ChatDotRound, Clock, Picture, RefreshRight, Star } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
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
const auth = useAuthStore()
const dashboard = useDashboardStore()
const profileMe = ref(null)
const periodStatus = ref(null)
const nowTick = ref(Date.now())
let sigTickTimer = 0
const togetherDays = computed(() => {
  const dt = auth.user?.loveDate
  if (!dt) return null
  const start = new Date(dt)
  if (Number.isNaN(start.getTime())) return null
  const s = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const n = new Date()
  const t = new Date(n.getFullYear(), n.getMonth(), n.getDate())
  const diff = Math.floor((t.getTime() - s.getTime()) / (24 * 3600 * 1000))
  if (diff < 0) return null
  return diff + 1
})

const loadMe = async () => {
  try {
    await auth.fetchMe()
  } catch (e) {
    ElMessage.error(e?.message || '获取用户信息失败')
  }
}

const loadProfile = async () => {
  try {
    const data = await http.get('/api/profile')
    profileMe.value = data?.me || null
  } catch (e) {}
}

const refresh = async () => {
  try {
    await dashboard.fetch()
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  }
}

const loadPeriodStatus = async () => {
  try {
    periodStatus.value = await http.get('/api/period/status')
  } catch (e) {
    periodStatus.value = null
  }
}

const now = new Date()
const acctYear = ref(now.getFullYear())
const acctMonth = ref(now.getMonth() + 1)
const acctLoading = ref(false)
const monthStats = ref(null)
const yearStats = ref(null)
const wheelSpinning = ref(false)
const wheelAngle = ref(0)
const wheelResult = ref(null)
const wheelActiveId = ref(null)
const wishPending = ref([])
const wheelPop = ref(false)
const wheelFx = reactive({ on: false, hearts: [], stars: [] })
let wheelTimer = 0
let wheelFxTimer = 0
let wheelPopTimer = 0

const wheelPalette = [
  'rgba(139, 92, 246, 0.28)',
  'rgba(255, 182, 130, 0.28)',
  'rgba(139, 92, 246, 0.28)',
  'rgba(244, 144, 160, 0.28)',
  'rgba(196, 168, 220, 0.28)',
  'rgba(255, 200, 210, 0.28)'
]

const loadWishPending = async (silent) => {
  try {
    const list = await http.get('/api/wish-list', { params: { status: 0 } })
    wishPending.value = Array.isArray(list) ? list : []
  } catch (e) {
    wishPending.value = []
    if (!silent) ElMessage.error(e?.message || '加载心愿失败')
  }
}

const polar = (cx, cy, r, deg) => {
  const rad = ((deg - 90) * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}
const sectorPath = (cx, cy, r0, r1, a0, a1) => {
  const large = a1 - a0 > 180 ? 1 : 0
  const p0 = polar(cx, cy, r1, a0)
  const p1 = polar(cx, cy, r1, a1)
  const p2 = polar(cx, cy, r0, a1)
  const p3 = polar(cx, cy, r0, a0)
  return `M ${p0.x} ${p0.y} A ${r1} ${r1} 0 ${large} 1 ${p1.x} ${p1.y} L ${p2.x} ${p2.y} A ${r0} ${r0} 0 ${large} 0 ${p3.x} ${p3.y} Z`
}

const wheelSlices = computed(() => {
  const items = Array.isArray(wishPending.value) ? wishPending.value : []
  const n = items.length
  if (n <= 0) return []
  const step = 360 / n
  const cx = 100
  const cy = 100
  const r0 = 56
  const r1 = 96
  return items.map((it, i) => {
    const a0 = i * step
    const a1 = (i + 1) * step
    return {
      id: it?.id,
      content: String(it?.content || ''),
      color: wheelPalette[i % wheelPalette.length],
      a0,
      a1,
      d: sectorPath(cx, cy, r0, r1, a0, a1)
    }
  })
})
const loadAccountStats = async (silent) => {
  try {
    acctLoading.value = true
    const y = Number(acctYear.value) || now.getFullYear()
    const m = Number(acctMonth.value) || now.getMonth() + 1
    const [ys, ms] = await Promise.all([
      http.get('/api/accounts/stats/year', { params: { year: y } }),
      http.get('/api/accounts/stats/month', { params: { year: y, month: m } })
    ])
    yearStats.value = ys || null
    monthStats.value = ms || null
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    acctLoading.value = false
  }
}

const spinWheel = async () => {
  if (wheelSpinning.value) return
  try {
    await loadWishPending(true)
    const n = Array.isArray(wishPending.value) ? wishPending.value.length : 0
    if (n <= 0) {
      ElMessage.error('暂无待完成心愿')
      return
    }
    wheelSpinning.value = true
    const picked = await http.post('/api/wish-list/roulette')
    wheelResult.value = picked || null
    wheelActiveId.value = picked?.id ?? null
    let items = Array.isArray(wishPending.value) ? wishPending.value : []
    let n2 = items.length
    let idx = items.findIndex((x) => x?.id === picked?.id)
    if (idx < 0) {
      await loadWishPending(true)
      items = Array.isArray(wishPending.value) ? wishPending.value : []
      n2 = items.length
      idx = items.findIndex((x) => x?.id === picked?.id)
    }
    const step = 360 / Math.max(1, n2)
    const center = (idx >= 0 ? idx : Math.floor(Math.random() * Math.max(1, n2))) * step + step / 2
    const jitter = (Math.random() - 0.5) * step * 0.7
    const stop = 360 - (center + jitter)
    const turns = 4 + Math.random() * 2
    wheelAngle.value = wheelAngle.value + turns * 360 + stop

    wheelFx.on = true
    wheelFx.hearts = Array.from({ length: 12 }).map(() => ({
      x: Math.round(Math.random() * 90) + 5,
      d: Math.round(Math.random() * 320),
      dur: Math.round(820 + Math.random() * 520),
      s: Math.round(10 + Math.random() * 12),
      o: +(0.35 + Math.random() * 0.55).toFixed(2)
    }))
    wheelFx.stars = Array.from({ length: 10 }).map(() => ({
      x: Math.round(Math.random() * 90) + 5,
      y: Math.round(Math.random() * 70) + 12,
      d: Math.round(Math.random() * 320),
      dur: Math.round(520 + Math.random() * 520),
      s: Math.round(8 + Math.random() * 12),
      o: +(0.28 + Math.random() * 0.55).toFixed(2)
    }))
    if (wheelFxTimer) window.clearTimeout(wheelFxTimer)
    wheelFxTimer = window.setTimeout(() => {
      wheelFx.on = false
      wheelFx.hearts = []
      wheelFx.stars = []
      wheelFxTimer = 0
    }, 1300)

    if (wheelTimer) window.clearTimeout(wheelTimer)
    wheelTimer = window.setTimeout(() => {
      wheelSpinning.value = false
      wheelTimer = 0
      wheelPop.value = true
      if (wheelPopTimer) window.clearTimeout(wheelPopTimer)
      wheelPopTimer = window.setTimeout(() => {
        wheelPop.value = false
        wheelPopTimer = 0
      }, 2400)
    }, 1600)
  } catch (e) {
    wheelSpinning.value = false
    ElMessage.error(e?.message || '暂无待完成心愿')
  }
}

const onAccountsChanged = () => {
  if (acctLoading.value) return
  if (acctSyncTimer) window.clearTimeout(acctSyncTimer)
  acctSyncTimer = window.setTimeout(() => {
    loadAccountStats(true)
  }, 400)
}
let acctSyncTimer = 0

watch(
  () => [acctYear.value, acctMonth.value],
  () => {
    loadAccountStats(true)
  }
)

watch(
  () => [monthStats.value, yearStats.value],
  () => {
    updateCharts()
  }
)

onMounted(async () => {
  if (!auth.user) await loadMe()
  await loadProfile()
  await refresh()
  await loadPeriodStatus()
  await loadAccountStats(true)
  await loadWishPending(true)
  window.addEventListener('dz_accounts_changed', onAccountsChanged)
  window.addEventListener('resize', onResize)
  sigTickTimer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 30000)
  nextTick(() => updateCharts())
})

onBeforeUnmount(() => {
  if (sigTickTimer) window.clearInterval(sigTickTimer)
  window.removeEventListener('dz_accounts_changed', onAccountsChanged)
  window.removeEventListener('resize', onResize)
  if (acctSyncTimer) window.clearTimeout(acctSyncTimer)
  if (wheelTimer) window.clearTimeout(wheelTimer)
  if (wheelFxTimer) window.clearTimeout(wheelFxTimer)
  if (wheelPopTimer) window.clearTimeout(wheelPopTimer)
  if (resizeTimer) window.clearTimeout(resizeTimer)
  if (pieChart) { pieChart.dispose(); pieChart = null }
  if (barChart) { barChart.dispose(); barChart = null }
})

const d = computed(() => dashboard.data || {})
const ann = computed(() => d.value?.anniversary || null)
const diaries = computed(() => d.value?.recent?.diaries || [])
const photos = computed(() => d.value?.recent?.photos || [])
const todayQuote = computed(() => d.value?.today?.quote || '把今天过成我们喜欢的样子。')
const coverPhoto = computed(() => d.value?.coverPhoto || null)
const heroStyle = computed(() => {
  const p = coverPhoto.value
  const cover = p ? assetUrl(p.thumbUrl || p.url) : ''
  return { '--hero-cover': cover ? `url(${cover})` : 'none' }
})

const goKpi = (key) => {
  switch (key) {
    case 'kpi_chat':
      router.push('/app/chat')
      break
    case 'kpi_agent':
      router.push('/app/agent')
      break
    case 'kpi_period':
      router.push({ path: '/app/records', query: { tab: 'period' } })
      break
    case 'kpi_ann':
      router.push({ path: '/app/records', query: { tab: 'ann' } })
      break
    case 'kpi_album':
      router.push('/app/albums')
      break
    default:
      break
  }
}

const kpis = computed(() => {
  const a = ann.value
  const annValue = a ? (a.daysLeft === 0 ? '今天' : String(a.daysLeft)) : '-'
  const annHint = a ? String(a.title || '') : '去添加'
  const ps = periodStatus.value?.settings
  const pp = periodStatus.value?.prediction
  const periodValue =
    ps && pp
      ? typeof pp.daysToNextPeriod === 'number'
        ? pp.daysToNextPeriod === 0
          ? '今天'
          : `${pp.daysToNextPeriod} 天`
        : '-'
      : '-'
  const periodHint =
    ps && pp
      ? `${pp.phase || '周期'} · ${pp.nextPeriodStart || ''}`
      : ps
        ? '加载中'
        : '去设置'
  return [
    {
      key: 'kpi_ann',
      title: '纪念日',
      value: annValue,
      hint: annHint,
      icon: Calendar,
      tone: 'violet'
    },
    {
      key: 'kpi_agent',
      title: 'AI 管家',
      value: '进入',
      hint: '恋爱陪伴',
      icon: Star,
      tone: 'violet'
    },
    {
      key: 'kpi_chat',
      title: '未读消息',
      value: Number(dashboard.badges.unreadMessages || 0) || 0,
      hint: '实时统计',
      icon: ChatDotRound,
      tone: 'blue'
    },
    {
      key: 'kpi_period',
      title: '生理期关怀',
      value: periodValue,
      hint: periodHint,
      icon: Calendar,
      tone: 'violet'
    },
    {
      key: 'kpi_album',
      title: '最近照片',
      value: photos.value.length,
      hint: '进入相册',
      icon: Picture,
      tone: 'green'
    }
  ]
})

const activities = computed(() => {
  const items = []
  for (const it of diaries.value || []) {
    if (!it) continue
    items.push({
      key: `d_${it.id}`,
      tone: 'blue',
      title: '新增日记',
      content: String(it.contentPreview || '写下了一条新的记录'),
      at: it.createdAt
    })
  }
  for (const p of photos.value || []) {
    if (!p) continue
    items.push({
      key: `p_${p.id}`,
      tone: 'green',
      title: '上传照片',
      content: '新增了一张照片',
      at: p.createdAt
    })
  }
  items.sort((a, b) => {
    const ta = a?.at ? new Date(a.at).getTime() : 0
    const tb = b?.at ? new Date(b.at).getTime() : 0
    return tb - ta
  })
  return items.slice(0, 8)
})

const tempExpireAt = computed(() => {
  const t = profileMe.value?.signatureExpireTime
  if (!t) return null
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? null : d
})
const tempActive = computed(() => {
  const temp = String(profileMe.value?.tempSignature || '').trim()
  if (!temp) return false
  const exp = tempExpireAt.value
  if (!exp) return true
  return exp.getTime() > nowTick.value
})
const homeSig = computed(() => {
  const u = profileMe.value
  if (!u) return ''
  const eff = String(u.effectiveSignature || '').trim()
  if (eff) return eff
  if (tempActive.value) return String(u.tempSignature || '').trim()
  return String(u.signature || '').trim()
})

const moneyText = (v) => {
  const n = typeof v === 'number' ? v : Number(v)
  if (!Number.isFinite(n)) return '0.00'
  return n.toFixed(2)
}

const monthTotal = computed(() => moneyText(monthStats.value?.totalAmount || 0))
const yearTotal = computed(() => moneyText(yearStats.value?.totalAmount || 0))
const acctHint = computed(() => {
  const amt = Number(monthStats.value?.totalAmount || 0) || 0
  if (amt <= 0) return '本月还没有记账，记一笔更清晰。'
  if (amt < 200) return '这个月花得不多，继续保持你的节奏。'
  if (amt < 800) return '本月支出稳定，别忘了偶尔给彼此一点小惊喜。'
  return '本月支出较多，看看分类占比，找找可以优化的地方。'
})

const pieWrapRef = ref(null)
const barWrapRef = ref(null)
let pieChart = null
let barChart = null

// ====== ECharts 饼图 ======
const initPieChart = () => {
  if (!pieWrapRef.value) return
  if (!pieChart) pieChart = echarts.init(pieWrapRef.value)
  const items = pieItems.value
  if (!items.length) { pieChart.clear(); return }
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: 'rgba(148,163,184,0.18)',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 13 },
      formatter: (p) => `${p.marker} ${p.name}<br/>¥${moneyText(p.value)}（${p.percent}%）`
    },
    legend: { show: false },
    graphic: [{
      type: 'text',
      left: 'center', top: '42%',
      style: { text: '月总支出', fontSize: 11, fontWeight: 900, fill: 'rgba(17,24,39,0.5)', textAlign: 'center' }
    }, {
      type: 'text',
      left: 'center', top: '52%',
      style: { text: `¥${monthTotal.value}`, fontSize: 14, fontWeight: 950, fill: 'rgba(99,102,241,0.92)', textAlign: 'center' }
    }],
    series: [{
      type: 'pie',
      radius: ['52%', '78%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        scaleSize: 10,
        label: { show: true, fontSize: 16, fontWeight: 'bold' }
      },
      animationType: 'scale',
      animationEasing: 'elasticOut',
      animationDuration: 800,
      data: items.map((x) => ({
        name: x.category,
        value: x.amount,
        itemStyle: { color: x.color }
      }))
    }]
  }, true)
}

// ====== ECharts 柱状图 ======
const initBarChart = () => {
  if (!barWrapRef.value) return
  if (!barChart) barChart = echarts.init(barWrapRef.value)
  const items = barItems.value
  if (!items.length) { barChart.clear(); return }
  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: 'rgba(148,163,184,0.18)',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 13 },
      formatter: (p) => `${p[0].axisValue}月<br/>${p[0].marker} ¥${moneyText(p[0].value)}`
    },
    grid: { left: 6, right: 12, top: 8, bottom: 22 },
    xAxis: {
      type: 'category',
      data: items.map((x) => `${x.month}月`),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { fontSize: 10, color: 'rgba(17,24,39,0.45)', fontWeight: 600 }
    },
    yAxis: {
      type: 'value', show: false,
      min: 0,
      max: (v) => (v.max || 1) * 1.18
    },
    series: [{
      type: 'bar',
      data: items.map((x) => ({
        value: x.amount,
        itemStyle: {
          borderRadius: [8, 8, 4, 4],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99,102,241,0.88)' },
            { offset: 1, color: 'rgba(139,92,246,0.82)' }
          ])
        }
      })),
      barWidth: '60%',
      emphasis: {
        itemStyle: { color: '#6366f1' }
      },
      animationDelay: (idx) => idx * 60,
      animationEasing: 'elasticOut',
      animationDuration: 700
    }]
  }, true)
}

const updateCharts = () => {
  nextTick(() => {
    initPieChart()
    initBarChart()
  })
}

// 窗口 resize 时重绘
let resizeTimer = 0
const onResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    if (pieChart) pieChart.resize()
    if (barChart) barChart.resize()
  }, 200)
}

const normalizeCategory = (s) => String(s || '').trim()
const categoryColor = (category) => {
  const c = normalizeCategory(category)
  if (c.includes('约会')) return 'rgba(139, 92, 246, 0.92)'
  if (c.includes('餐饮') || c.includes('吃饭') || c.includes('外卖')) return 'rgba(139, 92, 246, 0.92)'
  if (c.includes('礼物')) return 'rgba(96, 165, 250, 0.92)'
  if (c.includes('购物') || c.includes('网购')) return 'rgba(52, 211, 153, 0.92)'
  if (c.includes('交通') || c.includes('打车') || c.includes('公交') || c.includes('地铁')) return 'rgba(251, 146, 60, 0.92)'
  if (c === '其他' || c.includes('其他')) return 'rgba(148, 163, 184, 0.9)'
  const key = Array.from(c || '其他').reduce((a, ch) => (a * 131 + ch.charCodeAt(0)) % 2147483647, 7)
  const palette = [
    'rgba(139, 92, 246, 0.92)',
    'rgba(139, 92, 246, 0.92)',
    'rgba(96, 165, 250, 0.92)',
    'rgba(52, 211, 153, 0.92)',
    'rgba(251, 146, 60, 0.92)',
    'rgba(148, 163, 184, 0.9)'
  ]
  return palette[key % palette.length]
}

 

const pieItems = computed(() => {
  const total = Number(monthStats.value?.totalAmount || 0) || 0
  const arr = Array.isArray(monthStats.value?.byCategory) ? monthStats.value.byCategory : []
  const list = arr
    .map((x) => ({ category: String(x?.category || '其他'), amount: Number(x?.amount || 0) || 0 }))
    .filter((x) => x.amount > 0)
    .sort((a, b) => b.amount - a.amount)

  const top = list.slice(0, 6)
  const rest = list.slice(6).reduce((s, x) => s + x.amount, 0)
  const merged = rest > 0 ? [...top, { category: '其他', amount: rest }] : top

  return merged.map((x, idx) => {
    const pct = total > 0 ? x.amount / total : 0
    return { ...x, pct, color: categoryColor(x.category) }
  })
})

const barItems = computed(() => {
  const arr = Array.isArray(yearStats.value?.byMonth) ? yearStats.value.byMonth : []
  const map = new Map(arr.map((x) => [Number(x?.month || 0), Number(x?.amount || 0) || 0]))
  const items = Array.from({ length: 12 }, (_, i) => {
    const month = i + 1
    return { month, amount: map.get(month) || 0 }
  })
  const max = Math.max(1, ...items.map((x) => x.amount))
  return items.map((x) => ({ ...x, ratio: x.amount / max }))
})

const yearOptions = computed(() => {
  const y = now.getFullYear()
  return [y - 2, y - 1, y, y + 1].filter((v, i, a) => a.indexOf(v) === i)
})
const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1)
</script>

<template>
  <div class="dash">
    <div class="dash-hero app-card" :style="heroStyle">
      <div class="hero-cover" />
      <div class="hero-mask" />
      <div class="dash-head">
        <div class="dash-title">欢迎回来，{{ auth.user?.nickname || '你' }}</div>
        <div class="dash-sub">{{ todayQuote }}</div>
      </div>
      <div v-if="homeSig" class="sig-float">
        <div class="sig-float-text">{{ homeSig }}</div>
        <div v-if="tempActive" class="sig-float-tag">临时</div>
      </div>
      <div class="hero-tags">
        <span class="htag">双人模式</span>
        <span v-if="togetherDays" class="htag">今天是你们一起的第 {{ togetherDays }} 天</span>
        <span class="htag">今日未读 {{ Number(dashboard.badges.unreadMessages || 0) + Number(dashboard.badges.unreadNotifications || 0) }}</span>
        <span class="htag">纪念日 {{ ann ? (ann.daysLeft === 0 ? '今天' : `${ann.daysLeft} 天`) : '待添加' }}</span>
      </div>
    </div>

    <div class="kpis">
      <button
        v-for="k in kpis"
        :key="k.key"
        class="kpi app-card"
        :class="k.key === 'kpi_ann' ? 'ann' : k.key === 'kpi_album' ? 'photos' : ''"
        type="button"
        @click="goKpi(k.key)"
      >
        <span class="kpi-topline"></span>
        <div class="kpi-icon" :class="k.tone">
          <el-icon :size="18"><component :is="k.icon" /></el-icon>
        </div>
        <div class="kpi-main">
          <div class="kpi-title">{{ k.title }}</div>
          <div class="kpi-value">{{ k.value }}</div>
          <div class="kpi-sub app-muted">
            <span>{{ k.hint }}</span>
            <span v-if="k.key === 'kpi_album'" class="kpi-arrow">→</span>
          </div>
        </div>
        <div v-if="k.key === 'kpi_album' && photos.length" class="kpi-thumb">
          <img :src="assetUrl(photos[0].thumbUrl || photos[0].url)" alt="" />
        </div>
      </button>
    </div>

    <!-- ====== 记账模块（全宽） ====== -->
    <el-card class="app-card" shadow="never">
      <template #header>
        <div class="dz-cardhead">
          <div class="dz-cardtitle dz-cardtitle-heart">趋势分析（记账）</div>
          <div class="dz-cardactions">
            <el-select v-model="acctYear" class="acct-pill" size="small" style="width: 110px">
              <el-option v-for="y in yearOptions" :key="y" :label="`${y} 年`" :value="y" />
            </el-select>
            <el-select v-model="acctMonth" class="acct-pill" size="small" style="width: 92px">
              <el-option v-for="m in monthOptions" :key="m" :label="`${m} 月`" :value="m" />
            </el-select>
            <el-tooltip content="刷新" placement="bottom">
              <button class="acct-iconbtn" type="button" :disabled="acctLoading" @click="loadAccountStats()">
                <el-icon :size="18"><RefreshRight /></el-icon>
              </button>
            </el-tooltip>
            <el-button class="acct-primarybtn" size="small" @click="router.push({ path: '/app/records', query: { tab: 'accounts' } })">
              进入记账
            </el-button>
          </div>
        </div>
      </template>
      <div class="trend">
        <div v-if="acctLoading" class="acct-loading app-muted">正在加载记账统计…</div>
        <div v-else class="acct-layout">
          <div class="acct-core">
            <div class="acct-kpis">
              <div class="acct-kpi-card">
                <div class="k">{{ acctYear }} 年总支出</div>
                <div class="v">¥{{ yearTotal }}</div>
              </div>
              <div class="acct-kpi-card">
                <div class="k">{{ acctYear }}-{{ String(acctMonth).padStart(2, '0') }} 总支出</div>
                <div class="v">¥{{ monthTotal }}</div>
              </div>
            </div>
            <div class="acct-hint app-muted">{{ acctHint }}</div>
          </div>
          <div class="acct-charts">
            <div class="chart">
              <div class="chart-title">分类消费占比</div>
              <div v-if="!pieItems.length" class="chart-empty app-muted">本月还没有记账</div>
              <div v-else class="pie-echarts">
                <div ref="pieWrapRef" class="echart-box" />
                <div class="pie-leg">
                  <div v-for="it in pieItems" :key="it.category" class="leg">
                    <span class="dot" :style="{ background: it.color }" />
                    <div class="leg-main">
                      <div class="leg-name" :style="{ color: it.color }">{{ it.category }}</div>
                      <div class="leg-amt app-muted">¥{{ moneyText(it.amount) }}</div>
                    </div>
                    <div class="leg-pct" :style="{ color: it.color }">{{ Math.round(it.pct * 100) }}%</div>
                  </div>
                </div>
              </div>
            </div>
            <div class="chart">
              <div class="chart-head">
                <div class="chart-title">月度消费趋势</div>
                <el-tooltip content="共 12 个月，柱高为相对值" placement="top">
                  <button class="acct-iconbtn mini" type="button"><span class="mini-dot">i</span></button>
                </el-tooltip>
              </div>
              <div v-if="!barItems.some((x) => x.amount > 0)" class="chart-empty app-muted">今年还没有记账</div>
              <div v-else class="echart-box" ref="barWrapRef" />
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ====== 心愿轮盘 + 最近动态（并排） ====== -->
    <div class="grid grid-row">
      <el-card class="app-card" shadow="never">
        <template #header>
          <div class="dz-cardhead">
            <div class="dz-cardtitle dz-cardtitle-heart">心愿轮盘</div>
            <div class="dz-cardactions">
              <el-button class="acct-primarybtn" size="small" :loading="wheelSpinning" @click="spinWheel">抽一下</el-button>
              <el-button class="wish-link-btn" size="small" text @click="router.push({ path: '/app/records', query: { tab: 'wishlist' } })">去心愿清单</el-button>
            </div>
          </div>
        </template>
        <div class="wheel">
          <div class="wheel-stage">
            <div class="wheel-arrow" />
            <div class="wheel-disk" :class="{ spinning: wheelSpinning }" :style="{ transform: `rotate(${wheelAngle}deg)` }">
              <svg class="wheel-svg" viewBox="0 0 200 200" role="img" aria-label="心愿轮盘">
                <g>
                  <path v-for="s in wheelSlices" :key="s.id" class="w-slice" :class="{ active: wheelActiveId === s.id }" :d="s.d" :fill="s.color" stroke="rgba(255,255,255,0.78)" stroke-width="1">
                    <title>{{ s.content }}</title>
                  </path>
                </g>
                <circle cx="100" cy="100" r="54" fill="rgba(255,255,255,0.96)" />
              </svg>
            </div>
            <div class="wheel-core">
              <div class="wheel-core-title">随机心愿</div>
              <div class="wheel-core-sub">待完成 {{ wishPending.length }} 条</div>
            </div>
            <div v-if="wheelFx.on" class="wheel-fx">
              <span v-for="(h, i) in wheelFx.hearts" :key="`wh_${i}`" class="fxh"
                :style="{ '--x': h.x + '%', '--d': h.d + 'ms', '--dur': h.dur + 'ms', '--s': h.s + 'px', '--o': h.o }" />
              <span v-for="(s, i) in wheelFx.stars" :key="`ws_${i}`" class="fxs"
                :style="{ '--x': s.x + '%', '--y': s.y + '%', '--d': s.d + 'ms', '--dur': s.dur + 'ms', '--s': s.s + 'px', '--o': s.o }" />
            </div>
          </div>
          <div class="wheel-result">
            <div class="wheel-label app-muted">抽中结果</div>
            <div class="wheel-text">{{ wheelResult?.content || '点击抽一下' }}</div>
            <div class="wheel-meta app-muted" v-if="wheelResult?.expectedAt">期望完成：{{ wheelResult.expectedAt }}</div>
            <div v-if="wheelPop" class="wheel-pop">
              <div class="wheel-pop-card app-card">
                <div class="wheel-pop-title">抽中了</div>
                <div class="wheel-pop-main">{{ wheelResult?.content || '' }}</div>
                <div class="wheel-pop-sub app-muted" v-if="wheelResult?.expectedAt">期望完成：{{ wheelResult.expectedAt }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="app-card" shadow="never">
        <template #header>
          <div class="dz-cardhead">
            <div class="dz-cardtitle dz-cardtitle-heart">最近动态</div>
            <div class="dz-cardactions">
              <el-button size="small" text @click="router.push('/app/timeline')">时间轴</el-button>
            </div>
          </div>
        </template>
        <div class="acts">
          <div v-if="!activities.length" class="empty app-muted">暂无动态</div>
          <div v-else class="actlist">
            <div v-for="(a, i) in activities" :key="a.key" class="act stagger-item" :style="{ animationDelay: `${i * 60}ms` }">
              <div class="dot" :class="a.tone" />
              <div class="act-main">
                <div class="act-title">{{ a.title }}</div>
                <div class="act-sub app-muted">{{ a.content }}</div>
                <div class="act-time app-muted">{{ a.at ? new Date(a.at).toLocaleString() : '' }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.dash-hero {
  --hero-cover: none;
  position: relative;
  overflow: hidden;
  padding: 16px;
  border-color: rgba(139, 92, 246, 0.18);
}
.hero-cover {
  position: absolute;
  inset: 0;
  background-image: var(--hero-cover);
  background-size: cover;
  background-position: center;
  opacity: 0.16;
  pointer-events: none;
}
.hero-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(139, 92, 246, 0.18), rgba(139, 92, 246, 0.18)),
    radial-gradient(1100px 240px at -8% -20%, rgba(139, 92, 246, 0.22), transparent 65%),
    radial-gradient(900px 220px at 105% 0%, rgba(139, 92, 246, 0.2), transparent 60%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.82));
  pointer-events: none;
}
.dash-head {
  position: relative;
  z-index: 1;
  padding: 8px 2px;
}
.dash-title {
  font-size: 26px;
  font-weight: 950;
  letter-spacing: 0.2px;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.95), rgba(139, 92, 246, 0.95));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
  -webkit-text-stroke: 0.45px rgba(255, 255, 255, 0.42);
}
.dash-sub {
  margin-top: 6px;
  font-size: 15px;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.82);
}
.sig-float {
  position: absolute;
  right: 14px;
  top: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(204, 251, 241, 0.32);
  border: 1px solid rgba(99, 102, 241, 0.2);
  box-shadow: 0 14px 30px rgba(99, 102, 241, 0.08);
  backdrop-filter: blur(12px);
}
.sig-float-text {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 900;
  color: rgba(79, 70, 229, 0.92);
}
.sig-float-tag {
  font-size: 12px;
  font-weight: 900;
  color: rgba(79, 70, 229, 0.92);
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.18);
  padding: 1px 8px;
  border-radius: 999px;
}
.hero-tags {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}
.htag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: rgba(79, 70, 229, 0.9);
  background: rgba(204, 251, 241, 0.5);
  border: 1px solid rgba(139, 92, 246, 0.25);
  transition: background 0.15s, border-color 0.15s;
  cursor: default;
}
.htag:hover {
  background: rgba(204, 251, 241, 0.72);
  border-color: rgba(139, 92, 246, 0.42);
}

.kpis {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.kpi {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.7);
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
/* 渐变顶边：柔粉 → 浅粉 */
.kpi-topline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  border-radius: 2px 2px 0 0;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.45), rgba(255, 182, 193, 0.25));
  opacity: 0.72;
  transition: opacity 0.18s ease;
}
.kpi::before {
  content: '';
  position: absolute;
  right: -28px;
  top: -28px;
  width: 86px;
  height: 86px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.18) 0%, rgba(139, 92, 246, 0.04) 48%, transparent 70%);
  opacity: 0;
  transition: opacity 0.18s ease;
  pointer-events: none;
}
.kpi::after {
  content: '❤';
  position: absolute;
  right: 10px;
  top: 8px;
  font-size: 12px;
  color: rgba(99, 102, 241, 0.45);
  opacity: 0;
  transform: translateY(-1px);
  transition: opacity 0.18s ease, transform 0.18s ease;
  pointer-events: none;
}
.kpi:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 34px rgba(99, 102, 241, 0.1);
  border-color: rgba(139, 92, 246, 0.28);
}
.kpi:hover .kpi-topline {
  opacity: 1;
}
.kpi:hover::before,
.kpi:hover::after {
  opacity: 1;
}
.kpi:hover::after {
  transform: translateY(0);
}
.kpi-icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(139, 92, 246, 0.1);
  flex: none;
  transition: transform 0.18s ease;
}
.kpi:hover .kpi-icon {
  transform: scale(1.06);
}
/* 未读消息 → 浅柔粉 */
.kpi-icon.blue {
  background: rgba(139, 92, 246, 0.12);
  color: rgba(79, 70, 229, 0.88);
  border-color: rgba(139, 92, 246, 0.16);
}
/* AI管家 / 生理期 / 纪念日 → 浅柔紫 */
.kpi-icon.violet {
  background: rgba(139, 92, 246, 0.13);
  color: rgba(109, 40, 217, 0.88);
  border-color: rgba(139, 92, 246, 0.16);
}
/* 最近照片 → 柔绿 */
.kpi-icon.green {
  background: rgba(129, 140, 248, 0.12);
  color: rgba(99, 102, 241, 0.9);
  border-color: rgba(129, 140, 248, 0.16);
}
/* amber 降级为柔橙 */
.kpi-icon.amber {
  background: rgba(255, 182, 130, 0.14);
  color: rgba(194, 98, 38, 0.88);
  border-color: rgba(255, 182, 130, 0.18);
}
.kpi-main {
  min-width: 0;
}
.kpi-title {
  font-size: 13px;
  font-weight: 800;
  color: rgba(17, 24, 39, 0.7);
}
.kpi-value {
  margin-top: 4px;
  font-size: 28px;
  font-weight: 950;
}
.kpi-sub {
  margin-top: 6px;
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi.ann {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(255, 255, 255, 0.78));
}
.kpi.ann .kpi-topline {
  opacity: 1;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.6), rgba(255, 182, 193, 0.35));
}
.kpi.ann::after {
  content: '❤';
  right: 12px;
  top: 10px;
  font-size: 13px;
  color: rgba(99, 102, 241, 0.5);
}
.kpi.photos {
  background: linear-gradient(135deg, rgba(129, 140, 248, 0.06), rgba(255, 255, 255, 0.78));
}
.kpi.photos .kpi-topline {
  background: linear-gradient(90deg, rgba(129, 140, 248, 0.35), rgba(167, 243, 208, 0.2));
}
.kpi-thumb {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(17, 24, 39, 0.08);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.7);
}
.kpi-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.kpi-arrow {
  margin-left: 6px;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.55);
}

.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
@media (min-width: 960px) {
  .grid-row {
    grid-template-columns: 1fr 1fr;
  }
}
.wheel {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 18px;
  align-items: center;
}
@media (max-width: 860px) {
  .wheel {
    grid-template-columns: 1fr;
  }
}
.wheel-stage {
  position: relative;
  width: 240px;
  height: 240px;
  margin: 0 auto;
}
.wheel-arrow {
  position: absolute;
  top: -6px;
  left: 50%;
  width: 0;
  height: 0;
  border-left: 9px solid transparent;
  border-right: 9px solid transparent;
  border-bottom: 16px solid rgba(99, 102, 241, 0.8);
  transform: translateX(-50%);
  filter: drop-shadow(0 6px 12px rgba(99, 102, 241, 0.35));
}
.wheel-disk {
  width: 240px;
  height: 240px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  position: relative;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.6);
  transition: transform 1.55s cubic-bezier(0.2, 0.8, 0.1, 1);
}
.wheel-disk.spinning {
  filter: drop-shadow(0 16px 30px rgba(99, 102, 241, 0.2));
}
.wheel-svg {
  width: 100%;
  height: 100%;
  display: block;
}
.w-slice {
  transition: transform 0.25s ease, filter 0.25s ease, opacity 0.25s ease;
  transform-origin: 100px 100px;
  opacity: 0.92;
}
.w-slice.active {
  opacity: 1;
  transform: scale(1.03);
  filter: drop-shadow(0 10px 18px rgba(99, 102, 241, 0.22));
}
.wheel-fx {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 3;
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
    transform: translate(-50%, 280px) rotate(18deg) scale(1);
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
.wheel-core {
  position: absolute;
  inset: 68px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: grid;
  place-items: center;
  text-align: center;
  z-index: 1;
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}
.wheel-core-title {
  font-weight: 950;
  font-size: 14px;
}
.wheel-core-sub {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.55);
}
.wheel-result {
  display: grid;
  gap: 6px;
}
.wheel-label {
  font-size: 12px;
}
.wheel-text {
  font-size: 18px;
  font-weight: 950;
  line-height: 1.4;
  color: rgba(17, 24, 39, 0.9);
}
.wheel-meta {
  font-size: 12px;
}
.wheel-pop {
  margin-top: 8px;
  position: relative;
}
.wheel-pop-card {
  padding: 12px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 38px rgba(99, 102, 241, 0.12);
  animation: pop 0.22s ease-out both;
}
.wheel-pop-title {
  font-size: 12px;
  font-weight: 900;
  color: rgba(99, 102, 241, 0.85);
}
.wheel-pop-main {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 950;
  line-height: 1.35;
  color: rgba(17, 24, 39, 0.92);
}
.wheel-pop-sub {
  margin-top: 6px;
  font-size: 12px;
}
@keyframes pop {
  0% {
    transform: translateY(6px) scale(0.98);
    opacity: 0;
  }
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}
.trend {
  min-height: 260px;
}
.dz-cardactions {
  flex-wrap: wrap;
}
.acct-pill :deep(.el-input__wrapper) {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: none;
}
.acct-pill :deep(.el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.35);
}
.acct-pill :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(64, 158, 255, 0.55);
}
.acct-iconbtn {
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  padding: 8px 10px;
  cursor: pointer;
  color: rgba(17, 24, 39, 0.7);
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}
.acct-iconbtn:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.1);
}
.acct-iconbtn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}
.acct-iconbtn.mini {
  padding: 6px 9px;
}
.mini-dot {
  font-weight: 900;
  font-size: 12px;
  line-height: 1;
  display: inline-block;
  transform: translateY(-0.5px);
}
.acct-primarybtn {
  border-radius: 999px;
  padding: 0 14px;
  height: 32px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: 0;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  transition: transform 0.15s ease, filter 0.15s ease, box-shadow 0.15s ease;
  box-shadow: 0 2px 10px rgba(99, 102, 241, 0.22);
}
.acct-primarybtn:hover {
  transform: translateY(-1px);
  filter: brightness(1.06);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.32);
}
.acct-primarybtn:active {
  transform: scale(0.97);
}

.wish-link-btn {
  color: rgba(79, 70, 229, 0.82) !important;
  font-weight: 600;
}
.wish-link-btn:hover {
  color: #4f46e5 !important;
  background: rgba(99, 102, 241, 0.06) !important;
}
.acct-layout {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  padding-top: 6px;
}
.acct-core {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.acct-kpis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.acct-kpi-card {
  border-radius: 16px;
  padding: 12px 12px 10px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(139, 92, 246, 0.12), rgba(59, 130, 246, 0.08)),
    rgba(255, 255, 255, 0.62);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;
}
.acct-kpi-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 38px rgba(99, 102, 241, 0.12);
  filter: brightness(1.01);
}
.acct-kpi-card .k {
  font-size: 12px;
  font-weight: 900;
  color: rgba(17, 24, 39, 0.58);
}
.acct-kpi-card .v {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 950;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.95), rgba(139, 92, 246, 0.95));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.acct-hint {
  font-size: 12px;
  padding: 2px 2px 0;
}
.acct-loading {
  padding: 18px 6px;
  font-size: 12px;
}
.acct-charts {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
.chart {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.62);
  padding: 12px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.chart-title {
  font-weight: 950;
  font-size: 13px;
  color: rgba(17, 24, 39, 0.82);
}
.chart-empty {
  padding: 16px 0 6px;
  text-align: center;
  font-size: 12px;
}
/* ====== ECharts 图表容器 ====== */
.pie-echarts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: center;
  padding-top: 6px;
}
.echart-box {
  width: 100%;
  height: 180px;
}
.pie-leg {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.leg {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  gap: 8px;
  align-items: center;
  min-width: 0;
  padding: 6px 8px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.55);
  transition: border-color 0.15s ease, background-color 0.15s ease;
}
.leg.active {
  border-color: rgba(99, 102, 241, 0.38);
  background: rgba(99, 102, 241, 0.1);
}
.leg:hover {
  border-color: rgba(99, 102, 241, 0.28);
  background: rgba(99, 102, 241, 0.08);
}
.leg .dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}
.leg-main {
  min-width: 0;
}
.leg-name {
  font-weight: 950;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.leg-amt {
  margin-top: 2px;
  font-size: 12px;
}
.leg-pct {
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.2px;
}
.acts {
  min-height: 260px;
}
.actlist {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.act {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 12px;
  align-items: start;
  padding: 8px 10px;
  margin: 0 -10px;
  border-radius: 12px;
  transition: background 0.15s;
}
.act:hover {
  background: rgba(139, 92, 246, 0.04);
}
.dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  margin-top: 5px;
  flex-shrink: 0;
}
/* 统一柔粉圆点 */
.dot.blue,
.dot.green,
.dot.amber,
.dot.violet {
  background: rgba(99, 102, 241, 0.72);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}
.act-title {
  font-weight: 900;
  font-size: 13px;
  color: rgba(17, 24, 39, 0.82);
}
.act-sub {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.35;
}
.act-time {
  margin-top: 6px;
  font-size: 12px;
}
.empty {
  padding: 12px 4px;
  text-align: center;
  font-size: 12px;
}

/* 标题爱心图标 + 粉线下划线 */
.dz-cardtitle-heart::before {
  content: '❤ ';
  font-size: 12px;
  vertical-align: middle;
  color: rgba(99, 102, 241, 0.7);
}
.dz-cardtitle-heart {
  position: relative;
  padding-bottom: 4px;
}
.dz-cardtitle-heart::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  border-radius: 1px;
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.38), transparent);
}

@media (min-width: 960px) {
  .kpis {
    grid-template-columns: repeat(5, 1fr);
  }
  .grid {
    grid-template-columns: 1fr 360px;
    align-items: start;
  }
  .acct-charts {
    grid-template-columns: 1fr;
  }
  .acct-layout {
    grid-template-columns: 1fr 1.2fr;
    align-items: start;
  }
}
</style>

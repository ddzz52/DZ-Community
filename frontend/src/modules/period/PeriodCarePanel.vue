<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Calendar, InfoFilled, Loading, RefreshRight, Setting } from '@element-plus/icons-vue'
import http from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const isFemale = computed(() => auth.user?.gender === 2)

const loading = ref(false)
const status = ref(null)

const ymd = (d) => {
  const dt = d instanceof Date ? d : new Date(d)
  if (!dt || Number.isNaN(dt.getTime())) return ''
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const parseYmd = (s) => {
  const t = String(s || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(t)) return null
  const [y, m, d] = t.split('-').map((x) => Number(x))
  const dt = new Date(y, m - 1, d)
  if (Number.isNaN(dt.getTime())) return null
  if (ymd(dt) !== t) return null
  return dt
}

const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
const addDays = (d, days) => new Date(d.getFullYear(), d.getMonth(), d.getDate() + days)
const daysBetween = (a, b) => {
  if (!a || !b) return null
  const ta = startOfDay(a).getTime()
  const tb = startOfDay(b).getTime()
  return Math.round((tb - ta) / 86400000)
}

const disableFutureDate = (time) => {
  if (!time) return false
  const d = time instanceof Date ? time : new Date(time)
  if (Number.isNaN(d.getTime())) return false
  const now = new Date()
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999)
  return d.getTime() > end.getTime()
}

const load = async (silent) => {
  try {
    loading.value = true
    status.value = await http.get('/api/period/status')
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => load(true))

const settings = computed(() => status.value?.settings || null)
const prediction = computed(() => status.value?.prediction || null)

const headline = computed(() => {
  const p = prediction.value
  if (!p) return null
  const days = typeof p.daysToNextPeriod === 'number' ? p.daysToNextPeriod : null
  const phase = p.phase || ''
  const dayNo = typeof p.cycleDay === 'number' ? p.cycleDay : 1
  if (!phase) return null
  if (days == null) return { title: phase, sub: `第${dayNo}天` }
  if (p.predictedDatePassed) {
    return { title: `📋 请确认经期`, sub: `第${dayNo}天 · 预测日已过，请问经期来了吗？`, needConfirm: true }
  }
  if (days === 0) return { title: phase, sub: `第${dayNo}天 · 今天可能来潮` }
  return { title: phase, sub: `第${dayNo}天 · 距离经期约${days}日` }
})

const hint = computed(() => {
  const p = prediction.value
  if (!p) return ''
  const d = typeof p.daysToNextPeriod === 'number' ? p.daysToNextPeriod : null
  if (d == null) return '先完成一次周期设置，系统就能开始预测。'
  if (d >= 10) return '今天也要好好照顾彼此。'
  if (d >= 4) return '经期将近，记得多喝热水、少熬夜，我也会一直陪着你❤️'
  if (d >= 1) return '经期快到了，提前准备卫生用品更安心，我也会一直陪着你❤️'
  return '今天可能要开始啦，记得温柔一点，我也会一直陪着你❤️'
})

const calcFrom = computed(() => {
  const s = settings.value
  if (!s?.lastStartDate) return ''
  const cd = typeof s.cycleDays === 'number' ? s.cycleDays : 28
  const pd = typeof s.periodDays === 'number' ? s.periodDays : 5
  return `根据 ${s.lastStartDate}（周期 ${cd} 天 / 经期 ${pd} 天）推算`
})

const headTitle = computed(() => headline.value?.title || '生理期关怀')
const headSub = computed(() => {
  if (headline.value?.sub) return headline.value.sub
  if (!settings.value) return '完成周期设置后，系统会自动预测经期与排卵期'
  return '正在预测中…'
})

const phaseDetail = computed(() => {
  const p = prediction.value
  if (!p) return ''
  const ov = p.ovulationDay || ''
  const fs = p.fertileStart || ''
  const fe = p.fertileEnd || ''
  const ns = p.nextPeriodStart || ''
  const ne = p.nextPeriodEnd || ''
  const nsDt = parseYmd(ns)
  const feDt = parseYmd(fe)
  let luteal = ''
  if (feDt && nsDt) {
    const ls = new Date(feDt.getFullYear(), feDt.getMonth(), feDt.getDate() + 1)
    const le = new Date(nsDt.getFullYear(), nsDt.getMonth(), nsDt.getDate() - 1)
    luteal = `${ymd(ls)} ~ ${ymd(le)}`
  }
  const parts = []
  if (ov) parts.push(`🥚 排卵日 ${ov}`)
  if (fs && fe) parts.push(`🌱 易孕期 ${fs} ~ ${fe}`)
  if (luteal) parts.push(`🌼 黄体期 ${luteal}`)
  if (ns && ne) parts.push(`📅 下次经期 ${ns} ~ ${ne}`)
  return parts.join('  ·  ')
})

const dialog = ref(false)
const saving = ref(false)
const form = reactive({
  cycleDays: 28,
  periodDays: 5,
  lastStartDate: ''
})

watch(
  () => settings.value,
  (s) => {
    if (!s) return
    form.cycleDays = typeof s.cycleDays === 'number' ? s.cycleDays : 28
    form.periodDays = typeof s.periodDays === 'number' ? s.periodDays : 5
    form.lastStartDate = s.lastStartDate || ''
  },
  { immediate: true }
)

const openSettings = () => {
  if (!form.lastStartDate) form.lastStartDate = ymd(new Date())
  dialog.value = true
}

const saveSettings = async () => {
  const cycleDays = Number(form.cycleDays)
  const periodDays = Number(form.periodDays)
  const last = String(form.lastStartDate || '').trim()
  if (!Number.isFinite(cycleDays) || cycleDays < 25 || cycleDays > 45) {
    ElMessage.error('周期天数需在 25-45 天')
    return
  }
  if (!Number.isFinite(periodDays) || periodDays < 3 || periodDays > 7) {
    ElMessage.error('经期天数需在 3-7 天')
    return
  }
  if (!parseYmd(last)) {
    ElMessage.error('上次经期开始日期格式需为 yyyy-MM-dd')
    return
  }
  const lastDt = parseYmd(last)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (lastDt && lastDt.getTime() > today.getTime()) {
    ElMessage.error('上次经期开始日期不能晚于今天')
    return
  }
  try {
    saving.value = true
    status.value = await http.put('/api/period/settings', {
      cycleDays,
      periodDays,
      lastStartDate: last
    })
    dialog.value = false
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const confirming = ref(false)
const confirmingAction = ref(null)

const confirmPeriod = async (action) => {
  try {
    confirming.value = true
    confirmingAction.value = action
    status.value = await http.post('/api/period/confirm', {
      action,
      date: ymd(new Date())
    })
    if (action === 'started') {
      ElMessage.success('已记录，今天来潮了')
    } else {
      ElMessage.success('已记录，今天不再提醒')
    }
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    confirming.value = false
    confirmingAction.value = null
  }
}

const reminderSwitching = ref(false)
const reminderEnabled = computed(() => {
  const v = settings.value?.reminderEnabled
  return v !== false
})

const toggleReminder = async (v) => {
  try {
    reminderSwitching.value = true
    status.value = await http.patch('/api/period/reminder', { enabled: !!v })
    ElMessage.success(v ? '已开启提醒' : '已关闭提醒')
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
    await load(true)
  } finally {
    reminderSwitching.value = false
  }
}

const monthCursor = ref(new Date())
const monthInited = ref(false)
watch(
  () => prediction.value?.nextPeriodStart,
  (v) => {
    if (!v) return
    if (!monthInited.value) {
      const dt = parseYmd(v)
      if (dt) {
        monthCursor.value = new Date(dt.getFullYear(), dt.getMonth(), 1)
        monthInited.value = true
      }
    }
  },
  { immediate: true }
)

const monthTitle = computed(() => {
  const d = monthCursor.value
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月`
})

const monthDir = ref('next')
const monthAnimName = computed(() => (monthDir.value === 'prev' ? 'slide-right' : 'slide-left'))

const goPrevMonth = () => {
  monthDir.value = 'prev'
  const d = monthCursor.value
  monthCursor.value = new Date(d.getFullYear(), d.getMonth() - 1, 1)
}
const goNextMonth = () => {
  monthDir.value = 'next'
  const d = monthCursor.value
  monthCursor.value = new Date(d.getFullYear(), d.getMonth() + 1, 1)
}

const startOfMonth = computed(() => new Date(monthCursor.value.getFullYear(), monthCursor.value.getMonth(), 1))
const daysInMonth = computed(() => new Date(monthCursor.value.getFullYear(), monthCursor.value.getMonth() + 1, 0).getDate())
const leading = computed(() => startOfMonth.value.getDay())
const gridDays = computed(() => {
  const out = []
  for (let i = 0; i < leading.value; i += 1) out.push(null)
  for (let d = 1; d <= daysInMonth.value; d += 1) out.push(new Date(monthCursor.value.getFullYear(), monthCursor.value.getMonth(), d))
  while (out.length % 7 !== 0) out.push(null)
  return out
})

const ranges = computed(() => {
  const p = prediction.value
  if (!p) return {}
  const cps = parseYmd(p.currentPeriodStart)
  const cpe = parseYmd(p.currentPeriodEnd)
  const ps = parseYmd(p.nextPeriodStart)
  const pe = parseYmd(p.nextPeriodEnd)
  const fs = parseYmd(p.fertileStart)
  const fe = parseYmd(p.fertileEnd)
  const ov = parseYmd(p.ovulationDay)
  const lutealStart = fe ? new Date(fe.getFullYear(), fe.getMonth(), fe.getDate() + 1) : null
  const lutealEnd = ps ? new Date(ps.getFullYear(), ps.getMonth(), ps.getDate() - 1) : null

  const ps2 = parseYmd(p.next2PeriodStart)
  const pe2 = parseYmd(p.next2PeriodEnd)
  const fs2 = parseYmd(p.fertileStart2)
  const fe2 = parseYmd(p.fertileEnd2)
  const ov2 = parseYmd(p.ovulationDay2)
  const lutealStart2 = fe2 ? new Date(fe2.getFullYear(), fe2.getMonth(), fe2.getDate() + 1) : null
  const lutealEnd2 = ps2 ? new Date(ps2.getFullYear(), ps2.getMonth(), ps2.getDate() - 1) : null
  return {
    currentPeriodStart: cps,
    currentPeriodEnd: cpe,
    periodStart: ps,
    periodEnd: pe,
    fertileStart: fs,
    fertileEnd: fe,
    ovulation: ov,
    lutealStart,
    lutealEnd,
    periodStart2: ps2,
    periodEnd2: pe2,
    fertileStart2: fs2,
    fertileEnd2: fe2,
    ovulation2: ov2,
    lutealStart2,
    lutealEnd2
  }
})

const isSameDay = (a, b) => {
  if (!a || !b) return false
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}
const isInRange = (d, s, e) => {
  if (!d || !s || !e) return false
  const t = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const ts = new Date(s.getFullYear(), s.getMonth(), s.getDate()).getTime()
  const te = new Date(e.getFullYear(), e.getMonth(), e.getDate()).getTime()
  return t >= ts && t <= te
}

const cellClass = (d) => {
  if (!d) return 'cell empty'
  const today = new Date()
  const cls = ['cell']
  if (isSameDay(d, today)) cls.push('today')
  const r = ranges.value
  if (isInRange(d, r.currentPeriodStart, r.currentPeriodEnd)) cls.push('period')
  if (isSameDay(d, r.currentPeriodStart)) cls.push('pstart')
  if (isInRange(d, r.fertileStart, r.fertileEnd) || isInRange(d, r.fertileStart2, r.fertileEnd2)) cls.push('fertile')
  if (isInRange(d, r.periodStart, r.periodEnd) || isInRange(d, r.periodStart2, r.periodEnd2)) cls.push('period')
  if (isInRange(d, r.lutealStart, r.lutealEnd) || isInRange(d, r.lutealStart2, r.lutealEnd2)) cls.push('luteal')
  if (isSameDay(d, r.ovulation) || isSameDay(d, r.ovulation2)) cls.push('ovu')
  if (isSameDay(d, r.periodStart) || isSameDay(d, r.periodStart2)) cls.push('pstart')
  return cls.join(' ')
}

const dayInfo = (d) => {
  if (!d) return null
  const s = settings.value
  if (!s) return null
  const base = parseYmd(s.lastStartDate)
  const cd = typeof s.cycleDays === 'number' ? s.cycleDays : 28
  const pd = typeof s.periodDays === 'number' ? s.periodDays : 5
  if (!base || !Number.isFinite(cd) || cd <= 0 || !Number.isFinite(pd) || pd <= 0) return null

  const t = startOfDay(d)
  const diff = daysBetween(base, t)
  const cycleDay = diff == null ? null : ((diff % cd) + cd) % cd + 1

  const k = diff == null ? 0 : Math.floor(diff / cd)
  const cycleStart = addDays(base, k * cd)
  const periodEnd = addDays(cycleStart, pd - 1)
  const nextStart = addDays(cycleStart, cd)
  const ovulation = addDays(nextStart, -14)
  const fertileStart = addDays(ovulation, -5)
  const fertileEnd = addDays(ovulation, 4)
  const lutealStart = addDays(fertileEnd, 1)
  const lutealEnd = addDays(nextStart, -1)

  let phase = ''
  let phaseDay = null
  if (isInRange(t, cycleStart, periodEnd)) {
    phase = '经期'
    phaseDay = daysBetween(cycleStart, t) + 1
  } else if (isInRange(t, fertileStart, fertileEnd) || isSameDay(t, ovulation)) {
    phase = '排卵期'
    phaseDay = daysBetween(fertileStart, t) + 1
  } else if (t.getTime() > startOfDay(periodEnd).getTime() && t.getTime() < startOfDay(fertileStart).getTime()) {
    phase = '卵泡期'
    phaseDay = daysBetween(addDays(periodEnd, 1), t) + 1
  } else if (t.getTime() > startOfDay(fertileEnd).getTime() && t.getTime() < startOfDay(nextStart).getTime()) {
    phase = '黄体期'
    phaseDay = daysBetween(lutealStart, t) + 1
  } else {
    phase = '卵泡期'
    phaseDay = null
  }

  const daysToPeriod = Math.max(0, daysBetween(t, nextStart))

  return { phase, phaseDay, daysToPeriod, cycleDay }
}

const dayTip = (d) => {
  const info = dayInfo(d)
  if (!info) return '完善周期设置后可查看每日详情'
  const p1 = info.phaseDay ? `${info.phase}第 ${info.phaseDay} 天` : info.phase
  if (info.phase === '经期') return `${p1} · 这几天要更温柔一点`
  if (typeof info.daysToPeriod === 'number') return `${p1} · 距离经期约 ${info.daysToPeriod} 日`
  return p1
}

const cellStyle = (i) => ({ '--i': String(i) })

const currentPhase = computed(() => String(prediction.value?.phase || ''))
const phaseDocs = computed(() => [
  {
    key: '经期',
    title: '月经期（生理期）',
    range: '月经来潮当天至经期结束',
    body: ['子宫内膜脱落、出血', '容易疲惫、腹痛、腰酸、情绪敏感'],
    care: ['注意保暖，不吃生冷辛辣', '多喝热水、避免熬夜', '男生多包容、少吵架、多照顾情绪'],
    extra: ['痛经提醒', '暖心语录', '忌口推荐']
  },
  {
    key: '卵泡期',
    title: '卵泡期（黄金恢复期）',
    range: '经期结束后 → 排卵前',
    body: ['气血慢慢恢复、皮肤变好、精力回升', '新陈代谢旺盛，适合作息调理、运动'],
    care: ['适合食补、轻微运动', '状态心情普遍较好，适合约会、出行'],
    extra: []
  },
  {
    key: '排卵期',
    title: '排卵期（易孕期）',
    range: '排卵日前 5 天～后 4 天（共 10 天）',
    body: ['白带增多、体温轻微升高', '欲望稍高、心情愉悦'],
    care: ['备孕最佳时期 / 需做好防护', '情绪稳定、适合亲密互动'],
    extra: []
  },
  {
    key: '黄体期',
    title: '黄体期（经前烦躁期）',
    range: '排卵结束 → 下次月经来前（约 14 天）',
    body: ['激素波动大', '容易长痘、水肿、乳房胀痛', '易怒、焦虑、情绪低落（PMS 经前综合征）'],
    care: ['多迁就、少顶嘴', '提前提醒快要来月经，备好红糖、暖宝宝'],
    extra: []
  }
])

const warmLine = computed(() => {
  const phase = currentPhase.value
  const key = `${ymd(new Date())}_${phase}`
  const hash = Array.from(key).reduce((a, c) => (a * 31 + c.charCodeAt(0)) % 2147483647, 7)
  if (phase === '排卵期') {
    const arr = ['今天很适合约会哦～', '想你了，晚上一起散散步吧～', '今天的你特别闪闪发光～']
    return arr[hash % arr.length]
  }
  if (phase === '黄体期') {
    const arr = ['多喝热水，早点休息，我一直陪着你。', '别太累啦，我会一直站在你这边。', '今天也辛苦了，我们慢慢来。']
    return arr[hash % arr.length]
  }
  return ''
})
</script>

<template>
  <div class="wrap">
    <div class="head app-card">
      <div class="head-left">
        <div class="h">{{ headTitle }}</div>
        <div class="subrow">
          <div class="sub app-muted">{{ headSub }}</div>
          <el-tooltip v-if="calcFrom" :content="calcFrom" placement="top">
            <button class="head-iconbtn" type="button">
              <el-icon :size="16"><InfoFilled /></el-icon>
            </button>
          </el-tooltip>
        </div>
        <div class="phase-detail app-muted" v-if="phaseDetail">{{ phaseDetail }}</div>
        <div class="hint app-muted">{{ hint }}</div>
        <transition name="confirm-fade">
          <div v-if="headline?.needConfirm" class="confirm-actions">
            <button
              class="confirm-btn started"
              type="button"
              :disabled="confirming"
              @click="confirmPeriod('started')"
            >
              <el-icon v-if="confirming && confirmingAction === 'started'" class="is-loading" :size="14"><Loading /></el-icon>
              <span v-else>✅</span>
              {{ confirming && confirmingAction === 'started' ? '确认中…' : '经期来了' }}
            </button>
            <button
              class="confirm-btn delayed"
              type="button"
              :disabled="confirming"
              @click="confirmPeriod('delayed')"
            >
              <el-icon v-if="confirming && confirmingAction === 'delayed'" class="is-loading" :size="14"><Loading /></el-icon>
              <span v-else>⏳</span>
              {{ confirming && confirmingAction === 'delayed' ? '确认中…' : '还没来' }}
            </button>
          </div>
        </transition>
      </div>
      <div class="head-actions">
        <el-tooltip content="刷新" placement="bottom">
          <button class="head-iconbtn" type="button" :disabled="loading" @click="load()">
            <el-icon :size="18"><RefreshRight /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="周期设置" placement="bottom">
          <button class="head-iconbtn" type="button" @click="openSettings">
            <el-icon :size="18"><Setting /></el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>

    <div v-if="!settings" class="empty app-card">
      <el-icon class="empty-ic" :size="36"><Calendar /></el-icon>
      <div class="empty-title">还没有周期数据</div>
      <div class="empty-sub app-muted">记录一次上次经期开始日期，系统就能开始预测与提醒。</div>
      <el-button type="primary" round @click="openSettings">立即设置</el-button>
      <div v-if="!isFemale" class="app-muted">你也可以帮 TA 先填一次周期设置，后续随时可调整。</div>
    </div>

    <div v-else class="grid">
      <div class="cal app-card">
        <div class="cal-top">
          <div class="cal-title">{{ monthTitle }}</div>
          <div class="cal-nav">
            <el-button circle size="small" text @click="goPrevMonth"><el-icon :size="16"><ArrowLeft /></el-icon></el-button>
            <el-button circle size="small" text @click="goNextMonth"><el-icon :size="16"><ArrowRight /></el-icon></el-button>
          </div>
        </div>
        <div class="week app-muted">
          <div>日</div>
          <div>一</div>
          <div>二</div>
          <div>三</div>
          <div>四</div>
          <div>五</div>
          <div>六</div>
        </div>
          <transition :name="monthAnimName" mode="out-in">
            <div :key="monthTitle" class="grid7">
              <div v-for="(d, i) in gridDays" :key="i" :class="cellClass(d)" :style="cellStyle(i)">
                <el-tooltip v-if="d" :content="dayTip(d)" placement="top" :show-after="120">
                  <div class="day">{{ d.getDate() }}</div>
                </el-tooltip>
              </div>
            </div>
          </transition>
          <div class="legend">
            <div class="phase-row small">
              <el-popover
                v-for="it in phaseDocs"
                :key="it.key"
                placement="top-start"
                trigger="hover"
                :width="360"
                :teleported="true"
                popper-class="dz-phase-pop"
              >
                <template #reference>
                  <button class="phase-pill" :class="{ active: currentPhase === it.key }" type="button">
                    <span class="phase-name">{{ it.key }}</span>
                    <span v-if="currentPhase === it.key" class="phase-tag">当前</span>
                  </button>
                </template>
                <div class="pop">
                  <div class="pop-title">{{ it.title }}</div>
                  <div class="pop-range app-muted">{{ it.range }}</div>
                  <div class="pop-sec">
                    <div class="pop-k app-muted">身体特征</div>
                    <ul class="pop-ul">
                      <li v-for="(t, i) in it.body" :key="i">{{ t }}</li>
                    </ul>
                  </div>
                  <div class="pop-sec">
                    <div class="pop-k app-muted">情侣关怀提示</div>
                    <ul class="pop-ul">
                      <li v-for="(t, i) in it.care" :key="i">{{ t }}</li>
                    </ul>
                  </div>
                  <div v-if="it.extra && it.extra.length" class="pop-sec">
                    <div class="pop-k app-muted">适配功能</div>
                    <div class="docs-chips">
                      <span v-for="(t, i) in it.extra" :key="i" class="chip">{{ t }}</span>
                    </div>
                  </div>
                </div>
              </el-popover>
            </div>
            <div class="lgrow app-muted">
              <div class="lg ov"><span class="sw ov" /> 排卵期</div>
              <div class="lg pe"><span class="sw pe" /> 预测经期</div>
              <div class="lg ovd"><span class="sw ovd" /> 排卵日</div>
              <div class="lg lu"><span class="sw lu" /> 黄体期</div>
            </div>
          </div>
      </div>

      <div class="side app-card">
        <div class="side-title">📅 预测结果</div>
        <div v-if="warmLine" class="warm app-muted">{{ warmLine }}</div>
        <div class="side-list">
          <div class="item">
            <div class="label app-muted">下次经期</div>
            <div class="value">{{ prediction?.nextPeriodStart }} ~ {{ prediction?.nextPeriodEnd }}</div>
          </div>
          <div class="divider" />
          <div class="item">
            <div class="label app-muted">排卵期</div>
            <div class="value">{{ prediction?.fertileStart }} ~ {{ prediction?.fertileEnd }}</div>
          </div>
          <div class="divider" />
          <div class="item">
            <div class="label app-muted">排卵日</div>
            <div class="value">{{ prediction?.ovulationDay }}</div>
          </div>
          <div class="divider" />
          <div class="item">
            <div class="label app-muted">提醒</div>
            <div class="right">
              <span class="switch-state app-muted">{{ reminderEnabled ? '已开启' : '已关闭' }}</span>
              <el-switch
                :model-value="reminderEnabled"
                :loading="reminderSwitching"
                active-color="rgba(139, 92, 246, 0.86)"
                inactive-color="rgba(148, 163, 184, 0.55)"
                @change="toggleReminder"
              />
            </div>
          </div>
        </div>
        <div class="meta app-muted">
          <span v-if="settings?.ownerNickname">由 {{ settings.ownerNickname }} 更新</span>
          <span v-if="settings?.updatedAt"> · {{ new Date(settings.updatedAt).toLocaleString() }}</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialog" title="周期设置" width="360px" align-center>
      <div class="f">
        <div class="fi">
          <div class="fl app-muted">周期天数（25-45）</div>
          <el-input v-model="form.cycleDays" inputmode="numeric" />
        </div>
        <div class="fi">
          <div class="fl app-muted">经期天数（3-7）</div>
          <el-input v-model="form.periodDays" inputmode="numeric" />
        </div>
        <div class="fi">
          <div class="fl app-muted">上次经期开始日期</div>
          <el-date-picker
            v-model="form.lastStartDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFutureDate"
            style="width: 100%"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSettings">保存</el-button>
      </template>
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
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(139, 92, 246, 0.16));
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}
.head-left {
  min-width: 0;
  display: grid;
  gap: 6px;
}
.h {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0.2px;
}
.sub {
  font-size: 12px;
  opacity: 0.85;
}
.subrow {
  display: flex;
  align-items: center;
  gap: 8px;
}
.phase-detail {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
}
.hint {
  margin-top: 6px;
}
.confirm-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.confirm-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all .2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.confirm-btn.started {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  color: #fff;
}
.confirm-btn.started:hover:not(:disabled) {
  opacity: 0.85;
  transform: scale(1.02);
}
.confirm-btn.delayed {
  background: rgba(148, 163, 184, 0.18);
  color: rgba(17, 24, 39, 0.72);
}
.confirm-btn.delayed:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.28);
  transform: scale(1.02);
}
.head-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}
.head-iconbtn {
  appearance: none;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.65);
  border-radius: 12px;
  height: 34px;
  width: 34px;
  display: grid;
  place-items: center;
  padding: 0;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}
.head-iconbtn:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.78);
}
.head-iconbtn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
  box-shadow: none;
}
.empty {
  padding: 18px 14px;
  border-radius: 16px;
  text-align: center;
  display: grid;
  justify-items: center;
  gap: 10px;
}
.empty-title {
  font-size: 14px;
  font-weight: 800;
}
.grid {
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  gap: 12px;
}
@media (max-width: 860px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
.cal,
.side {
  border-radius: 16px;
  padding: 14px 14px;
}
.cal-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cal-title {
  font-size: 14px;
  font-weight: 800;
}
.week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 12px;
  margin-top: 10px;
}
.grid7 {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-top: 10px;
}
.cell {
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.65);
  transition: transform 0.14s ease, box-shadow 0.14s ease, border-color 0.14s ease, background-color 0.14s ease;
  animation: dzFadeIn 0.28s ease both;
  animation-delay: calc(var(--i, 0) * 6ms);
}
.cell.empty {
  background: transparent;
  border-color: transparent;
  animation: none;
}
.day {
  font-size: 13px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.78);
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
}
.cell:hover:not(.empty) {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.12);
}
.cell.today {
  border-color: rgba(255, 255, 255, 0.9);
  background: rgba(59, 130, 246, 0.1);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.95), 0 12px 26px rgba(15, 23, 42, 0.14);
}
.cell.fertile {
  background: rgba(139, 92, 246, 0.14);
  border-color: rgba(139, 92, 246, 0.25);
}
.cell.period {
  background: rgba(139, 92, 246, 0.14);
  border-color: rgba(139, 92, 246, 0.25);
}
.cell.luteal {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.22);
}
.cell.ovu {
  box-shadow: 0 10px 22px rgba(139, 92, 246, 0.2);
  border-color: rgba(139, 92, 246, 0.45);
  animation: dzFadeIn 0.28s ease both, dzBreathe 1.8s ease-in-out infinite;
}
.cell.pstart {
  animation: dzFadeIn 0.28s ease both, dzBreathe 2.1s ease-in-out infinite;
}
.legend {
  margin-top: 12px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}
.lg {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-size: 11px;
  white-space: nowrap;
}
.lgrow {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 11px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.lg.ov {
  color: rgba(139, 92, 246, 0.92);
}
.lg.pe {
  color: rgba(139, 92, 246, 0.88);
}
.lg.ovd {
  color: rgba(99, 102, 241, 0.88);
}
.lg.lu {
  color: rgba(34, 197, 94, 0.9);
}
.sw {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  display: inline-block;
}
.sw.ov {
  background: rgba(139, 92, 246, 0.9);
}
.sw.pe {
  background: rgba(139, 92, 246, 0.85);
}
.sw.ovd {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.9), rgba(139, 92, 246, 0.85));
}
.sw.lu {
  background: rgba(34, 197, 94, 0.9);
}
.side-title {
  font-size: 14px;
  font-weight: 800;
}
.warm {
  margin-top: 6px;
  font-size: 12px;
}
.side-list {
  margin-top: 10px;
  display: grid;
  gap: 10px;
}
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.label {
  font-size: 12px;
  flex: 0 0 auto;
}
.value {
  font-weight: 900;
  color: rgba(17, 24, 39, 0.84);
  text-align: right;
  min-width: 0;
}
.divider {
  height: 1px;
  background: rgba(148, 163, 184, 0.25);
}
.right {
  display: inline-flex;
  gap: 10px;
  align-items: center;
}
.switch-state {
  font-size: 12px;
}
.meta {
  margin-top: 10px;
  font-size: 12px;
  opacity: 0.8;
}
.f {
  display: grid;
  gap: 10px;
}
.fi {
  display: grid;
  gap: 6px;
}
.fl {
  font-size: 12px;
}

.phase-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.phase-row.small {
  gap: 8px;
}
.phase-pill {
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}
.phase-pill:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.12);
  border-color: rgba(99, 102, 241, 0.22);
}
.phase-pill.active {
  border-color: rgba(99, 102, 241, 0.3);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12), rgba(139, 92, 246, 0.14));
}
.phase-row.small .phase-pill {
  padding: 6px 10px;
}
.phase-row.small .phase-name {
  font-size: 12px;
}
.phase-row.small .phase-tag {
  transform: scale(0.92);
  transform-origin: center;
}
.phase-name {
  font-weight: 900;
  color: rgba(17, 24, 39, 0.82);
}
.phase-pill.active .phase-name {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95), rgba(139, 92, 246, 0.9));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.phase-tag {
  font-size: 12px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.95);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.9), rgba(139, 92, 246, 0.85));
}
.pop {
  display: grid;
  gap: 10px;
}
.pop-title {
  font-weight: 900;
  font-size: 14px;
  color: rgba(17, 24, 39, 0.86);
}
.pop-range {
  font-size: 12px;
  margin-top: -6px;
}
.pop-sec {
  display: grid;
  gap: 6px;
}
.pop-k {
  font-size: 12px;
}
.pop-ul {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 6px;
  color: rgba(17, 24, 39, 0.8);
  font-size: 13px;
  line-height: 1.45;
}
.docs-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.78);
  border: 1px solid rgba(17, 24, 39, 0.08);
  background: rgba(255, 255, 255, 0.65);
}

.confirm-fade-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.confirm-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.confirm-fade-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}
.confirm-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.22s ease, opacity 0.22s ease;
}
.slide-left-enter-from {
  transform: translateX(10px);
  opacity: 0;
}
.slide-left-leave-to {
  transform: translateX(-10px);
  opacity: 0;
}
.slide-right-enter-from {
  transform: translateX(-10px);
  opacity: 0;
}
.slide-right-leave-to {
  transform: translateX(10px);
  opacity: 0;
}

@keyframes dzFadeIn {
  from {
    opacity: 0;
    transform: translateY(2px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes dzBreathe {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.0), 0 10px 22px rgba(139, 92, 246, 0.18);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(139, 92, 246, 0.12), 0 14px 28px rgba(139, 92, 246, 0.22);
  }
}

:deep(.dialog-fade-enter-active .el-dialog) {
  animation: dzDialogIn 0.18s ease both;
}
:deep(.dialog-fade-leave-active .el-dialog) {
  animation: dzDialogOut 0.14s ease both;
}
@keyframes dzDialogIn {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
@keyframes dzDialogOut {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(6px) scale(0.99);
  }
}
</style>

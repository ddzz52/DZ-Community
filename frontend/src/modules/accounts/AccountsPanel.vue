<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const partner = ref(null)

const myId = computed(() => {
  const v = auth.user?.id
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : null
})
const myName = computed(() => auth.user?.nickname || auth.user?.username || '我')
const partnerName = computed(() => partner.value?.nickname || partner.value?.username || '对方')
const authorLabel = (userId) => {
  const v = typeof userId === 'number' ? userId : Number(userId)
  if (myId.value != null && Number.isFinite(v) && v === myId.value) return myName.value
  return partnerName.value
}

const cacheKey = computed(() => {
  const uid = myId.value
  return uid ? `dz_accounts_cache_${uid}` : 'dz_accounts_cache'
})
const loadCache = () => {
  try {
    const raw = localStorage.getItem(cacheKey.value)
    if (!raw) return null
    const v = JSON.parse(raw)
    return v && typeof v === 'object' ? v : null
  } catch (e) {
    return null
  }
}
const saveCache = (v) => {
  try {
    localStorage.setItem(cacheKey.value, JSON.stringify(v || {}))
  } catch (e) {}
}

const now = new Date()
const cache = loadCache()
const mode = ref(cache?.mode === 'year' ? 'year' : 'month')
const monthValue = ref(cache?.month ? new Date(`${cache.month}-01T00:00:00`) : new Date(now.getFullYear(), now.getMonth(), 1))
const yearValue = ref(typeof cache?.year === 'number' ? cache.year : now.getFullYear())
const categoryFilter = ref(typeof cache?.category === 'string' ? cache.category : '')

watch([mode, monthValue, yearValue, categoryFilter], () => {
  const m = monthValue.value instanceof Date ? monthValue.value : new Date()
  const ym = `${m.getFullYear()}-${String(m.getMonth() + 1).padStart(2, '0')}`
  saveCache({
    mode: mode.value,
    month: ym,
    year: yearValue.value,
    category: categoryFilter.value || ''
  })
})

const loading = ref(false)
const statsLoading = ref(false)
const hasMore = ref(false)
const items = ref([])
const categories = ref([])
const budgetVal = ref('')
const budgetEditing = ref(false)

const budgetPercent = computed(() => {
  const total = Number(monthStats.value?.totalAmount || 0)
  const budget = Number(monthStats.value?.monthlyBudget || budgetVal.value || 0)
  if (!budget || budget <= 0) return 0
  return Math.min(100, Math.round((total / budget) * 100))
})

const fmtDateTime = (d) => {
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
const startOfMonth = (d) => new Date(d.getFullYear(), d.getMonth(), 1, 0, 0, 0)
const startOfNextMonth = (d) => new Date(d.getFullYear(), d.getMonth() + 1, 1, 0, 0, 0)
const startOfYear = (y) => new Date(y, 0, 1, 0, 0, 0)
const startOfNextYear = (y) => new Date(y + 1, 0, 1, 0, 0, 0)

const range = computed(() => {
  if (mode.value === 'year') {
    const y = yearValue.value
    return { from: startOfYear(y), to: startOfNextYear(y) }
  }
  const m = monthValue.value instanceof Date ? monthValue.value : new Date()
  return { from: startOfMonth(m), to: startOfNextMonth(m) }
})

const monthStats = ref(null)
const yearStats = ref(null)

const loadPartner = async () => {
  try {
    const p = await http.get('/api/profile')
    partner.value = p?.partner || null
  } catch (e) {
    partner.value = null
  }
}

const loadBudget = async () => {
  try {
    const res = await http.get('/api/couple/budget')
    if (res?.monthlyBudget != null) {
      budgetVal.value = String(res.monthlyBudget)
    }
  } catch (e) { /* ignore */ }
}

const saveBudget = async () => {
  try {
    const v = parseFloat(budgetVal.value)
    if (isNaN(v) || v <= 0) {
      ElMessage.error('请输入有效的预算金额')
      return
    }
    await http.put('/api/couple/budget', { monthlyBudget: v })
    budgetEditing.value = false
    ElMessage.success('预算已保存')
    await loadStats()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const loadCategories = async () => {
  try {
    categories.value = (await http.get('/api/accounts/categories', { params: { limit: 50 } })) || []
  } catch (e) {
    categories.value = []
  }
}

const loadStats = async () => {
  try {
    statsLoading.value = true
    if (mode.value === 'year') {
      yearStats.value = await http.get('/api/accounts/stats/year', { params: { year: yearValue.value } })
      monthStats.value = null
    } else {
      const m = monthValue.value instanceof Date ? monthValue.value : new Date()
      monthStats.value = await http.get('/api/accounts/stats/month', { params: { year: m.getFullYear(), month: m.getMonth() + 1 } })
      yearStats.value = null
    }
  } catch (e) {
    ElMessage.error(e?.message || '统计加载失败')
  } finally {
    statsLoading.value = false
  }
}

const buildListParams = (cursor) => {
  const r = range.value
  const params = {
    from: fmtDateTime(r.from),
    to: fmtDateTime(r.to),
    limit: 40
  }
  if (categoryFilter.value) params.category = categoryFilter.value
  if (cursor?.id) params.beforeId = cursor.id
  if (cursor?.occurredAt) params.beforeAt = fmtDateTime(new Date(cursor.occurredAt))
  return params
}

const loadList = async (silent) => {
  try {
    loading.value = true
    const list = (await http.get('/api/accounts', { params: buildListParams(null) })) || []
    items.value = Array.isArray(list) ? list : []
    hasMore.value = items.value.length >= 40
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  const cur = items.value?.[items.value.length - 1]
  if (!cur?.id) return
  try {
    loading.value = true
    const more = (await http.get('/api/accounts', { params: buildListParams(cur) })) || []
    const arr = Array.isArray(more) ? more : []
    if (!arr.length) {
      hasMore.value = false
      return
    }
    const seen = new Set(items.value.map((x) => x.id))
    const merged = items.value.slice()
    for (const it of arr) {
      if (!it?.id || seen.has(it.id)) continue
      merged.push(it)
      seen.add(it.id)
    }
    items.value = merged
    hasMore.value = arr.length >= 40
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const refreshAll = async (silent) => {
  await loadPartner()
  await loadCategories()
  await loadStats()
  await loadBudget()
  await loadList(silent)
}

watch([mode, monthValue, yearValue], async () => {
  await loadStats()
  await loadList(true)
})

watch(categoryFilter, async () => {
  await loadList(true)
})

onMounted(async () => {
  await refreshAll(true)
  window.addEventListener('dz_accounts_changed', onAccountsChanged)
})

const onAccountsChanged = () => {
  if (loading.value || statsLoading.value || saving.value) return
  if (syncTimer) window.clearTimeout(syncTimer)
  syncTimer = window.setTimeout(() => {
    refreshAll(true)
  }, 400)
}
let syncTimer = 0

onBeforeUnmount(() => {
  window.removeEventListener('dz_accounts_changed', onAccountsChanged)
  if (syncTimer) window.clearTimeout(syncTimer)
})

const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({
  category: '',
  amount: 0,
  occurredAt: new Date(),
  remark: ''
})

const resetForm = () => {
  editingId.value = null
  form.category = ''
  form.amount = 0
  form.occurredAt = new Date()
  form.remark = ''
}

const openAdd = () => {
  resetForm()
  dialogOpen.value = true
}

const openEdit = (it) => {
  if (!it?.id) return
  editingId.value = it.id
  form.category = it.category || ''
  form.amount = typeof it.amount === 'number' ? it.amount : Number(it.amount || 0)
  form.occurredAt = it.occurredAt ? new Date(it.occurredAt) : new Date()
  form.remark = it.remark || ''
  dialogOpen.value = true
}

const save = async () => {
  const payload = {
    category: String(form.category || '').trim(),
    amount: form.amount,
    occurredAt: fmtDateTime(form.occurredAt),
    remark: String(form.remark || '').trim()
  }
  if (!payload.category) {
    ElMessage.error('请选择分类')
    return
  }
  if (!Number.isFinite(Number(payload.amount)) || Number(payload.amount) < 0.01) {
    ElMessage.error('金额需≥0.01')
    return
  }
  try {
    saving.value = true
    if (editingId.value) {
      await http.put(`/api/accounts/${editingId.value}`, payload)
      ElMessage.success('已保存')
    } else {
      await http.post('/api/accounts', payload)
      ElMessage.success('已添加')
    }
    dialogOpen.value = false
    await refreshAll(true)
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const removeOne = async (it) => {
  if (!it?.id) return
  try {
    await ElMessageBox.confirm('确认删除这条记账吗？', '删除记账', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/api/accounts/${it.id}`)
    items.value = items.value.filter((x) => x.id !== it.id)
    await loadStats()
    await loadCategories()
    ElMessage.success('已删除')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message || '删除失败')
  }
}

const money = (v) => {
  const n = typeof v === 'number' ? v : Number(v || 0)
  if (!Number.isFinite(n)) return '0.00'
  return n.toFixed(2)
}

const dayKey = (d) => {
  const dt = d ? new Date(d) : null
  if (!dt || Number.isNaN(dt.getTime())) return ''
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
}
const timeText = (d) => {
  const dt = d ? new Date(d) : null
  if (!dt || Number.isNaN(dt.getTime())) return ''
  return `${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`
}

const grouped = computed(() => {
  const map = new Map()
  for (const it of items.value || []) {
    if (!it) continue
    const k = dayKey(it.occurredAt) || '未知日期'
    if (!map.has(k)) map.set(k, [])
    map.get(k).push(it)
  }
  return Array.from(map.entries())
})
</script>

<template>
  <el-card class="app-card" shadow="never">
    <template #header>
      <div class="row">
        <div class="h">共享记账</div>
        <div class="top-actions">
          <div class="mode">
            <button class="pill" :class="{ active: mode === 'month' }" type="button" @click="mode = 'month'">按月</button>
            <button class="pill" :class="{ active: mode === 'year' }" type="button" @click="mode = 'year'">按年</button>
          </div>
          <el-date-picker
            v-if="mode === 'month'"
            v-model="monthValue"
            type="month"
            size="small"
            format="YYYY-MM"
            value-format=""
          />
          <el-input-number v-else v-model="yearValue" size="small" :min="1970" :max="2100" :controls="false" class="year" />
          <el-select
            v-model="categoryFilter"
            size="small"
            clearable
            filterable
            allow-create
            default-first-option
            class="cat"
            placeholder="全部分类"
          >
            <el-option v-for="c in categories" :key="c.category" :label="c.category" :value="c.category" />
          </el-select>
          <el-button size="small" :loading="loading || statsLoading" @click="refreshAll()">
            <span>刷新</span>
          </el-button>
          <el-button size="small" @click="budgetEditing = !budgetEditing">
            <span>{{ budgetEditing ? '取消' : '预算' }}</span>
          </el-button>
          <el-button size="small" type="primary" @click="openAdd">
            <span>新增</span>
          </el-button>
        </div>
      </div>
    </template>

    <div v-if="budgetEditing" class="budget-editor app-card">
      <span class="budget-edit-label">月度预算</span>
      <el-input v-model="budgetVal" placeholder="例如 2000" size="small" style="width:140px;margin:0 10px" @keydown.enter="saveBudget" />
      <el-button size="small" type="primary" @click="saveBudget">保存</el-button>
    </div>

    <div class="stats">
      <div v-if="mode === 'month'" class="statbox">
        <div class="kpi">
          <div class="kpi-title">本月总支出</div>
          <div class="kpi-value">¥ {{ money(monthStats?.totalAmount) }}</div>
        </div>
        <div v-if="monthStats?.monthlyBudget || budgetVal" class="budget-bar-wrap">
          <div class="budget-bar-top">
            <span class="budget-label">预算 ¥{{ money(monthStats?.monthlyBudget || budgetVal) }}</span>
            <span v-if="monthStats?.budgetRemaining != null" class="budget-label" :class="{ over: monthStats?.overBudget }">
              {{ monthStats?.overBudget ? '已超支' : '剩余 ¥' + money(monthStats?.budgetRemaining) }}
            </span>
          </div>
          <div class="budget-track">
            <div class="budget-fill" :class="{ over: monthStats?.overBudget }"
              :style="{ width: budgetPercent + '%' }" />
          </div>
          <div v-if="monthStats?.overBudget" class="budget-alert">⚠️ 本月支出已超出预算，请注意控制消费哦</div>
        </div>
        <div class="mini">
          <div class="mini-title">按分类</div>
          <div v-if="!monthStats?.byCategory?.length" class="app-muted">暂无数据</div>
          <div v-else class="mini-list">
            <div v-for="it in monthStats.byCategory.slice(0, 6)" :key="it.category" class="mini-row">
              <div class="mini-k">{{ it.category }}</div>
              <div class="mini-v">¥ {{ money(it.amount) }}</div>
            </div>
          </div>
        </div>
        <div class="mini">
          <div class="mini-title">TA 与我</div>
          <div v-if="!monthStats?.byUser?.length" class="app-muted">暂无数据</div>
          <div v-else class="mini-list">
            <div v-for="it in monthStats.byUser" :key="it.userId" class="mini-row">
              <div class="mini-k">{{ authorLabel(it.userId) }}</div>
              <div class="mini-v">¥ {{ money(it.amount) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="statbox">
        <div class="kpi">
          <div class="kpi-title">全年总支出</div>
          <div class="kpi-value">¥ {{ money(yearStats?.totalAmount) }}</div>
        </div>
        <div class="mini wide">
          <div class="mini-title">按月份</div>
          <div v-if="!yearStats?.byMonth?.length" class="app-muted">暂无数据</div>
          <div v-else class="mini-grid">
            <div v-for="m in yearStats.byMonth" :key="m.month" class="mini-chip">
              <div class="mm">{{ m.month }}月</div>
              <div class="vv">¥ {{ money(m.amount) }}</div>
            </div>
          </div>
        </div>
        <div class="mini">
          <div class="mini-title">按分类</div>
          <div v-if="!yearStats?.byCategory?.length" class="app-muted">暂无数据</div>
          <div v-else class="mini-list">
            <div v-for="it in yearStats.byCategory.slice(0, 6)" :key="it.category" class="mini-row">
              <div class="mini-k">{{ it.category }}</div>
              <div class="mini-v">¥ {{ money(it.amount) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="list">
      <div v-if="!items.length" class="empty app-muted">
        <el-empty description="本期还没有记账" />
      </div>
      <div v-else>
        <div v-for="[day, arr] in grouped" :key="day" class="day">
          <div class="dayhead">
            <div class="daytitle">{{ day }}</div>
            <div class="daysum">共 {{ arr.length }} 笔</div>
          </div>
          <div class="daylist">
            <div v-for="it in arr" :key="it.id" class="item">
              <div class="left">
                <div class="catname">{{ it.category }}</div>
                <div class="meta app-muted">
                  <span>{{ authorLabel(it.userId) }}</span>
                  <span>·</span>
                  <span>{{ timeText(it.occurredAt) }}</span>
                  <span v-if="it.remark">· {{ it.remark }}</span>
                </div>
              </div>
              <div class="right">
                <div class="amt">¥ {{ money(it.amount) }}</div>
                <div class="ops">
                  <button class="link" type="button" @click="openEdit(it)">编辑</button>
                  <span class="dot">·</span>
                  <button class="link danger" type="button" @click="removeOne(it)">删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="more">
          <el-button v-if="hasMore" size="small" :loading="loading" @click="loadMore">加载更多</el-button>
          <div v-else class="app-muted">没有更多了</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑记账' : '新增记账'" width="520px" align-center>
      <el-form label-width="86px">
        <el-form-item label="分类">
          <el-select v-model="form.category" filterable allow-create default-first-option class="full" placeholder="如：餐饮/交通/礼物">
            <el-option v-for="c in categories" :key="c.category" :label="c.category" :value="c.category" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1" class="full" controls-position="right" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="form.occurredAt" type="datetime" class="full" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="200" show-word-limit placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editingId ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.top-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
.mode {
  display: flex;
  gap: 6px;
  padding: 2px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.pill {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: rgba(17, 24, 39, 0.7);
  cursor: pointer;
}
.pill.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.95), rgba(139, 92, 246, 0.92));
  color: #fff;
}
.year {
  width: 92px;
}
.cat {
  width: 160px;
}
.budget-editor {
  display: flex; align-items: center; padding: 8px 12px; margin-bottom: 10px;
}
.budget-edit-label { font-size:13px; font-weight:700; white-space:nowrap; }
.budget-bar-wrap { margin-top: 10px; }
.budget-bar-top { display:flex; justify-content:space-between; margin-bottom:4px; }
.budget-label { font-size:12px; font-weight:700; color:var(--app-muted); }
.budget-label.over { color:#e11d48; }
.budget-track { height:6px; border-radius:3px; background:rgba(99,102,241,0.1); overflow:hidden; }
.budget-fill { height:100%; border-radius:3px; background:linear-gradient(90deg, #6366f1, #818cf8); transition:width .4s; }
.budget-fill.over { background:linear-gradient(90deg, #e11d48, #f43f5e); }
.budget-alert { margin-top:6px; font-size:12px; font-weight:700; color:#e11d48; }
.stats {
  margin-bottom: 14px;
}
.statbox {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 12px;
}
.kpi {
  padding: 12px 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.14), rgba(139, 92, 246, 0.12));
  border: 1px solid rgba(99, 102, 241, 0.18);
}
.kpi-title {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.65);
}
.kpi-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.3px;
  color: rgba(17, 24, 39, 0.92);
}
.mini {
  padding: 12px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.mini.wide {
  grid-column: span 2;
}
.mini-title {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.65);
  margin-bottom: 8px;
}
.mini-list {
  display: grid;
  gap: 6px;
}
.mini-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.mini-k {
  font-size: 13px;
  color: rgba(17, 24, 39, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mini-v {
  font-size: 13px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.92);
}
.mini-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.mini-chip {
  padding: 8px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.06));
  border: 1px solid rgba(139, 92, 246, 0.12);
}
.mm {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.65);
}
.vv {
  margin-top: 2px;
  font-size: 13px;
  font-weight: 750;
  color: rgba(17, 24, 39, 0.92);
}
.day {
  padding: 10px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
.day:first-child {
  border-top: 0;
  padding-top: 0;
}
.dayhead {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.daytitle {
  font-weight: 800;
  color: rgba(17, 24, 39, 0.9);
}
.daysum {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.55);
}
.daylist {
  display: grid;
  gap: 10px;
}
.item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.catname {
  font-weight: 800;
  color: rgba(17, 24, 39, 0.92);
}
.meta {
  margin-top: 3px;
  font-size: 12px;
}
.right {
  text-align: right;
  flex: 0 0 auto;
}
.amt {
  font-weight: 900;
  color: rgba(99, 102, 241, 0.92);
}
.ops {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(17, 24, 39, 0.5);
}
.link {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 0;
  font-size: 12px;
  cursor: pointer;
  color: rgba(59, 130, 246, 0.92);
}
.link.danger {
  color: rgba(239, 68, 68, 0.92);
}
.dot {
  padding: 0 6px;
}
.more {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
.full {
  width: 100%;
}
@media (max-width: 920px) {
  .statbox {
    grid-template-columns: 1fr;
  }
  .mini.wide {
    grid-column: span 1;
  }
  .mini-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .cat {
    width: 100%;
  }
}
</style>

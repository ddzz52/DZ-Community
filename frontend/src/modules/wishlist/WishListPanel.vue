<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Check, Close, Delete, Edit, Flag, Plus, RefreshRight } from '@element-plus/icons-vue'
import http from '../../api/http'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const list = ref([])
const tab = ref('pending')

const dialog = ref(false)
const saving = ref(false)
const editingId = ref(null)
const form = reactive({ content: '', expectedAt: '', priority: 1, remark: '' })

const todayStr = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const disablePastDate = (d) => {
  const t = new Date()
  t.setHours(0, 0, 0, 0)
  return d.getTime() < t.getTime()
}

const load = async (silent) => {
  try {
    loading.value = true
    list.value = await http.get('/api/wish-list')
    if (!silent) ElMessage.success({ message: '已刷新', customClass: 'dz-soft-success' })
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
    tryOpenFromQuery()
  }
}

onMounted(() => load(true))

const pending = computed(() => (Array.isArray(list.value) ? list.value.filter((x) => (x?.status ?? 0) === 0) : []))
const done = computed(() => (Array.isArray(list.value) ? list.value.filter((x) => (x?.status ?? 0) === 1) : []))
const canceled = computed(() => (Array.isArray(list.value) ? list.value.filter((x) => (x?.status ?? 0) === 2) : []))

const priorityText = (p) => (p === 0 ? '高' : p === 2 ? '低' : '中')
const priorityTone = (p) => (p === 0 ? 'high' : p === 2 ? 'low' : 'mid')
const hasScratchCard = (it) => it?.sourceType === 'SCRATCH_CARD' && !!it?.sourceId
const fromScratch = (it) => it?.sourceType === 'SCRATCH_FROM'

const openCreate = () => {
  editingId.value = null
  form.content = ''
  form.expectedAt = todayStr()
  form.priority = 1
  form.remark = ''
  dialog.value = true
}

const openEdit = (it) => {
  editingId.value = it.id
  form.content = it.content || ''
  form.expectedAt = it.expectedAt || todayStr()
  form.priority = it.priority ?? 1
  form.remark = it.remark || ''
  dialog.value = true
}

const submit = async () => {
  const content = String(form.content || '').trim()
  if (!content) {
    ElMessage.error('请输入心愿内容')
    return
  }
  if (content.length > 100) {
    ElMessage.error('心愿内容长度需≤100字')
    return
  }
  const expectedAt = String(form.expectedAt || '').trim()
  if (!expectedAt) {
    ElMessage.error('请选择期望完成时间')
    return
  }
  try {
    saving.value = true
    if (editingId.value) {
      const updated = await http.put(`/api/wish-list/${editingId.value}`, {
        content,
        expectedAt,
        priority: form.priority,
        remark: String(form.remark || '').trim()
      })
      list.value = (Array.isArray(list.value) ? list.value : []).map((x) => (x?.id === editingId.value ? { ...x, ...updated } : x))
      ElMessage.success({ message: '已保存', customClass: 'dz-soft-success' })
    } else {
      const created = await http.post('/api/wish-list', {
        content,
        expectedAt,
        priority: form.priority,
        remark: String(form.remark || '').trim()
      })
      list.value = [created, ...(Array.isArray(list.value) ? list.value : [])]
      ElMessage.success({ message: '已新增', customClass: 'dz-soft-success' })
    }
    dialog.value = false
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const changeStatus = async (it, status) => {
  if (!it?.id) return
  try {
    const updated = await http.post(`/api/wish-list/${it.id}/status`, { status })
    list.value = (Array.isArray(list.value) ? list.value : []).map((x) => (x?.id === it.id ? { ...x, ...updated } : x))
    ElMessage.success({ message: status === 1 ? '已完成' : status === 2 ? '已取消' : '已恢复', customClass: 'dz-soft-success' })
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
    await load(true)
  }
}

const remove = async (it) => {
  if (!it?.id) return
  try {
    await ElMessageBox.confirm('确定删除这条心愿吗？（仅待完成可删除）', '删除心愿', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await http.delete(`/api/wish-list/${it.id}`)
    list.value = (Array.isArray(list.value) ? list.value : []).filter((x) => x?.id !== it.id)
    ElMessage.success({ message: '已删除', customClass: 'dz-soft-success' })
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
    await load(true)
  }
}

const createScratch = async (it) => {
  if (!it?.id) return
  if ((it.status ?? 0) !== 0) return
  try {
    const card = await http.post(`/api/wish-list/${it.id}/scratch`)
    list.value = (Array.isArray(list.value) ? list.value : []).map((x) =>
      x?.id === it.id ? { ...x, sourceType: 'SCRATCH_CARD', sourceId: card?.id || x?.sourceId } : x
    )
    ElMessage.success({ message: '已生成刮刮卡', customClass: 'dz-soft-success' })
  } catch (e) {
    ElMessage.error(e?.message || '生成失败')
    await load(true)
  }
}

const detailDialog = ref(false)
const detailItem = ref(null)
const openDetail = (it) => {
  if (!it) return
  detailItem.value = it
  detailDialog.value = true
}

const viewRecord = (it) => {
  if ((it?.status ?? 0) !== 1) return
  openDetail(it)
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
  const it = done.value.find((x) => x?.id === id) || list.value.find((x) => x?.id === id)
  if (!it) return
  const key = `wishdone_${id}`
  if (lastOpenKey.value === key) return
  lastOpenKey.value = key
  pendingOpen.value = null
  tab.value = (it?.status ?? 0) === 1 ? 'done' : 'pending'
  openDetail(it)
  clearOpenQuery()
}

watch(
  () => [route.query.open, route.query.id],
  () => {
    const open = String(route.query.open || '')
    const id = Number(route.query.id)
    if (open === 'wishdone' && Number.isFinite(id)) {
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
</script>

<template>
  <div class="wrap">
    <div class="head app-card">
      <div class="left">
        <div class="h">心愿清单</div>
        <div class="sub app-muted">把小心愿列出来，慢慢实现。</div>
      </div>
      <div class="actions">
        <el-tooltip content="刷新" placement="bottom">
          <button class="iconbtn" type="button" :disabled="loading" @click="load()">
            <el-icon :size="18"><RefreshRight /></el-icon>
          </button>
        </el-tooltip>
        <el-button class="primary" type="primary" round @click="openCreate">
          <el-icon :size="16"><Plus /></el-icon>
          <span>新增</span>
        </el-button>
      </div>
    </div>

    <div class="tabs">
      <button class="tab" :class="{ active: tab === 'pending' }" type="button" @click="tab = 'pending'">待完成</button>
      <button class="tab" :class="{ active: tab === 'done' }" type="button" @click="tab = 'done'">已完成</button>
      <button class="tab" :class="{ active: tab === 'canceled' }" type="button" @click="tab = 'canceled'">已取消</button>
    </div>

    <div v-if="tab === 'pending'" class="list">
      <div v-if="!pending.length" class="empty app-card app-muted">还没有待完成心愿</div>
      <div v-else class="cards">
        <div v-for="it in pending" :key="it.id" class="card app-card">
          <div class="meta">
            <span class="pbadge" :class="priorityTone(it.priority)">
              <el-icon :size="14"><Flag /></el-icon>
              <span>{{ priorityText(it.priority) }}</span>
            </span>
            <span class="date app-muted">
              <el-icon :size="14"><Calendar /></el-icon>
              <span>{{ it.expectedAt }}</span>
            </span>
            <span v-if="hasScratchCard(it)" class="tag">已生成刮刮卡</span>
            <span v-else-if="fromScratch(it)" class="tag soft">来自刮刮乐</span>
          </div>
          <div class="content">{{ it.content }}</div>
          <div v-if="it.remark" class="remark app-muted">{{ it.remark }}</div>
          <div class="row">
            <div class="app-muted">{{ it.updatedByNickname ? `最近：${it.updatedByNickname}` : '' }}</div>
            <div class="btns">
              <el-tooltip content="编辑（仅待完成可改）" placement="bottom">
                <button class="mini" type="button" @click="openEdit(it)"><el-icon><Edit /></el-icon></button>
              </el-tooltip>
              <el-tooltip :content="hasScratchCard(it) ? '已生成刮刮卡' : '生成刮刮卡'" placement="bottom">
                <button class="mini" type="button" :disabled="hasScratchCard(it)" @click="createScratch(it)">
                  <span class="mini-txt">刮</span>
                </button>
              </el-tooltip>
              <el-tooltip content="完成" placement="bottom">
                <button class="mini ok" type="button" @click="changeStatus(it, 1)"><el-icon><Check /></el-icon></button>
              </el-tooltip>
              <el-tooltip content="取消" placement="bottom">
                <button class="mini" type="button" @click="changeStatus(it, 2)"><el-icon><Close /></el-icon></button>
              </el-tooltip>
              <el-tooltip content="删除（仅待完成可删）" placement="bottom">
                <button class="mini danger" type="button" @click="remove(it)"><el-icon><Delete /></el-icon></button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="tab === 'done'" class="list">
      <div v-if="!done.length" class="empty app-card app-muted">还没有已完成心愿</div>
      <div v-else class="cards">
        <div v-for="it in done" :key="it.id" class="card app-card done" @click="viewRecord(it)">
          <div class="meta">
            <span class="pbadge" :class="priorityTone(it.priority)">
              <el-icon :size="14"><Flag /></el-icon>
              <span>{{ priorityText(it.priority) }}</span>
            </span>
            <span class="date app-muted">
              <el-icon :size="14"><Calendar /></el-icon>
              <span>{{ it.expectedAt }}</span>
            </span>
          </div>
          <div class="content">{{ it.content }}</div>
          <div v-if="it.remark" class="remark app-muted">{{ it.remark }}</div>
          <div class="row">
            <div class="app-muted">{{ it.completedAt ? `完成于：${new Date(it.completedAt).toLocaleString()}` : '' }}</div>
            <div class="btns">
              <el-tooltip content="查看完成记录" placement="bottom">
                <button class="mini ok" type="button" @click.stop="viewRecord(it)"><el-icon><Check /></el-icon></button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="list">
      <div v-if="!canceled.length" class="empty app-card app-muted">还没有已取消心愿</div>
      <div v-else class="cards">
        <div v-for="it in canceled" :key="it.id" class="card app-card canceled">
          <div class="meta">
            <span class="pbadge" :class="priorityTone(it.priority)">
              <el-icon :size="14"><Flag /></el-icon>
              <span>{{ priorityText(it.priority) }}</span>
            </span>
            <span class="date app-muted">
              <el-icon :size="14"><Calendar /></el-icon>
              <span>{{ it.expectedAt }}</span>
            </span>
          </div>
          <div class="content">{{ it.content }}</div>
          <div v-if="it.remark" class="remark app-muted">{{ it.remark }}</div>
          <div class="row">
            <div class="app-muted">{{ it.canceledAt ? `取消于：${new Date(it.canceledAt).toLocaleString()}` : '' }}</div>
            <div class="btns">
              <el-tooltip content="恢复为待完成" placement="bottom">
                <button class="mini ok" type="button" @click="changeStatus(it, 0)"><el-icon><RefreshRight /></el-icon></button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialog" :title="editingId ? '编辑心愿' : '新增心愿'" width="380px" align-center>
      <el-form label-position="top">
        <el-form-item label="心愿内容（≤100字）">
          <el-input v-model="form.content" type="textarea" maxlength="100" show-word-limit :autosize="{ minRows: 3, maxRows: 5 }" />
        </el-form-item>
        <el-form-item label="期望完成时间">
          <el-date-picker
            v-model="form.expectedAt"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <div class="modepick">
            <button class="pill" :class="{ active: form.priority === 0 }" type="button" @click="form.priority = 0">高</button>
            <button class="pill" :class="{ active: form.priority === 1 }" type="button" @click="form.priority = 1">中</button>
            <button class="pill" :class="{ active: form.priority === 2 }" type="button" @click="form.priority = 2">低</button>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" maxlength="200" show-word-limit :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="完成记录" width="360px" align-center>
      <div v-if="detailItem" class="dview">
        <div class="meta">
          <span class="pbadge" :class="priorityTone(detailItem.priority)">
            <el-icon :size="14"><Flag /></el-icon>
            <span>{{ priorityText(detailItem.priority) }}</span>
          </span>
          <span class="date app-muted">
            <el-icon :size="14"><Calendar /></el-icon>
            <span>{{ detailItem.expectedAt }}</span>
          </span>
        </div>
        <div class="content">{{ detailItem.content }}</div>
        <div v-if="detailItem.remark" class="remark app-muted">{{ detailItem.remark }}</div>
        <div class="row">
          <div class="app-muted">{{ detailItem.completedAt ? `完成于：${new Date(detailItem.completedAt).toLocaleString()}` : '' }}</div>
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
@media (hover: hover) {
  .card:hover {
    transform: translateY(-2px);
    border-color: rgba(99, 102, 241, 0.26);
    box-shadow: 0 18px 38px rgba(99, 102, 241, 0.12);
  }
}
.card.done {
  cursor: pointer;
}
.card.canceled {
  opacity: 0.82;
}
.meta {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}
.pbadge {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-weight: 900;
  font-size: 12px;
}
.pbadge.high {
  background: rgba(249, 115, 22, 0.12);
  border: 1px solid rgba(249, 115, 22, 0.22);
  color: rgba(194, 65, 12, 0.92);
}
.pbadge.mid {
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: rgba(79, 70, 229, 0.92);
}
.pbadge.low {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: rgba(5, 150, 105, 0.92);
}
.tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid rgba(99, 102, 241, 0.22);
  background: rgba(139, 92, 246, 0.1);
  color: rgba(99, 102, 241, 0.85);
  font-weight: 900;
  font-size: 12px;
}
.tag.soft {
  border-color: rgba(99, 102, 241, 0.2);
  background: rgba(99, 102, 241, 0.08);
  color: rgba(79, 70, 229, 0.85);
}
.date {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-size: 12px;
}
.content {
  font-size: 16px;
  font-weight: 950;
  line-height: 1.4;
  color: rgba(17, 24, 39, 0.9);
  word-break: break-word;
}
.remark {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.4;
}
.dview {
  display: grid;
  gap: 10px;
}
.row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.btns {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}
.mini {
  appearance: none;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.62);
  border-radius: 999px;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
}
.mini:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}
.mini-txt {
  font-weight: 900;
  color: rgba(17, 24, 39, 0.7);
}
.mini:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.28);
  background: rgba(99, 102, 241, 0.08);
}
.mini.ok {
  border-color: rgba(16, 185, 129, 0.24);
  background: rgba(16, 185, 129, 0.1);
}
.mini.danger {
  border-color: rgba(239, 68, 68, 0.22);
  background: rgba(239, 68, 68, 0.08);
}
.empty {
  padding: 18px 14px;
  border-radius: 16px;
  text-align: center;
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
</style>

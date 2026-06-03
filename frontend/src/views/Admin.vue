<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const users = ref([])
const stats = ref(null)
const announcements = ref([])
const loading = ref(false)
const tab = ref('users') // 'users' | 'announcements'

const fetchAll = async () => {
  try {
    loading.value = true
    const [u, s, a] = await Promise.all([
      http.get('/api/admin/users'),
      http.get('/api/admin/stats'),
      http.get('/api/admin/announcements')
    ])
    users.value = u; stats.value = s; announcements.value = a
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
    if (e?.code === 40300) router.replace('/app/me')
  } finally { loading.value = false }
}

// ====== 用户管理 ======
const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${user.nickname || user.username}」（ID: ${user.id}）吗？此操作不可撤销。`,
      '删除确认', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await http.delete(`/api/admin/users/${user.id}`)
    ElMessage.success('已删除')
    await fetchAll()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '删除失败')
  }
}

const toggleRole = async (user) => {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await http.put(`/api/admin/users/${user.id}/role`, { role: newRole })
    ElMessage.success(`角色已更新为 ${newRole}`)
    await fetchAll()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

// 修改用户名
const editUsername = async (user) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新用户名（1-32位）', '修改登录名', {
      inputValue: user.username,
      inputValidator: (v) => v && v.trim().length > 0 && v.trim().length <= 32 ? true : '用户名需1-32位',
      confirmButtonText: '确认修改', cancelButtonText: '取消'
    })
    await http.put(`/api/admin/users/${user.id}/username`, { username: value.trim() })
    ElMessage.success('用户名已修改')
    await fetchAll()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '修改失败')
  }
}

// 强制密码重置
const forceReset = async (user) => {
  try {
    await ElMessageBox.confirm(
      `将为用户「${user.nickname || user.username}」生成6位重置验证码，确认继续？`,
      '强制密码重置', { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    const result = await http.post(`/api/admin/users/${user.id}/force-reset`)
    ElMessageBox.alert(
      `用户名: ${result.username}\n\n重置验证码: ${result.resetCode}\n\n有效期: 24小时\n\n告知用户在登录页「忘记密码」中使用此验证码重置密码。`,
      '重置验证码', { confirmButtonText: '已复制', type: 'success' }
    )
    navigator.clipboard?.writeText(result.resetCode).catch(() => {})
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.message || '操作失败')
  }
}

// ====== 公告管理 ======
const newAnnContent = ref('')
const createAnnouncement = async () => {
  if (!newAnnContent.value.trim()) { ElMessage.warning('请输入公告内容'); return }
  try {
    await http.post('/api/admin/announcements', { content: newAnnContent.value.trim() })
    ElMessage.success('公告已发布')
    newAnnContent.value = ''
    await fetchAll()
  } catch (e) { ElMessage.error(e?.message || '发布失败') }
}

const toggleAnnouncement = async (a) => {
  try {
    await http.put(`/api/admin/announcements/${a.id}/toggle`, { active: a.active !== 1 })
    await fetchAll()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
}

const deleteAnnouncement = async (a) => {
  try {
    await http.delete(`/api/admin/announcements/${a.id}`)
    ElMessage.success('已删除')
    await fetchAll()
  } catch (e) { ElMessage.error(e?.message || '删除失败') }
}

const coupleCount = computed(() => {
  if (!users.value.length) return 0
  return new Set(users.value.map(u => u.coupleId)).size
})

const genderText = (g) => {
  if (g === 1) return '男'; if (g === 2) return '女'; if (g === 0) return '其他'; return '-'
}
const formatDate = (d) => d ? new Date(d).toLocaleDateString() : '-'

onMounted(() => {
  if (auth.user?.role !== 'ADMIN') { ElMessage.error('仅管理员可访问'); router.replace('/app/me'); return }
  fetchAll()
})
</script>

<template>
  <div class="stack">
    <div class="top app-card">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <div class="title">系统管理</div>
      <el-button size="small" :loading="loading" @click="fetchAll">刷新</el-button>
    </div>

    <!-- ====== 统计卡片 ====== -->
    <div class="stat-row">
      <div class="stat-card app-card" style="--accent: #6366f1"><div class="stat-icon">👥</div><div class="stat-meta"><div class="stat-value">{{ stats?.totalUsers ?? '-' }}</div><div class="stat-label">总用户</div></div></div>
      <div class="stat-card app-card" style="--accent: #ec4899"><div class="stat-icon">💕</div><div class="stat-meta"><div class="stat-value">{{ stats?.totalCouples ?? coupleCount }}</div><div class="stat-label">情侣空间</div></div></div>
      <div class="stat-card app-card" style="--accent: #f59e0b"><div class="stat-icon">🛡️</div><div class="stat-meta"><div class="stat-value">{{ stats?.adminCount ?? '-' }}</div><div class="stat-label">管理员</div></div></div>
    </div>

    <!-- ====== Tab 切换 ====== -->
    <div class="tab-bar">
      <button :class="{ active: tab === 'users' }" @click="tab = 'users'">👥 用户管理</button>
      <button :class="{ active: tab === 'announcements' }" @click="tab = 'announcements'">📢 系统公告</button>
    </div>

    <!-- ====== 用户管理 Tab ====== -->
    <template v-if="tab === 'users'">
      <div class="section-header">
        <span class="section-title">注册用户</span>
        <span class="section-count">{{ users.length }} 人</span>
      </div>

      <div v-for="u in users" :key="u.id" class="user-card app-card">
        <div class="urow">
          <div class="uavatar">{{ (u.nickname || u.username || '?').slice(0, 1).toUpperCase() }}</div>
          <div class="umeta">
            <div class="uname">
              <span>{{ u.nickname || '-' }}</span>
              <el-tag v-if="u.role === 'ADMIN'" effect="light" type="danger" round size="small">管理员</el-tag>
              <el-tag v-else effect="light" round size="small">普通</el-tag>
            </div>
            <div class="usub"><span>ID: {{ u.id }}</span><span>· {{ u.username }}</span><span>· {{ genderText(u.gender) }}</span></div>
            <div class="usub"><span>空间 #{{ u.coupleId }}</span><span>· 相恋日: {{ formatDate(u.loveDate) }}</span><span v-if="u.zodiac">· {{ u.zodiac }}</span></div>
          </div>
          <div class="uactions">
            <el-button size="small" @click="editUsername(u)">改名</el-button>
            <el-button size="small" @click="toggleRole(u)">{{ u.role === 'ADMIN' ? '降级' : '升级' }}</el-button>
            <el-button size="small" type="warning" plain @click="forceReset(u)">重置密码</el-button>
            <el-button size="small" type="danger" plain @click="deleteUser(u)">删除</el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- ====== 公告管理 Tab ====== -->
    <template v-if="tab === 'announcements'">
      <div class="ann-form app-card">
        <textarea v-model="newAnnContent" class="ann-input" placeholder="输入公告内容（≤500字），登录页展示..." maxlength="500" rows="3" />
        <el-button type="primary" size="small" @click="createAnnouncement">发布公告</el-button>
      </div>

      <div v-for="a in announcements" :key="a.id" class="ann-card app-card" :class="{ off: a.active !== 1 }">
        <div class="ann-content">{{ a.content }}</div>
        <div class="ann-meta">
          <span>{{ formatDate(a.createdAt) }}</span>
          <el-tag v-if="a.active === 1" effect="light" type="success" round size="small">生效中</el-tag>
          <el-tag v-else effect="light" type="info" round size="small">已停用</el-tag>
        </div>
        <div class="ann-actions">
          <el-button size="small" @click="toggleAnnouncement(a)">{{ a.active === 1 ? '停用' : '启用' }}</el-button>
          <el-button size="small" type="danger" plain @click="deleteAnnouncement(a)">删除</el-button>
        </div>
      </div>

      <div v-if="announcements.length === 0" class="empty app-card"><div class="etitle">暂无公告</div></div>
    </template>
  </div>
</template>

<style scoped>
.stack { display: flex; flex-direction: column; gap: 14px; }
.top { padding: 12px; display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.title { font-weight: 900; }

/* ====== 统计 ====== */
.stat-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.stat-card { --accent: #6366f1; padding: 18px 16px; display: flex; align-items: center; gap: 14px; transition: transform 0.2s ease; }
.stat-card:hover { transform: translateY(-2px); }
.stat-icon { width: 46px; height: 46px; border-radius: 15px; display: grid; place-items: center; font-size: 20px; background: color-mix(in srgb, var(--accent) 10%, transparent); border: 1px solid color-mix(in srgb, var(--accent) 16%, transparent); flex-shrink: 0; }
.stat-value { font-size: 26px; font-weight: 800; color: var(--accent); line-height: 1.1; }
.stat-label { margin-top: 2px; font-size: 12px; color: var(--app-muted); }

/* ====== Tab ====== */
.tab-bar { display: flex; gap: 8px; }
.tab-bar button { border: 1px solid rgba(17,24,39,0.08); background: rgba(255,255,255,0.6); border-radius: 10px; padding: 8px 16px; font-size: 13px; font-weight: 600; font-family: inherit; cursor: pointer; transition: all 0.2s; color: var(--app-muted); }
.tab-bar button.active { background: #6366f1; color: #fff; border-color: #6366f1; }

.section-header { display: flex; align-items: baseline; justify-content: space-between; padding: 4px 4px 0; }
.section-title { font-weight: 700; font-size: 15px; }
.section-count { font-size: 13px; color: var(--app-muted); }

/* ====== 用户卡片 ====== */
.user-card { padding: 14px 16px; }
.urow { display: flex; align-items: center; gap: 12px; }
.uavatar { width: 44px; height: 44px; border-radius: 16px; display: grid; place-items: center; font-weight: 800; background: linear-gradient(135deg, rgba(99,102,241,0.18), rgba(139,92,246,0.16)); color: rgba(88,28,135,0.92); border: 1px solid rgba(255,255,255,0.7); flex-shrink: 0; }
.umeta { flex: 1; min-width: 0; }
.uname { display: inline-flex; align-items: center; gap: 8px; font-weight: 600; }
.usub { margin-top: 3px; font-size: 12px; color: var(--app-muted); display: flex; flex-wrap: wrap; gap: 4px; }
.uactions { display: inline-flex; flex-direction: column; gap: 6px; flex-shrink: 0; }

/* ====== 公告 ====== */
.ann-form { padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.ann-input { width: 100%; border: 1px solid rgba(17,24,39,0.1); border-radius: 10px; padding: 10px 14px; font-size: 14px; font-family: inherit; resize: vertical; outline: none; background: rgba(255,255,255,0.6); }
.ann-input:focus { border-color: rgba(99,102,241,0.4); box-shadow: 0 0 0 3px rgba(99,102,241,0.06); }
.ann-card { padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.ann-card.off { opacity: 0.55; }
.ann-content { font-size: 14px; line-height: 1.6; }
.ann-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--app-muted); }
.ann-actions { display: flex; gap: 8px; }

.empty { padding: 16px; text-align: center; }
.etitle { font-weight: 900; }

@media (max-width: 540px) {
  .stat-row { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .stat-card { padding: 14px 12px; gap: 10px; }
  .stat-icon { width: 38px; height: 38px; font-size: 16px; }
  .stat-value { font-size: 22px; }
  .urow { flex-wrap: wrap; }
  .uactions { flex-direction: row; width: 100%; justify-content: flex-end; flex-wrap: wrap; }
}
</style>

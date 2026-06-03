<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { ArrowLeft, RefreshRight, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const partner = ref(null)
const partnerOnline = ref(false)

const initialFor = (u) => {
  const n = u?.nickname || u?.username || ''
  return n ? n.trim().slice(0, 1).toUpperCase() : 'U'
}

const genderText = computed(() => {
  const g = partner.value?.gender
  if (g === 1) return '男'
  if (g === 2) return '女'
  if (g === 0) return '其他/保密'
  return '-'
})

const loveDateText = computed(() => {
  const d = partner.value?.loveDate
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
})

const refresh = async (silent) => {
  try {
    loading.value = true
    const data = await http.get('/api/profile')
    partner.value = data?.partner || null
    partnerOnline.value = !!data?.partnerOnline
    if (!silent) ElMessage.success('已刷新')
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '刷新失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => refresh(true))
</script>

<template>
  <div class="stack">
    <div class="top app-card">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <div class="title">对方资料</div>
      <el-button size="small" :loading="loading" @click="refresh()">
        <el-icon :size="16"><RefreshRight /></el-icon>
        <span>刷新</span>
      </el-button>
    </div>

    <div v-if="partner" class="profile app-card">
      <div class="row">
        <div class="avatar">
          <span>{{ initialFor(partner) }}</span>
          <span class="online-dot" :class="{ on: partnerOnline }" />
        </div>
        <div class="meta">
          <div class="name">{{ partner.nickname || '-' }}</div>
          <div class="sub app-muted">
            <span>{{ partner.username || '-' }}</span>
            <span class="status-text" :class="{ on: partnerOnline }">{{ partnerOnline ? '在线' : '离线' }}</span>
          </div>
        </div>
      </div>

      <div class="mini app-muted">
        <div class="mini-item">
          <el-icon :size="16"><UserFilled /></el-icon>
          <span>ID：{{ partner.id || '-' }}</span>
        </div>
        <div class="mini-item">
          <span>性别：{{ genderText }}</span>
        </div>
        <div class="mini-item">
          <span>相恋日期：{{ loveDateText }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty app-card">
      <div class="etitle">还没有绑定第二个账号</div>
      <div class="app-muted">绑定后这里会显示对方的基础资料。</div>
    </div>
  </div>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.top {
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.title {
  font-weight: 900;
}
.profile {
  padding: 16px;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
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
  position: relative;
}

/* 在线状态圆点 */
.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #9ca3af;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.1);
  transition: background 0.35s ease, box-shadow 0.35s ease;
}
.online-dot.on {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18), 0 1px 3px rgba(17, 24, 39, 0.1);
  animation: dotPulse 2.5s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18), 0 1px 3px rgba(17, 24, 39, 0.1); }
  50% { box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.08), 0 1px 3px rgba(17, 24, 39, 0.1); }
}

.status-text {
  font-size: 12px;
  font-weight: 600;
  margin-left: 8px;
  color: #9ca3af;
}
.status-text.on {
  color: #22c55e;
}

.meta {
  flex: 1;
  min-width: 0;
}
.name {
  font-weight: 900;
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
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
}
.mini-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.empty {
  padding: 16px;
  text-align: center;
}
.etitle {
  font-weight: 900;
  margin-bottom: 6px;
}
</style>

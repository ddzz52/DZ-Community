<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { ArrowLeft, RefreshRight, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const partner = ref(null)

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
        </div>
        <div class="meta">
          <div class="name">{{ partner.nickname || '-' }}</div>
          <div class="sub app-muted">{{ partner.username || '-' }}</div>
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


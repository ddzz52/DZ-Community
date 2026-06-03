<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const users = ref([])
const loading = ref(false)

const fetchUsers = async () => {
  try {
    loading.value = true
    users.value = await http.get('/api/admin/users')
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
    if (e?.code === 40300) {
      router.replace('/app/me')
    }
  } finally {
    loading.value = false
  }
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${user.nickname || user.username}」（ID: ${user.id}）吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await http.delete(`/api/admin/users/${user.id}`)
    ElMessage.success('已删除')
    await fetchUsers()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

const toggleRole = async (user) => {
  const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await http.put(`/api/admin/users/${user.id}/role`, { role: newRole })
    ElMessage.success(`角色已更新为 ${newRole}`)
    await fetchUsers()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const genderText = (g) => {
  if (g === 1) return '男'
  if (g === 2) return '女'
  if (g === 0) return '其他'
  return '-'
}

const formatDate = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
}

onMounted(() => {
  if (auth.user?.role !== 'ADMIN') {
    ElMessage.error('仅管理员可访问')
    router.replace('/app/me')
    return
  }
  fetchUsers()
})
</script>

<template>
  <div class="stack">
    <div class="top app-card">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <div class="title">用户管理</div>
      <el-button size="small" :loading="loading" @click="fetchUsers">刷新</el-button>
    </div>

    <div class="summary app-card">
      <span class="sum-num">共 {{ users.length }} 个用户</span>
    </div>

    <div v-for="u in users" :key="u.id" class="user-card app-card">
      <div class="urow">
        <div class="uavatar">{{ (u.nickname || u.username || '?').slice(0, 1).toUpperCase() }}</div>
        <div class="umeta">
          <div class="uname">
            <span>{{ u.nickname || '-' }}</span>
            <el-tag v-if="u.role === 'ADMIN'" effect="light" type="danger" round size="small">管理员</el-tag>
            <el-tag v-else effect="light" round size="small">普通用户</el-tag>
          </div>
          <div class="usub app-muted">
            <span>ID: {{ u.id }}</span>
            <span>· 用户名: {{ u.username }}</span>
            <span>· 性别: {{ genderText(u.gender) }}</span>
          </div>
          <div class="usub app-muted">
            <span>情侣空间: {{ u.coupleId }}</span>
            <span>· 注册: {{ formatDate(u.createdAt || u.loveDate) }}</span>
          </div>
        </div>
        <div class="uactions">
          <el-button size="small" @click="toggleRole(u)">
            {{ u.role === 'ADMIN' ? '降为普通' : '升为管理' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="deleteUser(u)">删除</el-button>
        </div>
      </div>
    </div>

    <div v-if="!loading && users.length === 0" class="empty app-card">
      <div class="etitle">暂无用户数据</div>
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
.summary {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
}
.sum-num {
  color: var(--app-muted);
}
.user-card {
  padding: 14px 16px;
}
.urow {
  display: flex;
  align-items: center;
  gap: 12px;
}
.uavatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-weight: 800;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(139, 92, 246, 0.16));
  color: rgba(88, 28, 135, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.7);
  flex: 0 0 auto;
}
.umeta {
  flex: 1;
  min-width: 0;
}
.uname {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.usub {
  margin-top: 4px;
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.uactions {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
  flex: 0 0 auto;
}
.empty {
  padding: 16px;
  text-align: center;
}
.etitle {
  font-weight: 900;
}
</style>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})

const submit = async () => {
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/app/home'
    router.replace(redirect)
  } catch (e) {
    ElMessage.error(e?.message || '登录失败')
  }
}

const resetDialog = ref(false)
const resetStep = ref(1)
const resetLoading = ref(false)
const resetForm = reactive({
  username: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const openReset = () => {
  resetStep.value = 1
  resetForm.username = form.username || ''
  resetForm.code = ''
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  resetDialog.value = true
}

const requestReset = async () => {
  try {
    resetLoading.value = true
    await http.post('/api/auth/password-reset/request', { username: resetForm.username })
    ElMessage.success('验证码已发送到对方的提醒中心')
    resetStep.value = 2
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
  } finally {
    resetLoading.value = false
  }
}

const confirmReset = async () => {
  try {
    resetLoading.value = true
    await http.post('/api/auth/password-reset/confirm', {
      username: resetForm.username,
      code: resetForm.code,
      newPassword: resetForm.newPassword,
      confirmPassword: resetForm.confirmPassword
    })
    ElMessage.success('密码已重置，请用新密码登录')
    resetDialog.value = false
  } catch (e) {
    ElMessage.error(e?.message || '重置失败')
  } finally {
    resetLoading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="shell">
      <div class="brand">
        <div class="logo"><svg width="32" height="32" viewBox="0 0 48 48" fill="none"><rect x="8" y="18" width="32" height="22" rx="3" fill="#6366f1" opacity="0.15"/><path d="M6 20L24 6L42 20" stroke="#6366f1" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><rect x="19" y="26" width="10" height="14" rx="3" fill="#6366f1"/></svg></div>
        <div class="name">双人情侣小屋</div>
        <div class="desc app-muted">属于你们的温暖小窝</div>
      </div>
      <el-card class="card app-card" shadow="never">
      <template #header>
        <div class="title">登录</div>
      </template>
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%" @click="submit">登录</el-button>
        <div class="links">
          <el-link type="info" @click="openReset">忘记密码</el-link>
          <el-link type="primary" @click="router.push('/register')">去注册</el-link>
        </div>
      </el-form>
      </el-card>
    </div>
  </div>

  <el-dialog v-model="resetDialog" title="找回/重置密码" width="92%">
    <el-form label-position="top">
      <el-form-item label="用户名">
        <el-input v-model="resetForm.username" :disabled="resetLoading || resetStep !== 1" />
      </el-form-item>
      <template v-if="resetStep === 2">
        <el-form-item label="验证码（在对方的提醒中心查看）">
          <el-input v-model="resetForm.code" maxlength="6" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <div class="hint app-muted">密码需≥8位且包含字母、数字、特殊符号。</div>
      </template>
      <template v-else>
        <div class="hint app-muted">将向对方账号发送验证码（在提醒中心查看），用于确认找回密码。</div>
      </template>
    </el-form>
    <template #footer>
      <el-button :disabled="resetLoading" @click="resetDialog = false">取消</el-button>
      <el-button v-if="resetStep === 1" type="primary" :loading="resetLoading" @click="requestReset">发送验证码</el-button>
      <el-button v-else type="primary" :loading="resetLoading" @click="confirmReset">重置密码</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 26px 16px;
}
.shell {
  width: 100%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.brand {
  padding: 10px 10px 2px;
  text-align: left;
}
.logo {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: rgba(17, 24, 39, 0.9);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 10px 30px rgba(17, 24, 39, 0.12);
}
.name {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 800;
}
.desc {
  margin-top: 4px;
  font-size: 13px;
}
.card {
  width: 100%;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.links {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
}
.hint {
  margin-top: 6px;
  font-size: 12px;
}
</style>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
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

const submitLoading = ref(false)

const submit = async () => {
  if (submitLoading.value) return
  try {
    submitLoading.value = true
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/app/home'
    router.replace(redirect)
  } catch (e) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    submitLoading.value = false
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

// ==================== 系统公告 ====================
const announcements = ref([])
const fetchAnnouncements = async () => {
  try {
    announcements.value = await http.get('/api/announcements/active')
  } catch (e) { /* ignore */ }
}

// ==================== 鼠标跟随光晕 ====================
const mouseX = ref(50)
const mouseY = ref(50)
const onMouseMove = (e) => {
  mouseX.value = (e.clientX / window.innerWidth) * 100
  mouseY.value = (e.clientY / window.innerHeight) * 100
}

// ==================== 密码可见性 ====================
const pwdVisible = ref(false)

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove, { passive: true })
  fetchAnnouncements()
})
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<template>
  <div class="login-page">
    <!-- ====== 动态渐变光球 ====== -->
    <div class="bg-orbs" :style="{ '--mx': mouseX, '--my': mouseY }">
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="orb orb-3" />
      <div class="orb orb-4" />
    </div>

    <!-- ====== 浮动粒子 ====== -->
    <div class="particles">
      <span v-for="i in 20" :key="i" class="particle" :style="{
        '--x': Math.random() * 100 + '%',
        '--y': Math.random() * 100 + '%',
        '--d': (Math.random() * 12 + 8) + 's',
        '--s': (Math.random() * 2 + 1) + 'px',
        '--delay': (Math.random() * 8) + 's'
      }" />
    </div>

    <div class="login-shell">
      <!-- ====== 品牌区 ====== -->
      <div class="brand-block">
        <div class="logo-ring">
          <div class="logo-inner">
            <svg width="34" height="34" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="logoGrad" x1="6" y1="6" x2="42" y2="40" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#818cf8" />
                  <stop offset="50%" stop-color="#a78bfa" />
                  <stop offset="100%" stop-color="#f472b6" />
                </linearGradient>
              </defs>
              <rect x="8" y="18" width="32" height="22" rx="3" fill="url(#logoGrad)" opacity="0.18" />
              <path d="M6 20L24 6L42 20" stroke="url(#logoGrad)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
              <rect x="19" y="26" width="10" height="14" rx="3" fill="url(#logoGrad)" />
            </svg>
          </div>
        </div>
        <div class="brand-text">
          <h1 class="brand-title">双人情侣小屋</h1>
          <p class="brand-sub">只属于你们的私密空间</p>
        </div>
      </div>

      <!-- ====== 系统公告 ====== -->
      <div v-if="announcements.length" class="announce-banner">
        <div v-for="a in announcements" :key="a.id" class="announce-item">
          <span class="announce-icon">📢</span>
          <span>{{ a.content }}</span>
        </div>
      </div>

      <!-- ====== 登录卡片 ====== -->
      <div class="login-card">
        <div class="card-header">
          <span class="card-title">欢迎回来</span>
          <span class="card-sub">登录你的专属空间</span>
        </div>

        <div class="card-body">
          <!-- 用户名 -->
          <div class="input-group">
            <label class="input-label">用户名</label>
            <div class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M6 20v-1a6 6 0 0112 0v1" />
                </svg>
              </span>
              <input
                v-model="form.username"
                type="text"
                class="input-field"
                placeholder="输入用户名"
                autocomplete="username"
                @keydown.enter="submit"
              />
              <span class="input-border" />
            </div>
          </div>

          <!-- 密码 -->
          <div class="input-group">
            <label class="input-label">密码</label>
            <div class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" />
                  <path d="M7 11V7a5 5 0 0110 0v4" />
                  <circle cx="12" cy="16" r="1" fill="currentColor" />
                </svg>
              </span>
              <input
                v-model="form.password"
                :type="pwdVisible ? 'text' : 'password'"
                class="input-field"
                placeholder="输入密码"
                autocomplete="current-password"
                @keydown.enter="submit"
              />
              <button type="button" class="pwd-toggle" @click="pwdVisible = !pwdVisible" :title="pwdVisible ? '隐藏密码' : '显示密码'">
                <svg v-if="!pwdVisible" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
              <span class="input-border" />
            </div>
          </div>

          <!-- 登录按钮 -->
          <button
            class="submit-btn"
            :class="{ loading: submitLoading }"
            :disabled="submitLoading"
            @click="submit"
          >
            <span class="btn-text">{{ submitLoading ? '登录中...' : '登  录' }}</span>
            <span class="btn-shimmer" />
          </button>

          <!-- 底部链接 -->
          <div class="footer-links">
            <button type="button" class="text-link" @click="openReset">忘记密码</button>
            <button type="button" class="text-link primary" @click="router.push('/register')">创建账号</button>
          </div>
        </div>
      </div>

      <!-- ====== 底部注脚 ====== -->
      <p class="footnote">DZ Community · 为每一对情侣打造的专属空间</p>
    </div>
  </div>

  <!-- ====== 找回密码弹窗（功能不变） ====== -->
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
/* ==================== 页面容器 ==================== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 18px;
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

/* ==================== 动态渐变光球 ==================== */
.bg-orbs {
  position: fixed;
  inset: -10%;
  pointer-events: none;
  z-index: 0;
  filter: blur(90px);
  opacity: 0.72;
}

.orb {
  position: absolute;
  border-radius: 50%;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
  animation-direction: alternate;
}

.orb-1 {
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, rgba(129, 140, 248, 0.35), transparent 70%);
  top: -8%;
  left: calc(10% + var(--mx, 50) * 0.04 * 1%);
  animation: orbDrift1 14s infinite alternate;
}

.orb-2 {
  width: 440px;
  height: 440px;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.28), transparent 70%);
  top: 45%;
  right: calc(2% + var(--my, 50) * 0.03 * 1%);
  animation: orbDrift2 16s infinite alternate;
}

.orb-3 {
  width: 380px;
  height: 380px;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.32), transparent 70%);
  bottom: -12%;
  left: calc(30% + var(--mx, 50) * 0.05 * 1%);
  animation: orbDrift3 18s infinite alternate;
}

.orb-4 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.18), transparent 70%);
  top: 25%;
  left: calc(55% + var(--my, 50) * 0.06 * 1%);
  animation: orbDrift4 20s infinite alternate;
}

@keyframes orbDrift1 {
  0%   { transform: translate(0, 0) scale(1); }
  100% { transform: translate(40px, -30px) scale(1.12); }
}
@keyframes orbDrift2 {
  0%   { transform: translate(0, 0) scale(1.06); }
  100% { transform: translate(-35px, 25px) scale(0.92); }
}
@keyframes orbDrift3 {
  0%   { transform: translate(0, 0) scale(0.94); }
  100% { transform: translate(30px, -20px) scale(1.08); }
}
@keyframes orbDrift4 {
  0%   { transform: translate(0, 0) scale(1.02); }
  100% { transform: translate(-25px, -35px) scale(1.14); }
}

/* ==================== 浮动粒子 ==================== */
.particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  left: var(--x);
  top: var(--y);
  width: var(--s);
  height: var(--s);
  border-radius: 50%;
  background: rgba(167, 139, 250, 0.35);
  animation: particleRise var(--d) linear infinite;
  animation-delay: var(--delay);
  opacity: 0;
}

@keyframes particleRise {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 0.7;
  }
  90% {
    opacity: 0.15;
  }
  100% {
    transform: translateY(-100vh) scale(0.3);
    opacity: 0;
  }
}

/* ==================== 主体容器 ==================== */
.login-shell {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
  animation: shellIn 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes shellIn {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ==================== 品牌区 ==================== */
.brand-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  animation: brandIn 0.6s 0.1s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

@keyframes brandIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Logo 脉动光环 */
.logo-ring {
  width: 80px;
  height: 80px;
  border-radius: 24px;
  position: relative;
  display: grid;
  place-items: center;
}

.logo-ring::before {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(129, 140, 248, 0.35), rgba(244, 114, 182, 0.35), rgba(167, 139, 250, 0.35));
  animation: logoGlow 3s ease-in-out infinite;
  z-index: 0;
}

.logo-ring::after {
  content: '';
  position: absolute;
  inset: -14px;
  border-radius: 34px;
  background: linear-gradient(135deg, rgba(129, 140, 248, 0.12), rgba(244, 114, 182, 0.1));
  animation: logoGlowFar 3s ease-in-out 0.5s infinite;
  z-index: 0;
}

@keyframes logoGlow {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.06);
  }
}

@keyframes logoGlowFar {
  0%, 100% {
    opacity: 0.25;
    transform: scale(1);
  }
  50% {
    opacity: 0.55;
    transform: scale(1.1);
  }
}

.logo-inner {
  position: relative;
  z-index: 1;
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 8px 32px rgba(99, 102, 241, 0.12),
    0 2px 8px rgba(139, 92, 246, 0.08);
  display: grid;
  place-items: center;
  animation: logoBeat 4.8s ease-in-out infinite;
}

@keyframes logoBeat {
  0%, 100% { transform: scale(1); }
  6%  { transform: scale(1.08); }
  12% { transform: scale(1); }
  18% { transform: scale(1.05); }
  24% { transform: scale(1); }
}

.brand-text {
  text-align: center;
}

.brand-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.3px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 40%, #db2777 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-sub {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--app-muted);
  letter-spacing: 0.3px;
}

/* ==================== 登录卡片 ==================== */
.login-card {
  width: 100%;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(28px) saturate(1.15);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow:
    0 4px 24px rgba(99, 102, 241, 0.06),
    0 16px 48px rgba(139, 92, 246, 0.08),
    0 0 0 0.5px rgba(167, 139, 250, 0.08) inset;
  overflow: hidden;
  transition: box-shadow 0.4s ease, border-color 0.4s ease;
  animation: cardIn 0.65s 0.2s cubic-bezier(0.22, 0.61, 0.36, 1) both;
  position: relative;
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 22px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(129, 140, 248, 0.25), rgba(244, 114, 182, 0.2), rgba(167, 139, 250, 0.25));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  animation: cardBorderShine 4s ease-in-out infinite;
}

@keyframes cardBorderShine {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (hover: hover) {
  .login-card:hover {
    border-color: rgba(167, 139, 250, 0.22);
    box-shadow:
      0 8px 40px rgba(99, 102, 241, 0.1),
      0 24px 64px rgba(139, 92, 246, 0.12),
      0 0 0 0.5px rgba(167, 139, 250, 0.12) inset;
  }
}

/* ==================== 卡片头部 ==================== */
.card-header {
  padding: 28px 28px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-title {
  font-size: 20px;
  font-weight: 750;
  letter-spacing: -0.2px;
  color: rgba(17, 24, 39, 0.9);
}

.card-sub {
  font-size: 13px;
  color: var(--app-muted);
}

/* ==================== 卡片主体 ==================== */
.card-body {
  padding: 24px 28px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ==================== 输入组 ==================== */
.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.72);
  letter-spacing: 0.2px;
  padding-left: 2px;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  height: 48px;
  border-radius: 14px;
  background: rgba(243, 244, 246, 0.65);
  border: 1.5px solid rgba(209, 213, 219, 0.45);
  transition: all 0.28s ease;
  overflow: hidden;
}

.input-wrap:focus-within {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(129, 140, 248, 0.55);
  box-shadow:
    0 0 0 4px rgba(129, 140, 248, 0.08),
    0 2px 8px rgba(99, 102, 241, 0.06);
}

.input-icon {
  display: flex;
  align-items: center;
  color: rgba(17, 24, 39, 0.36);
  transition: color 0.28s ease;
  flex-shrink: 0;
}

.input-wrap:focus-within .input-icon {
  color: #6366f1;
}

.input-field {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  font-family: inherit;
  color: rgba(17, 24, 39, 0.9);
  min-width: 0;
}

.input-field::placeholder {
  color: rgba(17, 24, 39, 0.28);
}

.input-field:-webkit-autofill {
  -webkit-box-shadow: 0 0 0 30px rgba(243, 244, 246, 0.9) inset !important;
  border-radius: 14px;
}

/* 输入框底部光条 */
.input-border {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #6366f1, #a78bfa, #f472b6);
  transition: all 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
  border-radius: 1px;
}

.input-wrap:focus-within .input-border {
  width: 70%;
  left: 15%;
}

/* 密码切换按钮 */
.pwd-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: rgba(17, 24, 39, 0.3);
  cursor: pointer;
  padding: 4px;
  border-radius: 8px;
  transition: color 0.2s ease, background 0.2s ease;
  flex-shrink: 0;
}

.pwd-toggle:hover {
  color: rgba(17, 24, 39, 0.55);
  background: rgba(17, 24, 39, 0.04);
}

/* ==================== 登录按钮 ==================== */
.submit-btn {
  position: relative;
  width: 100%;
  height: 50px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #6366f1 0%, #7c3aed 50%, #db2777 100%);
  background-size: 200% 100%;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  font-family: inherit;
  letter-spacing: 2px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.35s ease;
  box-shadow:
    0 4px 20px rgba(99, 102, 241, 0.28),
    0 1px 4px rgba(139, 92, 246, 0.15);
  margin-top: 6px;
}

.submit-btn:hover:not(:disabled) {
  background-position: 100% 0;
  box-shadow:
    0 6px 28px rgba(99, 102, 241, 0.38),
    0 2px 8px rgba(139, 92, 246, 0.25);
  transform: translateY(-1px);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.985);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.submit-btn.loading {
  background-position: 100% 0;
  animation: btnPulse 1.5s ease-in-out infinite;
}

@keyframes btnPulse {
  0%, 100% { box-shadow: 0 4px 20px rgba(99, 102, 241, 0.28); }
  50% { box-shadow: 0 8px 32px rgba(99, 102, 241, 0.45); }
}

.btn-text {
  position: relative;
  z-index: 1;
}

/* 按钮光泽扫过 */
.btn-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    transparent 30%,
    rgba(255, 255, 255, 0.12) 45%,
    rgba(255, 255, 255, 0.22) 50%,
    rgba(255, 255, 255, 0.12) 55%,
    transparent 70%
  );
  transform: translateX(-100%);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* ==================== 底部链接 ==================== */
.footer-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 2px;
}

.text-link {
  border: none;
  background: transparent;
  font-size: 13px;
  font-family: inherit;
  font-weight: 500;
  color: var(--app-muted);
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.text-link:hover {
  color: rgba(17, 24, 39, 0.82);
  background: rgba(17, 24, 39, 0.04);
}

.text-link.primary {
  color: #6366f1;
  font-weight: 600;
}

.text-link.primary:hover {
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.06);
}

/* ==================== 系统公告横幅 ==================== */
.announce-banner {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
  animation: cardIn 0.65s 0.15s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
.announce-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 12px;
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.2);
  font-size: 13px;
  color: rgba(180, 83, 9, 0.9);
  backdrop-filter: blur(8px);
}
.announce-icon { flex-shrink: 0; }

:root[data-theme='dark'] .announce-item {
  background: rgba(251, 191, 36, 0.08);
  border-color: rgba(251, 191, 36, 0.15);
  color: rgba(253, 224, 71, 0.85);
}

/* ==================== 底部注脚 ==================== */
.footnote {
  margin: 0;
  font-size: 12px;
  color: rgba(17, 24, 39, 0.25);
  letter-spacing: 0.5px;
  animation: brandIn 0.6s 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

/* ==================== 暗色模式 ==================== */
:root[data-theme='dark'] .login-card {
  background: rgba(15, 23, 42, 0.7);
  border-color: rgba(148, 163, 184, 0.14);
}

:root[data-theme='dark'] .logo-inner {
  background: rgba(15, 23, 42, 0.75);
  border-color: rgba(148, 163, 184, 0.16);
}

:root[data-theme='dark'] .card-title {
  color: rgba(226, 232, 240, 0.92);
}

:root[data-theme='dark'] .input-label {
  color: rgba(226, 232, 240, 0.62);
}

:root[data-theme='dark'] .input-wrap {
  background: rgba(30, 41, 59, 0.55);
  border-color: rgba(148, 163, 184, 0.15);
}

:root[data-theme='dark'] .input-wrap:focus-within {
  background: rgba(30, 41, 59, 0.78);
  border-color: rgba(129, 140, 248, 0.45);
}

:root[data-theme='dark'] .input-field {
  color: rgba(226, 232, 240, 0.92);
}

:root[data-theme='dark'] .input-field::placeholder {
  color: rgba(226, 232, 240, 0.2);
}

:root[data-theme='dark'] .input-field:-webkit-autofill {
  -webkit-box-shadow: 0 0 0 30px rgba(30, 41, 59, 0.9) inset !important;
  -webkit-text-fill-color: rgba(226, 232, 240, 0.92) !important;
}

:root[data-theme='dark'] .brand-title {
  background: linear-gradient(135deg, #a5b4fc 0%, #c4b5fd 40%, #f9a8d4 100%);
  -webkit-background-clip: text;
  background-clip: text;
}

:root[data-theme='dark'] .bg-orbs {
  opacity: 0.55;
}

:root[data-theme='dark'] .pwd-toggle:hover {
  background: rgba(226, 232, 240, 0.06);
}

:root[data-theme='dark'] .text-link:hover {
  background: rgba(226, 232, 240, 0.04);
}

:root[data-theme='dark'] .text-link.primary:hover {
  background: rgba(129, 140, 248, 0.1);
}

/* ==================== 响应式 ==================== */
@media (max-width: 480px) {
  .login-shell {
    max-width: 100%;
    gap: 22px;
  }
  .card-body {
    padding: 20px 20px 22px;
  }
  .card-header {
    padding: 22px 20px 0;
  }
  .brand-title {
    font-size: 22px;
  }
  .orb-1, .orb-2 {
    opacity: 0.5;
  }
}
</style>

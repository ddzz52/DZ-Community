<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  loveDate: null,
  gender: null,
  avatarUrl: '',
  inviteCode: ''
})

const submitLoading = ref(false)
const regOk = ref(false)
const newInviteCode = ref('')

const submit = async () => {
  if (submitLoading.value) return
  try {
    submitLoading.value = true
    const user = await auth.registerV2({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      loveDate: form.loveDate,
      gender: form.gender,
      avatarUrl: form.avatarUrl,
      inviteCode: form.inviteCode || undefined
    })
    if (user.inviteCode) {
      newInviteCode.value = user.inviteCode
      regOk.value = true
    } else {
      ElMessage.success('注册成功，请登录')
      router.replace('/login')
    }
  } catch (e) {
    ElMessage.error(e?.message || '注册失败')
  } finally {
    submitLoading.value = false
  }
}

const copyCode = () => {
  navigator.clipboard?.writeText(newInviteCode.value).then(() => {
    ElMessage.success('已复制邀请码')
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = newInviteCode.value
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制邀请码')
  })
}

const goLogin = () => {
  router.replace('/login')
}

// ==================== 鼠标跟随光晕 ====================
const mouseX = ref(50)
const mouseY = ref(50)
const onMouseMove = (e) => {
  mouseX.value = (e.clientX / window.innerWidth) * 100
  mouseY.value = (e.clientY / window.innerHeight) * 100
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove, { passive: true })
})
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<template>
  <div class="register-page">
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

    <div class="register-shell">
      <!-- ====== 品牌区 ====== -->
      <div class="brand-block">
        <div class="logo-ring">
          <div class="logo-inner">
            <svg width="34" height="34" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="regLogoGrad" x1="6" y1="6" x2="42" y2="40" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#818cf8" />
                  <stop offset="50%" stop-color="#a78bfa" />
                  <stop offset="100%" stop-color="#f472b6" />
                </linearGradient>
              </defs>
              <rect x="8" y="18" width="32" height="22" rx="3" fill="url(#regLogoGrad)" opacity="0.18" />
              <path d="M6 20L24 6L42 20" stroke="url(#regLogoGrad)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
              <rect x="19" y="26" width="10" height="14" rx="3" fill="url(#regLogoGrad)" />
            </svg>
          </div>
        </div>
        <div class="brand-text">
          <h1 class="brand-title">创建专属账号</h1>
          <p class="brand-sub">加入属于你们的情侣空间</p>
        </div>
      </div>

      <!-- ====== 注册卡片 ====== -->
      <div class="register-card">
        <div class="card-header">
          <span class="card-title">注册新账号</span>
          <span class="card-sub">填写信息，开启你们的故事</span>
        </div>

        <div class="card-body">
          <!-- 用户名 -->
          <div class="input-group">
            <label class="input-label">用户名 <span class="required">*</span></label>
            <div class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M6 20v-1a6 6 0 0112 0v1" />
                </svg>
              </span>
              <input v-model="form.username" type="text" class="input-field" placeholder="输入用户名" autocomplete="username" />
              <span class="input-border" />
            </div>
          </div>

          <!-- 密码 -->
          <div class="input-group">
            <label class="input-label">密码 <span class="required">*</span></label>
            <div class="input-wrap">
              <span class="input-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" />
                  <path d="M7 11V7a5 5 0 0110 0v4" />
                </svg>
              </span>
              <input v-model="form.password" type="password" class="input-field" placeholder="至少6位密码" autocomplete="new-password" />
              <span class="input-border" />
            </div>
          </div>

          <!-- 昵称 + 相恋日期 -->
          <div class="input-row">
            <div class="input-group flex-1">
              <label class="input-label">昵称</label>
              <div class="input-wrap">
                <input v-model="form.nickname" type="text" class="input-field" placeholder="你的昵称" />
                <span class="input-border" />
              </div>
            </div>
            <div class="input-group flex-1">
              <label class="input-label">相恋日期 <span class="required">*</span></label>
              <div class="input-wrap picker-wrap">
                <el-date-picker
                  v-model="form.loveDate"
                  type="date"
                  placeholder="选择日期"
                  class="inline-picker"
                />
                <span class="input-border" />
              </div>
            </div>
          </div>

          <!-- 性别 + 邀请码 -->
          <div class="input-row">
            <div class="input-group flex-1">
              <label class="input-label">性别</label>
              <div class="input-wrap picker-wrap">
                <el-select
                  v-model="form.gender"
                  placeholder="不填"
                  class="inline-select"
                >
                  <el-option :value="1" label="男" />
                  <el-option :value="2" label="女" />
                  <el-option :value="0" label="其他" />
                </el-select>
                <span class="input-border" />
              </div>
            </div>
            <div class="input-group flex-1">
              <label class="input-label">邀请码</label>
              <div class="input-wrap">
                <span class="input-icon icon-sm">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                    <path d="M15 7h2a5 5 0 010 10h-2m-6 0H7A5 5 0 017 7h2" />
                  </svg>
                </span>
                <input v-model="form.inviteCode" type="text" class="input-field" placeholder="伴侣的邀请码" maxlength="8" />
                <span class="input-border" />
              </div>
            </div>
          </div>

          <!-- 注册按钮 -->
          <button class="submit-btn" :class="{ loading: submitLoading }" :disabled="submitLoading" @click="submit">
            <span class="btn-text">{{ submitLoading ? '注册中...' : '注  册' }}</span>
            <span class="btn-shimmer" />
          </button>

          <!-- 底部链接 -->
          <div class="footer-links">
            <span />
            <button type="button" class="text-link primary" @click="router.push('/login')">已有账号？去登录</button>
          </div>
        </div>
      </div>

      <p class="footnote">DZ Community · 为每一对情侣打造的专属空间</p>
    </div>
  </div>

  <!-- ====== 注册成功弹窗 ====== -->
  <el-dialog v-model="regOk" title="注册成功" width="90%" :close-on-click-modal="false" :close-on-press-escape="false">
    <div style="text-align:center;padding:12px 0">
      <p style="font-size:15px;font-weight:700;margin:0 0 6px">你的情侣空间邀请码</p>
      <p style="font-size:13px;color:var(--app-muted);margin:0 0 14px">把邀请码发给你的伴侣，TA 注册时填写即可加入同一空间</p>
      <div style="display:flex;align-items:center;justify-content:center;gap:10px">
        <code style="font-size:28px;font-weight:950;letter-spacing:3px;padding:10px 20px;background:rgba(99,102,241,0.08);border-radius:12px;color:#6366f1">{{ newInviteCode }}</code>
        <el-button size="small" @click="copyCode">复制</el-button>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="goLogin">已保存，去登录</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
/* ==================== 页面容器 ==================== */
.register-page {
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
}
.orb-1 {
  width: 520px; height: 520px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.3), transparent 70%);
  top: -8%; left: 10%;
  animation: orbDrift1 14s infinite alternate;
}
.orb-2 {
  width: 440px; height: 440px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.28), transparent 70%);
  top: 45%; right: 2%;
  animation: orbDrift2 16s infinite alternate;
}
.orb-3 {
  width: 380px; height: 380px;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.3), transparent 70%);
  bottom: -12%; left: 30%;
  animation: orbDrift3 18s infinite alternate;
}
.orb-4 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.16), transparent 70%);
  top: 25%; left: 55%;
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
  position: fixed; inset: 0; pointer-events: none; z-index: 1;
}
.particle {
  position: absolute;
  left: var(--x); top: var(--y);
  width: var(--s); height: var(--s);
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.3);
  animation: particleRise var(--d) linear infinite;
  animation-delay: var(--delay);
  opacity: 0;
}
@keyframes particleRise {
  0%   { transform: translateY(0) scale(1); opacity: 0; }
  10%  { opacity: 0.7; }
  90%  { opacity: 0.15; }
  100% { transform: translateY(-100vh) scale(0.3); opacity: 0; }
}

/* ==================== 主体容器 ==================== */
.register-shell {
  position: relative; z-index: 2;
  width: 100%; max-width: 460px;
  display: flex; flex-direction: column; align-items: center; gap: 28px;
  animation: shellIn 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
@keyframes shellIn {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ==================== 品牌区 ==================== */
.brand-block {
  display: flex; flex-direction: column; align-items: center; gap: 16px;
  animation: brandIn 0.6s 0.1s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
@keyframes brandIn {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.logo-ring {
  width: 80px; height: 80px; border-radius: 24px;
  position: relative; display: grid; place-items: center;
}
.logo-ring::before {
  content: ''; position: absolute; inset: -6px; border-radius: 28px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.35), rgba(99, 102, 241, 0.35), rgba(167, 139, 250, 0.35));
  animation: logoGlow 3s ease-in-out infinite; z-index: 0;
}
.logo-ring::after {
  content: ''; position: absolute; inset: -14px; border-radius: 34px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(99, 102, 241, 0.1));
  animation: logoGlowFar 3s ease-in-out 0.5s infinite; z-index: 0;
}
@keyframes logoGlow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.06); }
}
@keyframes logoGlowFar {
  0%, 100% { opacity: 0.25; transform: scale(1); }
  50% { opacity: 0.55; transform: scale(1.1); }
}

.logo-inner {
  position: relative; z-index: 1;
  width: 60px; height: 60px; border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 32px rgba(99, 102, 241, 0.12), 0 2px 8px rgba(139, 92, 246, 0.08);
  display: grid; place-items: center;
  animation: logoBeat 4.8s ease-in-out infinite;
}
@keyframes logoBeat {
  0%, 100% { transform: scale(1); }
  6%  { transform: scale(1.08); }
  12% { transform: scale(1); }
  18% { transform: scale(1.05); }
  24% { transform: scale(1); }
}

.brand-text { text-align: center; }
.brand-title {
  margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.3px;
  background: linear-gradient(135deg, #059669 0%, #6366f1 50%, #7c3aed 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.brand-sub {
  margin: 6px 0 0; font-size: 14px; color: var(--app-muted); letter-spacing: 0.3px;
}

/* ==================== 注册卡片 ==================== */
.register-card {
  width: 100%; border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(28px) saturate(1.15);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06), 0 16px 48px rgba(139, 92, 246, 0.08), 0 0 0 0.5px rgba(167, 139, 250, 0.08) inset;
  overflow: visible;
  transition: box-shadow 0.4s ease, border-color 0.4s ease;
  animation: cardIn 0.65s 0.2s cubic-bezier(0.22, 0.61, 0.36, 1) both;
  position: relative;
}
.register-card::before {
  content: ''; position: absolute; inset: 0; border-radius: 22px; padding: 1px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.25), rgba(99, 102, 241, 0.2), rgba(167, 139, 250, 0.25));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor; mask-composite: exclude;
  pointer-events: none; animation: cardBorderShine 4s ease-in-out infinite;
}
@keyframes cardBorderShine {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.card-header { padding: 28px 28px 0; display: flex; flex-direction: column; gap: 6px; }
.card-title { font-size: 20px; font-weight: 750; letter-spacing: -0.2px; color: rgba(17, 24, 39, 0.9); }
.card-sub { font-size: 13px; color: var(--app-muted); }

.card-body { padding: 24px 28px 28px; display: flex; flex-direction: column; gap: 16px; }

/* ==================== 输入组 ==================== */
.input-group { display: flex; flex-direction: column; gap: 6px; }
.flex-1 { flex: 1; min-width: 0; }
.input-label { font-size: 13px; font-weight: 600; color: rgba(17, 24, 39, 0.72); letter-spacing: 0.2px; padding-left: 2px; }
.required { color: #ef4444; }

.input-row { display: flex; gap: 14px; }

.input-wrap {
  position: relative; display: flex; align-items: center; gap: 10px;
  padding: 0 14px; height: 44px; border-radius: 14px;
  background: rgba(243, 244, 246, 0.65);
  border: 1.5px solid rgba(209, 213, 219, 0.45);
  transition: all 0.28s ease; overflow: hidden;
}
.picker-wrap { overflow: visible; }
.input-wrap:focus-within {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(16, 185, 129, 0.45);
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.06), 0 2px 8px rgba(99, 102, 241, 0.05);
}

.input-icon { display: flex; align-items: center; color: rgba(17, 24, 39, 0.36); transition: color 0.28s ease; flex-shrink: 0; }
.input-icon.icon-sm { padding: 0 1px; }
.input-wrap:focus-within .input-icon { color: #059669; }

.input-field {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 14px; font-family: inherit; color: rgba(17, 24, 39, 0.9); min-width: 0;
}
.input-field::placeholder { color: rgba(17, 24, 39, 0.28); }

.input-border {
  position: absolute; bottom: 0; left: 50%; width: 0; height: 2px;
  background: linear-gradient(90deg, #059669, #6366f1, #a78bfa);
  transition: all 0.35s cubic-bezier(0.22, 0.61, 0.36, 1); border-radius: 1px;
}
.input-wrap:focus-within .input-border { width: 70%; left: 15%; }

/* Element Plus 组件融入 */
:deep(.inline-picker) { width: 100%; }
:deep(.inline-picker .el-input__wrapper) {
  background: transparent !important; border: none !important; box-shadow: none !important; padding: 0;
}
:deep(.inline-select) { width: 100%; }
:deep(.inline-select .el-input__wrapper) {
  background: transparent !important; border: none !important; box-shadow: none !important; padding: 0;
}

/* ==================== 注册按钮 ==================== */
.submit-btn {
  position: relative; width: 100%; height: 50px; border-radius: 14px; border: none;
  background: linear-gradient(135deg, #059669 0%, #6366f1 50%, #7c3aed 100%);
  background-size: 200% 100%;
  color: #fff; font-size: 16px; font-weight: 700; font-family: inherit;
  letter-spacing: 2px; cursor: pointer; overflow: hidden;
  transition: all 0.35s ease;
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.28), 0 1px 4px rgba(99, 102, 241, 0.15);
  margin-top: 6px;
}
.submit-btn:hover:not(:disabled) {
  background-position: 100% 0;
  box-shadow: 0 6px 28px rgba(16, 185, 129, 0.38), 0 2px 8px rgba(99, 102, 241, 0.25);
  transform: translateY(-1px);
}
.submit-btn:active:not(:disabled) { transform: translateY(0) scale(0.985); }
.submit-btn:disabled { cursor: not-allowed; opacity: 0.7; }
.submit-btn.loading { background-position: 100% 0; animation: btnPulse 1.5s ease-in-out infinite; }
@keyframes btnPulse {
  0%, 100% { box-shadow: 0 4px 20px rgba(16, 185, 129, 0.28); }
  50% { box-shadow: 0 8px 32px rgba(16, 185, 129, 0.45); }
}
.btn-text { position: relative; z-index: 1; }
.btn-shimmer {
  position: absolute; inset: 0;
  background: linear-gradient(105deg, transparent 30%, rgba(255,255,255,0.12) 45%, rgba(255,255,255,0.22) 50%, rgba(255,255,255,0.12) 55%, transparent 70%);
  transform: translateX(-100%);
  animation: shimmer 3s ease-in-out infinite;
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* ==================== 底部链接 ==================== */
.footer-links { display: flex; justify-content: space-between; align-items: center; padding-top: 2px; }
.text-link {
  border: none; background: transparent; font-size: 13px; font-family: inherit; font-weight: 500;
  color: var(--app-muted); cursor: pointer; padding: 6px 10px; border-radius: 8px; transition: all 0.2s ease;
}
.text-link:hover { color: rgba(17, 24, 39, 0.82); background: rgba(17, 24, 39, 0.04); }
.text-link.primary { color: #059669; font-weight: 600; }
.text-link.primary:hover { color: #047857; background: rgba(16, 185, 129, 0.06); }

.footnote {
  margin: 0; font-size: 12px; color: rgba(17, 24, 39, 0.25);
  letter-spacing: 0.5px; animation: brandIn 0.6s 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

/* ==================== 暗色模式 ==================== */
:root[data-theme='dark'] .register-card {
  background: rgba(15, 23, 42, 0.7); border-color: rgba(148, 163, 184, 0.14);
}
:root[data-theme='dark'] .logo-inner {
  background: rgba(15, 23, 42, 0.75); border-color: rgba(148, 163, 184, 0.16);
}
:root[data-theme='dark'] .card-title { color: rgba(226, 232, 240, 0.92); }
:root[data-theme='dark'] .input-label { color: rgba(226, 232, 240, 0.62); }
:root[data-theme='dark'] .input-wrap {
  background: rgba(30, 41, 59, 0.55); border-color: rgba(148, 163, 184, 0.15);
}
:root[data-theme='dark'] .input-wrap:focus-within {
  background: rgba(30, 41, 59, 0.78); border-color: rgba(16, 185, 129, 0.4);
}
:root[data-theme='dark'] .input-field { color: rgba(226, 232, 240, 0.92); }
:root[data-theme='dark'] .input-field::placeholder { color: rgba(226, 232, 240, 0.2); }
:root[data-theme='dark'] .bg-orbs { opacity: 0.55; }
:root[data-theme='dark'] .text-link:hover { background: rgba(226, 232, 240, 0.04); }
:root[data-theme='dark'] .text-link.primary:hover { background: rgba(16, 185, 129, 0.1); }

/* ==================== 响应式 ==================== */
@media (max-width: 480px) {
  .register-shell { max-width: 100%; gap: 22px; }
  .card-body { padding: 20px 20px 22px; }
  .card-header { padding: 22px 20px 0; }
  .input-row { flex-direction: column; gap: 14px; }
  .brand-title { font-size: 22px; }
}
</style>

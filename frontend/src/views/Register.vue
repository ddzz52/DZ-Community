<script setup>
import { reactive } from 'vue'
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

const submit = async () => {
  try {
    await auth.registerV2({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      loveDate: form.loveDate,
      gender: form.gender,
      avatarUrl: form.avatarUrl,
      inviteCode: form.inviteCode || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } catch (e) {
    ElMessage.error(e?.message || '注册失败')
  }
}
</script>

<template>
  <div class="page">
    <div class="shell">
      <div class="brand">
        <div class="logo"><svg width="32" height="32" viewBox="0 0 48 48" fill="none"><rect x="8" y="18" width="32" height="22" rx="3" fill="#6366f1" opacity="0.15"/><path d="M6 20L24 6L42 20" stroke="#6366f1" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><rect x="19" y="26" width="10" height="14" rx="3" fill="#6366f1"/></svg></div>
        <div class="name">加入双人情侣小屋</div>
        <div class="desc app-muted">仅限情侣双方，注册后即可开始记录生活</div>
      </div>
      <el-card class="card app-card" shadow="never">
        <template #header>
          <div class="title">注册</div>
        </template>
        <el-form label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="form.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
          </el-form-item>
          <el-form-item label="昵称（可选）">
            <el-input v-model="form.nickname" />
          </el-form-item>
          <el-form-item label="相恋日期">
            <el-date-picker v-model="form.loveDate" type="date" style="width: 100%" />
          </el-form-item>
          <el-form-item label="性别（可选）">
            <el-select v-model="form.gender" placeholder="不填也可以" style="width: 100%">
              <el-option :value="1" label="男" />
              <el-option :value="2" label="女" />
              <el-option :value="0" label="其他/保密" />
            </el-select>
          </el-form-item>
          <el-form-item label="头像地址（可选）">
            <el-input v-model="form.avatarUrl" placeholder="后续会支持上传" />
          </el-form-item>
          <el-form-item label="邀请码（可选，加入已有空间）">
            <el-input v-model="form.inviteCode" placeholder="输入伴侣的邀请码可加入同一空间" maxlength="32" />
          </el-form-item>
          <el-button type="primary" style="width: 100%" @click="submit">注册</el-button>
          <div class="links">
            <el-link type="primary" @click="router.push('/login')">去登录</el-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
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
  justify-content: flex-end;
}
</style>

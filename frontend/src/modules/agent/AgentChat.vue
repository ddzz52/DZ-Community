<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  Promotion, ArrowLeft, Delete, Edit, Close,
  MoreFilled, CopyDocument, Refresh, Select,
  CircleCheck, CircleCheckFilled, Menu, MagicStick
} from '@element-plus/icons-vue'
import http from '../../api/http'

const router = useRouter()

const STORAGE_KEY = 'dz_agent_messages'
const MAX_MESSAGES = 100
const DEBOUNCE_MS = 800

// ==================== 数据 ====================

const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const chatRef = ref(null)
let debounceTimer = null
let sendLock = false
let uidCounter = Date.now()

/** 每条消息的 uid，用于精确操作（不依赖数组索引） */
const nextUid = () => ++uidCounter

const quickActions = [
  { label: '💕 查询纪念日', command: '最近的纪念日是什么时候？' },
  { label: '📝 写日记', command: '帮我写日记，今天很开心' },
  { label: '📌 创建提醒', command: '提醒我明天去约会' },
  { label: '💑 相恋天数', command: '我们在一起多少天了？' }
]

// ==================== Emoji 面板 ====================

const showEmoji = ref(false)

const commonEmojis = [
  '😊', '😂', '❤️', '🥰', '😘', '💕', '✨', '🎉',
  '🥺', '😭', '🤗', '💝', '🌹', '💌', '💋', '👩‍❤️‍👨',
  '😎', '🤩', '😍', '💪', '🙏', '🔥', '💯', '🌸',
  '🫶', '😅', '🫣', '😴', '🤔', '👀', '🍰', '🎂',
  '💍', '🎁', '📝', '📅', '💰', '🎬', '🍜', '✈️'
]

const insertEmoji = (emoji) => {
  inputMessage.value += emoji
  showEmoji.value = false
}

// ==================== 输入区快捷指令 ====================

const inputCommands = [
  { label: '💌 帮我写情话', command: '帮我写一段甜甜的情话' },
  { label: '📅 查纪念日', command: '最近的纪念日是什么时候？' },
  { label: '📝 写日记', command: '帮我写日记，今天很开心' },
  { label: '💡 约会建议', command: '给我一些浪漫的约会建议' }
]

// ==================== 上下文菜单 ====================

const ctxMenu = ref({ visible: false, x: 0, y: 0, msg: null, msgIndex: -1 })

let longPressTimer = null
const LONG_PRESS_MS = 500

const showContextMenu = (e, msg, index) => {
  e.preventDefault()
  if (editMode.value) return

  // 移动端 touch 事件
  const clientX = e.touches?.[0]?.clientX ?? e.clientX
  const clientY = e.touches?.[0]?.clientY ?? e.clientY

  // 防止菜单溢出屏幕右边缘 / 底边缘
  const menuW = 160
  const menuH = 140
  const x = Math.min(clientX, window.innerWidth - menuW - 8)
  const y = Math.min(clientY, window.innerHeight - menuH - 8)

  ctxMenu.value = { visible: true, x, y, msg, msgIndex: index }
}

const hideContextMenu = () => {
  ctxMenu.value = { visible: false, x: 0, y: 0, msg: null, msgIndex: -1 }
}

// 移动端长按
const onTouchStart = (e, msg, index) => {
  longPressTimer = setTimeout(() => showContextMenu(e, msg, index), LONG_PRESS_MS)
}
const onTouchEnd = () => {
  if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null }
}
const onTouchMove = () => {
  if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null }
}

// 点击空白关闭菜单
const onGlobalClick = () => { if (ctxMenu.value.visible) hideContextMenu() }

// ==================== 上下文菜单操作 ====================

const copyMessageText = () => {
  const msg = ctxMenu.value.msg
  if (msg) {
    navigator.clipboard?.writeText(msg.content).then(() => {
      ElMessage.success('已复制')
    }).catch(() => {
      const ta = document.createElement('textarea')
      ta.value = msg.content
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success('已复制')
    })
  }
  hideContextMenu()
}

/** 用户消息「重发」：删除旧气泡 + 重新发送原文 */
const resendMessage = () => {
  const msg = ctxMenu.value.msg
  const index = ctxMenu.value.msgIndex
  hideContextMenu()
  if (!msg || msg.type !== 'user') return
  messages.value.splice(index, 1)
  if (msg.backendId) {
    http.delete(`/api/agent/messages/${msg.backendId}`).catch(() => {})
  }
  saveToStorage()
  doSend(msg.content)
}

/** AI 消息「重新生成」：删除本条回复 + 用上一条用户消息重新请求 */
const regenerateMessage = () => {
  const msg = ctxMenu.value.msg
  const index = ctxMenu.value.msgIndex
  if (!msg || msg.type !== 'agent' || index < 1) {
    hideContextMenu()
    return
  }
  const userMsg = messages.value[index - 1]
  if (!userMsg || userMsg.type !== 'user') {
    hideContextMenu()
    return
  }
  hideContextMenu()
  messages.value.splice(index, 1)
  saveToStorage()
  doSend(userMsg.content)
}

const deleteSingleMessage = async () => {
  const msg = ctxMenu.value.msg
  const index = ctxMenu.value.msgIndex
  hideContextMenu()

  try {
    await ElMessageBox.confirm(
      '确定删除这条消息？',
      '提示',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  if (msg.backendId) {
    http.delete(`/api/agent/messages/${msg.backendId}`).catch(() => {})
  }
  messages.value.splice(index, 1)
  saveToStorage()
  ElMessage.success('已删除')
}

// ==================== 时间戳显示规则 ====================

/**
 * 每 5 条消息或与前一条跨天时显示时间戳
 */
const shouldShowTime = (index) => {
  if (index === 0) return true
  const prev = messages.value[index - 1]
  if (!prev) return true
  // 每 5 条显示一次
  if ((index + 1) % 5 === 0) return true
  // 跨天显示
  const curMsg = messages.value[index]
  if (curMsg && prev.time) {
    const d1 = new Date(prev.time)
    const d2 = new Date(curMsg.time)
    if (d1.getFullYear() !== d2.getFullYear()) return true
    if (d1.getMonth() !== d2.getMonth()) return true
    if (d1.getDate() !== d2.getDate()) return true
  }
  return false
}

/**
 * 格式化时间戳：当天显示 HH:mm，跨天显示 MM-dd HH:mm
 */
const formatTime = (time) => {
  const d = new Date(time)
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  if (d.getFullYear() === now.getFullYear()
      && d.getMonth() === now.getMonth()
      && d.getDate() === now.getDate()) {
    return pad(d.getHours()) + ':' + pad(d.getMinutes())
  }
  return (d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

// ==================== 编辑模式 ====================

const editMode = ref(false)
const selectedUids = ref([])

const enterEditMode = () => {
  editMode.value = true
  selectedUids.value = []
}

const exitEditMode = () => {
  editMode.value = false
  selectedUids.value = []
}

const toggleSelect = (uid) => {
  const idx = selectedUids.value.indexOf(uid)
  if (idx > -1) selectedUids.value.splice(idx, 1)
  else selectedUids.value.push(uid)
}

const selectAll = () => {
  selectedUids.value = messages.value.map(m => m.uid)
}

const deselectAll = () => {
  selectedUids.value = []
}

const selectedCount = computed(() => selectedUids.value.length)

const batchDelete = async () => {
  if (selectedUids.value.length === 0) {
    ElMessage.info('请至少选择一条消息')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定删除选中聊天记录？仅清空对话消息，不会删除情侣记忆、纪念日、个人数据',
      '删除确认',
      { confirmButtonText: '删除选中', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  const uidSet = new Set(selectedUids.value)
  const toDelete = messages.value.filter(m => uidSet.has(m.uid))
  const backendIds = toDelete.filter(m => m.backendId).map(m => m.backendId)

  if (backendIds.length > 0) {
    http.post('/api/agent/messages/batch-delete', backendIds).catch(() => {})
  }

  messages.value = messages.value.filter(m => !uidSet.has(m.uid))
  saveToStorage()
  exitEditMode()
  ElMessage.success(`已删除 ${toDelete.length} 条消息`)
}

// ==================== 清空对话 ====================

const clearAllMessages = async () => {
  try {
    await ElMessageBox.confirm(
      '确定删除选中聊天记录？仅清空对话消息，不会删除情侣记忆、纪念日、个人数据',
      '清空对话',
      { confirmButtonText: '清空全部', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  http.delete('/api/agent/messages').catch(() => {})
  messages.value = []
  saveToStorage()
  ElMessage.success('对话已清空')
}

// ==================== 持久化 ====================

const saveToStorage = () => {
  try {
    const data = messages.value.slice(-MAX_MESSAGES)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  } catch { /* ignore */ }
}

const loadFromStorage = () => {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    if (data) {
      const parsed = JSON.parse(data)
      if (Array.isArray(parsed) && parsed.length > 0) {
        messages.value = parsed.map(msg => ({
          ...msg,
          time: new Date(msg.time)
        }))
        return true
      }
    }
  } catch { /* ignore */ }
  return false
}

// ==================== 后端历史记录 ====================
const historyLoading = ref(false)
const historyHasMore = ref(true)
const historyBeforeId = ref(null)
const showHistory = ref(true)
const historyUids = reactive(new Set())
const historyCount = computed(() => historyUids.size)

const displayMessages = computed(() => {
  if (showHistory.value) return messages.value
  return messages.value.filter(m => !historyUids.has(m.uid))
})

const loadHistory = async (silent) => {
  if (historyLoading.value || !historyHasMore.value) return
  try {
    historyLoading.value = true
    const params = { limit: 30 }
    if (historyBeforeId.value) params.beforeId = historyBeforeId.value
    const res = await http.get('/api/agent/history', { params })
    const rows = res?.rows || []
    historyHasMore.value = res?.hasMore === true
    if (rows.length) {
      const existing = new Set(messages.value.map(m => m.backendId).filter(Boolean))
      const newMsgs = []
      for (const row of rows) {
        if (row.id && existing.has(row.id)) continue
        newMsgs.push({
          uid: nextUid(),
          type: row.type,
          content: row.content,
          intent: row.intent,
          backendId: row.id,
          time: new Date(row.time)
        })
      }
      if (newMsgs.length) {
        newMsgs.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime())
        for (const m of newMsgs) historyUids.add(m.uid)
        messages.value = [...newMsgs, ...messages.value]
        showHistory.value = true
        if (rows.length && rows[0].id) historyBeforeId.value = rows[rows.length - 1].id
      }
    }
    if (!silent && rows.length === 0) ElMessage.info('没有更多历史记录')
    saveToStorage()
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

const toggleHistory = () => {
  showHistory.value = !showHistory.value
  if (!showHistory.value) {
    ElMessage.success('历史记录已收起')
  }
}

// ==================== 滚动 ====================

const scrollToBottom = async () => {
  await nextTick()
  if (chatRef.value) {
    chatRef.value.scrollTop = chatRef.value.scrollHeight
  }
}

// ==================== 发送消息 ====================

const doSend = async (text) => {
  if (!text || sendLock) return
  sendLock = true
  loading.value = true

  const userUid = nextUid()
  messages.value.push({
    uid: userUid,
    type: 'user',
    content: text,
    time: new Date()
  })

  inputMessage.value = ''
  await scrollToBottom()
  saveToStorage()

  try {
    const response = await http.post('/api/agent/chat', { message: text })

    const actions = (response.executedActions || []).filter(a => a && !a.includes('LLM'));
    messages.value.push({
      uid: nextUid(),
      type: 'agent',
      content: response.reply,
      intent: response.intent,
      actions: actions.length ? actions : undefined,
      intentConfidence: response.intentConfidence,
      recognitionSource: response.recognitionSource,
      backendId: response.interactionId || null,
      time: new Date()
    })

    await scrollToBottom()
    saveToStorage()
  } catch (error) {
    ElMessage.error(error?.message || '发送失败，请稍后再试')
    messages.value.push({
      uid: nextUid(),
      type: 'agent',
      content: '抱歉，我遇到了一点问题~ 🥺\n请检查网络后重试。',
      time: new Date()
    })
    saveToStorage()
  } finally {
    loading.value = false
    sendLock = false
  }
}

const sendMessage = () => {
  if (sendLock) return
  const text = inputMessage.value.trim()
  if (!text) return
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debounceTimer = null
    doSend(text)
  }, DEBOUNCE_MS)
}

const useQuickAction = (command) => {
  if (sendLock) return
  inputMessage.value = command
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = null
  doSend(command)
}

// ==================== 生命周期 ====================

onMounted(async () => {
  const hasLocal = loadFromStorage()
  document.addEventListener('click', onGlobalClick)
  if (!hasLocal) {
    await loadHistory(true)
  }
})

onUnmounted(() => {
  saveToStorage()
  if (debounceTimer) clearTimeout(debounceTimer)
  document.removeEventListener('click', onGlobalClick)
})
</script>

<template>
  <div class="agent-chat">
    <!-- ==================== 顶部导航栏 ==================== -->
    <header class="chat-header">
      <div class="header-left">
        <button type="button" class="header-back" aria-label="返回" @click="router.push('/app/home')">
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </button>
        <el-icon :size="22" color="#6366f1"><Promotion /></el-icon>
        <span class="header-title">AI 智能管家</span>
      </div>

      <div class="header-right">
        <!-- 快捷功能 -->
        <el-popover placement="bottom-end" :width="180" trigger="hover" :show-after="200">
          <template #reference>
            <el-button size="small" text>
              <el-icon :size="16"><MoreFilled /></el-icon>
              <span class="btn-label">快捷功能</span>
            </el-button>
          </template>
          <div class="quick-popover">
            <div
              v-for="action in quickActions"
              :key="action.command"
              class="quick-popover-item"
              @click="useQuickAction(action.command)"
            >
              {{ action.label }}
            </div>
          </div>
        </el-popover>

        <!-- 历史记录 -->
        <el-button
          v-if="!editMode"
          size="small"
          text
          :disabled="historyLoading || !historyHasMore"
          @click="loadHistory()"
        >
          <el-icon :size="16"><Refresh /></el-icon>
          <span class="btn-label">历史</span>
        </el-button>

        <!-- 编辑 / 取消 -->
        <el-button
          v-if="!editMode"
          size="small"
          text
          @click="enterEditMode"
        >
          <el-icon :size="16"><Edit /></el-icon>
          <span class="btn-label">编辑</span>
        </el-button>
        <el-button
          v-else
          size="small"
          text
          type="warning"
          @click="exitEditMode"
        >
          <el-icon :size="16"><Close /></el-icon>
          <span class="btn-label">取消</span>
        </el-button>

        <!-- 清空对话 -->
        <el-button
          v-if="!editMode"
          size="small"
          text
          type="danger"
          @click="clearAllMessages"
        >
          <el-icon :size="16"><Delete /></el-icon>
          <span class="btn-label">清空对话</span>
        </el-button>
      </div>
    </header>

    <!-- ==================== 消息列表 / 空状态 ==================== -->
    <div
      ref="chatRef"
      class="chat-messages"
      :class="{ 'is-empty': messages.length === 0 && !loading }"
      @click.self="hideContextMenu"
    >
      <!-- 空状态：无聊天记录 -->
      <div v-if="displayMessages.length === 0 && !loading" class="empty-state">
        <div class="empty-illustration">
          <div class="empty-icon-circle">
            <span class="empty-emoji">💕</span>
          </div>
          <div class="empty-orbit">
            <span class="orbit-dot dot-1">✨</span>
            <span class="orbit-dot dot-2">💝</span>
            <span class="orbit-dot dot-3">🌹</span>
          </div>
        </div>
        <p class="empty-title">我是你的专属智能管家</p>
        <p class="empty-desc">有什么想聊的，随时告诉我吧 ✨</p>
        <div class="empty-hints">
          <span v-for="cmd in inputCommands.slice(0, 3)" :key="cmd.command"
                class="empty-hint" @click="useQuickAction(cmd.command)">
            {{ cmd.label }}
          </span>
        </div>
      </div>

      <!-- 历史记录控制栏 -->
      <div v-if="historyCount > 0" class="history-bar">
        <div class="history-bar-line"></div>
        <div class="history-bar-actions">
          <button v-if="showHistory && historyHasMore" class="hbtn" :disabled="historyLoading" @click="loadHistory()">
            <el-icon v-if="historyLoading" class="spin" :size="12"><Refresh /></el-icon>
            <span>{{ historyLoading ? '加载中…' : '加载更早' }}</span>
          </button>
          <button class="hbtn" @click="toggleHistory">
            <span>{{ showHistory ? '收起历史' : `展开历史 (${historyCount}条)` }}</span>
          </button>
        </div>
        <div class="history-bar-line"></div>
      </div>

      <!-- 收起历史后的展开提示 -->
      <div v-if="!showHistory && historyCount > 0" class="history-collapsed" @click="toggleHistory">
        <span class="hc-ico">📜</span>
        <span>点击展开 {{ historyCount }} 条历史记录</span>
      </div>

      <!-- 消息列表 -->
      <template v-for="(msg, index) in displayMessages" :key="msg.uid">
        <div v-if="shouldShowTime(index)" class="time-divider">
          <span>{{ formatTime(msg.time) }}</span>
        </div>

        <div
          class="message-row"
          :class="[msg.type, { 'edit-mode': editMode }]"
          @contextmenu="(e) => showContextMenu(e, msg, index)"
          @touchstart="(e) => onTouchStart(e, msg, index)"
          @touchend="onTouchEnd"
          @touchmove="onTouchMove"
        >
          <div v-if="editMode" class="message-check" @click="toggleSelect(msg.uid)">
            <el-icon :size="22" :color="selectedUids.includes(msg.uid) ? '#6366f1' : '#cbd5e1'">
              <CircleCheckFilled v-if="selectedUids.includes(msg.uid)" />
              <CircleCheck v-else />
            </el-icon>
          </div>

          <div class="message" :class="msg.type">
            <div class="message-content">
              <div class="sender-tag">{{ msg.type === 'user' ? '我' : 'AI 管家' }}</div>
              <pre class="message-text">{{ msg.content }}</pre>
              <div v-if="msg.actions && msg.actions.length" class="message-actions">
                <el-tag
                  v-for="action in msg.actions"
                  :key="action"
                  size="small"
                  type="success"
                  effect="plain"
                >
                  ✓ {{ action }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 打字指示器 -->
      <div v-if="loading" class="message-row agent">
        <div class="message agent">
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 编辑模式底部操作栏 ==================== -->
    <div v-if="editMode" class="edit-toolbar">
      <el-button size="small" @click="selectAll">
        <el-icon :size="14"><Select /></el-icon> 全选
      </el-button>
      <el-button size="small" @click="deselectAll">取消全选</el-button>
      <span class="edit-count">已选 {{ selectedCount }} 条</span>
      <el-button
        size="small"
        type="danger"
        :disabled="selectedCount === 0"
        @click="batchDelete"
      >
        删除选中
      </el-button>
    </div>

    <!-- ==================== 底部输入区域 ==================== -->
    <div class="chat-footer">
      <!-- Emoji 弹出面板 -->
      <Teleport to="body">
        <div v-if="showEmoji" class="emoji-overlay" @click="showEmoji = false">
          <div class="emoji-panel" @click.stop>
            <div class="emoji-grid">
              <button
                v-for="emoji in commonEmojis"
                :key="emoji"
                class="emoji-item"
                @click="insertEmoji(emoji)"
              >{{ emoji }}</button>
            </div>
          </div>
        </div>
      </Teleport>

      <div class="chat-input-row">
        <!-- 左侧按钮组 -->
        <div class="input-left">
          <!-- 表情按钮 -->
          <button
            type="button"
            class="input-tool-btn"
            :class="{ active: showEmoji }"
            aria-label="表情"
            title="表情"
            @click="showEmoji = !showEmoji"
          >
            <el-icon :size="20"><MagicStick /></el-icon>
          </button>

          <!-- 快捷指令 -->
          <el-popover placement="top-start" :width="200" trigger="click" :show-after="100">
            <template #reference>
              <button type="button" class="input-tool-btn" aria-label="快捷指令" title="快捷指令">
                <el-icon :size="20"><Menu /></el-icon>
              </button>
            </template>
            <div class="quick-popover">
              <div
                v-for="cmd in inputCommands"
                :key="cmd.command"
                class="quick-popover-item"
                @click="useQuickAction(cmd.command)"
              >
                {{ cmd.label }}
              </div>
            </div>
          </el-popover>
        </div>

        <!-- 中间：输入框（自适应高度） -->
        <div class="input-center">
          <el-input
            ref="inputRef"
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="输入消息..."
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
          />
        </div>

        <!-- 右侧：发送按钮 -->
        <button
          type="button"
          class="send-btn"
          :class="{ loading: loading }"
          :disabled="!inputMessage.trim() || loading"
          @click="sendMessage"
        >
          <el-icon v-if="!loading" :size="20"><Promotion /></el-icon>
          <span v-else class="send-spinner"></span>
        </button>
      </div>
    </div>

    <!-- ==================== 上下文菜单（Teleport） ==================== -->
    <Teleport to="body">
      <div
        v-if="ctxMenu.visible"
        class="context-menu-overlay"
        @click="hideContextMenu"
      >
        <div
          class="context-menu"
          :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
          @click.stop
        >
          <div class="context-menu-item" @click="copyMessageText">
            <el-icon :size="14"><CopyDocument /></el-icon>
            <span>复制文本</span>
          </div>
          <!-- 用户消息 → 重发；AI 消息 → 重新生成 -->
          <div
            v-if="ctxMenu.msg?.type === 'user'"
            class="context-menu-item"
            @click="resendMessage"
          >
            <el-icon :size="14"><Refresh /></el-icon>
            <span>重发</span>
          </div>
          <div
            v-if="ctxMenu.msg?.type === 'agent'"
            class="context-menu-item"
            @click="regenerateMessage"
          >
            <el-icon :size="14"><Refresh /></el-icon>
            <span>重新生成</span>
          </div>
          <div class="context-menu-divider"></div>
          <div class="context-menu-item danger" @click="deleteSingleMessage">
            <el-icon :size="14"><Delete /></el-icon>
            <span>删除本条</span>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ==================== 容器 ==================== */
.agent-chat {
  display: flex;
  flex-direction: column;
  min-height: min(600px, calc(100vh - 120px));
  height: min(600px, calc(100vh - 120px));
  background: linear-gradient(135deg, #f0f0f5 0%, #e8ecf8 100%);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
}

/* ==================== 顶部导航栏 ==================== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(139, 92, 246, 0.12);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-back {
  border: 0;
  background: transparent;
  padding: 4px;
  border-radius: 10px;
  color: rgba(17, 24, 39, 0.45);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.header-back:hover {
  background: rgba(139, 92, 246, 0.1);
  color: #5b6af0;
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  background: linear-gradient(135deg, #8b5cf6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.btn-label {
  margin-left: 4px;
  font-size: 13px;
}

/* ==================== 快捷功能 Popover ==================== */
.quick-popover {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quick-popover-item {
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  color: var(--app-text);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.quick-popover-item:hover {
  background: rgba(139, 92, 246, 0.08);
  color: #8b5cf6;
}

/* ==================== 时间分隔线 ==================== */
.time-divider {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}

.time-divider span {
  font-size: 12px;
  color: rgba(17, 24, 39, 0.38);
  background: rgba(17, 24, 39, 0.06);
  padding: 4px 14px;
  border-radius: 12px;
  letter-spacing: 0.02em;
}

/* ==================== 消息列表 ==================== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scroll-behavior: smooth;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 82%;
  animation: msgIn 0.18s ease-out;
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.message-row.agent {
  align-self: flex-start;
}

.message-row.edit-mode {
  cursor: pointer;
}

.message-check {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-top: 10px;
  cursor: pointer;
}

.message {
  display: flex;
  max-width: 100%;
}

/* ==================== 聊天气泡 ==================== */

.message-content {
  padding: 12px 16px;
  line-height: 1.6;
  font-size: 14px;
  transition: box-shadow 0.15s;
  /* 长消息自动换行 */
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: pre-wrap;
}

/* 用户气泡：右对齐 / #8b5cf6 / 白色文字 / 圆角 18-18-4-18 */
.message.user .message-content {
  background: #8b5cf6;
  color: #FFFFFF;
  border-radius: 18px 18px 4px 18px;
  box-shadow: 0 2px 10px rgba(139, 92, 246, 0.18);
}

/* AI 气泡：左对齐 / 白色背景 / 边框 #E0E0E8 / 圆角 18-18-18-4 */
.message.agent .message-content {
  background: #FFFFFF;
  color: rgba(17, 24, 39, 0.9);
  border: 1px solid #E0E0E8;
  border-radius: 18px 18px 18px 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

/* 发送者标签 */
.sender-tag {
  font-size: 11px;
  font-weight: 800;
  margin-bottom: 6px;
  opacity: 0.6;
  letter-spacing: 0.03em;
}
.message.user .sender-tag {
  text-align: right;
  color: rgba(255, 255, 255, 0.75);
}
.message.agent .sender-tag {
  color: rgba(139, 92, 246, 0.7);
}

.message-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  font-family: inherit;
  /* 超长连续字符（URL/代码）强制断行 */
  overflow-wrap: break-word;
  word-break: break-word;
  white-space: pre-wrap;
}

/* 用户气泡内链接颜色 */
.message.user .message-text a {
  color: rgba(255, 255, 255, 0.92);
}

.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ==================== 打字指示器 ==================== */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
  align-items: center;
  justify-content: center;
}
.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #8b5cf6;
  animation: typing 1.4s ease-in-out infinite;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: scale(0.7); opacity: 0.4; }
  30% { transform: scale(1); opacity: 1; }
}

/* ==================== 编辑模式底部操作栏 ==================== */
.edit-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(139, 92, 246, 0.12);
}

.edit-count {
  margin-left: auto;
  font-size: 13px;
  color: var(--app-muted);
}

/* ==================== 空状态 ==================== */
.chat-messages.is-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 40px 32px;
  animation: emptyIn 0.4s ease-out;
}

@keyframes emptyIn {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.empty-illustration {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 24px;
}

.empty-icon-circle {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-emoji {
  font-size: 48px;
  line-height: 1;
}

.empty-orbit {
  position: absolute;
  inset: -20px;
  animation: orbitSpin 12s linear infinite;
}

@keyframes orbitSpin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.orbit-dot {
  position: absolute;
  font-size: 18px;
  line-height: 1;
}
.dot-1 { top: 0; left: 50%; transform: translateX(-50%); }
.dot-2 { right: 0; bottom: 30%; }
.dot-3 { bottom: 0; left: 30%; }

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.78);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: rgba(17, 24, 39, 0.48);
  margin: 0 0 24px;
  line-height: 1.5;
}

.empty-hints {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.empty-hint {
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(139, 92, 246, 0.08);
  color: #8b5cf6;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.empty-hint:hover {
  background: rgba(139, 92, 246, 0.16);
}

/* ==================== 底部输入区 ==================== */
.chat-footer {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(139, 92, 246, 0.1);
}

.chat-input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 16px 14px;
}

/* ---- 左侧工具按钮 ---- */
.input-left {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 2px;
}

.input-tool-btn {
  border: 0;
  background: transparent;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(17, 24, 39, 0.42);
  cursor: pointer;
  transition: all 0.15s;
}
.input-tool-btn:hover,
.input-tool-btn.active {
  background: rgba(139, 92, 246, 0.08);
  color: #8b5cf6;
}

/* ---- 中间输入框 ---- */
.input-center {
  flex: 1;
}

.input-center :deep(.el-textarea__inner) {
  border-radius: 20px;
  padding: 10px 18px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  border-color: #E0E0E8;
  background: rgba(139, 92, 246, 0.03);
  transition: all 0.2s;
}
.input-center :deep(.el-textarea__inner):focus {
  border-color: #8b5cf6;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}
.input-center :deep(.el-textarea__inner)::placeholder {
  color: rgba(17, 24, 39, 0.32);
}

/* ---- 右侧发送按钮 ---- */
.send-btn {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: 50%;
  background: #8b5cf6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
}
.send-btn:hover:not(:disabled) {
  background: #7c3aed;
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);
  transform: scale(1.04);
}
.send-btn:active:not(:disabled) {
  transform: scale(0.96);
}
.send-btn:disabled {
  background: #d4d8f0;
  box-shadow: none;
  cursor: not-allowed;
}

/* 发送按钮 loading 动画 */
.send-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: sendSpin 0.6s linear infinite;
}

@keyframes sendSpin {
  to { transform: rotate(360deg); }
}

/* ==================== Emoji 面板 ==================== */
.emoji-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: transparent;
}

.emoji-panel {
  position: fixed;
  bottom: 160px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
  animation: emojiIn 0.15s ease-out;
}

@keyframes emojiIn {
  from { opacity: 0; transform: translateX(-50%) translateY(8px); }
  to   { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.emoji-item {
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.12s;
}
.emoji-item:hover {
  background: rgba(139, 92, 246, 0.1);
}

/* ==================== 历史记录控制栏 ==================== */
.history-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}
.history-bar-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.14), transparent);
}
.history-bar-actions {
  display: flex;
  gap: 6px;
}
.hbtn {
  border: 1px solid rgba(139, 92, 246, 0.14);
  background: rgba(255, 255, 255, 0.72);
  color: rgba(139, 92, 246, 0.78);
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 11px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.hbtn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(139, 92, 246, 0.22);
  color: #5b6af0;
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.1);
}
.hbtn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* 收起后的展开提示 */
.history-collapsed {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 0;
  color: rgba(139, 92, 246, 0.6);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  border-radius: 12px;
}
.history-collapsed:hover {
  color: #5b6af0;
  background: rgba(139, 92, 246, 0.04);
}
.hc-ico {
  font-size: 18px;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==================== 上下文菜单 ==================== */
.context-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: transparent;
}

.context-menu {
  position: fixed;
  z-index: 10000;
  min-width: 164px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px;
  box-shadow:
    0 12px 36px rgba(0, 0, 0, 0.1),
    0 2px 6px rgba(0, 0, 0, 0.04);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  animation: ctxIn 0.12s ease-out;
}

@keyframes ctxIn {
  from { opacity: 0; transform: scale(0.92); }
  to   { opacity: 1; transform: scale(1); }
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  color: rgba(17, 24, 39, 0.78);
  cursor: pointer;
  transition: all 0.12s;
  white-space: nowrap;
}
.context-menu-item:hover {
  background: rgba(139, 92, 246, 0.07);
  color: #8b5cf6;
}
.context-menu-item.danger {
  color: rgba(239, 68, 68, 0.78);
}
.context-menu-item.danger:hover {
  background: rgba(239, 68, 68, 0.06);
  color: #ef4444;
}

.context-menu-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 4px 8px;
}
</style>

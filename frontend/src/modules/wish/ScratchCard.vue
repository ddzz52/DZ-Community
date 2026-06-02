<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  content: { type: String, default: '' },
  revealed: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['done', 'progress'])

const wrapRef = ref(null)
const canvasRef = ref(null)
const ready = ref(false)
const scratching = ref(false)
const done = ref(false)
const fading = ref(false)
const ratio = ref(0)

let dpr = 1
let ctx = null
let w = 0
let h = 0
let raf = 0
let lastPt = null
let lastEvalAt = 0
let capturedPointerId = null
let fadeTimer = 0

const showOverlay = computed(() => !props.revealed && !done.value)

const drawCover = () => {
  const c = canvasRef.value
  if (!c) return
  ctx = c.getContext('2d', { willReadFrequently: true })
  if (!ctx) return
  const el = wrapRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const cw = Math.max(240, Math.floor(rect.width))
  const ch = Math.max(120, Math.floor(rect.height))
  dpr = Math.max(1, Math.floor(window.devicePixelRatio || 1))
  w = cw
  h = ch
  c.width = cw * dpr
  c.height = ch * dpr
  c.style.width = `${cw}px`
  c.style.height = `${ch}px`
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.globalCompositeOperation = 'source-over'
  const g = ctx.createLinearGradient(0, 0, cw, ch)
  g.addColorStop(0, '#94a3b8')
  g.addColorStop(1, '#cbd5e1')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, cw, ch)

  ctx.save()
  ctx.globalAlpha = 0.085
  ctx.fillStyle = 'rgba(255,255,255,1)'
  const stepX = 34
  const stepY = 28
  for (let y = 18; y < ch; y += stepY) {
    for (let x = 18; x < cw; x += stepX) {
      drawHeart(ctx, x + ((y / stepY) % 2) * 10, y, 8)
    }
  }
  ctx.restore()

  ctx.save()
  ctx.translate(cw * 0.5, ch * 0.5)
  ctx.rotate(-0.06)
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.font = '900 16px system-ui, -apple-system, Segoe UI, Roboto, PingFang SC, Microsoft YaHei'
  ctx.fillStyle = 'rgba(15, 23, 42, 0.26)'
  ctx.fillText('刮开你的专属心愿✨', 0, 0)
  ctx.restore()
  ready.value = true
}

const drawHeart = (ctx, x, y, size) => {
  const s = size
  ctx.beginPath()
  ctx.moveTo(x, y + s * 0.35)
  ctx.bezierCurveTo(x, y, x - s, y, x - s, y + s * 0.35)
  ctx.bezierCurveTo(x - s, y + s * 0.75, x - s * 0.2, y + s * 1.05, x, y + s * 1.2)
  ctx.bezierCurveTo(x + s * 0.2, y + s * 1.05, x + s, y + s * 0.75, x + s, y + s * 0.35)
  ctx.bezierCurveTo(x + s, y, x, y, x, y + s * 0.35)
  ctx.closePath()
  ctx.fill()
}

const getPoint = (ev) => {
  const el = wrapRef.value
  if (!el) return null
  const rect = el.getBoundingClientRect()
  const x = (ev.clientX || 0) - rect.left
  const y = (ev.clientY || 0) - rect.top
  return { x: Math.max(0, Math.min(w, x)), y: Math.max(0, Math.min(h, y)) }
}

const brush = (p) => {
  if (!ctx || !p) return
  const r = Math.max(14, Math.min(24, Math.round(Math.min(w, h) * 0.08)))
  ctx.globalCompositeOperation = 'destination-out'
  ctx.beginPath()
  ctx.arc(p.x, p.y, r, 0, Math.PI * 2)
  ctx.fill()
  if (lastPt) {
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.lineWidth = r * 2
    ctx.beginPath()
    ctx.moveTo(lastPt.x, lastPt.y)
    ctx.lineTo(p.x, p.y)
    ctx.stroke()
  }
  lastPt = p
}

const estimateRatio = () => {
  if (!ctx || !canvasRef.value) return 0
  const now = Date.now()
  if (now - lastEvalAt < 240) return ratio.value
  lastEvalAt = now
  const step = 6
  const img = ctx.getImageData(0, 0, w, h).data
  let cleared = 0
  let total = 0
  for (let y = 0; y < h; y += step) {
    for (let x = 0; x < w; x += step) {
      const idx = (y * w + x) * 4 + 3
      total += 1
      if (img[idx] < 30) cleared += 1
    }
  }
  const r = total > 0 ? cleared / total : 0
  ratio.value = r
  emit('progress', r)
  return r
}

const finish = async () => {
  if (done.value) return
  fading.value = false
  done.value = true
  scratching.value = false
  if (raf) cancelAnimationFrame(raf)
  raf = 0
  try {
    const c = canvasRef.value
    if (c && capturedPointerId != null) c.releasePointerCapture(capturedPointerId)
  } catch (e) {}
  capturedPointerId = null
  await nextTick()
  emit('done')
}

const fadeOutAndFinish = () => {
  if (done.value || fading.value) return
  fading.value = true
  scratching.value = false
  lastPt = null
  if (raf) cancelAnimationFrame(raf)
  raf = 0
  try {
    const c = canvasRef.value
    if (c && capturedPointerId != null) c.releasePointerCapture(capturedPointerId)
  } catch (e) {}
  capturedPointerId = null
  if (fadeTimer) window.clearTimeout(fadeTimer)
  fadeTimer = window.setTimeout(() => {
    fadeTimer = 0
    finish()
  }, 220)
}

const onDown = (ev) => {
  if (props.disabled || done.value || fading.value) return
  if (!ready.value) return
  try {
    const c = canvasRef.value
    if (c && typeof ev.pointerId === 'number') {
      c.setPointerCapture(ev.pointerId)
      capturedPointerId = ev.pointerId
    }
  } catch (e) {}
  scratching.value = true
  lastPt = null
  const p = getPoint(ev)
  brush(p)
  estimateRatio()
}

const onMove = (ev) => {
  if (props.disabled || done.value || fading.value) return
  if (!ready.value) return
  if (!scratching.value) {
    const btn = typeof ev.buttons === 'number' ? ev.buttons : 0
    if (btn === 1 || btn === 4 || btn === 2) {
      scratching.value = true
      lastPt = null
    } else {
      return
    }
  }
  if (raf) cancelAnimationFrame(raf)
  const p = getPoint(ev)
  raf = requestAnimationFrame(() => {
    brush(p)
    const r = estimateRatio()
    if (r >= 0.8) fadeOutAndFinish()
  })
}

const onUp = () => {
  if (!scratching.value) return
  scratching.value = false
  lastPt = null
  try {
    const c = canvasRef.value
    if (c && capturedPointerId != null) c.releasePointerCapture(capturedPointerId)
  } catch (e) {}
  capturedPointerId = null
  const r = estimateRatio()
  if (r >= 0.8) fadeOutAndFinish()
}

const reset = () => {
  done.value = false
  fading.value = false
  scratching.value = false
  ratio.value = 0
  lastPt = null
  lastEvalAt = 0
  ready.value = false
  if (fadeTimer) window.clearTimeout(fadeTimer)
  fadeTimer = 0
  drawCover()
}

watch(
  () => [props.revealed, props.disabled],
  () => {
    if (props.revealed) {
      done.value = true
      fading.value = false
      scratching.value = false
    } else {
      reset()
    }
  },
  { immediate: true }
)

onMounted(() => {
  drawCover()
  const ro = new ResizeObserver(() => {
    if (props.disabled) return
    reset()
  })
  if (wrapRef.value) ro.observe(wrapRef.value)
  onBeforeUnmount(() => ro.disconnect())
})

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
  if (fadeTimer) window.clearTimeout(fadeTimer)
  fadeTimer = 0
})
</script>

<template>
  <div ref="wrapRef" class="sc">
    <div class="base">
      <div class="btitle">心愿</div>
      <div class="bcontent">{{ content }}</div>
    </div>
    <canvas
      v-if="showOverlay"
      ref="canvasRef"
      class="cover"
      :class="{ disabled: props.disabled, scratching: scratching, fading: fading }"
      @pointerdown.prevent="onDown"
      @pointermove.prevent="onMove"
      @pointerup.prevent="onUp"
      @pointercancel.prevent="onUp"
      @pointerenter.prevent="onMove"
    />
    <div v-if="scratching && !done" class="fx" />
  </div>
</template>

<style scoped>
.sc {
  position: relative;
  width: 100%;
  height: 160px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(139, 92, 246, 0.12), rgba(59, 130, 246, 0.08)),
    rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}
.base {
  position: absolute;
  inset: 0;
  padding: 14px 14px 12px;
  display: grid;
  gap: 10px;
  align-content: center;
  text-align: center;
}
.btitle {
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.2px;
  color: rgba(17, 24, 39, 0.6);
}
.bcontent {
  font-size: 18px;
  font-weight: 950;
  line-height: 1.35;
  color: rgba(17, 24, 39, 0.88);
  word-break: break-word;
  padding: 0 8px;
}
.cover {
  position: absolute;
  inset: 0;
  touch-action: none;
  cursor: grab;
  opacity: 1;
  transition: opacity 0.22s ease, filter 0.22s ease;
}
.cover.scratching {
  cursor: grabbing;
}
.cover.disabled {
  pointer-events: none;
}
.cover.fading {
  opacity: 0;
  filter: blur(0.2px);
  pointer-events: none;
}
.fx {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(115deg, transparent 0%, rgba(255, 255, 255, 0.18) 42%, transparent 70%);
  animation: shimmer 0.9s linear infinite;
}
@keyframes shimmer {
  from {
    transform: translateX(-20%);
  }
  to {
    transform: translateX(20%);
  }
}
</style>

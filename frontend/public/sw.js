/**
 * DZ-Community Service Worker — PWA 离线缓存
 * 安全策略：
 *   - 只缓存前端静态文件（JS/CSS/图片/字体）
 *   - 绝不缓存 API 请求（不碰用户数据、Token、聊天记录）
 *   - SW 更新时自动激活新版本
 */

const CACHE_NAME = 'dz-community-v1'

// 安装时预缓存核心静态资源
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll([
        '/',
        '/index.html',
        '/favicon.svg',
        '/icons.svg',
        '/manifest.json'
      ])
    })
  )
  // 立即激活，不等待旧 SW 关闭
  self.skipWaiting()
})

// 激活时清理旧缓存版本
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
      )
    })
  )
  // 立即接管所有页面
  self.clients.claim()
})

// 请求拦截：网络优先（HTML）+ 缓存兜底（静态资源）+ 跳过 API
self.addEventListener('fetch', (event) => {
  const url = event.request.url
  const method = event.request.method

  // 跳过非 GET 请求（POST/PUT/DELETE 不缓存）
  if (method !== 'GET') return

  // 跳过 API 请求 — 绝不缓存任何用户数据
  if (url.includes('/api/') || url.includes('/ws/')) return

  // 跳过 Chrome 扩展请求
  if (url.startsWith('chrome-extension://')) return

  // HTML 请求：网络优先，离线时回退到缓存
  if (event.request.mode === 'navigate' || url.endsWith('/') || url.endsWith('.html')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          const clone = response.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone))
          return response
        })
        .catch(() => caches.match(event.request))
    )
    return
  }

  // 静态资源（JS/CSS/图片/字体）：缓存优先，未命中时走网络
  if (
    url.endsWith('.js') ||
    url.endsWith('.css') ||
    url.endsWith('.svg') ||
    url.endsWith('.png') ||
    url.endsWith('.jpg') ||
    url.endsWith('.webp') ||
    url.endsWith('.woff2') ||
    url.endsWith('.woff')
  ) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        const fetched = fetch(event.request).then((response) => {
          const clone = response.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone))
          return response
        })
        return cached || fetched
      })
    )
    return
  }
})

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    // 代码分割：把大依赖拆成独立 chunk，加快首屏
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          'vendor': ['vue', 'vue-router', 'pinia', 'axios']
        }
      }
    },
    // 小于 4KB 的资源内联为 base64，减少 HTTP 请求
    assetsInlineLimit: 4096,
    // 生成 sourcemap 仅用于排查，生产可关闭
    sourcemap: false,
    // chunk 大小警告阈值
    chunkSizeWarningLimit: 600
  },
  server: {
    // 开发时开启 gzip 模拟
    proxy: {}
  }
})

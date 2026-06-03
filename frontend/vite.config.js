import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    // 代码分割：把大依赖拆成独立 chunk，加快首屏
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/element-plus') || id.includes('node_modules/@element-plus')) return 'element-plus'
          if (id.includes('node_modules/vue') || id.includes('node_modules/vue-router') || id.includes('node_modules/pinia') || id.includes('node_modules/axios')) return 'vendor'
        }
      }
    },
    assetsInlineLimit: 4096,
    sourcemap: false,
    chunkSizeWarningLimit: 600
  },
  server: {
    // 开发时开启 gzip 模拟
    proxy: {}
  }
})

import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Lab 5 Step 2：@/... 指向 src
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 测试产物不参与 HMR，否则 Playwright 报告目录会触发页面无限重载
    watch: {
      ignored: ['**/playwright-report/**', '**/test-results/**'],
    },
    // 开发期把 /api 代理到 Lab 4 后端，避免浏览器跨域；生产由网关/同域部署处理
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});

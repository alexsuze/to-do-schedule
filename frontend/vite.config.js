import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { quasar, transformAssetUrls } from '@quasar/vite-plugin'

// GitHub Pages 部署於 https://<user>.github.io/<repo>/，需設定 base
// 本機開發時 VITE_BASE_PATH 未設定則為 '/'
export default defineConfig(({ mode }) => ({
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [
    vue({ template: { transformAssetUrls } }),
    quasar({ sassVariables: fileURLToPath(new URL('./src/css/quasar-variables.sass', import.meta.url)) })
  ],
  server: { port: 5173 }
}))

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { Quasar, Notify, Dialog, Dark } from 'quasar'
import quasarLang from 'quasar/lang/zh-TW'
import '@quasar/extras/material-icons/material-icons.css'
import 'quasar/src/css/index.sass'
import './css/app.css'

import App from './App.vue'
import router from './router'
import { savedTheme } from './utils/theme'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Quasar, {
  plugins: { Notify, Dialog, Dark },
  // 沒選過就跟隨系統
  config: { dark: savedTheme() ?? 'auto' },
  lang: quasarLang
})
app.mount('#app')

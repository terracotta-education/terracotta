import Vue from 'vue'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import store from './store/index'
import VueRouterBackButton from 'vue-router-back-button'
import { TiptapVuetifyPlugin } from 'tiptap-vuetify'

Vue.config.productionTip = false

Vue.use(VueRouterBackButton, { router })
Vue.use(TiptapVuetifyPlugin, {vuetify, iconsGroup: 'mdi'})

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const tokenParam = params.get('token');

if (tokenParam) {
  store.dispatch('api/setLtiToken',tokenParam).then(startVue)
} else {
  startVue()
}

function startVue() {
  // always start with clean experiment/s list
  store.dispatch('experiment/resetExperiment')
  store.dispatch('experiment/resetExperiments')
  store.dispatch('consent/resetConsent')

  cleanURL()

  new Vue({
    store,
    router,
    vuetify,
    render: h => h(App),
  }).$mount('#app')
}

function cleanURL() {
  // delete the token from the url
  params.delete('token')
  // update the url without the token param
  window.history.replaceState(
      {},
      '',
      `${window.location.pathname}?${params}${window.location.hash}`,
  )
}
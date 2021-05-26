import Vue from 'vue'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import store from './store/index'
import VueRouterBackButton from 'vue-router-back-button'

Vue.config.productionTip = false

Vue.use(VueRouterBackButton, { router })

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const tokenParam = params.get('token');

if (tokenParam) {
  store.dispatch('api/setLtiToken',tokenParam).then(startVue)
} else {
  startVue()
}

function startVue() {
  cleanURL()

  new Vue({
    store,
    router,
    vuetify,
    render: h => h(App),
  }).$mount('#app')
}

function cleanURL() {
  params.delete('token')
  window.history.replaceState(
      {},
      '',
      `${window.location.pathname}?${params}${window.location.hash}`,
  )
}
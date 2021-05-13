import Vue from 'vue'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import VueRouterBackButton from 'vue-router-back-button'

Vue.use(VueRouterBackButton, { router })

// Vuex store
import store from './store/index'

Vue.config.productionTip = false

new Vue({
  store,
  router,
  vuetify,
  render: h => h(App)
}).$mount('#app')

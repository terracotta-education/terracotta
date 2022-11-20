import Vue from 'vue'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import store from './store/index'
import VueRouterBackButton from 'vue-router-back-button'
import { TiptapVuetifyPlugin } from 'tiptap-vuetify'
import VueSweetalert2 from 'vue-sweetalert2';

Vue.config.productionTip = false

Vue.use(VueRouterBackButton, { router })
Vue.use(TiptapVuetifyPlugin, {vuetify, iconsGroup: 'mdi'})
Vue.use(VueSweetalert2);

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const tokenParam = params.get('token');
const lmsApiOAuthURL = params.get("lms_api_oauth_url");

const operations = [];

if (tokenParam) {
  operations.push(store.dispatch('api/setLtiToken',tokenParam));
}
operations.push(store.dispatch('api/setLmsApiOAuthURL', lmsApiOAuthURL));
Promise.all(operations).then(startVue);

function startVue() {
  // always start with clean experiment/s list
  store.dispatch('experiment/resetExperiment')
  store.dispatch('experiment/resetExperiments')
  store.dispatch('consent/resetConsent')

  cleanURL()

  if (lmsApiOAuthURL) {
    router.replace({name: "oauth2-redirect"});
  }
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
  params.delete('lms_api_oauth_url');
  // update the url without the token param
  window.history.replaceState(
      {},
      '',
      `${window.location.pathname}?${params}${window.location.hash}`,
  )
}

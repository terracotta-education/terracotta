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
const targetLinkUriParam = params.get("targetLinkUri");

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

  const vue = new Vue({
    store,
    router,
    vuetify,
    render: (h) => h(App),
  }).$mount("#app");

  if (targetLinkUriParam) {
    // console.log("has targetLinkUri", targetLinkUriParam);
    const targetLinkUrl = new URL(targetLinkUriParam);
    const targetLinkUriParams = new URLSearchParams(targetLinkUrl.search);
    const platformRedirectUrl = targetLinkUriParams.get(
      "platform_redirect_url"
    );
    if (platformRedirectUrl) {
      // console.log('has platformRedirectUrl', platformRedirectUrl);
      vue.$router.replace({
        name: "first-party",
        params: { platformRedirectUrl },
      });
    }
  }
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

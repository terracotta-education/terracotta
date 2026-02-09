import DeepLink from "./DeepLink.vue";
import router from "./router";
import store from "./store/index";
import Vue from "vue";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);

new Vue({
  router,
  store,
  vuetify,
  render: (h) =>
    h(DeepLink, {
      props: {
        id: params.get("id"),
      },
    }),
}).$mount("#app");

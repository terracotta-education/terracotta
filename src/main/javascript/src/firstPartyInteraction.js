import Vue from "vue";
import FirstParty from "./FirstParty.vue";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const platformRedirectUrl = params.get("platform_redirect_url");

new Vue({
  vuetify,
  render: (h) =>
    h(FirstParty, {
      props: {
        platformRedirectUrl,
      },
    }),
}).$mount("#app");

import FirstParty from "./FirstParty.vue";
import Vue from "vue";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false;

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const targetLinkUri = params.get("targetLinkUri");
const targetLinkURL = new URL(targetLinkUri);
const targetLinkUriParams = new URLSearchParams(targetLinkURL.search);
const platformRedirectUrl = targetLinkUriParams.get("platform_redirect_url");
const assignmentId = targetLinkUriParams.get("assignment");

new Vue({
  vuetify,
  render: (h) =>
    h(FirstParty, {
      props: {
        platformRedirectUrl,
        assignmentId,
      },
    }),
}).$mount("#app");

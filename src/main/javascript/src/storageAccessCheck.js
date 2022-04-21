import Vue from "vue";
import vuetify from "./plugins/vuetify";
import StorageAccessCheck from "./StorageAccessCheck.vue";

Vue.config.productionTip = false;

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const oicdEndpointComplete = params.get("oicdEndpointComplete");
const targetLinkUri = params.get("targetLinkUri");
const iss = params.get("iss");
const loginHint = params.get("login_hint");
const clientId = params.get("client_id");
const ltiMessageHint = params.get("lti_message_hint");
const ltiDeploymentId = params.get("lti_deployment_id");

// TODO: maybe to hasStorageAccess check here? No need to load the UI if we already have storage access.
// TODO: or maybe even put it into the template so that it happens before the other JS loads?

new Vue({
  vuetify,
  render: (h) =>
    h(StorageAccessCheck, {
      props: {
        oicdEndpointComplete,
        targetLinkUri,
        iss,
        loginHint,
        clientId,
        ltiMessageHint,
        ltiDeploymentId,
      },
    }),
}).$mount("#app");

import Vue from "vue";
import vuetify from "./plugins/vuetify";
import StorageAccessRequest from "./StorageAccessRequest.vue";

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

function startVue() {
  new Vue({
    vuetify,
    render: (h) =>
      h(StorageAccessRequest, {
        props: {
          targetLinkUri,
          iss,
          loginHint,
          clientId,
          ltiMessageHint,
          ltiDeploymentId,
        },
      }),
  }).$mount("#app");
}

function continueOIDCLogin() {
  if (oicdEndpointComplete) {
    window.location.replace(oicdEndpointComplete);
  }
}

if (document.hasStorageAccess) {
  document
    .hasStorageAccess()
    .then((hasStorageAccess) => {
      if (hasStorageAccess) {
        continueOIDCLogin();
      } else {
        // Show UI that will request storage access
        startVue();
        console.log("Does not have storage access");
      }
    })
    .catch((error) => {
      console.error(error);
    });
} else {
  continueOIDCLogin();
}

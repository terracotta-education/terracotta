import App from "./App.vue";
import router from "./router";
import store from "./store/index";
import Vue from "vue";
import VueRouterBackButton from "vue-router-back-button";
import VueSweetalert2 from "vue-sweetalert2";
import vuetify from "./plugins/vuetify";

Vue.config.productionTip = false

Vue.use(VueRouterBackButton, { router })
Vue.use(VueSweetalert2);

const url = new URL(window.location.href);
const params = new URLSearchParams(url.search);
const tokenParam = params.get("token");
const lmsApiOAuthURL = params.get("lms_api_oauth_url");
const integration = {
  integration: params.get("integration") === "true",
  status: params.get("status"),
  preview: params.get("preview") === "true",
  client: params.get("client") === "null" ? null : params.get("client"),
  launchToken: params.get("launch_token"),
  score: params.get("score") === "" || params.get("score") === "null" ? null : params.get("score"),
  url: params.get("url"),
  errorCode: params.get("errorCode") === "null" ? null : params.get("errorCode"),
  previewUrl: params.get("previewUrl")
};
var appProps = {};

const operations = [];

if (tokenParam) {
  operations.push(store.dispatch("api/setLtiToken", tokenParam));
}

operations.push(store.dispatch("api/setLmsApiOAuthURL", decodeURIComponent(lmsApiOAuthURL)));
Promise.all(operations).then(startVue);

function startVue() {
  // always start with clean experiment/s list
  store.dispatch("experiment/resetExperiment")
  store.dispatch("experiment/resetExperiments")
  store.dispatch("consent/resetConsent")

  cleanURL()

  if (lmsApiOAuthURL) {
    router.replace({name: "oauth2-redirect"});
  }

  if (integration.integration) {
    appProps["integrationData"] = integration;
  }

  new Vue({
    store,
    router,
    vuetify,
    props: ["integrationData"],
    render: h => h(App, {props: appProps}),
  }).$mount("#app")
}

function cleanURL() {
  // delete the token from the url
  for (const key of [...params.keys()]) {
    params.delete(key);
  }

  // update the url without the params
  window.history.replaceState(
      {},
      "",
      `${window.location.pathname}?${params}${window.location.hash}`,
  )
}

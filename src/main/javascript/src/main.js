import { createApp } from "vue";
import { api } from "@/store/api.module";
import { alert as alertStore } from "@/store/alert.module";
import { experiment as experimentStore } from "@/store/experiment.module";
import { consent as consentStore } from "@/store/consent.module";
import { configuration as configurationStore } from "@/store/configuration.module";
import { pinia } from "@/pinia";
import { watchForLmsReauthorization } from "@/helpers/lms-reauthorization";
import Swal from "sweetalert2";

import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";

import "vuetify/styles";
import "sweetalert2/dist/sweetalert2.min.css";
import "@/styles/custom.scss";

import "@mdi/font/css/materialdesignicons.css";

const url = new URL(window.location.href);
const params = url.searchParams;

const getBooleanParam = name => params.get(name) === "true";

const getNullableParam = name => {
  const value = params.get(name);

  if (value === null || value === "" || value === "null") {
    return null;
  }

  return value;
};

// delivered via sessionStorage rather than a URL param - see lti3Launch.html/Lti3Controller#home
// for why (avoids exceeding Tomcat's request-header size limit for large LTI launch payloads).
// Read once and removed immediately so a later reload of this same tab doesn't replay them.
const tokenParam = sessionStorage.getItem("ltiToken");
sessionStorage.removeItem("ltiToken");
const lmsApiOAuthURL = sessionStorage.getItem("lmsApiOAuthUrl");
sessionStorage.removeItem("lmsApiOAuthUrl");

const integration = {
  integration: getBooleanParam("integration"),
  status: params.get("status"),
  preview: getBooleanParam("preview"),
  client: getNullableParam("client"),
  launchToken: params.get("launch_token"),
  score: getNullableParam("score"),
  url: params.get("url"),
  errorCode: getNullableParam("errorCode"),
  previewUrl: params.get("previewUrl"),
  moreAttemptsAvailable: getBooleanParam("moreAttemptsAvailable"),
  errorMessage: getNullableParam("errorMessage")
};

const obsolete = {
  obsolete: getBooleanParam("obsolete"),
  type: params.get("type")
};

const treatmentPreview = {
  preview: getBooleanParam("treatmentPreview"),
  experimentId: params.get("experiment"),
  conditionId: params.get("condition"),
  treatmentId: params.get("treatment"),
  previewId: params.get("previewId"),
  ownerId: params.get("ownerId"),
  complete: getBooleanParam("complete")
};

const appProps = {};

const initializeStore = async () => {
  const operations = [];

  if (tokenParam) {
        operations.push(api(pinia).setLtiToken(tokenParam));
  }

  if (lmsApiOAuthURL) {
    // no decodeURIComponent needed here - this arrives via sessionStorage as the raw URL, never
    // URL-encoded (unlike the old query-param delivery this replaced)
    operations.push(
      api(pinia).setLmsApiOAuthURL(lmsApiOAuthURL)
    );
  }

  await Promise.all(operations);
};

const resetInitialState = () => {
  experimentStore(pinia).resetExperiment();
  experimentStore(pinia).resetExperiments();
  consentStore(pinia).resetConsent();
};

const configureAppProps = () => {
  if (integration.integration) {
    appProps.integrationData = integration;
  }

  if (obsolete.obsolete) {
    appProps.obsoleteData = obsolete;
  }

  if (treatmentPreview.preview) {
    appProps.treatmentPreviewData = treatmentPreview;
  }
};

const registerRouteGuards = () => {
  router.beforeEach((to, from, next) => {
    const aStore = alertStore();

    if (to.params?.alertMessage) {
      const actionName = to.params.alertType || aStore.statuses.info;
      aStore[actionName](to.params.alertMessage);
    } else if (aStore.hasAlert) {
      if (aStore.pendingClear) {
        aStore.clear();
      } else {
        aStore.pendingClear = true;
      }
    }

    configurationStore().update({
      name: "showSkipLink",
      value: false
    });

    next();
  });
};

const cleanURL = () => {
  window.history.replaceState(
    {},
    "",
    `${window.location.pathname}${window.location.hash}`
  );
};

const startVue = () => {
  resetInitialState();
  configureAppProps();
  cleanURL();
  registerRouteGuards();
  watchForLmsReauthorization({
    isInstructor: () => api(pinia).userInfo === "Instructor",
    notify: async () => {
      // the configuration loads alongside the app's first requests, so it may not be here yet
      const configurations = configurationStore(pinia);

      if (!configurations.hasConfigurations) {
        await configurations.retrieve();
      }

      const lmsTitle = configurations.get?.lmsTitle || "LMS";

      Swal.fire({
        icon: "warning",
        text: `Terracotta has lost its connection to your ${lmsTitle} account. Relaunch Terracotta from your course to reconnect it.`
      });
    }
  });

  createApp(App, appProps)
    .use(pinia)
    .use(router)
    .use(vuetify)
    .mount("#app");

  // a pending LMS authorization is routed to oauth2-redirect by the router's own guard, before
  // anything else mounts - see requireLmsAuthorization
  router.isReady().then(() => {
    if (Object.keys(router.currentRoute.value.query).length) {
      router.replace({ ...router.currentRoute.value, query: {} });
    }
  });
};

initializeStore().then(startVue);

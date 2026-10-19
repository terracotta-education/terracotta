// Test-only entry point for the Playwright tests (it isn't one of Vite's build inputs, so it never
// ships). It mounts the real app - router, stores, components and global styles - the way
// src/main.js does, but signs in by putting the session from e2e/support/session.js straight
// into the store, since a real LTI launch needs an LMS. Every /api request is answered by the
// test (see e2e/support/api.js), so no backend runs either.
import { createApp } from "vue";

import App from "@/App.vue";
import router from "@/router";
import vuetify from "@/plugins/vuetify";
import { pinia } from "@/pinia";
import { api } from "@/store/api.module";

import "vuetify/styles";
import "sweetalert2/dist/sweetalert2.min.css";
import "@/styles/custom.scss";
import "@mdi/font/css/materialdesignicons.css";

const session = window.__E2E_SESSION__;

if (session) {
  const apiStore = api(pinia);

  apiStore.aud = window.location.origin;
  apiStore.ltiToken = session.token;
  apiStore.apiToken = session.token;
  apiStore.applyDecodedToken(session.claims);
  apiStore.setLmsApiOAuthURL(session.lmsApiOAuthURL);
}

createApp(App)
  .use(pinia)
  .use(router)
  .use(vuetify)
  .mount("#app");

router.push(window.__E2E_ROUTE__ || "/");

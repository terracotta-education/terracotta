import { createApp } from "vue";
import { pinia } from "@/pinia";

import DeepLink from "./DeepLink.vue";
import vuetify from "./plugins/vuetify";

import "vuetify/styles";
import "@/styles/custom.scss";

const url = new URL(window.location.href);
const params = url.searchParams;

createApp(
  DeepLink,
  {
    id: params.get("id")
  }
)
.use(pinia)
.use(vuetify)
.mount("#app");

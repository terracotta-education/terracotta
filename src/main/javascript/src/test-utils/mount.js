import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";

import vuetify from "@/plugins/vuetify";

export function mountComponent(component, options = {}) {
  const pinia = options.pinia || createPinia();
  setActivePinia(pinia);

  const { global: globalOptions = {}, ...rest } = options;

  return mount(component, {
    ...rest,
    global: {
      ...globalOptions,
      plugins: [
        vuetify,
        pinia,
        ...(globalOptions.plugins || [])
      ]
    }
  });
}

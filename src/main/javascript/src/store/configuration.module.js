import { defineStore } from "pinia";

import { configurationService } from "@/services";

export const configuration = defineStore("configuration", {
  state: () => ({
    configurations: null
  }),

  getters: {
    get: state => state.configurations,

    hasConfigurations: state =>
      Boolean(state.configurations),

    getConfiguration:
      state =>
      (name, defaultValue = null) =>
        state.configurations?.[name] ?? defaultValue
  },

  actions: {
    async retrieve() {
      try {
        const configurations =
          await configurationService.get();

        if (configurations?.message) {
          return configurations;
        }

        this.configurations = configurations || {};

        return configurations;
      } catch (error) {
        console.error(
          "configuration/retrieve | catch",
          error
        );

        return null;
      }
    },

    update(data) {
      if (!data?.name) {
        return;
      }

      this.configurations = {
        ...(this.configurations || {}),
        [data.name]: data.value
      };
    },

    reset() {
      this.configurations = null;
    }
  },

  persist: {
    key: "terracotta-configuration"
  }
});

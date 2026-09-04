import { defineStore } from "pinia";

import { integrationsService } from "@/services";

export const integrations = defineStore("integrations", {
  state: () => ({
    validation: {
      iframeUrlValid: false
    }
  }),

  getters: {
    isIframeUrlValid: state => state.validation.iframeUrlValid
  },

  actions: {
    setIframeValid(valid) {
      this.validation = {
        ...this.validation,
        iframeUrlValid: Boolean(valid)
      };
    },

    async validateIframeUrl(url) {
      try {
        const response =
          await integrationsService.validateIframeUrl(url);

        this.validation.iframeUrlValid = Boolean(response);

        return response;
      } catch (error) {
        console.error(
          "integrations/validateIframeUrl | catch",
          error
        );

        this.validation.iframeUrlValid = false;

        return false;
      }
    },

    resetValidation() {
      this.validation = {
        iframeUrlValid: false
      };
    }
  }
});

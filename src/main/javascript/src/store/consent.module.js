import { defineStore } from "pinia";

import { consentService } from "@/services";

export const consent = defineStore("consent", {
  state: () => ({
    file: null,
    title: ""
  }),

  getters: {
    consent: state => state,
    consentFile: state => state.file,
    consentTitle: state => state.title,
    hasConsent: state => Boolean(state.file || state.title)
  },

  actions: {
    resetConsent() {
      this.file = null;
      this.title = "";
    },

    async createConsent(payload) {
      try {
        const response = await consentService.create(...payload);

        if (response?.status !== 200) {
          throw new Error("Consent file upload failed");
        }

        return response;
      } catch (error) {
        console.error("consent/createConsent | catch", {
          error,
          state: this.$state
        });

        throw error;
      }
    },

    setConsentTitle(title) {
      this.title = title || "";
    },

    setConsentFile(file) {
      this.file = file || null;
    },

    async getConsentFile(experimentId) {
      try {
        const response =
          await consentService.getConsentFile(experimentId);

        if (response?.status === 200) {
          return response.base;
        }

        if (response?.status === 404) {
          return null;
        }

        console.error(
          "consent/getConsentFile | unexpected response",
          {
            state: this.$state,
            response
          }
        );

        return null;
      } catch (error) {
        console.error("consent/getConsentFile | catch", error);

        return null;
      }
    }
  }
});

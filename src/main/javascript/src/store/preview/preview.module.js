import { defineStore } from "pinia";

import { previewService } from "@/services";

export const preview = defineStore("preview", {
  state: () => ({
    treatmentPreview: null,
    isLoading: false,
    error: null
  }),

  getters: {
    hasPreview: state => !!state.treatmentPreview
  },

  actions: {
    async treatment(payload) {
      try {
        this.isLoading = true;
        this.error = null;

        const response =
          await previewService.treatmentPreview(...payload);

        const previewData = response?.data ?? null;

        this.treatmentPreview = previewData;

        return previewData;
      } catch (error) {
        console.error("preview/treatment | catch", error);

        this.error = error;
        this.treatmentPreview = null;

        return null;
      } finally {
        this.isLoading = false;
      }
    },

    reset() {
      this.treatmentPreview = null;
      this.isLoading = false;
      this.error = null;
    }
  }
});

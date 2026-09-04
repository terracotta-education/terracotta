import { defineStore } from "pinia";

import { exposuresService } from "@/services";

export const exposures = defineStore("exposures", {
  state: () => ({
    exposures: []
  }),

  getters: {
    hasExposures: state => state.exposures.length > 0
  },

  actions: {
    async fetchExposures(experimentId) {
      try {
        const data = await exposuresService.getAll(experimentId);

        this.exposures = data || [];

        return this.exposures;
      } catch (error) {
        console.error("exposures/fetchExposures | catch", error);

        this.exposures = [];

        return [];
      }
    },

    async createExposures(experimentId) {
      try {
        return await exposuresService.createExposures(experimentId);
      } catch (error) {
        console.error("exposures/createExposures | catch", error);

        return null;
      }
    },

    resetExposures() {
      this.exposures = [];
    }
  }
});

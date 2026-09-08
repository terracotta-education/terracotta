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

        // a failed HTTP response resolves to an error object (e.g. {status, error}) rather than
        // throwing, so an array check is needed here to catch that case as a failure too - without
        // it, this.exposures ends up non-array and callers doing exposures.value.find(...)/
        // .map(...)/.filter(...) throw "exposures.value.find is not a function"
        if (!Array.isArray(data)) {
          console.error(
            "exposures/fetchExposures | non-array response",
            { data }
          );

          this.exposures = [];

          return [];
        }

        this.exposures = data;

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

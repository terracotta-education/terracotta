import { defineStore } from "pinia";

import { resultsDashboardService } from "@/services";

export const resultsDashboard = defineStore("resultsDashboard", {
  state: () => ({
    resultsDashboard: {
      experimentId: null,
      overview: null,
      outcomes: null
    }
  }),

  getters: {
    overview: state => state.resultsDashboard.overview,
    outcomes: state => state.resultsDashboard.outcomes
  },

  actions: {
    async getOverview(experimentId) {
      try {
        const response =
          await resultsDashboardService.overview(experimentId);

        const overview = response?.data?.overview ?? null;

        this.resultsDashboard = {
          ...this.resultsDashboard,
          experimentId,
          overview
        };

        return overview;
      } catch (error) {
        console.error(
          "resultsDashboard/getOverview | catch",
          error
        );

        this.resultsDashboard = {
          ...this.resultsDashboard,
          overview: null
        };

        return null;
      }
    },

    async getOutcomes(payload) {
      try {
        const [experimentId, body] = payload;
        const response = await resultsDashboardService.outcomes(
          experimentId,
          body
        );

        const outcomes = response?.data?.outcomes ?? null;

        this.resultsDashboard = {
          ...this.resultsDashboard,
          experimentId,
          outcomes
        };

        return outcomes;
      } catch (error) {
        console.error(
          "resultsDashboard/getOutcomes | catch",
          error
        );

        this.resultsDashboard = {
          ...this.resultsDashboard,
          outcomes: null
        };

        return null;
      }
    },

    clearOutcomes() {
      this.resultsDashboard = {
        ...this.resultsDashboard,
        outcomes: null
      };
    },

    resetResultsDashboard() {
      this.resultsDashboard = {
        experimentId: null,
        overview: null,
        outcomes: null
      };
    }
  }
});

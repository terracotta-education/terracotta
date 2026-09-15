import { defineStore } from "pinia";

import { resultsDashboardService } from "@/services";

// getOutcomes re-fires on every outcome-selection change (see Input.vue's
// @hasSelections handler), so toggling selections quickly can leave an earlier,
// now-abandoned request's response landing after a more recent one's - guard it the
// same way as assessment.module.js's fetchAssessment.
let outcomesRequestId = 0;

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
      const requestId = ++outcomesRequestId;

      try {
        const [experimentId, body] = payload;
        const response = await resultsDashboardService.outcomes(
          experimentId,
          body
        );

        const outcomes = response?.data?.outcomes ?? null;

        if (requestId !== outcomesRequestId) {
          return this.resultsDashboard.outcomes;
        }

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

        if (requestId === outcomesRequestId) {
          this.resultsDashboard = {
            ...this.resultsDashboard,
            outcomes: null
          };
        }

        return null;
      }
    },

    clearOutcomes() {
      // invalidate any still-in-flight getOutcomes so its eventual response can't
      // repopulate outcomes after this deliberate clear
      outcomesRequestId += 1;

      this.resultsDashboard = {
        ...this.resultsDashboard,
        outcomes: null
      };
    },

    resetResultsDashboard() {
      outcomesRequestId += 1;

      this.resultsDashboard = {
        experimentId: null,
        overview: null,
        outcomes: null
      };
    }
  }
});

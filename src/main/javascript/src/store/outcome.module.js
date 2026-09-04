import { defineStore } from "pinia";

import { outcomeService } from "@/services";

export const outcome = defineStore("outcome", {
  state: () => ({
    outcome: null,
    outcomes: [],
    outcomeScores: [],
    outcomePotentials: [],
    experimentOutcomes: []
  }),

  getters: {
    hasOutcome: state => Boolean(state.outcome),
    hasOutcomes: state => state.outcomes.length > 0
  },

  actions: {
    resetOutcome() {
      this.outcome = null;
    },

    resetOutcomePotentials() {
      this.outcomePotentials = [];
    },

    async createOutcome(payload) {
      try {
        const response = await outcomeService.create(...payload);

        if (
          response?.status === 200 ||
          response?.status === 201
        ) {
          const outcomeData = response.data;
          this.setOutcome(outcomeData);

          return outcomeData;
        }

        return null;
      } catch (error) {
        console.error("outcome/createOutcome | catch", error);

        return null;
      }
    },

    async updateOutcome(payload) {
      try {
        const response =
          await outcomeService.updateOutcome(...payload);

        if (response?.status === 200) {
          this.setOutcome(payload[2]);
        }

        return response;
      } catch (error) {
        console.error("outcome/updateOutcome | catch", error);

        return null;
      }
    },

    async deleteOutcome(payload) {
      try {
        const response =
          await outcomeService.deleteOutcome(...payload);

        if (response?.status === 200) {
          this.outcome = null;
        }

        return response;
      } catch (error) {
        console.error("outcome/deleteOutcome | catch", error);

        return null;
      }
    },

    async fetchOutcomeById(payload) {
      try {
        const outcomeId = payload[2];
        const isDifferentOutcome =
          parseInt(this.outcome?.outcomeId) !== parseInt(outcomeId);

        const response = await outcomeService.getById(...payload);

        if (response?.status === 200) {
          this.setOutcome(response.data);
        } else if (isDifferentOutcome) {
          this.outcome = null;
        }

        return response;
      } catch (error) {
        console.error("outcome/fetchOutcomeById | catch", error);

        return null;
      }
    },

    async fetchOutcomes(payload) {
      try {
        const response = await outcomeService.getAll(...payload);

        if ([200, 204].includes(response?.status)) {
          this.outcomes = response?.data || [];
        }

        return response;
      } catch (error) {
        console.error("outcome/fetchOutcomes | catch", error);

        return null;
      }
    },

    async fetchOutcomesByExposures(payload) {
      try {
        // one request for all of the experiment's outcomes instead of one request per exposure
        const [experimentId] = payload;
        const response =
          await outcomeService.getAllByExperimentId(experimentId);

        this.experimentOutcomes = response?.data || [];

        return response;
      } catch (error) {
        console.error(
          "outcome/fetchOutcomesByExposures | catch",
          error
        );

        return null;
      }
    },

    async fetchOutcomesByExperimentId(payload) {
      try {
        const response =
          await outcomeService.getAllByExperimentId(...payload);

        this.outcomes = response?.data || [];

        return response;
      } catch (error) {
        console.error(
          "outcome/fetchOutcomesByExperimentId | catch",
          error
        );

        return null;
      }
    },

    async fetchOutcomeScores(payload) {
      try {
        const response =
          await outcomeService.getOutcomeScoresById(...payload);

        if ([200, 204].includes(response?.status)) {
          this.outcomeScores = response?.data || [];
        }

        return response;
      } catch (error) {
        console.error("outcome/fetchOutcomeScores | catch", error);

        return null;
      }
    },

    async updateOutcomeScores(payload) {
      try {
        await outcomeService.updateOutcomeScores(...payload);

        return this.fetchOutcomeScores(payload);
      } catch (error) {
        console.error("outcome/updateOutcomeScores | catch", error);

        return null;
      }
    },

    async fetchOutcomePotentials(experimentId) {
      try {
        const response =
          await outcomeService.getOutcomePotentials(
            parseInt(experimentId)
          );

        if (response?.status === 200) {
          this.outcomePotentials = response?.data || [];
        }

        return response;
      } catch (error) {
        console.error(
          "outcome/fetchOutcomePotentials | catch",
          error
        );

        return null;
      }
    },

    setOutcome(data) {
      const outcomeData = Array.isArray(data) ? data[2] : data;

      if (outcomeData?.outcomeId) {
        this.outcome = outcomeData;
      }
    }
  }
});

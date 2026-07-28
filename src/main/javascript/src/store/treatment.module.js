import { defineStore } from "pinia";

import { treatmentService } from "@/services";

export const treatment = defineStore("treatment", {
  state: () => ({
    treatments: [],
    treatment: null
  }),

  getters: {
    hasTreatment: state => Boolean(state.treatment),
    hasTreatments: state => state.treatments.length > 0
  },

  actions: {
    async createTreatment(payload) {
      try {
        const assignmentId = parseInt(payload[2]);

        const treat = this.treatments.find(
          t => t.assignmentId === assignmentId
        );

        if (treat) {
          this.treatment = treat;
          return { status: 200, data: treat };
        }

        const response = await treatmentService.create(...payload);

        if (response?.status !== 201) {
          return response;
        }

        const created = response?.data;
        this.treatment = created;
        this.upsertTreatment(created);

        return { status: response.status, data: created };
      } catch (error) {
        console.error("treatment/createTreatment | catch", error);

        return null;
      }
    },

    async updateTreatment(payload) {
      try {
        const response = await treatmentService.update(...payload);

        if (response?.status !== 201 && response?.status !== 200) {
          return response;
        }

        const treat = response?.data;

        this.treatment = treat;
        this.upsertTreatment(treat);

        return {
          status: response?.status,
          data: treat
        };
      } catch (error) {
        console.error("treatment/updateTreatment | catch", error);

        return null;
      }
    },

    async checkTreatment(payload) {
      try {
        const response =
          await treatmentService.fetchTreatment(...payload);

        return response
          ? {
              status: response.status,
              data: response.data
            }
          : null;
      } catch (error) {
        console.error("treatment/checkTreatment | catch", error);

        return null;
      }
    },

    resetTreatments() {
      this.treatments = [];
      this.treatment = null;
    },

    upsertTreatment(treat) {
      if (!treat?.treatmentId) {
        return;
      }

      const index = this.treatments.findIndex(
        item =>
          parseInt(item.treatmentId) === parseInt(treat.treatmentId)
      );

      if (index >= 0) {
        this.treatments.splice(index, 1, treat);
      } else {
        this.treatments.push(treat);
      }
    }
  }
});

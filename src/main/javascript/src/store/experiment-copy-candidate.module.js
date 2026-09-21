import { defineStore } from "pinia";

import { experimentCopyCandidateService } from "@/services";

export const experimentCopyCandidate = defineStore("experimentCopyCandidate", {
  state: () => ({
    copyCandidates: []
  }),

  actions: {
    async fetchAll() {
      try {
        const response = await experimentCopyCandidateService.getAll();

        this.copyCandidates = response?.data || [];

        return this.copyCandidates;
      } catch (e) {
        console.error("experimentCopyCandidate/fetchAll | catch", e);

        return [];
      }
    },

    // resolves EVERY currently-shown candidate in one call: importCandidateIds get imported (and
    // their corresponding copied LMS assignment(s) re-pointed server-side); everything else gets
    // declined and obsolete-processed. There's deliberately no separate per-item import/dismiss
    // action anymore - see ExperimentCopyCandidateServiceImpl.resolve for the full reasoning.
    async resolve(importCandidateIds) {
      try {
        const response = await experimentCopyCandidateService.resolve(importCandidateIds);

        // resolve() always disposes of every currently-PENDING candidate for this context, one
        // way or another (imported or declined) - nothing is left to keep around
        this.copyCandidates = [];

        return response?.data;
      } catch (e) {
        console.error("experimentCopyCandidate/resolve | catch", e);

        return null;
      }
    },

    reset() {
      this.copyCandidates = [];
    }
  }
});

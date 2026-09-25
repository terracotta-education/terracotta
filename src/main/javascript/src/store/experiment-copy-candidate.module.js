import { defineStore } from "pinia";

import { experimentCopyCandidateService } from "@/services";

export const experimentCopyCandidate = defineStore("experimentCopyCandidate", {
  state: () => ({
    copyCandidates: [],
    copyStatus: null
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

    // the result of automatically recreating this course's experiments after a course copy:
    // NONE, IN_PROGRESS, COMPLETE or ERROR (see ExperimentCopyCandidateService.getCopyStatus)
    async fetchCopyStatus() {
      try {
        const response = await experimentCopyCandidateService.getCopyStatus();

        this.copyStatus = response?.data || null;

        return this.copyStatus;
      } catch (e) {
        console.error("experimentCopyCandidate/fetchCopyStatus | catch", e);

        return null;
      }
    },

    async acknowledgeCopyStatus() {
      try {
        await experimentCopyCandidateService.acknowledgeCopyStatus();

        this.copyStatus = null;
      } catch (e) {
        console.error("experimentCopyCandidate/acknowledgeCopyStatus | catch", e);
      }
    },

    // tries a failed recreation again, as the current instructor - e.g. after they've re-approved
    // LMS access, as the failure email asks them to
    async retryCopy() {
      try {
        const response = await experimentCopyCandidateService.retryCopy();

        if (response?.data) {
          this.copyStatus = response.data;
        }

        return this.copyStatus;
      } catch (e) {
        console.error("experimentCopyCandidate/retryCopy | catch", e);

        return this.copyStatus;
      }
    },

    reset() {
      this.copyCandidates = [];
      this.copyStatus = null;
    }
  }
});

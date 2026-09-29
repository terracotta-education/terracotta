import { defineStore } from "pinia";

import { experimentCopyCandidateService } from "@/services";

export const experimentCopyCandidate = defineStore("experimentCopyCandidate", {
  state: () => ({
    copyStatus: null
  }),

  actions: {
    // the result of automatically recreating this course's experiments after a course copy:
    // NONE, IN_PROGRESS, COMPLETE or ERROR (see ExperimentCopyCandidateService.getCopyStatus);
    // retryCopy can also return AUTHORIZATION_REQUIRED (see DistributeController#retryCopy)
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
      this.copyStatus = null;
    }
  }
});

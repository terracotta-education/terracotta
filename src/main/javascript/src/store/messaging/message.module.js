import { defineStore } from "pinia";

import { messageService } from "@/services";

// Shared by fetchPreview and setPreview (both write the singular this.preview field)
// so a stale response can't clobber a newer one - see the identical guard in
// assessment.module.js's fetchAssessment for the full reasoning. Concretely:
// Preview.vue's participant list stays clickable while a preview is loading, so
// clicking student A then quickly clicking student B fires a second fetch before
// the first resolves; without this, whichever response lands last wins, even if
// it's A's now-abandoned one.
let previewRequestId = 0;

function normalizePipedText(pipedText) {
  if (!pipedText) {
    return null;
  }

  return {
    ...pipedText,
    items: Array.isArray(pipedText.items)
      ? pipedText.items.map(item => ({
          ...item,
          id: item.id || crypto.randomUUID()
        }))
      : []
  };
}

export const message = defineStore("messagingMessage", {
  state: () => ({
    assignments: [],
    isLoading: false,
    preview: null,
    pipedText: null,
    message: null
  }),

  getters: {
    hasAssignments: state => state.assignments.length > 0,
    hasPipedText: state => !!state.pipedText,
    hasMessage: state => !!state.message
  },

  actions: {
    setPreview(preview) {
      // invalidate any still-in-flight fetchPreview so its eventual response can't
      // overwrite this deliberate, synchronous set once it resolves
      previewRequestId += 1;
      this.preview = preview;
    },

    setPipedText(pipedText) {
      this.pipedText = pipedText;
    },

    async update(payload) {
      try {
        return await messageService.update(...payload);
      } catch (error) {
        console.error("message/update | catch", error);

        return null;
      }
    },

    async fetchPreview(payload) {
      const requestId = ++previewRequestId;

      try {
        const response =
          await messageService.fetchPreview(...payload);

        if (requestId !== previewRequestId) {
          return this.preview;
        }

        this.preview = response;

        return response;
      } catch (error) {
        console.error("message/fetchPreview | catch", error);

        if (requestId === previewRequestId) {
          this.preview = null;
        }

        return null;
      }
    },

    async sendTest(payload) {
      try {
        return await messageService.sendTest(...payload);
      } catch (error) {
        console.error("message/sendTest | catch", error);

        return null;
      }
    },

    async getAssignments(payload) {
      try {
        this.isLoading = true;

        const response = await messageService.getAssignments(...payload);
        const assignmentsData = Array.isArray(response) ? response : [];

        this.assignments = assignmentsData;

        return assignmentsData;
      } catch (error) {
        console.error("message/getAssignments | catch", error);

        this.assignments = [];

        return [];
      } finally {
        this.isLoading = false;
      }
    },

    async updatePlaceholders(payload) {
      try {
        return await messageService.updatePlaceholders(...payload);
      } catch (error) {
        console.error("message/updatePlaceholders | catch", error);

        return null;
      }
    },

    async uploadPipedText(payload) {
      try {
        const response =
          await messageService.uploadPipedText(...payload);

        if (response) {
          this.message = response;
          this.pipedText = normalizePipedText(
            response?.content?.pipedText || null
          );
        } else {
          this.pipedText = null;
          this.message = null;
        }

        return response || null;
      } catch (error) {
        console.error("message/uploadPipedText | catch", error);

        this.pipedText = null;
        this.message = null;

        return null;
      }
    },

    reset() {
      this.assignments = [];
      this.isLoading = false;
      this.preview = null;
      this.pipedText = null;
      this.message = null;
    }
  }
});

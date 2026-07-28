import { defineStore } from "pinia";

import { messageService } from "@/services";

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
      try {
        const response =
          await messageService.fetchPreview(...payload);

        this.preview = response;

        return response;
      } catch (error) {
        console.error("message/fetchPreview | catch", error);

        this.preview = null;

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

    async getAssignments() {
      try {
        this.isLoading = true;

        const response = await messageService.getAssignments();
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

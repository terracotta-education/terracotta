import { defineStore } from "pinia";

import { messageContentAttachmentService } from "@/services";

export const attachment = defineStore("messagingContentAttachment", {
  state: () => ({
    messageContentAttachments: []
  }),

  getters: {
    attachments: state => state.messageContentAttachments,

    attachmentCount: state =>
      state.messageContentAttachments.length,

    hasAttachments: state =>
      state.messageContentAttachments.length > 0
  },

  actions: {
    async getAll(payload) {
      try {
        const response =
          await messageContentAttachmentService.getAll(...payload);

        this.messageContentAttachments = Array.isArray(response)
          ? response
          : [];

        return this.messageContentAttachments;
      } catch (error) {
        console.error("attachment/getAll | catch", error);

        this.messageContentAttachments = [];

        return [];
      }
    },

    resetAttachments() {
      this.messageContentAttachments = [];
    }
  }
});

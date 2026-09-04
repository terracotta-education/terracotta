import { defineStore } from "pinia";

import { messageContainerService } from "@/services";

export const container = defineStore("messagingMessageContainer", {
  state: () => ({
    messageContainers: [],
    messageContainer: null
  }),

  getters: {
    hasMessageContainers: state =>
      state.messageContainers.length > 0
  },

  actions: {
    async get(payload) {
      try {
        const response =
          await messageContainerService.get(...payload);

        this.messageContainer = response;
        this.upsertMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/get | catch", error);

        return null;
      }
    },

    async getAll(payload) {
      try {
        const response =
          await messageContainerService.getAll(...payload);

        const containers = Array.isArray(response) ? response : [];
        this.upsertMessageContainers(containers);

        return containers;
      } catch (error) {
        console.error("container/getAll | catch", error);

        return [];
      }
    },

    async create(payload) {
      try {
        const response =
          await messageContainerService.create(...payload);

        this.messageContainer = response;
        this.upsertMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/create | catch", error);

        return null;
      }
    },

    async update(payload) {
      try {
        const response =
          await messageContainerService.update(...payload);

        this.messageContainer = response;
        this.upsertMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/update | catch", error);

        return null;
      }
    },

    async updateAll(payload) {
      try {
        const response =
          await messageContainerService.updateAll(...payload);

        this.messageContainers = Array.isArray(response)
          ? response
          : [];

        return this.messageContainers;
      } catch (error) {
        console.error("container/updateAll | catch", error);

        return [];
      }
    },

    async send(payload) {
      try {
        const response =
          await messageContainerService.send(...payload);

        this.messageContainer = response;
        this.upsertMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/send | catch", error);

        return null;
      }
    },

    async deleteContainer(payload) {
      try {
        const response =
          await messageContainerService.deleteContainer(...payload);

        this.deleteMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/deleteContainer | catch", error);

        return null;
      }
    },

    async move(payload) {
      try {
        const response =
          await messageContainerService.move(...payload);

        this.upsertMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/move | catch", error);

        return null;
      }
    },

    async duplicate(payload) {
      try {
        const response =
          await messageContainerService.duplicate(...payload);

        this.upsertMessageContainers([response]);

        return response;
      } catch (error) {
        console.error("container/duplicate | catch", error);

        return null;
      }
    },

    reset() {
      this.messageContainers = [];
      this.messageContainer = null;
    },

    upsertMessageContainers(messageContainers) {
      if (!Array.isArray(messageContainers)) {
        return;
      }

      messageContainers.filter(Boolean).forEach(c => {
        const index = this.messageContainers.findIndex(
          item => item.id === c.id
        );

        if (index !== -1) {
          this.messageContainers.splice(index, 1, c);
        } else {
          this.messageContainers.push(c);
        }
      });
    },

    deleteMessageContainers(messageContainers) {
      if (!Array.isArray(messageContainers)) {
        return;
      }

      messageContainers.filter(Boolean).forEach(c => {
        const index = this.messageContainers.findIndex(
          item => item.id === c.id
        );

        if (index !== -1) {
          this.messageContainers.splice(index, 1);
        }
      });
    }
  }
});

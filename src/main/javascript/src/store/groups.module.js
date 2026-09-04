import { defineStore } from "pinia";

import { groupsService } from "@/services";

export const groups = defineStore("groups", {
  state: () => ({
    groups: []
  }),

  getters: {
    hasGroups: state => state.groups.length > 0
  },

  actions: {
    async createAndAssignGroups(experimentId) {
      try {
        const response =
          await groupsService.createAndAssignGroups(experimentId);

        this.groups = Array.isArray(response) ? response : [];

        return response;
      } catch (error) {
        console.error(
          "groups/createAndAssignGroups | catch",
          error
        );

        return null;
      }
    },

    resetGroups() {
      this.groups = [];
    }
  }
});

import { defineStore } from "pinia";

export const navigation = defineStore("navigation", {
  state: () => ({
    editMode: null
  }),

  getters: {
    hasEditMode: state => Boolean(state.editMode),
    initialPage: state => state.editMode?.initialPage || null,
    callerPage: state => state.editMode?.callerPage || null
  },

  actions: {
    saveEditMode(editMode) {
      this.editMode = editMode || null;
    },

    deleteEditMode() {
      this.editMode = null;
    },

    resetNavigation() {
      this.editMode = null;
    }
  }
});

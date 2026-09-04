import { defineStore } from "pinia";

export const conditionaltext = defineStore("messagingConditionalText", {
  state: () => ({
    messageConditionalTexts: [],
    messageConditionalText: null,
    messageConditionalTextEditId: null
  }),

  getters: {
    hasConditionalTexts: state =>
      state.messageConditionalTexts.length > 0
  },

  actions: {
    reset() {
      this.messageConditionalTexts = [];
      this.messageConditionalText = null;
      this.messageConditionalTextEditId = null;
    },

    addConditionalTexts(messageConditionalTexts) {
      if (!Array.isArray(messageConditionalTexts)) {
        return;
      }

      messageConditionalTexts.forEach(item => {
        if (!item) return;

        const index = this.messageConditionalTexts.findIndex(
          existing => existing && existing.label === item.label
        );

        if (index !== -1) {
          this.messageConditionalTexts.splice(index, 1, item);
        } else {
          this.messageConditionalTexts.push(item);
        }
      });
    },

    addMessageConditionalTexts(messageConditionalTexts) {
      this.addConditionalTexts(messageConditionalTexts);
    },

    setConditionalText(messageConditionalText) {
      this.messageConditionalText = messageConditionalText;
    },

    setMessageConditionalText(messageConditionalText) {
      this.setConditionalText(messageConditionalText);
    },

    setConditionalTextEditId(messageConditionalTextEditId) {
      this.messageConditionalTextEditId = messageConditionalTextEditId;
    },

    setMessageConditionalTextEditId(messageConditionalTextEditId) {
      this.setConditionalTextEditId(messageConditionalTextEditId);
    }
  }
});

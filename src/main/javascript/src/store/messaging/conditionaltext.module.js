const state = {
  messageConditionalTexts: [],
  messageConditionalText: null,
  messageConditionalTextEditId: null
}

const actions = {
  reset({ commit }) {
    commit("setMessageConditionalTexts", []);
    commit("setMessageConditionalText", null);
    commit("setMessageConditionalTextEditId", null);
  }
}

const mutations = {
  addMessageConditionalTexts(state, messageConditionalTexts) {
    if (messageConditionalTexts === null) {
      return;
    }

    messageConditionalTexts.forEach((messageConditionalText) => {
      const index = state.messageConditionalTexts.findIndex(m => m.label === messageConditionalText.label);

      if (index !== -1) {
        state.messageConditionalTexts.splice(index, 1, messageConditionalText);
      } else {
        state.messageConditionalTexts.push(messageConditionalText);
      }
    });
  },
  setMessageConditionalTexts(state, messageConditionalTexts) {
    state.messageConditionalTexts = messageConditionalTexts;
  },
  setMessageConditionalText(state, messageConditionalText) {
    state.messageConditionalText = messageConditionalText;
  },
  setMessageConditionalTextEditId(state, messageConditionalTextEditId) {
    state.messageConditionalTextEditId = messageConditionalTextEditId;
  }
}

const getters = {
  messageConditionalTexts: (state) => {
    return state.messageConditionalTexts;
  },
  messageConditionalText: (state) => {
    return state.messageConditionalText;
  },
  messageConditionalTextEditId: (state) => {
    return state.messageConditionalTextEditId;
  }
}

export const conditionaltext = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

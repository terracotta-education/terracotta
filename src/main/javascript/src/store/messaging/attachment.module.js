import { messageContentAttachmentService } from "@/services";

const state = {
  messageContentAttachments: []
}

const actions = {
  async getAll({ commit }, payload) {
    // payload = experimentId, exposureId, containerId, messageId, contentId
    try {
      const response = await messageContentAttachmentService.getAll(...payload);
      commit("set", response);
    } catch (e) {
      console.error("getAll catch", {e});
    }
  }
}

const mutations = {
  set(state, messageContentAttachments) {
    state.messageContentAttachments = messageContentAttachments || [];
  }
}

const getters = {
  get: (state) => {
    return state.messageContentAttachments;
  }
}

export const attachment = {
  namespaced: true,
  state,
  actions,
  getters,
  mutations
}

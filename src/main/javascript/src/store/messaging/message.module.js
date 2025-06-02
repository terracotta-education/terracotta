import { messageService } from "@/services";

const state = {
  assignments: [],
  isLoading: false,
  preview: null,
  pipedText: null,
  message: null
}

const actions = {
  async update(_, payload) {
    // payload: experimentId, exposureId, containerId, messageId, message_dto
    try {
      return await messageService.update(...payload);
    } catch (e) {
      console.error("update catch", {e});
    }
  },
  async fetchPreview({ commit }, payload) {
    // payload: experimentId, exposureId, containerId, messageId, messagePreviewDto
    try {
      const response = await messageService.fetchPreview(...payload);
      commit("setPreview", response);
    } catch (e) {
      console.error("fetchPreview catch", {e});
    }
  },
  async sendTest(_, payload) {
    // payload: experimentId, exposureId, containerId, messageId, messageSendTestDto
    try {
      await messageService.sendTest(...payload);
    } catch (e) {
      console.error("sendTest catch", {e});
    }
  },
  async getAssignments({ commit }) {
    try {
      commit("setIsLoading", true);
      const response = await messageService.getAssignments();
      commit("setAssignments", response);
    } catch (e) {
      console.error("update catch", {e});
    } finally {
      commit("setIsLoading", false);
    }
  },
  async updatePlaceholders(_, payload) {
    // payload: experimentId, exposureId, containerId, messageId, contentId, contentDto
    try {
      return await messageService.updatePlaceholders(...payload);
    } catch (e) {
      console.error("updatePlaceholders catch", {e});
    }
  },
  async uploadPipedText({ commit }, payload) {
    // payload: experimentId, exposureId, containerId, messageId, contentId, file
    try {
      // response = message DTO
      const response = await messageService.uploadPipedText(...payload);

      if (response) {
        commit("setMessage", response);
      } else {
        // error occured
        commit("setPipedText", null);
        commit("setMessage", null);
      }

      return response;
    } catch (e) {
      console.error("pipedText catch", {e});
    }
  }
}

const mutations = {
  setAssignments(state, assignments) {
    state.assignments = [...assignments];
  },
  setIsLoading(state, isLoading) {
    state.isLoading = isLoading && state.assignments.length === 0;
  },
  setPreview(state, preview) {
    state.preview = preview;
  },
  setPipedText(state, pipedText) {
    state.pipedText = pipedText;
  },
  setMessage(state, message) {
    state.message = message;

    // update pipedText if it exists in the message
    state.pipedText = message?.content?.pipedText || null;

    if (state.pipedText) {
      // ensure pipedText has a uuid for editor placement
      state.pipedText.items = state.pipedText.items
        .map(
          item => ({
            ...item,
           id: item.id || crypto.randomUUID()
          })
        );
    }
  }
}

const getters = {
  assignments: (state) => {
    return state.assignments;
  },
  isLoading: (state) => {
    return state.isLoading;
  },
  preview: (state) => {
    return state.preview;
  },
  pipedText: (state) => {
    return state.pipedText;
  },
  message: (state) => {
    return state.message;
  }
}

export const message = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

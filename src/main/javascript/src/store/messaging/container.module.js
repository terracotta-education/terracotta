import { messageContainerService } from "@/services";

const state = {
  messageContainers: [],
  messageContainer: null
}
const actions = {
  async get({ commit }, payload) {
    // payload = experimentId, exposureId, containerId
    try {
      const response = await messageContainerService.get(...payload);
      commit("setMessageContainer", response);
      commit("setMessageContainers", [response]);
    } catch (e) {
      console.error("get catch", {e});
    }
  },
  async getAll({ commit }, payload) {
    // payload = experimentId, exposureId
    try {
       const response = await messageContainerService.getAll(...payload);
       commit("setMessageContainers", response);
    } catch (e) {
      console.error("getAll catch", {e});
    }
  },
  async create({ commit }, payload) {
    // payload = experimentId, exposureId, single
    try {
      const response = await messageContainerService.create(...payload);
      commit("setMessageContainer", response);
      commit("setMessageContainers", [response]);
      return response;
    } catch (e) {
      console.error("create catch", {e});
    }
  },
  async update({ commit }, payload) {
    // payload: experimentId, exposureId, containerId, container_dto
    try {
      const response = await messageContainerService.update(...payload);
      commit("setMessageContainer", response);
      commit("setMessageContainers", [response]);
    } catch (e) {
      console.error("update catch", {e});
    }
  },
  async updateAll({ commit }, payload) {
    // payload: experimentId, exposureId, container_dtos
    try {
      const response = await messageContainerService.updateAll(...payload);
      commit("setMessageContainers", response);
    } catch (e) {
      console.error("updateAll catch", {e});
    }
  },
  async send({ commit }, payload) {
    // payload = experimentId, exposureId, containerId
    try {
      const response = await messageContainerService.send(...payload);
      commit("setMessageContainer", response);
      commit("setMessageContainers", [response]);
    } catch (e) {
      console.error("send catch", {e});
    }
  },
  async deleteContainer({ commit }, payload) {
    // payload: experimentId, exposureId, containerId
    try {
      const response = await messageContainerService.deleteContainer(...payload);
      commit("deleteMessageContainers", [response]);
    } catch (e) {
      console.error("delete catch", {e});
    }
  },
  async move({ commit }, payload) {
    // payload: experimentId, exposureId, containerId, container_dto
    try {
      const response = await messageContainerService.move(...payload);
      commit("setMessageContainers", [response]);
    } catch (error) {
      console.error("move catch", {error});
    }
  },
  async duplicate({ commit }, payload) {
    // payload: experimentId, exposureId, containerId
    try {
      const response = await messageContainerService.duplicate(...payload);
      commit("setMessageContainers", [response]);
    } catch (error) {
      console.error("duplicate catch", {error});
    }
  },
  async reset({ state }) {
    state.messageContainers = [];
    state.messageContainer = null;
  },
}

const mutations = {
  setMessageContainers(state, messageContainers) {
    messageContainers.forEach((container) => {
      const index = state.messageContainers.findIndex(c => c.id === container.id);

      if (index !== -1) {
        state.messageContainers.splice(index, 1, container);
      } else {
        state.messageContainers.push(container);
      }
    });
  },
  setMessageContainer(state, messageContainer) {
    state.messageContainer = messageContainer;
  },
  deleteMessageContainers(state, messageContainers) {
    messageContainers.forEach((container) => {
      const index = state.messageContainers.findIndex(c => c.id === container.id);

      if (index !== -1) {
        state.messageContainers.splice(index, 1);
      }
    });
  }
}

const getters = {
  messageContainers: (state) => {
    return state.messageContainers;
  },
  messageContainer: (state) => {
    return state.messageContainer;
  }
}
export const container = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

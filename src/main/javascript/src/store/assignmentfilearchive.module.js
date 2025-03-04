import {assignmentfilearchiveService} from "@/services";

const state = {
  fileRequest: null
}

const actions = {
  async prepare({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId
    try {
      const fileRequest = await assignmentfilearchiveService.prepare(...payload);
      commit("addFileRequest", fileRequest);
    } catch (e) {
      console.error("prepare catch", e);
    }
  },
  async poll({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId, createNewOnOutdated
    try {
      const fileRequest = await assignmentfilearchiveService.poll(...payload);
      commit("addFileRequest", fileRequest);
    } catch (e) {
      console.error("poll catch", e);
    }
  },
  async retrieve({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId, fileRequest
    try {
      const fileRequest = await assignmentfilearchiveService.retrieve(...payload);
      commit("addFileRequest", fileRequest);
    } catch (e) {
      console.error("retrieve catch", e);
    }
  },
  async acknowledgeError({commit}, payload) {
    // payload = experimentId, exposureId, assignmentId, fileId
    try {
      await assignmentfilearchiveService.acknowledgeError(...payload);
      commit("reset");
    } catch (e) {
      console.error("acknowledgeError catch", e);
    }
  },
  reset: ({commit}) => {
    commit("reset");
  }
}
const mutations = {
  addFileRequest(state, fileRequest) {
    if (!fileRequest) {
      return;
    }

    if (!fileRequest.id) {
      return;
    }

    state.fileRequest = {
      ...fileRequest,
      downloaded: fileRequest?.status === "DOWNLOADED",
      error: fileRequest?.status === "ERROR",
      outdated: fileRequest?.status === "OUTDATED",
      processing: fileRequest?.status === "PROCESSING",
      ready: fileRequest?.status === "READY",
      reprocessing: fileRequest?.status === "REPROCESSING"
    }
  },
  reset(state)  {
    state.fileRequest = null;
  }
}
const getters = {
  fileRequest: (state) => {
    return state.fileRequest;
  }
}

export const assignmentfilearchive = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

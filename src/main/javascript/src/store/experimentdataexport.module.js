import { experimentdataexportService } from "@/services";

const state = {
  dataExportRequests: []
}

const actions = {
  async prepare({commit}, payload) {
    // payload = experimentId
    try {
      const dataExportRequest = await experimentdataexportService.prepare(...payload);
      commit("addDataExportRequest", dataExportRequest);
    } catch (e) {
      console.error("prepare catch", e);
    }
  },
  async poll({commit}, payload) {
    // payload = experimentId, createNewOnOutdated
    try {
      const dataExportRequest = await experimentdataexportService.poll(...payload);
      commit("addDataExportRequest", dataExportRequest);
    } catch (e) {
      console.error("poll catch", e);
    }
  },
  async pollList({commit}, payload) {
    // payload = experimentIds, createNewOnOutdated
    try {
      const dataExportRequests = await experimentdataexportService.pollList(...payload);
      commit("addDataExportRequests", dataExportRequests);
    } catch (e) {
      console.error("pollList catch", e);
    }
  },
  async retrieve({commit}, payload) {
    // payload = experimentId, dataExportRequest
    try {
      const dataExportRequest = await experimentdataexportService.retrieve(...payload);
      commit("addDataExportRequest", dataExportRequest);
    } catch (e) {
      console.error("retrieve catch", e);
    }
  },
  async acknowledge({commit}, payload) {
    // payload = experimentId, dataExportRequestId, status
    try {
      const dataExportRequest = await experimentdataexportService.acknowledge(...payload);
      commit("addDataExportRequest", dataExportRequest);
    } catch (e) {
      console.error("acknowledge catch", e);
    }
  },
  reset: ({commit}) => {
    commit("reset");
  }
}
const mutations = {
  addDataExportRequest(state, dataExportRequest) {
    if (!dataExportRequest) {
      return;
    }

    if (!dataExportRequest.id) {
      return;
    }

    dataExportRequest = {
      ...dataExportRequest,
      downloaded: dataExportRequest?.status === "DOWNLOADED",
      error: dataExportRequest?.status === "ERROR",
      outdated: dataExportRequest?.status === "OUTDATED",
      processing: dataExportRequest?.status === "PROCESSING",
      ready: dataExportRequest?.status === "READY",
      reprocessing: dataExportRequest?.status === "REPROCESSING"
    }

    const foundIndex = state.dataExportRequests.findIndex(sder => sder.experimentId === dataExportRequest.experimentId);

    if (foundIndex >= 0) {
      state.dataExportRequests.splice(foundIndex, 1, dataExportRequest);
    } else {
      state.dataExportRequests = [...state.dataExportRequests, dataExportRequest];
    }
  },
  addDataExportRequests(state, dataExportRequests) {
    state.dataExportRequests = [];

    if (!dataExportRequests || dataExportRequests.length === 0) {
      return;
    }

    dataExportRequests.forEach(
      (dataExportRequest) => {
        if (!dataExportRequest.id) {
          return;
        }

        state.dataExportRequests.push(
          {
            ...dataExportRequest,
            downloaded: dataExportRequest?.status === "DOWNLOADED",
            error: dataExportRequest?.status === "ERROR",
            outdated: dataExportRequest?.status === "OUTDATED",
            processing: dataExportRequest?.status === "PROCESSING",
            ready: dataExportRequest?.status === "READY",
            reprocessing: dataExportRequest?.status === "REPROCESSING"
          }
        );
      }
    );
  },
  reset(state) {
    state.dataExportRequests = [];
  }
}
const getters = {
  dataExportRequests: (state) => {
    return state.dataExportRequests;
  }
}

export const dataexportrequest = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

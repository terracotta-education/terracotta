import {experimentService} from "@/services";

const state = {
  experiment: null,
  experiments: [],
  importRequests: []
}

const actions = {
  resetExperiment: ({commit}) => {
    commit("resetExperiment");
  },
  resetExperiments: ({commit}) => {
    commit("resetExperiments");
  },
  createExperiment: () => {
    return experimentService.create();
  },
  fetchExperimentById: ({commit}, experimentId) => {
    return experimentService.getById(experimentId)
      .then(response => {
        if (response.status === 200) {
          commit("setExperiment", response.data);
        }
      })
      .catch(response => console.log("fetchExperimentById | catch", {response}))
  },
  fetchExperiments: ({commit}) => {
    return experimentService.getAll()
      .then(response => {
        if (response.status===200) {
          commit("setExperiments", response.data);
        }
      })
      .catch(response => console.log("fetchExperimentById | catch", {response}))
  },
  updateExperiment: ({commit}, experiment) => {
    return experimentService.update(experiment)
      .then(response => {
        if (response.status===200) {
          commit("setExperiment", experiment);
        }
        return response;
      })
      .catch(response => console.log("updateExperiment | catch", {response}))
  },
  deleteExperiment: ({commit}, experimentId) => {
    return experimentService.delete(experimentId)
      .then(response => {
        if (response?.status === 200) {
          commit("deleteExperiment", experimentId)
          return response;
        }
      })
      .catch(response => console.log("deleteExperiment | catch", {response}))
  },
  async exportExperiment(_, experimentId) {
    try {
      await experimentService.export(experimentId);
    } catch (e) {
      console.error("exportExperiment catch", e);
    }
  },
  async importExperiment({commit}, payload) {
    // payload = zipFile
    try {
      const importRequest = await experimentService.import(payload);
      commit("addImportRequest", importRequest);
      return importRequest;
    } catch (e) {
      console.error("importExperiment catch", e);
    }
  },
  async pollImport({commit}, payload) {
    // payload = importId
    try {
      const importRequest = await experimentService.pollImport(payload);
      commit("addImportRequest", importRequest.data);
    } catch (e) {
      console.error("pollImport catch", e);
    }
  },
  async pollImports({commit}) {
    try {
      const importRequests = await experimentService.pollImports();
      commit("addImportRequests", importRequests.data);
    } catch (e) {
      console.error("pollImports catch", e);
    }
  },
  async acknowledgeImport({commit}, payload) {
    // payload = importId, status
    try {
      await experimentService.acknowledgeImport(...payload);
      commit("resetImportRequest");
    } catch (e) {
      console.error("acknowledgeImport catch", e);
    }
  },
  resetImportRequests: ({commit}) => {
    commit("resetImportRequests");
  }
}

const mutations = {
  resetExperiment(state) {
    state.experiment = null;
    state.importRequests = [];
  },
  setExperiment(state, data) {
    state.experiment = data;

    if (!state.experiments.length) {
      state.experiments.push(data);
      return;
    }

    const foundIndex = state.experiments.findIndex(e => e.experimentId === data.experimentId);

    if (foundIndex >= 0) {
      state.experiments.splice(foundIndex, 1, data);
    } else {
      state.experiments.push(data);
    }
  },
  resetExperiments(state) {
    state.experiments = null;
    state.importRequests = [];
  },
  setExperiments(state, data) {
    state.experiments = data || [];
  },
  setConditions(state, conditions) {
    state.experiment.conditions = conditions;
  },
  setCondition(state, condition) {
    const foundIndex = state.experiment.conditions.findIndex(c => c.conditionId === condition.conditionId);
    if (foundIndex >= 0) {
      state.experiment.conditions.splice(foundIndex, 1, condition);
    } else {
      state.experiment.conditions.push(condition);
    }
  },
  deleteExperiment(state, experimentId) {
    state.experiments = state.experiments.filter((item) => item.experimentId !== experimentId)
  },
  deleteCondition(state, condition) {
    state.experiment.conditions = state.experiment.conditions.filter((item) => item.conditionId !== condition.conditionId)
  },
  addImportRequest(state, importRequest) {
    if (!importRequest) {
      return;
    }

    if (!importRequest.id) {
      return;
    }

    importRequest = {
      ...importRequest,
      complete: importRequest?.status === "COMPLETE",
      completeAcknowledged: importRequest?.status === "COMPLETE_ACKNOWLEDGED",
      error: importRequest?.status === "ERROR",
      errorAcknowledged: importRequest?.status === "ERROR_ACKNOWLEDGED",
      processing: importRequest?.status === "PROCESSING"
    }

    const foundIndex = state.importRequests.findIndex(ir => ir.id === importRequest.id);

    if (foundIndex >= 0) {
      state.importRequests.splice(foundIndex, 1, importRequest);
    } else {
      state.importRequests.push(importRequest);
    }
  },
  addImportRequests(state, importRequests) {
    if (!importRequests) {
      return;
    }

    state.importRequests = [];

    for (const importRequest of importRequests) {
      if (!importRequest.id) {
        continue;
      }

      state.importRequests.push({
        ...importRequest,
        complete: importRequest?.status === "COMPLETE",
        completeAcknowledged: importRequest?.status === "COMPLETE_ACKNOWLEDGED",
        error: importRequest?.status === "ERROR",
        errorAcknowledged: importRequest?.status === "ERROR_ACKNOWLEDGED",
        processing: importRequest?.status === "PROCESSING"
      });
    }
  },
  resetImportRequests(state) {
    state.importRequests = [];
  }
}

const getters = {
  conditions(state) {
    return state.experiment.conditions;
  },
  experiment(state) {
    return state.experiment;
  },
  experiments(state) {
    return state.experiments;
  },
  importRequests(state) {
    return state.importRequests;
  }
}

export const experiment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

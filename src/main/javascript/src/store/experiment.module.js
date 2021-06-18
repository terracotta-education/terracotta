import {experimentService} from '@/services'

const state = {
  experiment: null,
  experiments: null
};

const actions = {
  resetExperiment: ({commit}) => {
    commit('setExperiment', {})
  },
  createExperiment: () => {
    return experimentService.create()
  },
  fetchExperimentById: ({commit}, experimentId) => {
    return experimentService.getById(experimentId)
      .then(data => {
        commit('setExperiment', data)
      })
      .catch(response => {
        console.log("fetchExperimentById | catch", {response})
      })
  },
  fetchExperiments: ({commit}) => {
    return experimentService.getAll()
      .then(data => {
        commit('setExperiments', data)
      })
      .catch(response => {
        console.log("fetchExperimentById | catch", {response})
      })
  },
  updateExperiment: ({commit}, experiment) => {
    return experimentService.update(experiment)
      .then(commit('setExperiment', experiment))
      .catch(response => {
        console.log("updateExperiment | catch", {response})
      })
  },
  deleteExperiment: ({commit}, experimentId) => {
    return experimentService.delete(experimentId)
      .then(response => {
        if (response?.status === 200) {
          commit('deleteExperiment', experimentId)
        }
      })
      .catch(response => {
        console.log("deleteExperiment | catch", {response})
      })
  }
};

const mutations = {
  setExperiment(state, data) {
    state.experiment = data
  },
  setExperiments(state, data) {
    state.experiments = data
  },
  setConditions(state, conditions) {
    state.experiment.conditions = conditions
  },
  setCondition(state, condition) {
    const foundIndex = state.experiment.conditions.findIndex(c => c.conditionId === condition.conditionId)
    if (foundIndex >= 0) {
      state.experiment.conditions[foundIndex] = condition
    } else {
      state.experiment.conditions.push(condition)
    }
  },
  deleteExperiment(state, experimentId) {
    state.experiments = state.experiments.filter(function (item) {
      return item.experimentId !== experimentId
    })
  },
  deleteCondition(state, condition) {
    state.experiment.conditions = state.experiment.conditions.filter(function (item) {
      return item.conditionId !== condition.conditionId
    })
  }
};

const getters = {
  conditions(state) {
    return state.experiment.conditions
  },
  experiments(state) {
    return state.experiments
  }
};

export const experiment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
};
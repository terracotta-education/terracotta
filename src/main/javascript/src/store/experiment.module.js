import {experimentService} from '@/services'

const state = {
  experiment: null,
  experiments: null
}

const actions = {
  resetExperiment: ({commit}) => {
    commit('resetExperiment')
  },
  resetExperiments: ({commit}) => {
    commit('resetExperiments')
  },
  createExperiment: () => {
    return experimentService.create()
  },
  fetchExperimentById: ({commit,state}, experimentId) => {
    if (parseInt(state.experiment?.experimentId) !== parseInt(experimentId)) {
      commit('resetExperiment')
    }
    return experimentService.getById(experimentId)
      .then(response => {
        if (response.status===200) {
          commit('setExperiment', response.data)
        }
      })
      .catch(response => console.log('fetchExperimentById | catch', {response}))
  },
  fetchExperiments: ({commit}) => {
    commit('resetExperiments')
    return experimentService.getAll()
      .then(response => {
        if (response.status===200) {
          commit('setExperiments', response.data)
        }
      })
      .catch(response => console.log('fetchExperimentById | catch', {response}))
  },
  updateExperiment: ({commit}, experiment) => {
    return experimentService.update(experiment)
      .then(response => {
        if (response.status===200) {
          commit('setExperiment', experiment)
        }
        return response
      })
      .catch(response => console.log('updateExperiment | catch', {response}))
  },
  updateExperimentAndExposures: ({commit}, experiment) => {
    return experimentService.updateExperimentAndExposures(experiment)
      .then(response => {
        if (response.status === 200) {
          commit('setExperiment', experiment)
        }
        return response
      })
      .catch(response => console.log('updateExperiment | catch', {response}))
  },
  deleteExperiment: ({commit}, experimentId) => {
    return experimentService.delete(experimentId)
      .then(response => {
        if (response?.status === 200) {
          commit('deleteExperiment', experimentId)
          return response
        }
      })
      .catch(response => console.log('deleteExperiment | catch', {response}))
  }
}

const mutations = {
  resetExperiment(state) {
    state.experiment = null
  },
  setExperiment(state, data) {
    state.experiment = data
  },
  resetExperiments(state) {
    state.experiments = null
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
}

const getters = {
  conditions(state) {
    return state.experiment.conditions
  },
  experiment(state) {
    return state.experiment
  },
  experiments(state) {
    return state.experiments
  }
}

export const experiment = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

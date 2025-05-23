import {outcomeService} from '@/services'

const state = {
  outcome: null,
  outcomes: [],
  outcomeScores: [],
  outcomePotentials: [],
  experimentOutcomes: null
}

const actions = {
  async resetOutcome ({commit}) {
    commit('resetOutcome')
  },
  async resetOutcomePotentials ({commit}) {
    commit('resetOutcomePotentials')
  },
  async createOutcome ({commit}, payload) {
    // payload = experimentId, exposureId, title, max_points, external, lmsType, lmsOutcomeId
    return outcomeService.create(...payload)
      .then(response => {
        if (response?.status === 200 || response?.status === 201) {
          const outcome = response.data
          commit('setOutcome', outcome)
          return outcome
        }
      })
      .catch(response => console.log('createOutcome | catch', {response}))
  },
  async updateOutcome({commit}, payload) {
    // payload = experimentId, exposureId, outcome
    return outcomeService.updateOutcome(...payload)
      .then(response => {
        if (response.status===200) {
          commit('setOutcome', payload)
        }
        return response
      })
      .catch(response => console.log('updateOutcome | catch', {response, commit}))
  },
  async deleteOutcome ({commit}, payload) {
    return outcomeService.deleteOutcome(...payload)
    .then(response => {
      if (response?.status === 200) {
        commit('resetOutcome')
        return response
      }
    })
    .catch(response => console.log('deleteOutcome | catch', {response}))
  },
  async fetchOutcomeById({commit,state}, payload) {
    // payload = experimentId, exposureId, outcomeId
    const outcomeId = payload[2]
    if (parseInt(state.outcome?.outcomeId) !== parseInt(outcomeId)) {
      commit('resetOutcome')
    }
    return outcomeService.getById(...payload)
    .then(response => {
      if (response.status===200) {
        commit('setOutcome', response.data)
      }
    })
    .catch(response => console.log('fetchOutcomeById | catch', {response}))
  },
  async fetchOutcomes({commit}, payload) {
    // payload = experimentId, exposureId
    return outcomeService.getAll(...payload)
      .then(response => {
        if ((response.status===200 || response.status===204) && response.data) {
          commit('setOutcomes', response.data)
        }
        return response
      })
      .catch(response => console.log('fetchOutcomes | catch', {response}))
  },
  async fetchOutcomesByExposures({commit}, payload) {
    // payload = experimentId, exposureIds
    return outcomeService.getByExperimentId(...payload)
      .then(response => {
        commit('setExperimentOutcomes', response)
      })
  },
  async fetchOutcomesByExperimentId({commit}, payload) {
    // payload = experimentId
    return outcomeService.getAllByExperimentId(...payload)
      .then(response => {
        commit('setOutcomes', response.data)
      })
  },
  async fetchOutcomeScores({commit}, payload) {
    // payload = experimentId, exposureId, outcomeId
    return outcomeService.getOutcomeScoresById(...payload)
      .then(response => {
        if ((response.status===200 || response.status===204) && response.data) {
          commit('setOutcomeScores', response.data)
        }
        return response
      })
      .catch(response => console.log('fetchOutcomeScores | catch', {response}))
  },
  async updateOutcomeScores({dispatch}, payload) {
    // payload = experimentId, exposureId, outcomeId, scores
    return outcomeService.updateOutcomeScores(...payload)
      .then(() => {
        dispatch('fetchOutcomeScores', payload);
      })
      .catch(response => console.log('updateOutcomeScores | catch', {response}))
  },
  async fetchOutcomePotentials({commit}, experimentId) {
    return outcomeService.getOutcomePotentials(parseInt(experimentId))
      .then(response => {
        if (response.status===200 && response.data) {
          commit('setOutcomePotentials', response.data)
        }
        return response
      })
      .catch(response => console.log('fetchOutcomePotentials | catch', {response}))
  }
}

const mutations = {
  resetOutcome(state) {
    state.outcome = null
  },
  resetOutcomePotentials(state) {
    state.outcomePotentials = []
  },
  setOutcome(state, data) {
    // data = experimentId, exposureId, outcome
    const outcome = (Array.isArray(data))? data[2] : data
    if (outcome.outcomeId) {
      state.outcome = outcome
    }
  },
  setOutcomes(state, data) {
    state.outcomes = data
  },
  setOutcomePotentials(state, data) {
    state.outcomePotentials = data
  },
  setOutcomeScores(state, data) {
    state.outcomeScores = data
  },
  setExperimentOutcomes(state, data) {
    let arr = []
    for (const exposureList of data) {
      if (exposureList.data?.length) {
        for (const outcome of exposureList.data) {
          arr.push(outcome)
        }
      }
    }
    state.experimentOutcomes = arr
  }
}

const getters = {
  outcome(state) {
    return state.outcome
  },
  outcomes(state) {
    return state.outcomes
  },
  outcomeScores(state) {
    return state.outcomeScores
  },
  outcomePotentials(state) {
    return state.outcomePotentials
  },
  experimentOutcomes(state) {
    return state.experimentOutcomes
  }
}

export const outcome = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
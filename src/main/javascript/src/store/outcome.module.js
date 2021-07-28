import {outcomeService} from '@/services'

const state = {
  outcome: null,
  outcomes: [],
  experimentOutcomes: null
}

const actions = {
  async resetOutcome ({commit}) {
    commit('resetOutcome')
  },
  async createOutcome ({commit}, payload) {
    // payload = experiment_id, exposure_id, title, max_points, external
    try {
      const response = await outcomeService.create(...payload)
      if (response?.status === 200 || response?.status === 201) {
        const outcome = response.data
        commit('setOutcome', outcome)
        return outcome
      }
    } catch (e) {
      console.error(e)
    }
  },
  async fetchOutcomes({commit}, payload) {
    // payload = experiment_id, exposure_id
    return outcomeService.getAll(...payload)
      .then(response => {
        if (response.status===200 && response.data) {
          commit('setOutcomes', response.data)
        }
        return response
      })
      .catch(response => console.log('updateOutcome | catch', {response}))
  },
  async fetchOutcomeById({commit,state}, payload) {
    // payload = experiment_id, exposure_id, outcome_id
    const outcome_id = payload[2]
    if (parseInt(state.outcome?.outcomeId) !== parseInt(outcome_id)) {
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
  async fetchOutcomesByExperimentId({commit}, payload) {
    // payload = experiment_id, exposure_ids
    return outcomeService.getByExperimentId(...payload)
      .then(response => {
        commit('setExperimentOutcomes', response)
      })
  },
  async updateOutcomeScores ({commit}, payload) {
    // payload = experiment_id, exposure_id, outcome_id, scores
    return outcomeService.updateOutcomeScores(payload)
      .then(response => {
        console.log({response})
        if (response.status===200) {
          // commit('setOutcomeScores', response)
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
  }
}

const mutations = {
  resetOutcome(state) {
    state.outcome = null
  },
  setOutcome(state, data) {
    state.outcome = data
  },
  setOutcomes(state, data) {
    state.outcomes = data
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
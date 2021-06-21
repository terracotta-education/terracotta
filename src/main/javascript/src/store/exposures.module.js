import { exposuresService } from '@/services'

const state = {
  exposures: null,
}

const actions = {
  fetchExposures: ({ commit }, experimentId) => {
    return exposuresService
      .getAll(experimentId)
      .then((data) => {
        commit('setExposuresService', data)
      })
      .catch((response) => {
        console.log('fetchExposures | catch', { response })
      })
  },
}

const mutations = {
  setExposuresService(state, data) {
    state.exposures = data
  },
}

const getters = {
  exposures(state) {
    return state.exposures
  },
}

export const exposures = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
}

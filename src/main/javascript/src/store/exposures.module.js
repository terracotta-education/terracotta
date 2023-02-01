import { exposuresService } from '@/services'

const state = {
  exposures: null,
}

const actions = {
  async fetchExposures ({ commit }, experimentId) {
    return exposuresService
      .getAll(experimentId)
      .then((data) => {
        commit('setExposuresService', data)
      })
      .catch((response) => {
        console.log('fetchExposures | catch', { response })
      });
  },
  async createExposures ({state}, experimentId) {
    return await exposuresService
      .createExposures(experimentId)
      .catch((response) => {
        console.log('createExposures | catch', { response, state })
      });
  },
  resetExposures({state}) {
    state.exposures = [];
  },
}

const mutations = {
  setExposuresService(state, data) {
    state.exposures = data
  },
};

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

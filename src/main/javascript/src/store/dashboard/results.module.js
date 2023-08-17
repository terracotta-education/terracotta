import {resultsDashboardService} from '@/services'

const state = {
  resultsDashboard: {
    experimentId: null,
    overview: null,
    outcomes: null
  }
}

const actions = {
  async getOverview({commit}, experimentId) {
    try {
      const response = await resultsDashboardService.overview(experimentId);
      const results = response?.data;

      commit('setOverview', results.overview);
    } catch (error) {
      console.error('overview catch', error);
    }
  },
  async getOutcomes ({commit}, payload) {
    // payload = experimentId, body
    try {
      const response = await resultsDashboardService.outcomes(...payload);
      const results = response?.data;

      commit('setOutcomes', results.outcomes);
    } catch (error) {
      console.log('outcomes catch', error);
    }
  },
  clearOverview({commit}) {
    commit('setOverview', null);
  },
  clearOutcomes({commit}) {
    commit('setOutcomes', null);
  },
  resetResultsDashboard({commit}) {
    commit('setExperimentId', null);
    commit('setOverview', null);
    commit('setOutcomes', null);
  }
}

const mutations = {
  setExperimentId(state, experimentId) {
    state.resultsDashboard.experimentId = experimentId;
  },
  setOverview(state, overview) {
    state.resultsDashboard.overview = overview;
  },
  setOutcomes(state, outcomes) {
    state.resultsDashboard.outcomes = outcomes;
  }
}

const getters = {
  resultsDashboard: (state) => {
    return state.resultsDashboard;
  },
  overview: (state) => {
    return state.resultsDashboard.overview;
  },
  outcomes: (state) => {
    return state.resultsDashboard.outcomes;
  }
}

export const resultsDashboard = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

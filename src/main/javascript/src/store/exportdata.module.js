import {exportDataService} from '@/services'

const state = {
  file: null,
}

const actions = {
  fetchExportData: ({commit}, experimentId) => {
    return exportDataService.getZip(experimentId)
      .then(response => {
        if (response.status===200) {
          commit('setExportData', response.data)
        }
      })
      .catch(response => console.log('fetchExportData | catch', {response}))
  }
}

const mutations = {
  setExportData(state, data) {
    state.file = data
  },
}

const getters = {
  exportData(state) {
    return state.file
  },
}

export const exportdata = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
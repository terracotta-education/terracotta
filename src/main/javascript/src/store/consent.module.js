import {consentService} from '@/services'

const state = {
  file: null,
  title: ''
}

const actions = {
  resetConsent: ({commit}) => {
    commit('setConsentTitle', '')
    commit('setConsentFile', null)
  },
  createConsent: ({state}, experiment_id) => {
    return consentService.create(experiment_id, state)
    .catch(response => {
      console.log('setCondition | catch', {response})
    })
  },
  setConsentFile: ({commit}, file) => {
    commit('setConsentFile', file)
  }
}

const mutations = {
  setConsent(state, consent) {
    state = consent
  },
  setConsentTitle(state, title) {
    state.title = title
  },
  setConsentFile(state, file) {
    state.file = file
  },
}

const getters = {
  consent(state) {
    return state
  },
}

export const consent = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}
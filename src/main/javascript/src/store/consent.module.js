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
    return consentService.create(experiment_id, state).then((response) => {
      if (response.status !== 200) {
        throw new Error("Consent file upload failed");
      }
    });
  },
  setConsentFile: ({commit}, file) => {
    commit('setConsentFile', file)
  },
  getConsentFile: ({commit}, experiment_id) => {
    return consentService.getConsentFile(experiment_id).then(response => {
      if (response.status === 200) {
        commit('setConsentFile', response.base)
      } else {
        console.log('getConsentFile | catch', {response})
      }
    })
    .catch(response => console.log('getConsentFile | catch', {response}))
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

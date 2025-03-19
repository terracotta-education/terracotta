import {consentService} from '@/services'

const state = {
  file: null,
  title: ''
}

const actions = {
  resetConsent: ({commit}) => {
    commit('setConsentTitle', '')
  },
  createConsent: ({state}, payload) => {
    // payload = experimentId, pdfFile, state.title
    return consentService.create(...payload).then((response) => {
      if (response.status !== 200) {
        throw new Error("Consent file upload failed", {state});
      }
    });
  },
  setConsentTitle: ({commit}, title) => {
    commit('setConsentTitle', title);
  },
  async getConsentFile({state}, experimentId) {
    return await consentService.getConsentFile(experimentId).then(response => {
      if (response.status === 200) {
        return response.base;
      } else if (response.status === 404) {
        return null;
      } else {
        console.log('getConsentFile | catch', {state, response})
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
  }
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

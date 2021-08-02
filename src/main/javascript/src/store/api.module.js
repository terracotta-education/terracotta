import {apiService} from '@/services'
import jwt_decode from 'jwt-decode'
import { userInfo } from '../helpers'

const state = {
  lti_token: '',
  api_token: '',
  aud: '',
  userInfo: '',
  experimentId: '',
  consent: '',
  userId: ''
}

const actions = {
  setLtiToken: ({commit, dispatch}, token) => {
    // decode token to get the aud (api base url)
    const decodedToken = jwt_decode(token)
    console.log(decodedToken)
    commit('setLtiToken', token)
    commit('setAud', decodedToken.aud)
    commit('setExperimentId', decodedToken.experimentId)
    commit('setConsent', decodedToken.consent)
    commit('setUserId', decodedToken.userId)
    commit('setUserInfo', userInfo(decodedToken.roles))
    dispatch('setApiToken', token)
  },
  setApiToken: ({commit}, token) => {
    // send a token to the API to receive an API token for the bearer auth header
    return apiService.getApiToken(token)
      .then(data => {
        if (typeof data === 'string') {
          const decodedToken = jwt_decode(data)
          commit('setApiToken', data)
          commit('setExperimentId', decodedToken.experimentId)
          commit('setConsent', decodedToken.consent)
          commit('setUserId', decodedToken.userId)
          commit('setUserInfo', userInfo(decodedToken.roles))
        }
      })
      .catch(response => {
        console.log('setApiToken | catch', {response})
      })
  },
  refreshToken: ({commit}) => {
    // send a refresh to the API and receive an API token for the bearer auth header
    return apiService.refreshToken()
      .then(data => {
        if (typeof data === 'string') {
          const decodedToken = jwt_decode(data)
          commit('setApiToken', data)
          commit('setExperimentId', decodedToken.experimentId)
          commit('setConsent', decodedToken.consent)
          commit('setUserId', decodedToken.userId)
          commit('setUserInfo', userInfo(decodedToken.roles))
        }
      })
      .catch(response => {
        console.log('refreshToken | catch', {response})
      })
  },
  reportStep: ({state}, {experimentId, step}) => {
    // report the current step to the server to do some magic
    // used for exposure_type, participation_type, and distribution_type selection steps
    return apiService.reportStep(experimentId, step)
      .then(data => {
        console.log('reportStep | then', {state, data})
      })
      .catch(response => {
        console.log('reportStep | catch', {response})
      })
  }
}

const mutations = {
  setLtiToken(state, data) {
    state.lti_token = data
  },
  setApiToken(state, data) {
    state.api_token = data
  },
  setAud(state, data) {
    state.aud = data
  },
  setUserInfo(state, data) {
    state.userInfo = data
  },
  setExperimentId(state, data) {
    state.experimentId = data
  },
  setConsent(state, data) {
    state.consent = data
  },
  setUserId(state, data) {
    state.userId = data
  }
}

const getters = {
  lti_token(state) {
    return state.lti_token
  },
  aud(state) {
    return state.aud
  },
  hasTokens(state) {
    // check if both tokens are set in the state
    return state.lti_token.length > 0 && state.api_token.length > 0
  },
  userInfo(state) {
    return state.userInfo
  },
  experimentId(state) {
    return state.experimentId
  },
  consent(state) {
    return state.consent
  },
  userId(state) {
    return state.userId
  }
}

export const api = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

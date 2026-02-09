import {apiService} from '@/services'
import jwt_decode from 'jwt-decode'
import { userInfo } from '../helpers'

const state = {
  lti_token: '',
  api_token: '',
  aud: '',
  userInfo: '',
  experimentId: '',
  assignmentId: '',
  consent: '',
  userId: '',
  lmsApiOAuthURL: '',
}

const actions = {
  setLtiToken: ({commit, dispatch}, token) => {
    // decode token to get the aud (api base url)
    const decodedToken = jwt_decode(token)
    commit('setLtiToken', token)
    commit('setAud', decodedToken.aud)
    commit('setExperimentId', decodedToken.experimentId)
    commit('setConsent', decodedToken.consent)
    commit('setAssignmentId', decodedToken.lmsAssignmentId)
    commit('setUserId', decodedToken.userId)
    commit('setUserInfo', userInfo(decodedToken.roles))
    return dispatch('setApiToken', token)
  },
  setApiToken: ({commit}, token) => {
    // send a token to the API to receive an API token for the bearer auth header
    return apiService.getApiToken(token)
      .then(data => {
        if (typeof data === 'string') {
          const decodedToken = jwt_decode(data)
          commit('setApiToken', data)
          commit('setAud', decodedToken.aud)
          commit('setExperimentId', decodedToken.experimentId)
          commit('setAssignmentId', decodedToken.lmsAssignmentId)
          commit('setConsent', decodedToken.consent)
          commit('setUserId', decodedToken.userId)
          commit('setUserInfo', userInfo(decodedToken.roles))
        }
      })
      .catch(response => {
        console.error('setApiToken | catch', {response})
      })
  },
  refreshToken: ({commit}, token) => {
    // send a refresh to the API and receive an API token for the bearer auth header
    return apiService.refreshToken(token)
      .then(data => {
        if (typeof data === 'string') {
          const decodedToken = jwt_decode(data)
          commit('setAud', decodedToken.aud)
          commit('setApiToken', data)
          commit('setExperimentId', decodedToken.experimentId)
          commit('setAssignmentId', decodedToken.lmsAssignmentId)
          commit('setConsent', decodedToken.consent)
          commit('setUserId', decodedToken.userId)
          commit('setUserInfo', userInfo(decodedToken.roles))
        }
      })
      .catch(response => {
        console.error('refreshToken | catch', {response})
      })
  },
  async reportStep({state}, {experimentId, step, parameters = null, preferLmsChecks = false}) {
    // report the current step to the server to do some magic
    // used for exposure_type, participation_type, and distribution_type selection steps
    return await apiService.reportStep(experimentId, step, parameters, preferLmsChecks)
      .then(data => {
        return data
      })
      .catch(response => {
        console.error('reportStep | catch', {response, state})
        return response
      })
  },
  async deepLinkJwt({state}, id) {
    // get a deeplink jwt from the server
    return await apiService.deepLinkJwt(id)
      .then(data => {
        return JSON.parse(data);
      })
      .catch(response => {
        console.error('deepLinkJwt | catch', {response, state})
        return response;
      })
  },
  setLmsApiOAuthURL({commit}, url) {
    commit('setLmsApiOAuthURL', url);
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
  setAssignmentId(state, data) {
    state.assignmentId = data
  },
  setConsent(state, data) {
    state.consent = data
  },
  setUserId(state, data) {
    state.userId = data
  },
  setLmsApiOAuthURL(state, data) {
    state.lmsApiOAuthURL = data;
  }
}

const getters = {
  lti_token(state) {
    return state.lti_token
  },
  api_token(state) {
    return state.api_token
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
  assignmentId(state) {
    return state.assignmentId
  },
  consent(state) {
    return state.consent
  },
  userId(state) {
    return state.userId
  },
  lmsApiOAuthURL(state) {
    return state.lmsApiOAuthURL;
  }
}

export const api = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters
}

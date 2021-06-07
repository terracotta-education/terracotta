import { apiService } from "@/services";

const state = {
    lti_token: "",
    api_token: "",
}


const actions = {
    setLtiToken: ({commit,dispatch}, token) => {
        commit('setLtiToken', token)
        dispatch('setApiToken', token)
    },
    setApiToken: ({commit}, token) => {
        // send a token to the API to receive an API token for the bearer auth header
        return apiService.getApiToken(token)
                .then(data => {
                    if (typeof data === 'string') {
                        commit('setApiToken', data)
                    }
                })
                .catch(response => {
                    console.log("fetchExperimentById | catch",{response})
                })
    },
    refreshToken: ({commit}) => {
        // send a refresh to the API and receive an API token for the bearer auth header
        return apiService.refreshToken()
                .then(data => {
                    if (typeof data === 'string') {
                        commit('setApiToken', data)
                    }
                })
                .catch(response => {
                    console.log("fetchExperimentById | catch",{response})
                })
    },
}

const mutations = {
    setLtiToken(state, data) {
        state.lti_token = data
    },
    setApiToken(state, data) {
        state.api_token = data
    },
}

const getters = {
    hasTokens(state) {
        // check if both tokens are set in the state
        return state.lti_token.length>0 && state.api_token.length>0
    }
}

export const api = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
}

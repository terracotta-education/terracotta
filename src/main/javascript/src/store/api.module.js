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
        return state.lti_token && state.api_token
    }
}

export const api = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
}

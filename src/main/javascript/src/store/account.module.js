import { userService } from '@/services'
import router from '../router/index.js'
import { authHeader } from '@/helpers'

const state = {
    user: null
}

const actions = {
    login({ dispatch, commit }, { email, password }) {
        commit('loginRequest', { email })
        dispatch('alert/error', null, { root: true })

        return userService.login(email, password)
        .then(
            response => {
                if (response && response.status === 'success') {
                    commit('loginSuccess', response)
                    if (response.user?.length === 1) {
                        router.push('/')
                    }
                    return {
                        status: 'success'
                    }
                } else {
                    commit('loginFailure', response)
                    dispatch('alert/error', response.message, { root: true })
                }
            },
            error => {
                commit('loginFailure', error)
                dispatch('alert/error', error, { root: true })
                return {
                    status: 'fail',
                    error: error
                }
            }
        ).catch(error => {
            return {
                status: 'fail',
                error: error
            }
        })
    },
    logout({ commit }) {
        const requestOptions = {
            method: 'POST',
            headers: authHeader(),
            // TODO - pass user data for logout
            body: JSON.stringify({})
        }

        // TODO - set correct API route
        fetch('/api/users/logout', requestOptions).then(() => {
            // TODO - dispatch logout actions
            commit('userLogout')
        })
    }
}

const mutations = {
    loginRequest(state, user) {
        state.user = user
    },
    loginSuccess(state, response) {
        state.user = response.user
    },
    loginFailure(state) {
        state.user = null
    },
    userLogout(state) {
        state.user = null
        localStorage.removeItem('terracotta')
    }
}

const getters = {
    isLoggedIn: state => {
        return state.user
    }
}

export const account = {
    namespaced: true,
    state,
    actions,
    mutations,
    getters
}

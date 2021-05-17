// import { userService } from '@/services'
// import router from '../router/index.js'
// import { authHeader } from '@/helpers'

const state = {
    user: {
        api_token: null,
        lti_token: null,
    }
}

const actions = {

}

const mutations = {

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

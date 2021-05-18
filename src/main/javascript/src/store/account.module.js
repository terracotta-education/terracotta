// import { userService } from '@/services'
// import router from '../router/index.js'
// import { authHeader } from '@/helpers'

// TODO - set LTI & API tokens when local Canvas instance is set up
const state = {
    user: {
        lti_token: "",
        api_token: "",
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

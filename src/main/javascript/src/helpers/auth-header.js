import store from '../store/index.js'

export function initHeader() {
    if (store.state.api?.lti_token) {
        return {
            'Authorization': 'Bearer ' + store.state.api.lti_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}
export function authHeader() {
    console.log('Store.state.api.api_token', store.state.api.api_token)
    console.log('store.state.api.lti_token', store.state.api.lti_token)
    if (store.state.api?.api_token) {
        return {
            'Authorization': 'Bearer ' + store.state.api.api_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}
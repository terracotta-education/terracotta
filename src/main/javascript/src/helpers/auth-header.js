import store from '../store/index.js'

export function authHeader() {
    if (store.state.account && store.state.account.user && store.state.account.user.lti_token) {
        return {
            'Authorization': 'Bearer ' + store.state.account.user.lti_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}
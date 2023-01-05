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
    if (store.state.api?.api_token) {
        return {
            'Authorization': 'Bearer ' + store.state.api.api_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}

export function fileAuthHeader() {
    if (store.state.api?.api_token) {
        return {
            'Authorization': 'Bearer ' + store.state.api.api_token,
            // 'Content-Type': 'multipart/form-data'
        };
    } else {
        return {};
    }
}

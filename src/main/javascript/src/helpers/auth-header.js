import { api } from "@/store/api.module";

export function initHeader() {
    if (api().lti_token) {
        return {
            'Authorization': 'Bearer ' + api().lti_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}

export function authHeader() {
    if (api().api_token) {
        return {
            'Authorization': 'Bearer ' + api().api_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}

export function fileAuthHeader() {
    if (api().api_token) {
        return {
            'Authorization': 'Bearer ' + api().api_token,
            // 'Content-Type': 'multipart/form-data'
        };
    } else {
        return {};
    }
}

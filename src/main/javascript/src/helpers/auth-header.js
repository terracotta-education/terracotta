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
    const apiStore = api();

    if (apiStore.api_token) {
        // Every service builds its request headers through here, so this is the one shared
        // choke point that sees a token about to be used regardless of which page/component is
        // making the call - catching an expired token here (rather than relying solely on
        // App.vue's 59-minute interval/visibility-change checks, which can miss a tab that's
        // been backgrounded or otherwise throttled) means expiry is flagged the moment anything
        // actually tries to use the stale token, not only when that periodic check happens to run.
        if (apiStore.isApiTokenExpired()) {
            apiStore.markSessionExpired();
        }

        return {
            'Authorization': 'Bearer ' + apiStore.api_token,
            'Content-Type': 'application/json'
        };
    } else {
        return {};
    }
}

export function fileAuthHeader() {
    const apiStore = api();

    if (apiStore.api_token) {
        if (apiStore.isApiTokenExpired()) {
            apiStore.markSessionExpired();
        }

        return {
            'Authorization': 'Bearer ' + apiStore.api_token,
            // 'Content-Type': 'multipart/form-data'
        };
    } else {
        return {};
    }
}

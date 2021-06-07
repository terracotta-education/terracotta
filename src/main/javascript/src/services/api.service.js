import { authHeader } from "@/helpers";

const TEMP_canvas_url = "https://test-lti.unicon.net"
const base_url = `${window.location.protocol}//${window.location.host}`

/**
 * Register methods
 */
export const apiService = {
    getApiToken,
    refreshToken
}

/**
 * get the lti token and set the api token
 */
function getApiToken(token) {
    const requestOptions = {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    }

    return fetch(`${TEMP_canvas_url}/api/oauth/trade`, requestOptions).then(response => {
        if (response.ok) {
            return response.text()
        }
    })
}

/**
 * Refresh the api token
 */
function refreshToken() {
    const requestOptions = {
        method: 'POST',
        headers: { ...authHeader() }
    }

    return fetch(`${base_url}/api/oauth/refresh`, requestOptions).then(response => {
        console.log({response})
        if (response.ok) {
            console.log(response.text())
            return response.text()
        }
    })
}

import {authHeader, isJson} from '@/helpers'
import store from '@/store/index'

/**
 * Register methods
 */
export const apiService = {
  deepLinkJwt,
  getApiToken,
  refreshToken,
  reportStep
}

/**
 * get the lti token and set the api token
 */
async function getApiToken(token) {
  const requestOptions = {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  }
  return fetch(`${store.getters['api/aud']}/api/oauth/trade`, requestOptions).then(response => {
    if (response.ok) {
      return response.text()
    }
  })
}

/**
 * Refresh the api token
 */
async function refreshToken() {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/oauth/refresh`, requestOptions).then(response => {
    if (response.ok) {
      return response.text()
    }
  })
}

/**
 * Refresh the api token
 */
async function deepLinkJwt(id) {
  const requestOptions = {
    method: "GET",
    headers: { ...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/deeplink/toJwt/${id}`, requestOptions).then(response => {
    if (response.ok) {
      return response.text();
    }
  })
}

/**
 * Report to the server which step has been completed
 */
async function reportStep(experimentId, step, parameters, preferLmsChecks = false) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader()},
    body: JSON.stringify({
      'step': step,
      'parameters': parameters
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/step?preferLmsChecks=${preferLmsChecks}`, requestOptions).then(handleResponse)
}


/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
  .then(text => {
    const data = (text && isJson(text)) ? JSON.parse(text) : text

    if (
      !response ||
      response.status === 401 ||
      response.status === 402 ||
      response.status === 500 ||
      response.status === 404
    ) {
      console.log('handleResponse | 401/402/500', {response})
    } else if (response.status === 409) {
      return {
        message: data
      }
    } else if (response.status === 204) {
      console.log('handleResponse | 204', {text, data, response})
      return []
    }

    const dataResponse = (data) ? {
      data,
      status: response.status
    } : null

    return dataResponse || response
  }).catch(text => {
    console.error('handleResponse | catch', {text})
  })
}

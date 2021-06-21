import {authHeader} from '@/helpers'
import store from '@/store/index'

/**
 * Register methods
 */
export const apiService = {
  getApiToken,
  refreshToken,
  reportStep
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
  return fetch(`${store.getters['api/aud']}/api/oauth/trade`, requestOptions).then(response => {
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
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/oauth/refresh`, requestOptions).then(response => {
    if (response.ok) {
      console.log(response.text())
      return response.text()
    }
  })
}

/**
 * Report to the server which step has been completed
 */
function reportStep(experiment_id, step) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader()},
    body: JSON.stringify({
      'step': step
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/step`, requestOptions).then(response => {
    if (response.ok) {
      console.log(response.text())
      return response.text()
    }
  })
}

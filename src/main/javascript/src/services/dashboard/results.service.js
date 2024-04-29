import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const resultsDashboardService = {
  overview,
  outcomes
}

/**
 * Get Results Summary
 */
async function overview(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/dashboard/results/overview`, requestOptions).then(handleResponse)
}

/**
 * Post Results Comparison
 */
async function outcomes(experimentId, body) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify(body)
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/dashboard/results/outcomes`, requestOptions).then(handleResponse)
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

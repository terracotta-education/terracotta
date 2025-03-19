import { authHeader } from '@/helpers'
import store from '@/store/index.js'

// /**
//  * Register methods
//  */
export const exposuresService = {
  getAll,
  getById,
  createExposures
}

/**
 * Get all Exposures
 */
async function getAll(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/exposures`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get individual Exposure
 */
function getById(experimentId, exposureId) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/exposures/${exposureId}`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Create Exposures for Experiment
 */
async function createExposures(experimentId) {
  const requestOptions = {
    method: 'POST',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/exposures/create`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response
    .text()
    .then((text) => {
      const data = text && JSON.parse(text)

      if (!response || !response.ok) {
        if (
          response.status === 401 ||
          response.status === 402 ||
          response.status === 500
        ) {
          console.log('handleResponse | 401/402/500', { response })
        } else if (response.status === 404) {
          console.log('handleResponse | 404', { response })
        }

        return response
      }

      return data || response
    })
    .catch((text) => {
      console.log('handleResponse | catch', { text })
    })
}

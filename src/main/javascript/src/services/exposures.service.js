import { authHeader } from '@/helpers'
import store from '@/store/index.js'

// /**
//  * Register methods
//  */
export const exposuresService = {
  getAll,
  getById,
}

/**
 * Get all Exposures
 */
function getAll(experiment_id) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get individual Exposure
 */
function getById(experiment_id, exposure_id) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}`,
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

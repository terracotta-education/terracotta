import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const experimentService = {
  getAll,
  getById,
  create,
  update,
  delete: _delete
}

/**
 * Get all Experiments
 */
function getAll() {
  const requestOptions = {
    method: 'GET',
    headers: authHeader()
  }

  return fetch(`${store.getters['api/aud']}/api/experiments?participants=true`, requestOptions).then(handleResponse)
}

/**
 * Create Experiment
 */
function create() {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify({})
  }

  return fetch(`${store.getters['api/aud']}/api/experiments`, requestOptions).then(handleResponse)
}

/**
 * Get individual Experiment
 */
function getById(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()},
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}?conditions=true`, requestOptions).then(handleResponse)
}

/**
 * Update Experiment
 */
function update(experiment) {
  const requestOptions = {
    method: 'PUT',
    headers: {...authHeader()},
    body: JSON.stringify(experiment)
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment.experimentId}`, requestOptions).then(handleResponse)
}

/**
 * Delete Experiment
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
function _delete(id) {
  const requestOptions = {
    method: 'DELETE',
    headers: authHeader()
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${id}`, requestOptions).then(handleResponse)
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
    .then((text) => {
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
      console.log('handleResponse | catch', {text})
    })
}
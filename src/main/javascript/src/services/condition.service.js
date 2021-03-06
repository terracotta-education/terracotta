import {authHeader, isJson} from '@/helpers'
import store from '@/store/index'

/**
 * Register methods
 */
export const conditionService = {
  create,
  update,
  updateAll,
  delete: _delete
}

/**
 * Create Condition
 */
function create(condition) {
  const requestOptions = {
    method: 'POST',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify(condition)
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${condition.experiment_experiment_id}/conditions`, requestOptions).then(handleResponse)
}

/**
 * Update Condition
 */
function update(condition) {
  const requestOptions = {
    method: 'PUT',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify(condition)
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${condition.experimentId}/conditions/${condition.conditionId}`, requestOptions).then(handleResponse)
}

/**
 * Update Condtions
 * @param condition
 */
function updateAll(conditions) {
  const requestOptions = {
    method: 'PUT',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
    body: JSON.stringify(conditions)
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${conditions.experimentId}/conditions`, requestOptions).then(handleResponse)
}



/**
 * Delete Condition
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
function _delete(condition) {
  const requestOptions = {
    method: 'DELETE',
    headers: {...authHeader(), 'Content-Type': 'application/json'},
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${condition.experimentId}/conditions/${condition.conditionId}`, requestOptions).then(handleResponse)
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

    return data || response
  }).catch(text => {
    console.log('handleResponse | catch', {text})
  })
}
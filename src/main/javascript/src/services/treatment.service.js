import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const treatmentService = {
  create,
  fetchTreatment
}

/**
 * Fetch Treatment
 */
async function fetchTreatment(experiment_id, condition_id) {
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments`, requestOptions).then(handleResponse)
}

/**
 * Create Treatment
 */
async function create(experiment_id, condition_id, assignment_id) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      assignmentId: parseInt(assignment_id)
    })
  }
  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments`, requestOptions).then(handleResponse)
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
    console.error('handleResponse | catch',{text})
  })
}
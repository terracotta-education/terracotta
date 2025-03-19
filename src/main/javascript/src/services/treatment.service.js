import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const treatmentService = {
  create,
  update,
  fetchTreatment,
}

/**
 * Fetch Treatment
 */
async function fetchTreatment(experimentId, conditionId) {
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/conditions/${conditionId}/treatments`, requestOptions).then(handleResponse)
}

/**
 * Create Treatment
 */
async function create(experimentId, conditionId, assignmentId) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      assignmentId: parseInt(assignmentId)
    })
  }
  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/conditions/${conditionId}/treatments`, requestOptions).then(handleResponse)
}

/**
 * Update Treatment
 */
async function update(experimentId, conditionId, treatmentId, body = {}) {
  const requestOptions = {
    method: 'PUT',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      ...body
    })
  }
  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}`, requestOptions).then(handleResponse)
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
      return {
        data: null,
        status: response.status
      }
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
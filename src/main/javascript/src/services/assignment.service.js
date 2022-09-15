import { authHeader } from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const assignmentService = {
  fetchAssignment,
  fetchAssignmentsByExposure,
  create,
  deleteAssignment,
  updateAssignments,
  duplicateAssignment
}

/**
 * Fetch Assignment
 */
async function fetchAssignment(experiment_id, exposure_id, assignment_id, submissions=false) {
  const includeSubmissions = (submissions) ? '?submissions=true' : ''
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/assignments/${assignment_id}${includeSubmissions}`, requestOptions).then(handleResponse)
}

/**
 * Fetch Assignments by Exposure
 */
async function fetchAssignmentsByExposure(experiment_id, exposure_id, submissions=false) {
  const includeSubmissions = (submissions) ? '?submissions=true' : ''
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/assignments${includeSubmissions}`, requestOptions).then(handleResponse)
}

/**
 * Create Assignment
 */
function create(experiment_id, exposure_id, title, order) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title,
      assignmentOrder: order
    })
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/assignments`, requestOptions).then(handleResponse)
}

/**
 * Duplicate Assignment
 */
function duplicateAssignment(experiment_id, exposure_id, assignment_id) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({})
  };

  console.log(experiment_id, exposure_id, assignment_id);

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/assignments/${assignment_id}/duplicate`, requestOptions).then(handleResponse)
}

/**
 * Delete Assignment
 */
async function deleteAssignment(experiment_id, exposure_id, assignment_id) {
  const requestOptions = {
    method: 'DELETE',
    headers: {...authHeader()}
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/assignments/${assignment_id}`, requestOptions).then(handleResponse)
}

/**
 * Delete Assignment
 */
async function updateAssignments(experiment_id, exposure_id, payload) {
  const requestOptions = {
    method: 'PUT',
    headers: {...authHeader()},
    body: JSON.stringify([
      ...payload
    ])
  }

  return fetch(`${store.getters['api/aud']}/api/experiments/${experiment_id}/exposures/${exposure_id}/assignments`, requestOptions).then(handleResponse)
}



/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
  .then(text => {
    const data = text && JSON.parse(text)

    if (!response || !response.ok) {
      if (response.status === 401 || response.status === 402 || response.status === 500) {
        console.log('handleResponse | 401/402/500',{response})
      } else if (response.status===404) {
        console.log('handleResponse | 404',{response})
      }

      return response
    } else if (response.status===204) {
      console.log('handleResponse | 204',{text,data,response})
      return []
    }

    return data || response
  }).catch(text => {
    console.error('handleResponse | catch',{text})
  })
}
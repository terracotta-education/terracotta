import { authHeader } from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const treatmentService = {
  create
}

/**
 * Create Treatment
 */
function create(experiment_id, condition_id, assignment_id) {
  const requestOptions = {
    method: 'POST',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      assignmentId: assignment_id
    })
  }
  console.log({experiment_id, condition_id, assignment_id, requestOptions})
  return fetch(`${store.getters['api/aud']}api/experiments/${experiment_id}/conditions/${condition_id}/treatments`, requestOptions).then(handleResponse)
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
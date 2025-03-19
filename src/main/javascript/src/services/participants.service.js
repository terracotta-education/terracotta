import { authHeader, isJson } from '@/helpers'
import store from '@/store/index.js'

// /**
//  * Register methods
//  */
export const participantService = {
  getAll,
  getById,
  updateParticipants,
  updateParticipant
}

/**
 * Get all Participants
 */
function getAll(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/participants`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get individual Participant
 */
function getById(experimentId, participantId) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/participants/${participantId}`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Update Participants
 */
function updateParticipants(experiementId, participantDetails) {
  const requestOptions = {
    method: 'PUT',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify(participantDetails),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiementId}/participants`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Update Participant
 */
 function updateParticipant(experiementId, participantDetails) {
  const requestOptions = {
    method: 'PUT',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
    body: JSON.stringify(participantDetails),
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experiementId}/participants/${participantDetails.participantId}`,
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
      const data = (text && isJson(text)) ? JSON.parse(text) : text

      if (!response || !response.ok) {
        if (
          response.status === 402 ||
          response.status === 500
        ) {
          console.log('handleResponse | 402/500', { response })
        } else if (response.status === 401) {
          console.log('handleResponse | 401', { response })
          return {
            message: data
          }
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

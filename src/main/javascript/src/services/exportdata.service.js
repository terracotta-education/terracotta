import {authHeader, isJson} from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const exportdataService = {
  getZip,
}

/**
 * Get Zip File
 */
function getZip(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: {...authHeader(), "Content-Type": 'application/json', 'Accept-Encoding': 'gzip, deflate, br'},
  }
  console.log('Auth Headers', authHeader())
  console.log('experimentId', experimentId)
  return fetch(`${store.getters['api/aud']}/api/experiments/${experimentId}/zip`, requestOptions).then(handleResponse)
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
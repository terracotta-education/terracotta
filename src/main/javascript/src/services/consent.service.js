import { authHeader } from '@/helpers'
import axios from 'axios'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const consentService = {
  create,
  update,
  delete: _delete,
  getConsentFile,
}

/**
 * Create Assignment
 */
function create(experimentId, pdfFile, title) {
  const requestOptions = {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...authHeader()
    }
  }

  let formData = new FormData();
  formData.append('consent', pdfFile);

  // Axios was required for correct formData boundary
  return (
    axios
      .post(
        `${store.getters["api/aud"]}/api/experiments/${experimentId}/consent?title=${title}`,
        formData,
        requestOptions
      )
      // can't use handleResponse here since this is the Axios API, not Fetch API
      .then((response) => {
        return {
          status: response.status,
          message: response.statusText,
        };
      })
      .catch((error) => {
        if (error.response) {
          return {
            status: error.response.status,
            message: error.response.statusText,
          };
        } else {
          throw error; // re-raise error, something unexpected happened
        }
      })
  );
}

/**
 * Update Assignment
 */
function update(experimentId) {
  const requestOptions = {
    method: 'PUT',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/consent`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Get Consent File
 */
async function getConsentFile(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: { ...authHeader() },
  };

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/consent`,
    requestOptions
  ).then(handleResponseFile)
}

/**
 * Delete Assignment
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
function _delete(experimentId) {
  const requestOptions = {
    method: 'DELETE',
    headers: { ...authHeader(), 'Content-Type': 'application/json' },
  }

  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/consent`,
    requestOptions
  ).then(handleResponse)
}

/**
 * Handle API response
 */
function handleResponseFile(response) {
  return response.arrayBuffer().then((data) => {
    const base64Str = Buffer.from(data).toString('base64');
    return {status: response.status, base: base64Str}
})
}

/**
 * Handle API response
 */
function handleResponse(response) {

  return response
    .text()
    .then(text => {
      const data = text && JSON.parse(text)

      if (!response || !response.ok) {
        if (
          response.status === 401 ||
          response.status === 402 ||
          response.status === 500 ||
          response.status === 404
        ) {
          console.log('handleResponse | 401/402/404/500', { response });
        } else if (response.status === 409) {
          return {
            message: data
          }
        }

        return response
      }

      console.log('handleResponse | then', { text, data, response });
      return data || response
    })
    .catch((text) => {
      console.log('handleResponse | catch', { text })
    })
}

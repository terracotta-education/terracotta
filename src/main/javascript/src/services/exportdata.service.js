import { authHeader } from '@/helpers'
import store from '@/store/index.js'

/**
 * Register methods
 */
export const exportDataService = {
  getZip,
};

/**
 * Get Zip File
 */
function getZip(experimentId) {
  const requestOptions = {
    method: 'GET',
    headers: authHeader(),
  };
  return fetch(
    `${store.getters['api/aud']}/api/experiments/${experimentId}/zip`,
    requestOptions
  ).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.blob().then((resp) => {
    if (response.status === 200) {
      return {
        data: resp,
        status: response.status,
      };
    } else {
        console.log('handleResponse | 204', {response})
    }
  });
}

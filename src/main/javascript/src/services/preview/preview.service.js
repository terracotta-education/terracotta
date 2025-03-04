import {authHeader, isJson} from "@/helpers";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const previewService = {
  treatmentPreview
}

/**
 * Get Treatment Preview
 */
async function treatmentPreview(experimentId, conditionId, treatmentId, previewId, ownerId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/preview/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/id/${previewId}?ownerId=${ownerId}`, requestOptions).then(handleResponse)
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
      console.log("handleResponse | 401/402/500", {response})
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
    console.error("handleResponse | catch",{text})
  })
}

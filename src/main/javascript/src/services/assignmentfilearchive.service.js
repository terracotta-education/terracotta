import { authHeader } from "@/helpers";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const assignmentfilearchiveService = {
  acknowledgeError,
  prepare,
  poll,
  retrieve
}

/**
 * Prepare
 */
async function prepare(experimentId, exposureId, assignmentId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files`, requestOptions).then(handleResponse);
}

/**
 * Poll assignment file status
 */
async function poll(experimentId, exposureId, assignmentId, createNewOnOutdated) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files/poll?createNewOnOutdated=${createNewOnOutdated}`, requestOptions).then(handleResponse);
}

/**
 * Retrieve assignment file archive
 */
async function retrieve(experimentId, exposureId, assignmentId, fileRequest) {
const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
}

return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files/${fileRequest.id}/retrieve`, requestOptions)
  .then(async (response) => {
    if (response.status !== 200) {
      return handleResponse(response);
    }
    const blob = await response.blob();
    const newBlob = new Blob([blob]);
    const url = window.URL.createObjectURL(newBlob, { type: fileRequest.mimeType });
    const link = document.createElement("a");

    link.href = url;
    link.setAttribute("download", fileRequest.fileName);
    document.body.appendChild(link);

    link.click();
    link.remove();

    return new Promise(resolve => {
        setTimeout(() => {
          resolve();
        }, 1000);
      });
  });
}

/**
 * Acknowledge error
 */
async function acknowledgeError(experimentId, exposureId, assignmentId, fileId) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files/${fileId}/error/acknowledge`, requestOptions).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
    .then(text => {
      const data = text && JSON.parse(text);

      if (!response || !response.ok) {
        if (response.status === 401 || response.status === 402 || response.status === 500) {
          console.log("handleResponse | 401/402/500", {response});
        } else if (response.status === 404) {
          console.log("handleResponse | 404", {response});
        } else if (response.status === 409) {
          console.log("handleResponse | 409", {response});
          return data || response;
        }

        return response;
      } else if (response.status === 204) {
        console.log("handleResponse | 204", {text, data, response});
        return [];
      }

      return data || response;
    }).catch(text => {
      console.error("handleResponse | catch", {text});
    })
}

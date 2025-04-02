import { authHeader } from "@/helpers";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const experimentdataexportService = {
  acknowledge,
  prepare,
  poll,
  pollList,
  retrieve
}

/**
 * Prepare data export
 */
async function prepare(experimentId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/export/data`, requestOptions).then(handleResponse);
}

/**
 * Poll data export status
 */
async function poll(experimentId, createNewOnOutdated) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/export/data/poll?createNewOnOutdated=${createNewOnOutdated}`, requestOptions).then(handleResponse);
}

/**
 * Poll data export status for a list of experiment IDs
 */
async function pollList(experimentIds, createNewOnOutdated) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify(experimentIds)
  }

  // experiment ID = 0, as it's unused in the endpoint but required by the API
  return fetch(`${store.getters["api/aud"]}/api/experiments/0/export/data/poll/list?createNewOnOutdated=${createNewOnOutdated}`, requestOptions).then(handleResponse);
}

/**
 * Retrieve data export
 */
async function retrieve(experimentId, experimentDataExportRequest) {
  const requestOptions = {
      method: "GET",
      headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/export/data/${experimentDataExportRequest.id}/retrieve`, requestOptions)
    .then(async (response) => {
      if (response.status !== 200) {
        return handleResponse(response);
      }
      const blob = await response.blob();
      const newBlob = new Blob([blob]);
      const url = window.URL.createObjectURL(newBlob, { type: experimentDataExportRequest.mimeType });
      const link = document.createElement("a");

      link.href = url;
      link.setAttribute("download", experimentDataExportRequest.fileName);
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
 * Acknowledge alert
 */
async function acknowledge(experimentId, fileId, status) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/export/data/${fileId}/acknowledge?status=${status}`, requestOptions).then(handleResponse);
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

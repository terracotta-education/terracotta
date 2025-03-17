import {authHeader, fileAuthHeader, isJson} from "@/helpers";
import axios from "axios";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const experimentService = {
  getAll,
  getById,
  create,
  update,
  pollImport,
  pollImports,
  acknowledgeImport,
  export: _export,
  import: _import,
  delete: _delete
}

/**
 * Get all Experiments
 */
async function getAll() {
  const requestOptions = {
    method: "GET",
    headers: authHeader()
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments?participants=true`, requestOptions).then(handleResponse);
}

/**
 * Create Experiment
 */
async function create() {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({})
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments`, requestOptions).then(handleResponse);
}

/**
 * Get individual Experiment
 */
async function getById(experimentId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}?conditions=true`, requestOptions).then(handleResponse)
}

/**
 * Update Experiment
 */
async function update(experiment) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader()},
    body: JSON.stringify(experiment)
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experiment.experimentId}`, requestOptions).then(handleResponse);
}

/**
 * Delete Experiment
 *
 * (Prefixed function name with underscore because delete is a reserved word in javascript)
 */
async function _delete(id) {
  const requestOptions = {
    method: "DELETE",
    headers: authHeader()
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${id}`, requestOptions).then(handleResponse);
}

/**
 * Export Experiment
 */
async function _export(experimentId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/export`, requestOptions)
    .then(async (response) => {
      if (response.status !== 200) {
        return handleResponse(response);
      }
      const contentDisposition = response.headers.get("content-disposition");
      // Extract filename (if present)
      let filename = null;
      if (contentDisposition && contentDisposition.indexOf("filename") > -1) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
          if (filenameMatch && filenameMatch[1]) {
              filename = filenameMatch[1].replace(/['"]/g, '');
          }
      }
      const blob = await response.blob();
      const newBlob = new Blob([blob]);
      const url = window.URL.createObjectURL(newBlob, { type: "application/zip" });
      const link = document.createElement("a");

      link.href = url;
      link.setAttribute("download", filename);
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
 * Import Experiment
 */
async function _import(zipFile) {
  const requestOptions = {
    headers: {
      "Content-Type": "multipart/form-data",
      ...fileAuthHeader()
    }
  }

  const formData = new FormData();
  formData.append("file", zipFile);

  // Axios was required for correct formData boundary
  return (
    axios
      .post(
        `${store.getters["api/aud"]}/api/experiments/import`,
        formData,
        requestOptions
      )
      // can't use handleResponse here since this is the Axios API, not Fetch API
      .then((response) => {
        return response.data;
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
 * Poll experiment import status
 */
async function pollImport(importId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/import/${importId}/poll`, requestOptions).then(handleResponse);
}

/**
 * Poll all import status for user and context
 */
async function pollImports() {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/import/poll`, requestOptions).then(handleResponse);
}

/**
 * Acknowledge import experiment status
 */
async function acknowledgeImport(importId, status) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/import/${importId}/acknowledge?status=${status}`, requestOptions).then(handleResponse);
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
        console.log("handleResponse | 401/402/500", {response});
      } else if (response.status === 409) {
        return {
          message: data
        }
      } else if (response.status === 204) {
        console.log("handleResponse | 204", {text, data, response});
        return [];
      }

      const dataResponse = (data) ? {
        data,
        status: response.status
      } : null;

      return dataResponse || response;
    }).catch(text => {
      console.log("handleResponse | catch", {text});
    })
}

import { authHeader } from "@/helpers"
import axios from "axios"
import store from "@/store/index.js"

/**
 * Register methods
 */
export const messageService = {
  update,
  fetchPreview,
  sendTest,
  getAssignments,
  uploadPipedText,
  updatePlaceholders
}

/**
 * Update message
 */
async function update(experimentId, exposureId, containerId, messageId, payload) {
  const requestOptions = {
    method: "PUT",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}`, requestOptions).then(handleResponse);
}

/**
 * Preview message
 */
async function fetchPreview(experimentId, exposureId, containerId, messageId, messagePreviewDto) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify(messagePreviewDto)
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/preview`, requestOptions).then(handleResponse);
}

/**
 * Send test message
 */
async function sendTest(experimentId, exposureId, containerId, messageId, messageSendTestDto) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify(messageSendTestDto)
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/sendtest`, requestOptions).then(handleResponse);
}

/**
 * Get all assignments for messaging rules
 */
async function getAssignments() {
  const requestOptions = {
    method: "GET",
    headers: { ...authHeader() }
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/0/exposures/0/messaging/container/0/message/assignments`, requestOptions).then(handleResponse);
}

/**
 * Update content placeholders
 */
async function updatePlaceholders(experimentId, exposureId, containerId, messageId, contentId, contentDto) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify(contentDto)
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/content/${contentId}/piped/updatePlaceholders`, requestOptions).then(handleResponse);
}

/**
 * Upload file
 */
function uploadPipedText(experimentId, exposureId, containerId, messageId, contentId, file) {
  const requestOptions = {
    headers: {
      "Content-Type": "multipart/form-data",
      ...authHeader()
    }
  }

  let formData = new FormData();
  formData.append("file", file);

  // Axios was required for correct formData boundary
  return (
    axios
      .post(
        `${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/content/${contentId}/piped/file`,
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
            content: null,
            validationErrors: ["An unspecified error occurred"]
          };
        } else {
          throw error; // re-raise error, something unexpected happened
        }
      })
  );
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
          console.log("handleResponse | 401/402/500",{response});
        } else if (response.status === 404) {
          console.log("handleResponse | 404",{response});
        }

        return response;
      } else if (response.status === 204) {
        return [];
      }

      return data || response;
    }).catch(text => {
      console.error("handleResponse | catch",{text});
    })
}

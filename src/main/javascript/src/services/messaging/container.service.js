import { authHeader } from "@/helpers"
import store from "@/store/index.js"

/**
 * Register methods
 */
export const messageContainerService = {
  getAll,
  create,
  update,
  updateAll,
  send,
  deleteContainer,
  move,
  duplicate
}

/**
 * Get all message containers for an experiment and owner
 */
async function getAll(experimentId, exposureId) {
    const requestOptions = {
      method: "GET",
      headers: {...authHeader()}
    }

    return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container`, requestOptions).then(handleResponse);
  }

/**
 * Create message container and all child messages
 */
async function create(experimentId, exposureId, single) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" }
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container?single=${single}`, requestOptions).then(handleResponse);
}

/**
 * Update message container
 */
async function update(experimentId, exposureId, containerId, message_container_dto) {
  const requestOptions = {
    method: "PUT",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify(
      message_container_dto
    )
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}`, requestOptions).then(handleResponse);
}

/**
 * Update containers
 */
async function updateAll(experimentId, exposureId, container_dto) {
  const requestOptions = {
    method: "PUT",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify([
        ...container_dto
    ])
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container`, requestOptions).then(handleResponse);
}

/**
 * Mark message container and all child messages as ready to send
 */
async function send(experimentId, exposureId, containerId) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" }
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/send`, requestOptions).then(handleResponse);
}

/**
 * Delete message container
 */
async function deleteContainer(experimentId, exposureId, containerId) {
    const requestOptions = {
      method: "DELETE",
      headers: { ...authHeader(), "Content-Type": "application/json" }
    }

    return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}`, requestOptions).then(handleResponse);
  }

/**
 * Move message container
 */
async function move(experimentId, exposureId, containerId, container_dto) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader()},
    body: JSON.stringify(
      container_dto
    )
  }
  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/move`, requestOptions).then(handleResponse);
}

/**
 * Duplicate message container
 */
async function duplicate(experimentId, exposureId, containerId) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader()}
  }
  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/duplicate`, requestOptions).then(handleResponse);
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

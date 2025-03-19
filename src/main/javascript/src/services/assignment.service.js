import { authHeader } from "@/helpers"
import store from "@/store/index.js"

/**
 * Register methods
 */
export const assignmentService = {
  fetchAssignment,
  fetchAssignmentsByExposure,
  create,
  deleteAssignment,
  updateAssignments,
  updateAssignment,
  moveAssignment,
  duplicateAssignment
}

/**
 * Fetch Assignment
 */
async function fetchAssignment(experimentId, exposureId, assignmentId, submissions=false) {
  const includeSubmissions = (submissions) ? "?submissions=true" : ""
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}${includeSubmissions}`, requestOptions).then(handleResponse)
}

/**
 * Fetch Assignments by Exposure
 */
async function fetchAssignmentsByExposure(experimentId, exposureId, submissions=false) {
  const includeSubmissions = (submissions) ? "?submissions=true" : ""
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments${includeSubmissions}`, requestOptions).then(handleResponse)
}

/**
 * Create Assignment
 */
function create(experimentId, exposureId, body, order) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify({
      ...body,
      assignmentOrder: order
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments`, requestOptions).then(handleResponse)
}

/**
 * Duplicate Assignment
 */
function duplicateAssignment(experimentId, exposureId, assignmentId) {
  const requestOptions = {
    method: "POST",
    headers: { ...authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify({})
  };

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/duplicate`, requestOptions).then(handleResponse)
}

/**
 * Delete Assignment
 */
async function deleteAssignment(experimentId, exposureId, assignmentId) {
  const requestOptions = {
    method: "DELETE",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}`, requestOptions).then(handleResponse)
}

/**
 * Update Assignments
 */
async function updateAssignments(experimentId, exposureId, payload) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader()},
    body: JSON.stringify([
      ...payload
    ])
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments`, requestOptions).then(handleResponse)
}

async function updateAssignment(experimentId, exposureId, assignmentId, body) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader()},
    body: JSON.stringify({
      ...body
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}`, requestOptions).then(handleResponse);
}
/**
 * Update Assignments
 */
async function moveAssignment(experimentId, exposureId, assignmentId, update) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader()},
    body: JSON.stringify({
      ...update
    })
  }
  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/move`, requestOptions).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
  .then(text => {
    const data = text && JSON.parse(text)

    if (!response || !response.ok) {
      if (response.status === 401 || response.status === 402 || response.status === 500) {
        console.log("handleResponse | 401/402/500",{response})
      } else if (response.status===404) {
        console.log("handleResponse | 404",{response})
      }

      return response
    } else if (response.status===204) {
      console.log("handleResponse | 204",{text,data,response})
      return []
    }

    return data || response
  }).catch(text => {
    console.error("handleResponse | catch",{text})
  })
}
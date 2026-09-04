import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const assignmentService = {
  fetchAssignment,
  fetchAssignmentsByExposure,
  create,
  deleteAssignment,
  updateAssignments,
  updateAssignment,
  moveAssignment,
  duplicateAssignment
};

async function fetchAssignment(
  experimentId,
  exposureId,
  assignmentId,
  submissions = false
) {
  const query = submissions ? "?submissions=true" : "";

  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}${query}`
  );
}

async function fetchAssignmentsByExposure(
  experimentId,
  exposureId,
  submissions = false
) {
  const query = submissions ? "?submissions=true" : "";

  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments${query}`
  );
}

async function create(
  experimentId,
  exposureId,
  body,
  order
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments`,
    {
      method: "POST",
      body: {
        ...body,
        assignmentOrder: order
      }
    }
  );
}

async function duplicateAssignment(
  experimentId,
  exposureId,
  assignmentId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/duplicate`,
    {
      method: "POST",
      body: {}
    }
  );
}

async function deleteAssignment(
  experimentId,
  exposureId,
  assignmentId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}`,
    {
      method: "DELETE"
    }
  );
}

async function updateAssignments(
  experimentId,
  exposureId,
  payload
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments`,
    {
      method: "PUT",
      body: payload
    }
  );
}

async function updateAssignment(
  experimentId,
  exposureId,
  assignmentId,
  body
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}`,
    {
      method: "PUT",
      body
    }
  );
}

async function moveAssignment(
  experimentId,
  exposureId,
  assignmentId,
  update
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/move`,
    {
      method: "POST",
      body: {
        ...update
      }
    }
  );
}

async function request(path, options = {}) {
  const {
    method = "GET",
    body
  } = options;

  const response = await fetch(`${api().aud}${path}`, {
    method,
    headers: {
      ...authHeader(),
      ...(body ? { "Content-Type": "application/json" } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
  });

  return handleResponse(response);
}

async function handleResponse(response) {
  try {
    const text = await response.text();

    const data = text
      ? JSON.parse(text)
      : null;

    if (response.status === 204) {
      return [];
    }

    if (!response?.ok) {
      if ([401, 402, 500].includes(response.status)) {
        console.error("handleResponse | auth/server error", {
          response
        });
      } else if (response.status === 404) {
        console.warn("handleResponse | not found", {
          response
        });
      }

      return {
        status: response.status,
        error: data || response
      };
    }

    return data || response;
  } catch (error) {
    console.error("handleResponse | catch", {
      error
    });

    return {
      error,
      status: response?.status
    };
  }
}

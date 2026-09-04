import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const treatmentService = {
  create,
  update,
  fetchTreatment
};

async function fetchTreatment(
  experimentId,
  conditionId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments`
  );
}

async function create(
  experimentId,
  conditionId,
  assignmentId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments`,
    {
      method: "POST",
      body: {
        assignmentId: parseInt(assignmentId)
      }
    }
  );
}

async function update(
  experimentId,
  conditionId,
  treatmentId,
  body = {}
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}`,
    {
      method: "PUT",
      body
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

    const data = text && isJson(text)
      ? JSON.parse(text)
      : text;

    if (response.status === 204) {
      return {
        data: null,
        status: response.status
      };
    }

    if (response.status === 409) {
      return {
        message: data,
        status: response.status
      };
    }

    if (!response?.ok) {
      console.error("handleResponse | error", {
        response,
        data
      });

      return {
        data,
        status: response.status,
        error: data
      };
    }

    return data
      ? {
          data,
          status: response.status
        }
      : response;
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

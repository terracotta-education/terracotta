import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const exposuresService = {
  getAll,
  getById,
  createExposures
};

async function getAll(experimentId) {
  return request(
    `/api/experiments/${experimentId}/exposures`
  );
}

async function getById(
  experimentId,
  exposureId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}`
  );
}

async function createExposures(experimentId) {
  return request(
    `/api/experiments/${experimentId}/exposures/create`,
    {
      method: "POST"
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
    const data = text ? JSON.parse(text) : null;

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

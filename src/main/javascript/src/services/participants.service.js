import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const participantService = {
  getAll,
  getById,
  updateParticipants,
  updateParticipant
};

async function getAll(experimentId, refresh = false) {
  return request(
    `/api/experiments/${experimentId}/participants?refresh=${refresh}`
  );
}

async function getById(
  experimentId,
  participantId
) {
  return request(
    `/api/experiments/${experimentId}/participants/${participantId}`
  );
}

async function updateParticipants(
  experimentId,
  participantDetails
) {
  return request(
    `/api/experiments/${experimentId}/participants`,
    {
      method: "PUT",
      body: participantDetails
    }
  );
}

async function updateParticipant(
  experimentId,
  participantDetails
) {
  return request(
    `/api/experiments/${experimentId}/participants/${participantDetails.participantId}`,
    {
      method: "PUT",
      body: participantDetails
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
      return [];
    }

    if (response.status === 401) {
      return {
        message: data,
        status: response.status
      };
    }

    if (!response?.ok) {
      if ([402, 500].includes(response.status)) {
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

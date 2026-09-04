import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const messageContainerService = {
  getAll,
  create,
  update,
  updateAll,
  send,
  deleteContainer,
  move,
  duplicate
};

async function getAll(experimentId, exposureId) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container`
  );
}

async function create(experimentId, exposureId, single) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container?single=${single}`,
    {
      method: "POST"
    }
  );
}

async function update(
  experimentId,
  exposureId,
  containerId,
  messageContainerDto
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}`,
    {
      method: "PUT",
      body: messageContainerDto
    }
  );
}

async function updateAll(
  experimentId,
  exposureId,
  containerDto
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container`,
    {
      method: "PUT",
      body: [...containerDto]
    }
  );
}

async function send(
  experimentId,
  exposureId,
  containerId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/send`,
    {
      method: "POST"
    }
  );
}

async function deleteContainer(
  experimentId,
  exposureId,
  containerId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}`,
    {
      method: "DELETE"
    }
  );
}

async function move(
  experimentId,
  exposureId,
  containerId,
  containerDto
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/move`,
    {
      method: "POST",
      body: containerDto
    }
  );
}

async function duplicate(
  experimentId,
  exposureId,
  containerId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/duplicate`,
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

  const requestOptions = {
    method,
    headers: {
      ...authHeader(),
      ...(body ? { "Content-Type": "application/json" } : {})
    },
    ...(body
      ? { body: JSON.stringify(body) }
      : {})
  };

  const response = await fetch(
    `${api().aud}${path}`,
    requestOptions
  );

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
        console.error(
          "handleResponse | auth/server error",
          { response }
        );
      } else if (response.status === 404) {
        console.warn(
          "handleResponse | not found",
          { response }
        );
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
      error
    };
  }
}

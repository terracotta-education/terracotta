import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const assignmentFileArchiveService = {
  acknowledgeError,
  prepare,
  poll,
  retrieve
};

async function prepare(
  experimentId,
  exposureId,
  assignmentId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files`
  );
}

async function poll(
  experimentId,
  exposureId,
  assignmentId,
  createNewOnOutdated
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files/poll?createNewOnOutdated=${createNewOnOutdated}`
  );
}

async function retrieve(
  experimentId,
  exposureId,
  assignmentId,
  fileRequest
) {
  const response = await fetch(
    `${api().aud}/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files/${fileRequest.id}/retrieve`,
    {
      method: "GET",
      headers: {
        ...authHeader()
      }
    }
  );

  if (response.status !== 200) {
    return handleResponse(response);
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(
    new Blob([blob], {
      type: fileRequest.mimeType
    })
  );

  const link = document.createElement("a");
  link.href = url;
  link.download = fileRequest.fileName;

  document.body.appendChild(link);
  link.click();
  link.remove();

  window.URL.revokeObjectURL(url);

  return {
    status: response.status
  };
}

async function acknowledgeError(
  experimentId,
  exposureId,
  assignmentId,
  fileId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/assignments/${assignmentId}/files/${fileId}/error/acknowledge`,
    {
      method: "PUT"
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

    if (response.status === 409) {
      return data || {
        status: response.status
      };
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

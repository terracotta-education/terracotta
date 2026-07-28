import { authHeader, fileAuthHeader } from "@/helpers";
import axios from "axios";
import { api } from "@/store/api.module";

export const messageService = {
  update,
  fetchPreview,
  sendTest,
  getAssignments,
  uploadPipedText,
  updatePlaceholders
};

async function update(
  experimentId,
  exposureId,
  containerId,
  messageId,
  payload
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}`,
    {
      method: "PUT",
      body: payload
    }
  );
}

async function fetchPreview(
  experimentId,
  exposureId,
  containerId,
  messageId,
  messagePreviewDto
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/preview`,
    {
      method: "POST",
      body: messagePreviewDto
    }
  );
}

async function sendTest(
  experimentId,
  exposureId,
  containerId,
  messageId,
  messageSendTestDto
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/sendtest`,
    {
      method: "POST",
      body: messageSendTestDto
    }
  );
}

async function getAssignments() {
  return request(
    "/api/experiments/0/exposures/0/messaging/container/0/message/assignments"
  );
}

async function updatePlaceholders(
  experimentId,
  exposureId,
  containerId,
  messageId,
  contentId,
  contentDto
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/content/${contentId}/piped/updatePlaceholders`,
    {
      method: "POST",
      body: contentDto
    }
  );
}

async function uploadPipedText(
  experimentId,
  exposureId,
  containerId,
  messageId,
  contentId,
  file
) {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await axios.post(
      `${api().aud}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/content/${contentId}/piped/file`,
      formData,
      {
        headers: {
          ...fileAuthHeader()
        }
      }
    );

    return response.data;
  } catch (error) {
    if (error.response) {
      return {
        content: null,
        validationErrors: ["An unspecified error occurred"]
      };
    }

    throw error;
  }
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
      error
    };
  }
}

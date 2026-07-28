import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const messageContentAttachmentService = {
  getAll
};

async function getAll(
  experimentId,
  exposureId,
  containerId,
  messageId,
  contentId
) {
  const response = await fetch(
    `${api().aud}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/content/${contentId}/file`,
    {
      method: "GET",
      headers: {
        ...authHeader()
      }
    }
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

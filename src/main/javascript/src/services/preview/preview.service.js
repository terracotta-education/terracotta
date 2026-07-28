import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const previewService = {
  treatmentPreview
};

async function treatmentPreview(
  experimentId,
  conditionId,
  treatmentId,
  previewId,
  ownerId
) {
  const query = new URLSearchParams({
    ownerId
  });

  const response = await fetch(
    `${api().aud}/preview/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/id/${previewId}?${query}`,
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
    const data = text && isJson(text) ? JSON.parse(text) : text;

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

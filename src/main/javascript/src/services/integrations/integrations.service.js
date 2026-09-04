import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const integrationsService = {
  validateIframeUrl
};

async function validateIframeUrl(url) {
  const response = await fetch(
    `${api().aud}/integrations/validate/iframe?url=${encodeURIComponent(url)}`,
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
    await response.text();

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

      return false;
    }

    return true;
  } catch (error) {
    console.error("handleResponse | catch", {
      error
    });

    return false;
  }
}

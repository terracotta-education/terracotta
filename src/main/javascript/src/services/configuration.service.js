import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const configurationService = {
  get
};

async function get() {
  const response = await fetch(
    `${api().aud}/api/configuration`,
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

    const data = text && isJson(text)
      ? JSON.parse(text)
      : text;

    if (response.status === 204) {
      return [];
    }

    if (response.status === 409) {
      return {
        message: data,
        status: response.status
      };
    }

    if (!response?.ok) {
      return {
        data,
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

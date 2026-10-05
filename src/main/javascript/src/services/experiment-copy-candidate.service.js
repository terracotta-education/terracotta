import {
  authHeader,
  isJson
} from "@/helpers";

import { api } from "@/store/api.module";

export const experimentCopyCandidateService = {
  getCopyStatus,
  acknowledgeCopyStatus,
  retryCopy
};

async function getCopyStatus() {
  return request(
    "/api/experiments/copy-status"
  );
}

async function acknowledgeCopyStatus() {
  return request(
    "/api/experiments/copy-status/acknowledge",
    {
      method: "POST"
    }
  );
}

async function retryCopy() {
  return request(
    "/api/experiments/copy-status/retry",
    {
      method: "POST"
    }
  );
}

async function request(path, options = {}) {
  const { method = "GET" } = options;

  const response = await fetch(
    `${api().aud}${path}`,
    {
      method,
      headers: authHeader()
    }
  );

  return handleResponse(response);
}

async function handleResponse(response) {
  try {
    const text = await response.text();

    const data =
      text && isJson(text)
        ? JSON.parse(text)
        : text;

    if (response.status === 204) {
      return [];
    }

    if (!response?.ok) {
      console.error(
        "handleResponse | error",
        {
          response,
          data
        }
      );

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
    console.error(
      "handleResponse | catch",
      {
        error
      }
    );

    return {
      error,
      status: response?.status
    };
  }
}

import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const resultsDashboardService = {
  overview,
  outcomes
};

async function overview(experimentId) {
  return request(
    `/api/experiments/${experimentId}/dashboard/results/overview`
  );
}

async function outcomes(experimentId, body) {
  return request(
    `/api/experiments/${experimentId}/dashboard/results/outcomes`,
    {
      method: "POST",
      body
    }
  );
}

async function request(path, options = {}) {
  const { method = "GET", body } = options;

  const requestOptions = {
    method,
    headers: {
      ...authHeader(),
      ...(body ? { "Content-Type": "application/json" } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
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
    const data = text && isJson(text) ? JSON.parse(text) : text;

    if (response.status === 204) {
      return [];
    }

    if (response.status === 409) {
      return {
        message: data,
        status: response.status
      };
    }

    if (!response.ok) {
      console.error("handleResponse | error", { response, data });

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
    console.error("handleResponse | catch", { error });

    return {
      error,
      status: response?.status
    };
  }
}

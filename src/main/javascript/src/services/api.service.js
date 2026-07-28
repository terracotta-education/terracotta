import { authHeader, isJson } from "@/helpers";
import { pinia } from "@/pinia";
import { api } from "@/store/api.module";

export const apiService = {
  deepLinkJwt,
  getApiToken,
  refreshToken,
  reportStep
};

async function getApiToken(token) {
  const response = await fetch(`${api(pinia).aud}/api/oauth/trade`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    }
  });

  return response.ok ? response.text() : null;
}

async function refreshToken() {
  const response = await fetch(`${api(pinia).aud}/api/oauth/refresh`, {
    method: "POST",
    headers: {
      ...authHeader()
    }
  });

  return response.ok ? response.text() : null;
}

async function deepLinkJwt(id) {
  const response = await fetch(`${api(pinia).aud}/deeplink/toJwt/${id}`, {
    method: "GET",
    headers: {
      ...authHeader()
    }
  });

  return response.ok ? response.text() : null;
}

async function reportStep(
  experimentId,
  step,
  parameters,
  preferLmsChecks = false
) {
  const response = await fetch(
    `${api(pinia).aud}/api/experiments/${experimentId}/step?preferLmsChecks=${preferLmsChecks}`,
    {
      method: "POST",
      headers: {
        ...authHeader(),
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        step,
        parameters
      })
    }
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

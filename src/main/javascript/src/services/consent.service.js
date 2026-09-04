import { authHeader, fileAuthHeader } from "@/helpers";
import axios from "axios";
import { api } from "@/store/api.module";

export const consentService = {
  create,
  update,
  delete: deleteConsent,
  getConsentFile
};

async function create(
  experimentId,
  pdfFile,
  title
) {
  const formData = new FormData();
  formData.append("consent", pdfFile);

  const query = new URLSearchParams({
    title: title || ""
  });

  try {
    const response = await axios.post(
      `${api().aud}/api/experiments/${experimentId}/consent?${query}`,
      formData,
      {
        headers: {
          ...fileAuthHeader()
        }
      }
    );

    return {
      status: response.status,
      message: response.statusText
    };
  } catch (error) {
    if (error.response) {
      return {
        status: error.response.status,
        message: error.response.statusText
      };
    }

    throw error;
  }
}

async function update(experimentId) {
  return request(
    `/api/experiments/${experimentId}/consent`,
    {
      method: "PUT"
    }
  );
}

async function getConsentFile(experimentId) {
  const response = await fetch(
    `${api().aud}/api/experiments/${experimentId}/consent`,
    {
      method: "GET",
      headers: {
        ...authHeader()
      }
    }
  );

  return handleResponseFile(response);
}

async function deleteConsent(experimentId) {
  return request(
    `/api/experiments/${experimentId}/consent`,
    {
      method: "DELETE"
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

async function handleResponseFile(response) {
  const data = await response.arrayBuffer();

  const base = btoa(
    new Uint8Array(data)
      .reduce((binary, byte) => binary + String.fromCharCode(byte), "")
  );

  return {
    status: response.status,
    base
  };
}

async function handleResponse(response) {
  try {
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

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
      if ([401, 402, 404, 500].includes(response.status)) {
        console.error("handleResponse | auth/not-found/server error", {
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

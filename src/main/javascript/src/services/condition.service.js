import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const conditionService = {
  create,
  update,
  updateAll,
  delete: deleteCondition
};

async function create(experimentId) {
  return request(
    `/api/experiments/${experimentId}/conditions`,
    {
      method: "POST"
    }
  );
}

async function update(condition) {
  return request(
    `/api/experiments/${condition.experimentId}/conditions/${condition.conditionId}`,
    {
      method: "PUT",
      body: condition
    }
  );
}

async function updateAll(conditions) {
  return request(
    `/api/experiments/${conditions[0].experimentId}/conditions`,
    {
      method: "PUT",
      body: conditions
    }
  );
}

async function deleteCondition(condition) {
  return request(
    `/api/experiments/${condition.experimentId}/conditions/${condition.conditionId}`,
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

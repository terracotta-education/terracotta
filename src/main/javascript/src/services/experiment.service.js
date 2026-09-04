import {
  authHeader,
  fileAuthHeader,
  isJson
} from "@/helpers";

import axios from "axios";
import { api } from "@/store/api.module";

export const experimentService = {
  getAll,
  getById,
  create,
  update,
  pollImport,
  pollImports,
  acknowledgeImport,
  export: exportExperiment,
  import: importExperiment,
  delete: deleteExperiment
};

async function getAll() {
  return request(
    "/api/experiments"
  );
}

async function create() {
  return request(
    "/api/experiments",
    {
      method: "POST"
    }
  );
}

async function getById(experimentId) {
  return request(
    `/api/experiments/${experimentId}?conditions=true`
  );
}

async function update(experiment) {
  return request(
    `/api/experiments/${experiment.experimentId}`,
    {
      method: "PUT",
      body: experiment
    }
  );
}

async function deleteExperiment(id) {
  return request(
    `/api/experiments/${id}`,
    {
      method: "DELETE"
    }
  );
}

async function exportExperiment(experimentId) {
  const response = await fetch(
    `${api().aud}/api/experiments/${experimentId}/export`,
    {
      method: "GET",
      headers: {
        ...authHeader()
      }
    }
  );

  if (response.status !== 200) {
    return handleResponse(response);
  }

  const contentDisposition =
    response.headers.get("content-disposition");

  let filename = "experiment-export.zip";

  if (contentDisposition?.includes("filename")) {
    const match = contentDisposition.match(
      /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/
    );

    if (match?.[1]) {
      filename = match[1].replace(/['"]/g, "");
    }
  }

  const blob = await response.blob();

  const url = window.URL.createObjectURL(
    new Blob([blob], {
      type: "application/zip"
    })
  );

  const link = document.createElement("a");

  link.href = url;
  link.download = filename;

  document.body.appendChild(link);

  link.click();
  link.remove();

  window.URL.revokeObjectURL(url);

  return {
    status: response.status
  };
}

async function importExperiment(zipFile) {
  const formData = new FormData();

  formData.append("file", zipFile);

  try {
    const response = await axios.post(
      `${api().aud}/api/experiments/import`,
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
        status: error.response.status,
        message: error.response.statusText
      };
    }

    throw error;
  }
}

async function pollImport(importId) {
  return request(
    `/api/experiments/import/${importId}/poll`
  );
}

async function pollImports() {
  return request(
    "/api/experiments/import/poll"
  );
}

async function acknowledgeImport(
  importId,
  status
) {
  const query = new URLSearchParams({
    status
  });

  return request(
    `/api/experiments/import/${importId}/acknowledge?${query}`,
    {
      method: "PUT"
    }
  );
}

async function request(path, options = {}) {
  const {
    method = "GET",
    body
  } = options;

  const response = await fetch(
    `${api().aud}${path}`,
    {
      method,
      headers: {
        ...authHeader(),
        ...(body
          ? {
              "Content-Type":
                "application/json"
            }
          : {})
      },
      ...(body
        ? {
            body: JSON.stringify(body)
          }
        : {})
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

    if (response.status === 409) {
      return {
        message: data,
        status: response.status
      };
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

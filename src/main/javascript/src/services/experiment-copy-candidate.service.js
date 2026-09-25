import {
  authHeader,
  isJson
} from "@/helpers";

import { api } from "@/store/api.module";

export const experimentCopyCandidateService = {
  getAll,
  resolve,
  getCopyStatus,
  acknowledgeCopyStatus,
  retryCopy
};

async function getAll() {
  return request(
    "/api/experiments/copy-candidates"
  );
}

// resolves EVERY currently-PENDING candidate for this context in one call: candidates named in
// importCandidateIds are imported (and their corresponding copied LMS assignment(s) re-pointed);
// everything else PENDING is declined and obsolete-processed server-side. See
// ExperimentCopyCandidateServiceImpl.resolve for the full reasoning - there's deliberately no
// separate per-item import/dismiss call anymore.
async function resolve(importCandidateIds) {
  return request(
    "/api/experiments/copy-candidates/resolve",
    {
      method: "POST",
      body: { importCandidateIds }
    }
  );
}

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

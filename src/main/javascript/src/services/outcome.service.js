import {
  authHeader,
  isJson
} from "@/helpers";

import { api } from "@/store/api.module";

export const outcomeService = {
  getAll,
  getById,
  getAllByExperimentId,
  create,
  updateOutcome,
  deleteOutcome,
  getOutcomeScoresById,
  getScoreById,
  createOutcomeScores,
  updateOutcomeScores,
  getOutcomePotentials
};

async function getAll(
  experimentId,
  exposureId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes`
  );
}

async function getById(
  experimentId,
  exposureId,
  outcomeId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcomeId}`
  );
}

async function getAllByExperimentId(experimentId) {
  return request(
    `/api/experiments/${experimentId}/outcomes`
  );
}

async function create(
  experimentId,
  exposureId,
  title,
  maxPoints,
  external = false,
  lmsType = "NONE",
  lmsOutcomeId = null
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes`,
    {
      method: "POST",
      body: {
        title,
        maxPoints,
        external,
        lmsType,
        lmsOutcomeId
      }
    }
  );
}

async function updateOutcome(
  experimentId,
  exposureId,
  outcome
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcome.outcomeId}`,
    {
      method: "PUT",
      body: {
        title: outcome.title,
        maxPoints: outcome.maxPoints,
        external: outcome.external
      }
    }
  );
}

async function deleteOutcome(
  experimentId,
  exposureId,
  outcomeId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcomeId}`,
    {
      method: "DELETE"
    }
  );
}

async function getOutcomeScoresById(
  experimentId,
  exposureId,
  outcomeId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcomeId}/outcome_scores`
  );
}

async function getScoreById(
  experimentId,
  exposureId,
  outcomeId,
  outcomeScoreId
) {
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcomeId}/outcome_scores/${outcomeScoreId}`
  );
}

async function createOutcomeScores(
  experimentId,
  exposureId,
  outcomeId,
  scores = []
) {
  const validScores =
    Array.isArray(scores) ||
    (typeof scores === "object" &&
      scores?.participantId);

  if (!validScores) {
    return false;
  }

  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcomeId}/outcome_scores`,
    {
      method: "POST",
      body: scores
    }
  );
}

async function updateOutcomeScores(
  experimentId,
  exposureId,
  outcomeId,
  scores = []
) {
  if (
    !scores ||
    !Array.isArray(scores) ||
    !scores.some(score => score.participantId)
  ) {
    return false;
  }

  // single batched request; the backend creates entries without an outcomeScoreId
  // and updates entries that already have one, instead of one request per score
  return request(
    `/api/experiments/${experimentId}/exposures/${exposureId}/outcomes/${outcomeId}/outcome_scores`,
    {
      method: "PUT",
      body: scores.map(score => ({
        outcomeScoreId: score.outcomeScoreId,
        participantId: score.participantId,
        scoreNumeric: score.scoreNumeric
      }))
    }
  );
}

async function getOutcomePotentials(experimentId) {
  return request(
    `/api/experiments/${experimentId}/outcome_potentials`
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
  });

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

    if (
      !response?.ok ||
      [401, 402, 404, 500].includes(response.status)
    ) {
      console.error(
        "handleResponse | error",
        {
          response,
          data
        }
      );

      return {
        status: response.status,
        error: data || response
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

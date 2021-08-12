import { authHeader } from "@/helpers";
import store from "@/store/index.js";

// /**
//  * Register methods
//  */
export const submissionService = {
  getAll,
  updateSubmission,
};

/**
 * Get all Submissions
 */
async function getAll(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id
) {
  const requestOptions = {
    method: "GET",
    headers: authHeader(),
  };

  return fetch(
    `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions`,
    requestOptions
  ).then(handleResponse);
}

/**
 * Get all Submissions
 */
async function updateSubmission(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  submission_id,
  alteredCalculatedGrade,
  totalAlteredGrade
) {
  const requestOptions = {
    method: "PUT",
    headers: authHeader(),
    body: JSON.stringify({
      alteredCalculatedGrade: alteredCalculatedGrade,
      totalAlteredGrade: totalAlteredGrade,
    }),
  };

  return fetch(
    `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}`,
    requestOptions
  ).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response
    .text()
    .then((text) => {
      const data = text && JSON.parse(text);

      if (!response || !response.ok) {
        if (
          response.status === 401 ||
          response.status === 402 ||
          response.status === 500
        ) {
          console.log("handleResponse | 401/402/500", { response });
        } else if (response.status === 404) {
          console.log("handleResponse | 404", { response });
        }

        return response;
      }

      return data || response;
    })
    .catch((text) => {
      console.log("handleResponse | catch", { text });
    });
}

import { authHeader } from "@/helpers";
import store from "@/store/index.js";

// /**
//  * Register methods
//  */
export const submissionService = {
  getAll,
  updateSubmission,
  createQuestionSubmission,
  updateQuestionSubmission,
  studentResponse
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
 * Update Individual Submission
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
 * Send Question Submissions
 */
 async function createQuestionSubmission(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  submission_id,
  questions
) {
  const requestOptions = {
    method: "POST",
    headers: authHeader(),
    body: JSON.stringify(questions),
  };

  return fetch(
    `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions`,
    requestOptions
  ).then(handleResponse);
}

/**
 * Update Individual Question Submission
 */
 async function updateQuestionSubmission(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  submission_id,
  updatedResponseBody
) {
  const requestOptions = {
    method: "PUT",
    headers: authHeader(),
    body: JSON.stringify(updatedResponseBody),
  };

  return fetch(
    `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions`,
    requestOptions
  ).then(handleResponse);
}

/**
 * Get Student Response
 */
async function studentResponse(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  submission_id,
) {
  const requestOptions = {
    method: "GET",
    headers: authHeader(),
  };

  return fetch(
    `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions/?answer_submissions=true`,
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
      } else if (response.status === 204) {
        console.log('handleResponse | 204', {text, data, response})
        return []
      }

      return data || response;
    })
    .catch((text) => {
      console.log("handleResponse | catch", { text });
    });
}

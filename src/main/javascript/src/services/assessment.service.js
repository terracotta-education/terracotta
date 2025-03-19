import {authHeader, isJson} from "@/helpers";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const assessmentService = {
  fetchAssessment,
  fetchAssessmentForSubmission,
  fetchAssessments,
  createAssessment,
  updateAssessment,
  createQuestion,
  updateQuestion,
  deleteQuestion,
  deleteQuestions,
  createAnswer,
  updateAnswer,
  deleteAnswer,
  regradeQuestions
}

/**
 * Fetch Assessment
 */
async function fetchAssessment(experimentId, conditionId, treatmentId, assessmentId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }
  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}?questions=true&answers=true&submissions=true`, requestOptions).then(handleResponse);
}

async function fetchAssessmentForSubmission(experimentId, conditionId, treatmentId, assessmentId, submissionId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }
  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}?questions=true&answers=true&submission_id=${submissionId}`, requestOptions).then(handleResponse);
}

/**
 * Fetch Assessments
 */
async function fetchAssessments(experimentId, conditionId, treatmentId) {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments`, requestOptions).then(handleResponse);
}

/**
 * Create Assessment
 */
async function createAssessment(experimentId, conditionId, treatmentId, title, body) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({
      "html": body
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments`, requestOptions).then(handleResponse);
}

/**
 * Update Assessment
 */
async function updateAssessment(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  body,
  allowStudentViewResponses,
  studentViewResponsesAfter,
  studentViewResponsesBefore,
  allowStudentViewCorrectAnswers,
  studentViewCorrectAnswersAfter,
  studentViewCorrectAnswersBefore,
  numOfSubmissions,
  multipleSubmissionScoringScheme,
  hoursBetweenSubmissions,
  cumulativeScoringInitialPercentage
) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({
      "html": body,
      allowStudentViewResponses,
      studentViewResponsesAfter,
      studentViewResponsesBefore,
      allowStudentViewCorrectAnswers,
      studentViewCorrectAnswersAfter,
      studentViewCorrectAnswersBefore,
      numOfSubmissions,
      multipleSubmissionScoringScheme,
      hoursBetweenSubmissions,
      cumulativeScoringInitialPercentage
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}`, requestOptions).then(handleResponse);
}

/**
 * Regrade Assessment Questions
 */
async function regradeQuestions(experimentId, conditionId, treatmentId, assessmentId, body) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify(body)
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/regrade`, requestOptions).then(handleResponse);
}

/**
 * Create Question
 */
async function createQuestion(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  question_order,
  question_type,
  points,
  html,
  integrationClientId
) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({
      questionOrder: question_order,
      questionType: question_type,
      points,
      html,
      integrationClientId
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions`, requestOptions).then(handleResponse);
}

/**
 * Update Question
 */
async function updateQuestion(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId,
  html,
  points,
  questionOrder,
  questionType,
  randomizeAnswers,
  answers,
  integration
) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({
      html,
      points,
      questionOrder,
      questionType,
      randomizeAnswers,
      answers,
      integration
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}`, requestOptions).then(handleResponse);
}

/**
 * Delete Question
 */
async function deleteQuestion(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId
) {
  const requestOptions = {
    method: "DELETE",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}`, requestOptions).then(handleResponse);
}

/**
 * Delete Questions
 */
async function deleteQuestions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questions
) {
  const requestOptions = {
    method: "DELETE",
    headers: {...authHeader()},
    body: JSON.stringify(
      questions
    )
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions`, requestOptions).then(handleResponse);
}

/**
 * Create Answer
 */
async function createAnswer(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId,
  html,
  correct,
  answerOrder
) {
  const requestOptions = {
    method: "POST",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({
      html,
      correct,
      answerOrder
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers`, requestOptions).then(handleResponse);
}

/**
 * Update Answer
 */
async function updateAnswer(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId,
  answerId,
  answer_type,
  html,
  correct,
  answer_order
) {
  const requestOptions = {
    method: "PUT",
    headers: {...authHeader(), "Content-Type": "application/json"},
    body: JSON.stringify({
      "answerType": answer_type,
      html,
      correct,
      "answerOrder": answer_order
    })
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers/${answerId}`, requestOptions).then(handleResponse);
}

/**
 * Delete Answer
 */
async function deleteAnswer(experimentId, conditionId, treatmentId, assessmentId, questionId, answerId) {
  const requestOptions = {
    method: "DELETE",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers/${answerId}`, requestOptions).then(handleResponse);
}


/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
  .then(text => {
    const data = (text && isJson(text)) ? JSON.parse(text) : text

    if (
      !response ||
      response.status === 401 ||
      response.status === 402 ||
      response.status === 500 ||
      response.status === 404
    ) {
      console.log("handleResponse | 401/402/500", {response});
    } else if (response.status === 409) {
      return {
        message: data
      }
    } else if (response.status === 204) {
      console.log("handleResponse | 204", {text, data, response});
      return [];
    }

    const dataResponse = (data) ? {
      data,
      status: response.status
    } : null;

    return dataResponse || response;
  }).catch(text => {
    console.error("handleResponse | catch", {text});
  })
}

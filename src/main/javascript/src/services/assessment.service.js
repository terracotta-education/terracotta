import { authHeader, isJson } from "@/helpers";
import { api } from "@/store/api.module";

export const assessmentService = {
  fetchAssessment,
  fetchAssessmentForSubmission,
  fetchAssessments,
  createAssessment,
  updateAssessment,
  createQuestion,
  updateQuestion,
  updateQuestions,
  deleteQuestion,
  deleteQuestions,
  createAnswer,
  updateAnswer,
  updateAnswers,
  deleteAnswer,
  regradeQuestions
};


async function fetchAssessment(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}?questions=true&answers=true&submissions=true`
  );
}

async function fetchAssessmentForSubmission(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}?questions=true&answers=true&submission_id=${submissionId}`
  );
}

async function fetchAssessments(
  experimentId,
  conditionId,
  treatmentId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments`
  );
}

async function createAssessment(
  experimentId,
  conditionId,
  treatmentId,
  title,
  body
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments`,
    {
      method: "POST",
      body: {
        title,
        html: body
      }
    }
  );
}

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
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}`,
    {
      method: "PUT",
      body: {
        html: body,
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
      }
    }
  );
}

async function regradeQuestions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  body
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/regrade`,
    {
      method: "POST",
      body
    }
  );
}

async function createQuestion(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionOrder,
  questionType,
  points,
  html,
  integrationClientId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions`,
    {
      method: "POST",
      body: {
        questionOrder,
        questionType,
        points,
        html,
        integrationClientId
      }
    }
  );
}

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
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}`,
    {
      method: "PUT",
      body: {
        html,
        points,
        questionOrder,
        questionType,
        randomizeAnswers,
        answers,
        integration
      }
    }
  );
}

async function updateQuestions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questions
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions`,
    {
      method: "PUT",
      body: questions
    }
  );
}

async function deleteQuestion(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}`,
    {
      method: "DELETE"
    }
  );
}

async function deleteQuestions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questions
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions`,
    {
      method: "DELETE",
      body: questions
    }
  );
}

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
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers`,
    {
      method: "POST",
      body: {
        html,
        correct,
        answerOrder
      }
    }
  );
}

async function updateAnswer(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId,
  answerId,
  answerType,
  html,
  correct,
  answerOrder
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers/${answerId}`,
    {
      method: "PUT",
      body: {
        answerType,
        html,
        correct,
        answerOrder
      }
    }
  );
}

async function updateAnswers(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId,
  answers
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers`,
    {
      method: "PUT",
      body: answers
    }
  );
}

async function deleteAnswer(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  questionId,
  answerId
) {
  return request(
    `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/questions/${questionId}/answers/${answerId}`,
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

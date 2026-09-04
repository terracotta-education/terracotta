import {
  authHeader,
  fileAuthHeader,
  isJson
} from "@/helpers";

import { api } from "@/store/api.module";

export const submissionService = {
  getAll,
  getSubmission,
  updateSubmission,
  updateSubmissions,
  getQuestionSubmissions,
  createQuestionSubmissions,
  updateQuestionSubmissions,
  studentResponse,
  createAnswerSubmissions,
  updateAnswerSubmission,
  downloadAnswerFileSubmission
};

async function getAll(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId
) {
  return request(
    buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )
  );
}

async function getSubmission(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId
) {
  return request(
    `${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}`
  );
}

async function updateSubmission(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  alteredCalculatedGrade,
  totalAlteredGrade,
  gradeOverridden
) {
  return request(
    `${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}`,
    {
      method: "PUT",
      body: {
        alteredCalculatedGrade,
        totalAlteredGrade,
        gradeOverridden
      }
    }
  );
}

async function updateSubmissions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissions
) {
  return request(
    buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    ),
    {
      method: "PUT",
      body: submissions
    }
  );
}

async function createQuestionSubmissions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  questions
) {
  const fileSubmissions = [];
  const nonFileSubmissions = [];

  questions.forEach(question => {
    const hasFile =
      question.answerSubmissionDtoList?.some(
        answer => answer.response instanceof File
      );

    if (hasFile) {
      fileSubmissions.push(question);
    } else {
      nonFileSubmissions.push(question);
    }
  });

  const requests = [];

  for (const question of fileSubmissions) {
    for (const answer of question.answerSubmissionDtoList) {
      const formData = new FormData();
      const file = answer.response;

      answer.response = null;
      delete answer.type;

      formData.append(
        "question_dto",
        JSON.stringify(question)
      );

      formData.append("file", file);

      requests.push(
        fetch(
          `${api().aud}${buildSubmissionPath(
            experimentId,
            conditionId,
            treatmentId,
            assessmentId
          )}/${submissionId}/question_submissions/file`,
          {
            method: "POST",
            headers: fileAuthHeader(),
            body: formData
          }
        )
      );
    }
  }

  if (nonFileSubmissions.length) {
    requests.push(
      fetch(
        `${api().aud}${buildSubmissionPath(
          experimentId,
          conditionId,
          treatmentId,
          assessmentId
        )}/${submissionId}/question_submissions`,
        {
          method: "POST",
          headers: authHeader(),
          body: JSON.stringify(nonFileSubmissions)
        }
      )
    );
  }

  return handleParallelRequests(requests);
}

async function updateQuestionSubmissions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  updatedResponseBody
) {
  const isFileUpload =
    updatedResponseBody?.response instanceof File;

  if (isFileUpload) {
    const formData = new FormData();

    const file =
      updatedResponseBody.answerSubmissionDtoList?.[0]
        ?.response;

    updatedResponseBody.answerSubmissionDtoList[0].response =
      null;

    delete updatedResponseBody.answerSubmissionDtoList[0]
      .type;

    formData.append(
      "question_dto",
      JSON.stringify(updatedResponseBody)
    );

    formData.append("file", file);

    return fetch(
      `${api().aud}${buildSubmissionPath(
        experimentId,
        conditionId,
        treatmentId,
        assessmentId
      )}/${submissionId}/question_submissions/${updatedResponseBody.questionSubmissionId}/file`,
      {
        method: "PUT",
        headers: fileAuthHeader(),
        body: formData
      }
    ).then(handleResponse);
  }

  return request(
    `${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}/question_submissions`,
    {
      method: "PUT",
      body: updatedResponseBody
    }
  );
}

async function getQuestionSubmissions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId
) {
  return request(
    `${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}/question_submissions?answer_submissions=true&question_submission_comments=true`
  );
}

async function studentResponse(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId
) {
  return request(
    `${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}/question_submissions/?answer_submissions=true`
  );
}

async function createAnswerSubmissions(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  answerSubmissions
) {
  const fileSubmissions = [];
  const nonFileSubmissions = [];

  answerSubmissions.forEach(answer => {
    if (answer.type === "FILE") {
      delete answer.type;
      fileSubmissions.push(answer);
    } else {
      delete answer.type;
      nonFileSubmissions.push(answer);
    }
  });

  const requests = [];

  for (const answer of fileSubmissions) {
    const formData = new FormData();
    const file = answer.response;

    answer.response = null;

    formData.append(
      "answer_dto",
      JSON.stringify(answer)
    );

    formData.append("file", file);

    requests.push(
      fetch(
        `${api().aud}${buildSubmissionPath(
          experimentId,
          conditionId,
          treatmentId,
          assessmentId
        )}/${submissionId}/answer_submissions/file`,
        {
          method: "POST",
          headers: fileAuthHeader(),
          body: formData
        }
      )
    );
  }

  if (nonFileSubmissions.length) {
    requests.push(
      fetch(
        `${api().aud}${buildSubmissionPath(
          experimentId,
          conditionId,
          treatmentId,
          assessmentId
        )}/${submissionId}/answer_submissions`,
        {
          method: "POST",
          headers: authHeader(),
          body: JSON.stringify(nonFileSubmissions)
        }
      )
    );
  }

  return handleParallelRequests(requests);
}

async function updateAnswerSubmission(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  questionSubmissionId,
  answerSubmissionId,
  answerSubmission
) {
  const isFileUpload =
    answerSubmission.response instanceof File;

  if (isFileUpload) {
    const formData = new FormData();
    const file = answerSubmission.response;

    answerSubmission.response = null;

    delete answerSubmission.type;

    formData.append(
      "answer_dto",
      JSON.stringify(answerSubmission)
    );

    formData.append("file", file);

    return fetch(
      `${api().aud}${buildSubmissionPath(
        experimentId,
        conditionId,
        treatmentId,
        assessmentId
      )}/${submissionId}/answer_submissions/${answerSubmissionId}/file`,
      {
        method: "PUT",
        headers: fileAuthHeader(),
        body: formData
      }
    ).then(handleResponse);
  }

  return request(
    `${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}/question_submissions/${questionSubmissionId}/answer_submissions/${answerSubmissionId}`,
    {
      method: "PUT",
      body: answerSubmission
    }
  );
}

async function downloadAnswerFileSubmission(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  questionSubmissionId,
  answerSubmissionId,
  mimeType,
  fileName
) {
  const response = await fetch(
    `${api().aud}${buildSubmissionPath(
      experimentId,
      conditionId,
      treatmentId,
      assessmentId
    )}/${submissionId}/question_submissions/${questionSubmissionId}/answer_submissions/${answerSubmissionId}/file`,
    {
      method: "GET",
      headers: authHeader()
    }
  );

  const blob = await response.blob();

  const url = window.URL.createObjectURL(
    new Blob([blob], { type: mimeType })
  );

  const link = document.createElement("a");

  link.href = url;
  link.setAttribute("download", fileName);

  document.body.appendChild(link);

  link.click();
  link.remove();

  setTimeout(() => {
    window.URL.revokeObjectURL(url);
  }, 1000);

  return true;
}

function buildSubmissionPath(
  experimentId,
  conditionId,
  treatmentId,
  assessmentId
) {
  return `/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/submissions`;
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

async function handleParallelRequests(requests = []) {
  try {
    const responses = await Promise.all(requests);

    const failed = responses.find(
      response => !response.ok
    );

    return handleResponse(
      failed || responses[0]
    );
  } catch (error) {
    console.error(
      "handleParallelRequests | catch",
      error
    );

    return {
      error
    };
  }
}

async function handleResponse(response) {
  try {
    const text = await response.text();

    const data =
      text && isJson(text)
        ? JSON.parse(text)
        : text;

    if (response.status === 204) {
      return {
        data: [],
        status: response.status
      };
    }

    if (!response?.ok) {
      if ([401, 402, 500].includes(response.status)) {
        console.error(
          "handleResponse | auth/server error",
          { response }
        );
      } else if (response.status === 404) {
        console.warn(
          "handleResponse | not found",
          { response }
        );
      }

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
      { error }
    );

    return {
      error,
      status: response?.status
    };
  }
}

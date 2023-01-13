import {authHeader, fileAuthHeader, isJson} from "@/helpers";
import store from "@/store/index.js";

// /**
//  * Register methods
//  */
export const submissionService = {
  getAll,
  updateSubmission,
  getQuestionSubmissions,
  createQuestionSubmissions,
  updateQuestionSubmissions,
  studentResponse,
  createAnswerSubmissions,
  updateAnswerSubmission,
  downloadAnswerFileSubmission,
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
async function createQuestionSubmissions(
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  submission_id,
  questions
) {

    const fileSubmissions = questions.filter(q => q.answerSubmissionDtoList[0].response instanceof File);
    const nonFileSubmissions = questions.filter(q => !(q.answerSubmissionDtoList[0].response instanceof File));

    const requests = [];

    if (fileSubmissions.length > 0) {
        for (const file of fileSubmissions) {
            for (const answer of file.answerSubmissionDtoList) {
                const bodyFormData = new FormData();
                const file_ex = answer.response;
                answer.response = null;
                delete answer.type;
                const val = JSON.stringify(file)
                bodyFormData.append('question_dto', val);
                bodyFormData.append('file', file_ex);

                const requestOptions = {
                    method: "POST",
                    headers: fileAuthHeader(),
                    body: bodyFormData,
                };

                requests.push(
                    fetch(
                        `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions/file`,
                        requestOptions
                    )
                )
            }
        }
    }

    if (nonFileSubmissions.length > 0) {
        const requestOptions = {
            method: "POST",
            headers: authHeader(),
            body: JSON.stringify(nonFileSubmissions),
        };

        requests.push(
            fetch(
                `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions`,
                requestOptions
            )
        )
    }

    const allRequests = Promise.all(requests);

    try {
        const responses = await allRequests;
        const unsuccessful = responses.filter((r) => !r.ok);

        if (unsuccessful.length > 0) {
            return handleResponse(unsuccessful[0]);
        }

        return handleResponse(responses[0]);
    } catch (error) {
        console.log("createQuestionSubmissions | catch", error);
    }
}
    /**
     * Update Question Submissions
     */
    async function updateQuestionSubmissions(
        experiment_id,
        condition_id,
        treatment_id,
        assessment_id,
        submission_id,
        updatedResponseBody
    ) {

        if (updatedResponseBody.response instanceof File) {
            // file update
            const bodyFormData = new FormData();
            const file_ex = updatedResponseBody.answerSubmissionDtoList[0].response;
            updatedResponseBody.answerSubmissionDtoList[0].response = null;
            delete updatedResponseBody.answerSubmissionDtoList[0].type;
            const val = JSON.stringify(updatedResponseBody)
            bodyFormData.append('question_dto', val);
            bodyFormData.append('file', file_ex);

            const requestOptions = {
                method: "PUT",
                headers: fileAuthHeader(),
                body: bodyFormData,
            };

            return fetch(
                `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions/${updatedResponseBody.questionSubmissionId}/file`,
                requestOptions
            ).then(handleResponse);
        } else {
            // non-file update
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
    }

    async function getQuestionSubmissions(
        experiment_id,
        condition_id,
        treatment_id,
        assessment_id,
        submission_id
    ) {
        const requestOptions = {
            method: "GET",
            headers: authHeader(),
        };

        return fetch(
            `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions?answer_submissions=true&question_submission_comments=true`,
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
        submission_id
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
     * POST Answer Submissions
     */
    async function createAnswerSubmissions(
        experiment_id,
        condition_id,
        treatment_id,
        assessment_id,
        submission_id,
        answerSubmissions
    ) {
        const file_submissions = [];
        const non_file_submission = [];

        for (const x of answerSubmissions) {
            if (x.type === 'FILE') {
                delete x.type;
                file_submissions.push(x);
            } else {
                delete x.type;
                non_file_submission.push(x)
            }

        }

        const requests = [];

        if (file_submissions.length > 0) {
            for (const file of file_submissions) {
                const bodyFormData = new FormData();
                const file_ex = file.response;
                file.response = null;
                delete file.type;
                const val = JSON.stringify(file)
                bodyFormData.append('answer_dto', val);
                bodyFormData.append('file', file_ex);

                const requestOptions = {
                    method: "POST",
                    headers: fileAuthHeader(),
                    body: bodyFormData,
                };

                requests.push(
                    fetch(
                        `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/answer_submissions/file`,
                        requestOptions
                    )
                )
            }
        }

        if (non_file_submission.length > 0) {
            const requestOptions = {
                method: "POST",
                headers: authHeader(),
                body: JSON.stringify(answerSubmissions),
            };

            requests.push(
                fetch(
                    `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/answer_submissions`,
                    requestOptions
                )
            )
        }

        const allRequests = Promise.all(requests);

        try {
            const responses = await allRequests;
            const unsuccessful = responses.filter((r) => !r.ok);

            if (unsuccessful.length > 0) {
                return handleResponse(unsuccessful[0]);
            }

            return handleResponse(responses[0]);
        } catch (error) {
            console.log("createAnswerSubmissions | catch", error);
        }
    }

    /**
     * PUT Answer Submission
     */
    async function updateAnswerSubmission(
        experiment_id,
        condition_id,
        treatment_id,
        assessment_id,
        submission_id,
        question_submission_id,
        answer_submission_id,
        answerSubmission
    ) {
        if (answerSubmission.response instanceof File) {
            // file update
            const bodyFormData = new FormData();
            const file_ex = answerSubmission.response;
            answerSubmission.response = null;
            delete answerSubmission.type;
            const val = JSON.stringify(answerSubmission)
            bodyFormData.append('answer_dto', val);
            bodyFormData.append('file', file_ex);

            const requestOptions = {
                method: "PUT",
                headers: fileAuthHeader(),
                body: bodyFormData,
            };

            return fetch(
                `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/answer_submissions/${answer_submission_id}/file`,
                requestOptions
            ).then(handleResponse);
        } else {
            // non-file update
            const requestOptions = {
                method: "PUT",
                headers: authHeader(),
                body: JSON.stringify(answerSubmission),
            };

            return fetch(
                `${store.getters["api/aud"]}/api/experiments/${experiment_id}/conditions/${condition_id}/treatments/${treatment_id}/assessments/${assessment_id}/submissions/${submission_id}/question_submissions/${question_submission_id}/answer_submissions/${answer_submission_id}`,
                requestOptions
            ).then(handleResponse);
            }
    }

    /**
     * GET download student submission file
     * */
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
        const requestOptions = {
            method: "GET",
            headers: authHeader()
        };

        console.log("url: " + `${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/submissions/${submissionId}/question_submissions/${questionSubmissionId}/answer_submissions/${answerSubmissionId}/file`);
        return fetch(
            `${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/submissions/${submissionId}/question_submissions/${questionSubmissionId}/answer_submissions/${answerSubmissionId}/file`,
            requestOptions
        ).then(async (response) => {
            const blob = await response.blob();
            const newBlob = new Blob([blob]);
            const url = window.URL.createObjectURL(newBlob, { type: mimeType });
            const link = document.createElement('a');

            link.href = url;
            link.setAttribute('download', fileName);
            document.body.appendChild(link);

            link.click();
            link.remove();

            return new Promise(resolve => {
                setTimeout(() => {
                  resolve();
                }, 1000);
              });
        });
    }

    /**
     * Handle API response
     */
    function handleResponse(response) {
        return response
            .text()
            .then((text) => {
                const data = text && isJson(text) ? JSON.parse(text) : text;

                if (!response || !response.ok) {
                    if (
                        response.status === 401 ||
                        response.status === 402 ||
                        response.status === 500
                    ) {
                        console.log("handleResponse | 401/402/500", {response});
                    } else if (response.status === 404) {
                        console.log("handleResponse | 404", {response});
                    }
                } else if (response.status === 204) {
                    console.log("handleResponse | 204", {text, data, response});
                    return {data: [], status: response.status};
                }

                const dataResponse = data ? {data, status: response.status} : null;

                return dataResponse || response;
            })
            .catch((text) => {
                console.log("handleResponse | catch", {text});
            });
    }

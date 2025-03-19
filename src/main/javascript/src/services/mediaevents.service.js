import { authHeader } from "@/helpers";
import store from "@/store/index.js";

// /**
//  * Register methods
//  */
export const mediaEventsService = {
  createVideoEvent,
};

/**
 * Send Video Started Event
 */
async function createVideoEvent({
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  questionId,
  event,
}) {
  const requestOptions = {
    method: "POST",
    headers: authHeader(),
    body: JSON.stringify(event),
  };

  const url = `${store.getters["api/aud"]}/api/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/assessments/${assessmentId}/submissions/${submissionId}/questions/${questionId}/media_event`;
  // console.log(requestOptions.method, url, event);
  return fetch(url, requestOptions).then(handleResponse);
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
        console.log("handleResponse | 204", { text, data, response });
        return [];
      }

      return data || response;
    })
    .catch((text) => {
      console.log("handleResponse | catch", { text });
    });
}

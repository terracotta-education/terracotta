import { authHeader } from "@/helpers";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const messageContentAttachmentService = {
  getAll
}

/**
 * Get all message content attachments
 */
async function getAll(experimentId, exposureId, containerId, messageId, contentId) {
  const requestOptions = {
      method: "GET",
      headers: {...authHeader()}
    }

  return fetch(`${store.getters["api/aud"]}/api/experiments/${experimentId}/exposures/${exposureId}/messaging/container/${containerId}/message/${messageId}/content/${contentId}/file`, requestOptions).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
    .then(text => {
      const data = text && JSON.parse(text);

      if (!response || !response.ok) {
        if (response.status === 401 || response.status === 402 || response.status === 500) {
          console.log("handleResponse | 401/402/500",{response});
        } else if (response.status === 404) {
          console.log("handleResponse | 404",{response});
        }

        return response;
      } else if (response.status === 204) {
        return [];
      }

      return data || response;
    }).catch(text => {
      console.error("handleResponse | catch",{text});
    })
}

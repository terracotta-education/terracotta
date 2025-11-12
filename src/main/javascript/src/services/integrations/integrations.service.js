import { authHeader } from "@/helpers";
import store from "@/store/index.js";

/**
 * Register methods
 */
export const integrationsService = {
  validateIframeUrl
}

/**
 * Validate iframe URL
 */
async function validateIframeUrl(url) {
  const requestOptions = {
      method: "GET",
      headers: {...authHeader()}
    }

  return fetch(`${store.getters["api/aud"]}/integrations/validate/iframe?url=${encodeURIComponent(url)}`, requestOptions).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
    .then(() => {
      if (!response || !response.ok) {
        if (response.status === 401 || response.status === 402 || response.status === 500) {
          console.log("handleResponse | 401/402/500",{response});
        } else if (response.status === 404) {
          console.log("handleResponse | 404",{response});
        }

        return false;
      }

      return true;
    }).catch(text => {
      console.error("handleResponse | catch",{text});
    })
}

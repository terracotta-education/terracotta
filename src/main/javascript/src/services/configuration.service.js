import {authHeader, isJson} from "@/helpers";
import store from "@/store/index";

/**
 * Register methods
 */
export const configurationService = {
  get
}

/**
 * Get configurations
 */
async function get() {
  const requestOptions = {
    method: "GET",
    headers: {...authHeader()}
  }

  return fetch(`${store.getters["api/aud"]}/api/configuration`, requestOptions).then(handleResponse);
}

/**
 * Handle API response
 */
function handleResponse(response) {
  return response.text()
  .then(text => {
    const data = (text && isJson(text)) ? JSON.parse(text) : text;

    if (response.status === 409) {
      return {
        message: data
      }
    } else if (response.status === 204) {
      return [];
    }

    return data || response;
  }).catch(text => {
    console.log("handleResponse | catch", { text });
  })
}

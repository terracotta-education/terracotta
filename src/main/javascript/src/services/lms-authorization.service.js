import { authHeader } from "@/helpers";
import { api } from "@/store/api.module";

export const lmsAuthorizationService = {
  getAuthorization
};

// whether the current instructor has LMS API access; anything short of a clear yes counts as no,
// since the caller only uses it to decide whether to keep waiting
async function getAuthorization() {
  try {
    const response = await fetch(
      `${api().aud}/api/lms/authorization`,
      {
        method: "GET",
        headers: {
          ...authHeader()
        }
      }
    );

    if (!response.ok) {
      return { authorized: false };
    }

    const data = await response.json();

    return { authorized: data?.authorized === true };
  } catch (error) {
    console.error("lmsAuthorizationService.getAuthorization | catch", error);

    return { authorized: false };
  }
}

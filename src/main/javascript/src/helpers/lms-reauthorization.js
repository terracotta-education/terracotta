// the server flags a response with this header when an LMS call failed because the user's LMS
// API token no longer works (see RestResponseEntityExceptionHandler). Only relaunching Terracotta
// can fix that, since the authorization link needs the launch.
export const LMS_REAUTHORIZATION_HEADER = "X-Terracotta-Lms-Reauthorization";

// Every service reads its own responses, so this watches fetch once instead of changing each of
// them. It tells an instructor once per page load; students are never told, because their
// requests can fail on an instructor's token, which they can't fix.
export function watchForLmsReauthorization({ target = window, isInstructor, notify }) {
  const originalFetch = target.fetch.bind(target);
  let notified = false;

  target.fetch = async (...args) => {
    const response = await originalFetch(...args);

    if (!notified && response?.headers?.get?.(LMS_REAUTHORIZATION_HEADER) === "true" && isInstructor()) {
      notified = true;
      notify();
    }

    return response;
  };
}

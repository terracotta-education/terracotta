// Signs a test page in as an instructor or a student, without an LTI launch: the harness
// (e2e/harness/main.js) copies this session into the api store before the app mounts.
const INSTRUCTOR_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";
const LEARNER_ROLE = "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";

const base64Url = value => Buffer.from(JSON.stringify(value)).toString("base64url");

// an unsigned stand-in for the API token: the app only decodes it, and no real server checks it
const fakeToken = claims => `${base64Url({ alg: "RS256", typ: "JWT" })}.${base64Url(claims)}.signature`;

/**
 * @param {import("@playwright/test").Page} page
 * @param {object} options
 * @param {"instructor" | "student"} options.as
 * @param {string | object} [options.route] the app route to open: a path, or better a router
 *   location such as { name: "ExperimentSummary", params: { experimentId: "exp-1" } }, which
 *   doesn't depend on how the route's URL is spelled
 * @param {string} [options.experimentId]
 * @param {string} [options.lmsAssignmentId] the LMS assignment a student launched from ("" for none)
 * @param {boolean} [options.consent] whether a student launched the consent form
 * @param {string} [options.lmsApiOAuthUrl] set when the instructor still has to authorize LMS access
 */
export async function signIn(page, { as, route = "/", experimentId = "", lmsAssignmentId = "", consent = false, lmsApiOAuthUrl = "" }) {
  const claims = {
    userId: "e2e-user",
    roles: [as === "instructor" ? INSTRUCTOR_ROLE : LEARNER_ROLE],
    experimentId,
    lmsAssignmentId,
    consent,
    exp: Math.floor(Date.now() / 1000) + 3600
  };

  await page.addInitScript(
    ({ session, route: initialRoute }) => {
      session.claims.aud = window.location.origin;
      window.__E2E_SESSION__ = session;
      window.__E2E_ROUTE__ = initialRoute;
    },
    { session: { token: fakeToken(claims), claims, lmsApiOAuthURL: lmsApiOAuthUrl }, route }
  );
}

export async function openApp(page) {
  await page.goto("/e2e/harness/index.html");
}

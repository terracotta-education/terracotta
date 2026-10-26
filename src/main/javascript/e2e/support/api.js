// Answers every /api request a test page makes, so the tests need no backend. Give the
// responses by path, either exact ("/api/experiments/exp-1") or as a RegExp; anything else gets
// an empty 200 list, which is what most list endpoints return for "nothing here".

/**
 * @param {import("@playwright/test").Page} page
 * @param {Array<[string | RegExp, unknown]>} responses [path, JSON body] pairs, first match wins
 * @returns {string[]} the paths requested, in order, for asserting on what the page asked for
 */
export async function mockApi(page, responses = []) {
  const requested = [];

  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    requested.push(path);

    const match = responses.find(([pattern]) => (pattern instanceof RegExp ? pattern.test(path) : pattern === path));
    const body = match ? match[1] : [];

    return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
  });

  return requested;
}

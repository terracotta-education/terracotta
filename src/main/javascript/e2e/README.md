# Browser tests

Playwright tests that run the real app in Chromium: its router, stores, components and styles,
served by Vite. There's no backend and no LMS. Each test signs in with a fake session and answers
the app's `/api` requests itself.

```sh
yarn test:e2e                 # all of them
yarn test:e2e e2e/home.e2e.js # one file
yarn test:e2e --ui            # watch them run, step through, inspect the page
```

The first run needs the browser: `yarn playwright install chromium`.

## Writing one

Tests are `*.e2e.js` files in this folder (Vitest only picks up `src/**/*.spec.js`).

```js
import { expect, test } from "@playwright/test";

import { mockApi } from "./support/api.js";
import { openApp, signIn } from "./support/session.js";
import { EXPERIMENT_ID, experimentResponses } from "./fixtures/experiment.js";

test("the components tab lists the experiment's assignments", async ({ page }) => {
  await mockApi(page, experimentResponses);
  await signIn(page, { as: "instructor", route: { name: "ExperimentSummary", params: { experimentId: EXPERIMENT_ID } } });

  await openApp(page);

  await expect(page.getByText("Assignment 1", { exact: true })).toBeVisible();
});
```

- `signIn(page, { as, route, ... })`: an instructor or a student session, and the route to open.
  It goes straight into the store, standing in for an LTI launch (see `harness/main.js`).
- `mockApi(page, [[path, body], ...])`: answers `/api` requests by exact path or RegExp, first
  match wins. Anything else gets an empty list. It returns the paths requested, in order.
- `fixtures/`: API data shared between tests. Add to it rather than repeating data in each test.

Find elements the way a user would (`getByText`, `getByRole`) where you can, and scope to a
section when the same text appears more than once on a page.

## Accessibility scans

`expectNoAccessibilityViolations(page)` (in `support/accessibility.js`) scans the page with axe
against WCAG 2.2 A and AA and fails on anything it finds, listing each rule, how serious it is and
the elements it found it on. Each page the tests open gets a scan once it has loaded; add one when
you add a page. Run them like any other test (`yarn test:e2e`), before you push.

If a rule can't be fixed yet, skip it for that page with `{ disableRules: ["rule-id"] }` and a
comment saying why.

Automated checks find roughly a third to a half of real accessibility problems. Behavior a screen
reader user depends on, such as the arrow keys moving between a question's answers, needs a test
of its own (see `student-quiz.e2e.js`), and nothing replaces trying a page with a screen reader.

## What these don't cover

The API is mocked, so these tests can't tell that the frontend and backend disagree about a
response. That's what the Java tests are for.

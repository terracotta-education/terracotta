// Playwright global setup. On a fresh dev server, the first page to load a lazy route makes Vite
// discover more dependencies, re-optimize them and reload the page, which can land in the middle
// of a test. Opening the app's pages once here, before any test runs, gets that out of the way.
import { chromium } from "@playwright/test";

import { mockApi } from "./api.js";
import { openApp, signIn } from "./session.js";
import { EXPERIMENT_ID, experimentResponses } from "../fixtures/experiment.js";

const ROUTES = [
  "/",
  { name: "ExperimentSummary", params: { experimentId: EXPERIMENT_ID } }
];

export default async function warmUp(config) {
  const browser = await chromium.launch();
  const page = await browser.newPage({ baseURL: config.projects[0].use.baseURL });

  try {
    await mockApi(page, experimentResponses);

    for (const route of ROUTES) {
      await signIn(page, { as: "instructor", route });
      await openApp(page);
      await page.waitForLoadState("networkidle");
    }
  } finally {
    await browser.close();
  }
}

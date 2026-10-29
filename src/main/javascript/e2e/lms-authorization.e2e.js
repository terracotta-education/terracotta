import { expect, test } from "@playwright/test";

import { expectNoAccessibilityViolations } from "./support/accessibility.js";
import { mockApi } from "./support/api.js";
import { openApp, signIn } from "./support/session.js";
import { EXPERIMENT_ID, configuration } from "./fixtures/experiment.js";

const AUTHORIZATION_URL = "https://canvas.example.edu/login/oauth2/auth?client_id=1&response_type=code";

// until an instructor authorizes Terracotta in the LMS, nothing else may load first
test("an instructor who has to authorize LMS access goes to the authorization page", async ({ page }) => {
  await mockApi(page, [["/api/configuration", configuration]]);
  await signIn(page, {
    as: "instructor",
    route: { name: "ExperimentSummary", params: { experimentId: EXPERIMENT_ID } },
    lmsApiOAuthUrl: AUTHORIZATION_URL
  });

  await openApp(page);

  await expect(page.getByText("Terracotta wants to access your Canvas account")).toBeVisible();
  await expect(page.getByRole("link", { name: "Go to the Canvas Authorization Page" })).toHaveAttribute("href", AUTHORIZATION_URL);
  await expectNoAccessibilityViolations(page);
});

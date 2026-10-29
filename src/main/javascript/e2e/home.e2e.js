import { expect, test } from "@playwright/test";

import { expectNoAccessibilityViolations } from "./support/accessibility.js";
import { mockApi } from "./support/api.js";
import { openApp, signIn } from "./support/session.js";
import { configuration, experiment } from "./fixtures/experiment.js";

test("an instructor's home page lists their experiments", async ({ page }) => {
  await mockApi(page, [
    ["/api/experiments", [experiment, { ...experiment, experimentId: "exp-2", title: "Feedback timing study" }]],
    ["/api/experiments/copy-status", { status: "NONE", importIds: [] }],
    ["/api/configuration", configuration]
  ]);
  await signIn(page, { as: "instructor" });

  await openApp(page);

  await expect(page.getByText("Reading strategies study")).toBeVisible();
  await expect(page.getByText("Feedback timing study")).toBeVisible();
  await expectNoAccessibilityViolations(page);
});

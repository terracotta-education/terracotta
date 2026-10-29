import { expect, test } from "@playwright/test";

import { expectNoAccessibilityViolations } from "./support/accessibility.js";
import { mockApi } from "./support/api.js";
import { openApp, signIn } from "./support/session.js";
import { EXPERIMENT_ID, experimentResponses } from "./fixtures/experiment.js";

test.describe("experiment summary, components tab", () => {
  test.beforeEach(async ({ page }) => {
    await mockApi(page, experimentResponses);
    await signIn(page, { as: "instructor", route: { name: "ExperimentSummary", params: { experimentId: EXPERIMENT_ID } } });

    await openApp(page);
  });

  test("lists each assignment with a treatment per condition", async ({ page }) => {
    for (const title of ["Assignment 1", "Assignment 2", "Assignment 3"]) {
      await expect(page.getByText(title, { exact: true })).toBeVisible();
    }

    // two conditions, three assignments: a treatment row per condition under each (counted within
    // the treatment rows, since the design card further down names each condition too)
    const treatmentRows = page.locator(".treatment-info-group");

    await expect(treatmentRows.filter({ hasText: "Condition A" })).toHaveCount(3);
    await expect(treatmentRows.filter({ hasText: "Condition B" })).toHaveCount(3);
    // none of the fixture's treatments has content yet
    await expect(page.getByText("Click to build treatment")).toHaveCount(6);
  });

  test("explains the experiment's exposure sets", async ({ page }) => {
    await expect(page.getByRole("heading", { name: "Exposure Sets" })).toBeVisible();
    await expect(page.getByText("we set you up with")).toContainText("1 exposure sets");
  });

  test("has no accessibility problems axe can find", async ({ page }) => {
    await expect(page.getByText("Assignment 1", { exact: true })).toBeVisible();
    await expectNoAccessibilityViolations(page);
  });
});

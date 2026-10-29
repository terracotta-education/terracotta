// Scans the page with axe (the engine most accessibility checkers use) and fails on anything it
// finds against WCAG 2.2 A and AA. Automated checks catch about a third to a half of real problems,
// so they don't replace testing with a keyboard and a screen reader.
import AxeBuilder from "@axe-core/playwright";
import { expect } from "@playwright/test";

const WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22a", "wcag22aa"];

/**
 * @param {import("@playwright/test").Page} page
 * @param {object} [options]
 * @param {string[]} [options.disableRules] axe rule IDs to skip, each with a comment saying why
 */
export async function expectNoAccessibilityViolations(page, { disableRules = [] } = {}) {
  const { violations } = await new AxeBuilder({ page })
    .withTags(WCAG_TAGS)
    .disableRules(disableRules)
    .analyze();

  // one line per problem, naming the rule, how serious it is, and where it was found
  const found = violations.map(violation =>
    `${violation.id} (${violation.impact}): ${violation.help}. Found on: ${violation.nodes.map(node => node.target.join(" ")).join(", ")}. See ${violation.helpUrl}`
  );

  expect(found, "accessibility problems found by axe").toEqual([]);
}

import { describe, expect, it } from "vitest";
import router from "./index.js";

describe("router scrollBehavior", () => {
  // no scrollBehavior at all meant every navigation (create/edit assignment,
  // create/edit message, etc.) just kept whatever scroll position the previous page
  // happened to be at, since this is one persistent document across route changes,
  // not a real page load.
  it("scrolls to top on a fresh forward navigation", () => {
    expect(router.options.scrollBehavior({}, {}, null)).toEqual({ top: 0 });
  });

  it("restores the saved position on browser back/forward instead of forcing top", () => {
    const savedPosition = { top: 450, left: 0 };

    expect(router.options.scrollBehavior({}, {}, savedPosition)).toBe(savedPosition);
  });
});

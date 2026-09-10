import { afterEach, describe, expect, it, vi } from "vitest";
import router from "./index.js";

describe("router scrollBehavior", () => {
  const originalTop = window.top;

  afterEach(() => {
    Object.defineProperty(window, "top", { value: originalTop, configurable: true });
    vi.restoreAllMocks();
  });

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

  // window.scrollTo (what returning {top: 0} above actually does) only touches this
  // document's own window - for the real LTI-launched case, the app's iframe is kept
  // resized to fit its content exactly, so there's nothing to scroll inside the
  // iframe at all: the page that actually scrolls is the LMS's own outer page, which
  // this window can't reach. Canvas's own documented postMessage API is the real fix
  // for that case - see doc/api/lti_window_post_message.md in canvas-lms.
  it("asks the LMS parent to scroll to top on a fresh forward navigation when embedded in an iframe", () => {
    Object.defineProperty(window, "top", { value: {}, configurable: true });
    const postMessageSpy = vi.spyOn(window.parent, "postMessage");

    router.options.scrollBehavior({}, {}, null);

    expect(postMessageSpy).toHaveBeenCalledWith({ subject: "lti.scrollToTop" }, "*");
  });

  it("does not ask the LMS parent to scroll when not embedded in an iframe", () => {
    const postMessageSpy = vi.spyOn(window, "postMessage");

    router.options.scrollBehavior({}, {}, null);

    expect(postMessageSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ subject: "lti.scrollToTop" }),
      "*"
    );
  });

  it("does not ask the LMS parent to scroll on browser back/forward (a savedPosition navigation)", () => {
    Object.defineProperty(window, "top", { value: {}, configurable: true });
    const postMessageSpy = vi.spyOn(window.parent, "postMessage");

    router.options.scrollBehavior({}, {}, { top: 450, left: 0 });

    expect(postMessageSpy).not.toHaveBeenCalled();
  });
});

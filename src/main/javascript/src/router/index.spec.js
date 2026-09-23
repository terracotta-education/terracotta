import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const fetchExperimentByIdMock = vi.fn();

vi.mock("@/store/experiment.module", () => ({
  experiment: () => ({ fetchExperimentById: fetchExperimentByIdMock })
}));

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

  // document.referrer reliably names the actual current parent frame in the real
  // LTI-launched case - "*" (asserted above) is only the fallback for when it isn't
  // available, not the normal path.
  it("narrows the postMessage target to the parent's own origin when document.referrer is available", () => {
    Object.defineProperty(window, "top", { value: {}, configurable: true });
    Object.defineProperty(document, "referrer", {
      value: "https://canvas.instructure.com/courses/123",
      configurable: true
    });
    const postMessageSpy = vi.spyOn(window.parent, "postMessage");

    router.options.scrollBehavior({}, {}, null);

    expect(postMessageSpy).toHaveBeenCalledWith(
      { subject: "lti.scrollToTop" },
      "https://canvas.instructure.com"
    );

    Object.defineProperty(document, "referrer", { value: "", configurable: true });
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

// The route table itself, and the two `beforeEnter` guards (`beforeExperimentSteps`/
// `beforeExperimentOutcome`), are defined in router/index.js but not individually exported -
// they're reached here through the real, already-constructed `router` singleton, exactly the way
// Vue Router itself would invoke them during navigation, without needing to drive a full
// navigation.
describe("router route table", () => {
  beforeEach(() => {
    fetchExperimentByIdMock.mockReset();
  });

  it("registers the expected named routes", () => {
    const names = router.getRoutes().map(route => route.name).filter(Boolean);

    expect(names).toContain("Home");
    expect(names).toContain("oauth2-redirect");
    expect(names).toContain("ExperimentDesignTitle");
    expect(names).toContain("OutcomeScoring");
    expect(names).toContain("StudentSubmissionGrading");
    expect(names).toContain("TerracottaBuilder");
    expect(names).toContain("AssignmentEditor");
  });

  it("redirects any unmatched path to Home", () => {
    const catchAll = router
      .getRoutes()
      .find(route => route.path === "/:pathMatch(.*)*");

    expect(catchAll.redirect).toEqual({ name: "Home" });
  });

  it("carries custom appStyle meta on the oauth2-redirect route", () => {
    const oauthRoute = router
      .getRoutes()
      .find(route => route.name === "oauth2-redirect");

    expect(oauthRoute.meta.appStyle).toEqual({
      backgroundColor: "#fdf5f2",
      "overflow-y": "visible",
      "min-height": "fit-content"
    });
  });

  it("resolves every lazily-loaded route component", async () => {
    const loaders = router
      .getRoutes()
      .map(route => route.components?.default)
      .filter(component => typeof component === "function");

    expect(loaders.length).toBeGreaterThan(0);

    const modules = await Promise.all(loaders.map(loader => loader()));

    modules.forEach(module => {
      expect(module.default).toBeDefined();
    });
  }, 30000);

  describe("beforeExperimentSteps guard", () => {
    // Vue Router creates a separate flattened route record for the parent (which carries
    // `beforeEnter`) AND for its own `path: ""` child (which does not), both resolving to the
    // exact same "/experiment/:experimentId/participation" URL - so the lookup must require the
    // guard itself to be present, not just match on path, or `.find()` can land on the wrong one.
    function getGuard() {
      return router
        .getRoutes()
        .find(route =>
          route.path === "/experiment/:experimentId/participation" &&
          typeof route.beforeEnter === "function"
        )
        .beforeEnter;
    }

    it("skips refetching the experiment when moving from consent title straight to consent file", async () => {
      const guard = getGuard();
      const next = vi.fn();

      await guard(
        { name: "ParticipationTypeConsentFile" },
        { name: "ParticipationTypeConsentTitle" },
        next
      );

      expect(fetchExperimentByIdMock).not.toHaveBeenCalled();
      expect(next).toHaveBeenCalledWith();
      expect(next).toHaveBeenCalledTimes(1);
    });

    it("fetches the experiment by id and proceeds on success", async () => {
      fetchExperimentByIdMock.mockResolvedValue({ status: 200 });

      const guard = getGuard();
      const next = vi.fn();

      await guard(
        { name: "ExperimentParticipationIntro", params: { experimentId: "42" } },
        { name: "Home" },
        next
      );

      expect(fetchExperimentByIdMock).toHaveBeenCalledWith("42");
      expect(next).toHaveBeenCalledWith();
    });

    it("still calls next (with the error) when the fetch rejects", async () => {
      const error = new Error("network down");

      fetchExperimentByIdMock.mockRejectedValue(error);

      const guard = getGuard();
      const next = vi.fn();

      await guard(
        { name: "ExperimentParticipationIntro", params: { experimentId: "42" } },
        { name: "Home" },
        next
      );

      expect(next).toHaveBeenCalledWith(error);
    });
  });

  describe("beforeExperimentOutcome guard", () => {
    function getGuard() {
      return router
        .getRoutes()
        .find(route =>
          route.path === "/experiment/:experimentId/exposure/:exposureId" &&
          typeof route.beforeEnter === "function"
        )
        .beforeEnter;
    }

    it("fetches the experiment by id and proceeds on success", async () => {
      fetchExperimentByIdMock.mockResolvedValue({ status: 200 });

      const guard = getGuard();
      const next = vi.fn();

      await guard(
        { params: { experimentId: "7" } },
        { name: "Home" },
        next
      );

      expect(fetchExperimentByIdMock).toHaveBeenCalledWith("7");
      expect(next).toHaveBeenCalledWith();
    });

    it("still calls next (with the error) when the fetch rejects", async () => {
      const error = new Error("network down");

      fetchExperimentByIdMock.mockRejectedValue(error);

      const guard = getGuard();
      const next = vi.fn();

      await guard(
        { params: { experimentId: "7" } },
        { name: "Home" },
        next
      );

      expect(next).toHaveBeenCalledWith(error);
    });
  });
});

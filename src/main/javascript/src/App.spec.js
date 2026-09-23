import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { flushPromises } from "@vue/test-utils";

vi.mock("vue-router", () => ({
  useRoute: () => ({ meta: {} })
}));

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn().mockResolvedValue({})
  }
}));

vi.mock("@/services", () => ({
  apiService: {
    getApiToken: vi.fn(),
    refreshToken: vi.fn(),
    reportStep: vi.fn(),
    deepLinkJwt: vi.fn()
  },
  configurationService: {
    get: vi.fn().mockResolvedValue({ data: {} })
  }
}));

import Swal from "sweetalert2";
import { apiService, configurationService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import { api as apiModule } from "@/store/api.module";
import { configuration as configurationModule } from "@/store/configuration.module";
import App from "./App.vue";

const FIFTY_NINE_MINUTES_MS = 1000 * 60 * 59;

function base64url(obj) {
  return Buffer.from(JSON.stringify(obj))
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function makeToken(payload) {
  return `${base64url({ alg: "HS256", typ: "JWT" })}.${base64url(payload)}.signature`;
}

function validToken() {
  return makeToken({ exp: Math.floor(Date.now() / 1000) + 3600 });
}

function expiredToken() {
  return makeToken({ exp: Math.floor(Date.now() / 1000) - 3600 });
}

async function mountApp(props = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const apiStore = apiModule();

  const wrapper = mountComponent(App, {
    shallow: true,
    pinia,
    props
  });

  await flushPromises(); // let onMounted's await retrieveConfiguration() resolve

  return { wrapper, apiStore };
}

function foregroundTab() {
  Object.defineProperty(document, "visibilityState", { value: "visible", configurable: true });
  document.dispatchEvent(new Event("visibilitychange"));
}

// mountApp() above always mounts with { shallow: true }, which - per @vue/test-utils -
// auto-stubs EVERY child component App.vue's template encounters, including Vuetify's own
// globally-registered ones (v-app, v-main, v-row, v-col, v-alert...). Since App.vue's whole
// template lives inside v-app's default slot, and VTU's default stubs don't render a stubbed
// component's slot content, that means none of App.vue's own template (the Instructor/
// Treatment-Preview/Learner/Error/Integration/Obsolete v-if branches, and every computed only
// read from the template) ever actually renders under mountApp() - confirmed by inspecting
// wrapper.html(), which comes back as a bare `<v-app-stub>` with no children. mountApp()'s
// existing tests still work because everything they assert on (setInterval/localStorage/
// visibilitychange/the app--embedded class/frame-resize reporting) lives in script-level logic
// or on attributes of v-app itself, not inside that swallowed slot.
//
// To actually exercise App.vue's own template branches, mount WITHOUT shallow (so v-app/v-main/
// v-row/v-col/v-alert render for real, like ExperimentSummary.spec.js's non-shallow pattern),
// while explicitly stubbing out the heavy child views/components App.vue imports - the same
// role `shallow` would normally play, just scoped to this component's own children instead of
// every descendant.
const templateStubs = {
  SkipTo: true,
  StatusAlert: true,
  StudentQuiz: true,
  StudentConsent: true,
  TreatmentPreviewComplete: true,
  PageLoading: true,
  IntegrationsTokenAlert: true,
  Integrations: true,
  IntegrationsPreview: true,
  Assignment: true,
  "router-view": true
};

async function mountAppRendered(props = {}, configure = () => {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const apiStore = apiModule();
  const configurationStore = configurationModule();

  configure(apiStore, configurationStore);

  const wrapper = mountComponent(App, {
    pinia,
    props,
    global: {
      stubs: templateStubs,
      mocks: {
        $route: { path: "/some-route" }
      }
    }
  });

  await flushPromises(); // let onMounted's await retrieveConfiguration() resolve

  return { wrapper, apiStore, configurationStore };
}

describe("App", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("registers a refresh interval and a visibilitychange listener on mount, and tears both down on unmount", async () => {
    const setIntervalSpy = vi.spyOn(window, "setInterval");
    const clearIntervalSpy = vi.spyOn(window, "clearInterval");
    const addListenerSpy = vi.spyOn(document, "addEventListener");
    const removeListenerSpy = vi.spyOn(document, "removeEventListener");

    const { wrapper } = await mountApp();

    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), FIFTY_NINE_MINUTES_MS);
    expect(addListenerSpy).toHaveBeenCalledWith("visibilitychange", expect.any(Function));

    wrapper.unmount();

    expect(clearIntervalSpy).toHaveBeenCalled();
    expect(removeListenerSpy).toHaveBeenCalledWith("visibilitychange", expect.any(Function));
  });

  it("refreshes the token when the tab is foregrounded with a still-valid token", async () => {
    const token = validToken();
    apiService.refreshToken.mockResolvedValue(token);
    const { apiStore } = await mountApp();
    apiStore.apiToken = token;

    foregroundTab();
    await flushPromises();

    expect(apiService.refreshToken).toHaveBeenCalled();
    expect(Swal.fire).not.toHaveBeenCalled();
  });

  it("shows the session-expired dialog exactly once and stops monitoring when the tab is foregrounded with an already-expired token", async () => {
    const clearIntervalSpy = vi.spyOn(window, "clearInterval");
    const removeListenerSpy = vi.spyOn(document, "removeEventListener");
    const { apiStore } = await mountApp();
    apiStore.apiToken = expiredToken();

    foregroundTab();
    await flushPromises();

    expect(apiService.refreshToken).not.toHaveBeenCalled();
    expect(apiStore.sessionExpired).toBe(true);
    expect(Swal.fire).toHaveBeenCalledTimes(1);
    expect(Swal.fire).toHaveBeenCalledWith(expect.stringContaining("return to your course"));
    expect(clearIntervalSpy).toHaveBeenCalled();
    expect(removeListenerSpy).toHaveBeenCalledWith("visibilitychange", expect.any(Function));

    foregroundTab();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledTimes(1);
  });

  // authHeader()/fileAuthHeader() (src/helpers/auth-header.js) call apiStore.markSessionExpired()
  // directly, independent of this component's own interval/visibility-change check - e.g. some
  // other component's API call notices the expired token first, before the 59-minute interval
  // or a visibility change ever fires here (a backgrounded/throttled tab). The dialog must still
  // show when sessionExpired flips to true from that path, not only from this component's own
  // detection.
  it("shows the session-expired dialog when sessionExpired is set from outside this component's own check", async () => {
    const clearIntervalSpy = vi.spyOn(window, "clearInterval");
    const { apiStore } = await mountApp();
    apiStore.apiToken = expiredToken();

    apiStore.markSessionExpired();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledTimes(1);
    expect(Swal.fire).toHaveBeenCalledWith(expect.stringContaining("return to your course"));
    expect(clearIntervalSpy).toHaveBeenCalled();
  });

  it("does nothing when there is no apiToken yet", async () => {
    await mountApp();

    foregroundTab();
    await flushPromises();
    await vi.advanceTimersByTimeAsync(FIFTY_NINE_MINUTES_MS);

    expect(apiService.refreshToken).not.toHaveBeenCalled();
    expect(Swal.fire).not.toHaveBeenCalled();
  });

  it("refreshes on the 59-minute interval alone, without a visibility event", async () => {
    const token = validToken();
    apiService.refreshToken.mockResolvedValue(token);
    const { apiStore } = await mountApp();
    apiStore.apiToken = token;

    await vi.advanceTimersByTimeAsync(FIFTY_NINE_MINUTES_MS);

    expect(apiService.refreshToken).toHaveBeenCalled();
  });

  it("applies the same monitoring/dialog behavior in treatment preview mode", async () => {
    const { apiStore } = await mountApp({
      treatmentPreviewData: {
        preview: true,
        experimentId: "1",
        conditionId: "1",
        treatmentId: "1",
        previewId: "1",
        ownerId: "1"
      }
    });
    apiStore.apiToken = expiredToken();

    foregroundTab();
    await flushPromises();

    expect(Swal.fire).toHaveBeenCalledTimes(1);
  });

  it("clears stale localStorage keys on mount but preserves quiz-draft keys", async () => {
    localStorage.setItem("terracotta-api", "stale-token");
    localStorage.setItem("terracotta-quiz-draft-1-2", "{}");

    await mountApp();

    expect(localStorage.getItem("terracotta-api")).toBeNull();
    expect(localStorage.getItem("terracotta-quiz-draft-1-2")).toBe("{}");
  });

  describe("app--embedded class (see _global.scss's .v-application__wrap override)", () => {
    const originalTop = window.top;

    afterEach(() => {
      Object.defineProperty(window, "top", { value: originalTop, configurable: true });
    });

    it("is not applied to a plain top-level page (e.g. the treatment preview window)", async () => {
      const { wrapper } = await mountApp();

      expect(wrapper.classes()).not.toContain("app--embedded");
    });

    it("is applied when embedded in an iframe (the real LTI tool)", async () => {
      Object.defineProperty(window, "top", { value: {}, configurable: true });

      const { wrapper } = await mountApp();

      expect(wrapper.classes()).toContain("app--embedded");
    });
  });

  describe("frame resize reporting (lti.frameResize)", () => {
    const originalTop = window.top;

    afterEach(() => {
      Object.defineProperty(window, "top", { value: originalTop, configurable: true });
    });

    it("does not report a height to the parent when not embedded in an iframe", async () => {
      const postMessageSpy = vi.spyOn(window, "postMessage");

      await mountApp();

      expect(postMessageSpy).not.toHaveBeenCalledWith(
        expect.objectContaining({ subject: "lti.frameResize" }),
        "*"
      );
    });

    it("reports the real measured page height to the parent LMS on mount when embedded in an iframe", async () => {
      Object.defineProperty(window, "top", { value: {}, configurable: true });
      Object.defineProperty(document.body, "offsetHeight", { value: 1234, configurable: true });
      Object.defineProperty(document.documentElement, "offsetHeight", { value: 1000, configurable: true });
      const postMessageSpy = vi.spyOn(window.parent, "postMessage");

      await mountApp();

      expect(postMessageSpy).toHaveBeenCalledWith({ subject: "lti.frameResize", height: 1234 }, "*");
    });

    it("stops reporting once unmounted", async () => {
      Object.defineProperty(window, "top", { value: {}, configurable: true });
      const disconnectSpy = vi.spyOn(ResizeObserver.prototype, "disconnect");

      const { wrapper } = await mountApp();
      wrapper.unmount();

      expect(disconnectSpy).toHaveBeenCalled();
    });

    // a SweetAlert2 dialog renders as a `position: fixed` overlay appended to <body> -
    // fixed-position content never changes document.body's own offsetHeight, so the
    // ResizeObserver watching body alone would never notice a modal taller than the
    // current viewport (e.g. a long checkbox list). This proves the separate popup-watching
    // path picks it up instead.
    it("also reports a height based on an open SweetAlert2 popup, since it's excluded from document.body's own layout", async () => {
      Object.defineProperty(window, "top", { value: {}, configurable: true });
      Object.defineProperty(document.body, "offsetHeight", { value: 400, configurable: true });
      Object.defineProperty(document.documentElement, "offsetHeight", { value: 400, configurable: true });
      const postMessageSpy = vi.spyOn(window.parent, "postMessage");

      await mountApp();
      postMessageSpy.mockClear();

      const popup = document.createElement("div");
      popup.className = "swal2-popup";
      Object.defineProperty(popup, "offsetHeight", { value: 900, configurable: true });
      document.body.appendChild(popup);

      await vi.waitFor(() => {
        expect(postMessageSpy).toHaveBeenCalledWith({ subject: "lti.frameResize", height: 996 }, "*");
      });

      document.body.removeChild(popup);
    });
  });

  // authStore.sessionExpired only ever flips false -> true once per real session (see the
  // comment above the watcher in App.vue), so the watcher's `if (!expired) return;` guard is
  // never exercised by the app's actual usage. It's still real defensive code guarding real
  // behavior (don't show/re-run the expiry flow for a falsy value), so it's worth pinning down
  // directly rather than leaving it untested.
  it("does nothing when the sessionExpired watcher fires with a falsy value", async () => {
    const { apiStore } = await mountApp();

    apiStore.sessionExpired = true;
    await flushPromises();
    Swal.fire.mockClear();

    apiStore.sessionExpired = false;
    await flushPromises();

    expect(Swal.fire).not.toHaveBeenCalled();
  });

  describe("template branches (full mount - see mountAppRendered's comment above)", () => {
    it("renders the generic error state when unauthenticated and no other mode applies", async () => {
      const { wrapper } = await mountAppRendered();

      expect(wrapper.text()).toContain("Error");
    });

    it("renders router-view for an authenticated Instructor", async () => {
      const { wrapper } = await mountAppRendered({}, apiStore => {
        apiStore.ltiToken = "lti-token";
        apiStore.apiToken = "api-token";
        apiStore.userInfo = "Instructor";
      });

      expect(wrapper.find("router-view-stub").exists()).toBe(true);
    });

    it("renders StudentConsent for a Learner with pending consent", async () => {
      const { wrapper } = await mountAppRendered({}, apiStore => {
        apiStore.ltiToken = "lti-token";
        apiStore.apiToken = "api-token";
        apiStore.userInfo = "Learner";
        apiStore.consent = true;
        apiStore.experimentId = "1";
        apiStore.userId = "2";
      });

      expect(wrapper.find("student-consent-stub").exists()).toBe(true);
      expect(wrapper.find("student-quiz-stub").exists()).toBe(false);
    });

    it("renders StudentQuiz for a Learner without consent who has an assignment", async () => {
      const { wrapper } = await mountAppRendered({}, apiStore => {
        apiStore.ltiToken = "lti-token";
        apiStore.apiToken = "api-token";
        apiStore.userInfo = "Learner";
        apiStore.consent = false;
        apiStore.assignmentId = "5";
        apiStore.experimentId = "1";
      });

      expect(wrapper.find("student-quiz-stub").exists()).toBe(true);
      expect(wrapper.find("student-consent-stub").exists()).toBe(false);
    });

    it("shows IntegrationsTokenAlert once StudentQuiz has loaded and reports an alert, for a Learner without consent", async () => {
      const { wrapper } = await mountAppRendered({}, apiStore => {
        apiStore.ltiToken = "lti-token";
        apiStore.apiToken = "api-token";
        apiStore.userInfo = "Learner";
        apiStore.consent = false;
        apiStore.assignmentId = "5";
        apiStore.experimentId = "1";
      });

      expect(wrapper.find("integrations-token-alert-stub").exists()).toBe(false);

      const quiz = wrapper.findComponent({ name: "StudentQuiz" });
      quiz.vm.$emit("loaded");
      quiz.vm.$emit("integrationsTokenAlert", { message: "an alert" });
      await flushPromises();

      expect(wrapper.find("integrations-token-alert-stub").exists()).toBe(true);
    });

    it("renders Integrations for the integration entry point", async () => {
      const { wrapper } = await mountAppRendered({ integrationData: { foo: "bar" } });

      expect(wrapper.find("integrations-stub").exists()).toBe(true);
      expect(wrapper.find("integrations-preview-stub").exists()).toBe(false);
    });

    it("renders IntegrationsPreview when integrationData carries a previewUrl", async () => {
      const { wrapper } = await mountAppRendered({
        integrationData: { previewUrl: "https://example.test/preview" }
      });

      expect(wrapper.find("integrations-preview-stub").exists()).toBe(true);
      expect(wrapper.find("integrations-stub").exists()).toBe(false);
    });

    it("renders the obsolete Assignment view for obsoleteData.type === 'assignment'", async () => {
      const { wrapper } = await mountAppRendered({ obsoleteData: { type: "assignment" } });

      expect(wrapper.find("assignment-stub").exists()).toBe(true);
    });

    it("renders no Assignment component for an obsoleteData.type it doesn't recognize", async () => {
      const { wrapper } = await mountAppRendered({ obsoleteData: { type: "something-else" } });

      expect(wrapper.find("assignment-stub").exists()).toBe(false);
    });

    it("renders TreatmentPreviewComplete once the treatment preview is marked complete", async () => {
      const { wrapper } = await mountAppRendered({
        treatmentPreviewData: {
          preview: true,
          complete: true,
          experimentId: "1",
          conditionId: "1",
          treatmentId: "1",
          previewId: "1",
          ownerId: "1"
        }
      });

      expect(wrapper.find("treatment-preview-complete-stub").exists()).toBe(true);
      expect(wrapper.find("student-quiz-stub").exists()).toBe(false);
      expect(wrapper.find("page-loading-stub").exists()).toBe(false);
    });

    it("renders PageLoading and StudentQuiz while the treatment preview is not yet complete", async () => {
      const { wrapper } = await mountAppRendered({
        treatmentPreviewData: {
          preview: true,
          complete: false,
          experimentId: "1",
          conditionId: "1",
          treatmentId: "1",
          previewId: "1",
          ownerId: "1"
        }
      });

      expect(wrapper.find("student-quiz-stub").exists()).toBe(true);
      expect(wrapper.find("page-loading-stub").exists()).toBe(true);
      expect(wrapper.find("treatment-preview-complete-stub").exists()).toBe(false);
    });

    it("renders SkipTo when the retrieved configuration enables showSkipLink", async () => {
      configurationService.get.mockResolvedValueOnce({ showSkipLink: true });

      const { wrapper } = await mountAppRendered();

      expect(wrapper.find("skip-to-stub").exists()).toBe(true);
    });

    it("does not render SkipTo when the retrieved configuration doesn't enable showSkipLink", async () => {
      const { wrapper } = await mountAppRendered();

      expect(wrapper.find("skip-to-stub").exists()).toBe(false);
    });
  });
});

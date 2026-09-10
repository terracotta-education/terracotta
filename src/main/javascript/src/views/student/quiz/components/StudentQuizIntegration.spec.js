import { afterEach, describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import StudentQuizIntegration from "./StudentQuizIntegration.vue";

const assessment = { html: "<p>Assessment instructions</p>" };
const integration = { launchUrl: "https://example.com/launch" };

describe("StudentQuizIntegration", () => {
  it("renders the assessment html and an iframe pointed at the launch url when not readonly", () => {
    const wrapper = mountComponent(StudentQuizIntegration, {
      props: { assessment, integration, readonly: false, submitted: false }
    });

    expect(wrapper.html()).toContain("Assessment instructions");

    const iframe = wrapper.find("iframe#integration-iframe");
    expect(iframe.exists()).toBe(true);
    expect(iframe.attributes("src")).toBe(integration.launchUrl);
    expect(wrapper.findComponent({ name: "IntegrationFeedback" }).exists()).toBe(false);
  });

  it("marks the iframe with the no-resize class until a resize message has been received", async () => {
    const wrapper = mountComponent(StudentQuizIntegration, {
      props: { assessment, integration, hasResizeMessage: false }
    });

    expect(wrapper.find("iframe").classes()).toContain("no-resize");

    await wrapper.setProps({ hasResizeMessage: true });

    expect(wrapper.find("iframe").classes()).not.toContain("no-resize");
  });

  describe("fallback height (before a resize message arrives)", () => {
    const originalInnerHeight = window.innerHeight;

    afterEach(() => {
      vi.restoreAllMocks();
      Object.defineProperty(window, "innerHeight", { value: originalInnerHeight, configurable: true });
    });

    // real content height is unknown until the embedded tool posts a resize message
    // (which - see StudentQuizIntegration.vue's own comment - isn't guaranteed to
    // ever happen at all), so in the meantime the iframe is sized to whatever
    // viewport space is actually left below it, not a fixed guess.
    it("sizes the iframe to the remaining viewport space below it", async () => {
      const wrapper = mountComponent(StudentQuizIntegration, {
        props: { assessment, integration, hasResizeMessage: false }
      });

      Object.defineProperty(window, "innerHeight", { value: 900, configurable: true });
      vi.spyOn(wrapper.find("iframe").element, "getBoundingClientRect").mockReturnValue({ top: 200 });
      window.dispatchEvent(new Event("resize"));
      await wrapper.vm.$nextTick();

      expect(wrapper.find("iframe").attributes("style")).toContain("height: 700px");
    });

    it("clamps to a sane minimum instead of collapsing when little viewport room is left", async () => {
      const wrapper = mountComponent(StudentQuizIntegration, {
        props: { assessment, integration, hasResizeMessage: false }
      });

      Object.defineProperty(window, "innerHeight", { value: 900, configurable: true });
      vi.spyOn(wrapper.find("iframe").element, "getBoundingClientRect").mockReturnValue({ top: 850 });
      window.dispatchEvent(new Event("resize"));
      await wrapper.vm.$nextTick();

      expect(wrapper.find("iframe").attributes("style")).toContain("height: 300px");
    });

    it("clears the inline height once a resize message has arrived, leaving the embedded tool's own real height in control", async () => {
      const wrapper = mountComponent(StudentQuizIntegration, {
        props: { assessment, integration, hasResizeMessage: false }
      });

      expect(wrapper.find("iframe").attributes("style")).toContain("height");

      await wrapper.setProps({ hasResizeMessage: true });

      expect(wrapper.find("iframe").attributes("style")).toBeFalsy();
    });
  });

  it("renders the IntegrationFeedback component with the selected submission when readonly", () => {
    const selectedSubmission = { submissionId: 5, integrationFeedbackEnabled: true };

    const wrapper = mountComponent(StudentQuizIntegration, {
      props: {
        assessment,
        integration,
        readonly: true,
        selectedSubmission
      }
    });

    expect(wrapper.find("iframe#integration-iframe").exists()).toBe(false);

    const feedback = wrapper.findComponent({ name: "IntegrationFeedback" });
    expect(feedback.exists()).toBe(true);
    expect(feedback.props("submission")).toEqual(selectedSubmission);
  });

  it("shows a success alert instead of the iframe/feedback once submitted", () => {
    const wrapper = mountComponent(StudentQuizIntegration, {
      props: { assessment, integration, submitted: true }
    });

    expect(wrapper.text()).toContain("Your answers have been submitted.");
    expect(wrapper.find("iframe").exists()).toBe(false);
  });
});

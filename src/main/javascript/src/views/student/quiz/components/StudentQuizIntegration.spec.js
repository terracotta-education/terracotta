import { describe, expect, it } from "vitest";

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

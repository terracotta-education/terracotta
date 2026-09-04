import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ExternalIntegrationResponseEditor from "./ExternalIntegrationResponseEditor.vue";

describe("ExternalIntegrationResponseEditor", () => {
  it("renders an iframe pointing at the integration launch URL when feedback is enabled", () => {
    const wrapper = mountComponent(ExternalIntegrationResponseEditor, {
      props: {
        submission: {
          integrationFeedbackEnabled: true,
          integrationLaunchUrl: "https://example.com/feedback"
        }
      }
    });

    const iframe = wrapper.find("iframe");
    expect(iframe.exists()).toBe(true);
    expect(iframe.attributes("src")).toBe("https://example.com/feedback");
  });

  it("renders nothing when feedback is not enabled", () => {
    const wrapper = mountComponent(ExternalIntegrationResponseEditor, {
      props: {
        submission: {
          integrationFeedbackEnabled: false,
          integrationLaunchUrl: "https://example.com/feedback"
        }
      }
    });

    expect(wrapper.find("iframe").exists()).toBe(false);
  });

  it("treats a missing submission gracefully as feedback disabled", () => {
    const wrapper = mountComponent(ExternalIntegrationResponseEditor, {
      props: {
        submission: {}
      }
    });

    expect(wrapper.find("iframe").exists()).toBe(false);
  });
});

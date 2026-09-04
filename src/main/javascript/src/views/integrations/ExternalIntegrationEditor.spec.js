import { describe, expect, it, vi, beforeEach } from "vitest";

vi.mock("@/services", () => ({
  integrationsService: {
    validateIframeUrl: vi.fn()
  }
}));

import { integrationsService } from "@/services";
import { mountComponent } from "@/test-utils/mount";
import ExternalIntegrationEditor from "./ExternalIntegrationEditor.vue";
import { integrations as integrationsModule } from "@/store/integrations/integrations.module";
import { configuration as configurationModule } from "@/store/configuration.module";

function buildQuestion(overrides = {}) {
  return {
    id: "question-1",
    points: 5,
    integration: {
      previewUrl: "/preview",
      configuration: {
        launchUrl: "https://example.com/survey",
        client: {
          name: "Qualtrics",
          returnUrl: "https://terracotta.example.com/return"
        }
      }
    },
    ...overrides
  };
}

function buildAssessment(overrides = {}) {
  return {
    integrationIframeInfoUrl: "https://example.com/info",
    allowStudentViewResponses: false,
    ...overrides
  };
}

describe("ExternalIntegrationEditor", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    integrationsService.validateIframeUrl.mockResolvedValue(true);

    if (!navigator.clipboard) {
      Object.defineProperty(navigator, "clipboard", {
        value: { writeText: vi.fn() },
        configurable: true
      });
    }

    navigator.clipboard.writeText = vi.fn().mockResolvedValue();
  });

  it("renders Qualtrics-specific copy for a non-custom-web-activity client", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion()
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Qualtrics Integration");
    expect(wrapper.text()).toContain("Launch to Qualtrics");
    expect(wrapper.find(".v-checkbox").exists()).toBe(false);
  });

  it("renders Custom Web Activity copy and the feedback checkbox for that client", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion({
          integration: {
            previewUrl: "/preview",
            configuration: {
              launchUrl: "https://example.com/activity",
              client: {
                name: "Custom Web Activity",
                returnUrl: "https://terracotta.example.com/return"
              }
            }
          }
        })
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Custom Web Activity Integration");
    expect(
      wrapper.text()
    ).toContain("Tool allows students to view past submissions");
  });

  it("disables the preview button when there is no launch URL", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion({
          integration: {
            previewUrl: "/preview",
            configuration: {
              launchUrl: "",
              client: { name: "Qualtrics", returnUrl: "https://x.test" }
            }
          }
        })
      }
    });

    await wrapper.vm.$nextTick();

    const previewButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("PREVIEW"));

    expect(previewButton.props("disabled")).toBe(true);
  });

  it("enables the preview button and emits integration-updated once the launch URL validates", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion()
      }
    });

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(integrationsService.validateIframeUrl).toHaveBeenCalledWith(
      "https://example.com/survey"
    );

    const previewButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("PREVIEW"));

    expect(previewButton.props("disabled")).toBe(false);

    const emitted = wrapper.emitted("integration-updated");
    expect(emitted).toBeTruthy();
    expect(emitted.at(-1)[0]).toMatchObject({
      launchUrlValidated: true,
      pointsValidated: true
    });
  });

  it("shows the iframe validation error and disables the preview button when the URL fails validation", async () => {
    integrationsService.validateIframeUrl.mockResolvedValue(false);

    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion()
      }
    });

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("Error rendering content");

    const previewButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("PREVIEW"));

    expect(previewButton.props("disabled")).toBe(true);
  });

  it("re-validates the iframe URL and emits validation-in-progress events on blur", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion()
      }
    });

    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    integrationsService.validateIframeUrl.mockClear();

    await wrapper.findComponent({ name: "VTextarea" }).trigger("blur");
    await wrapper.vm.$nextTick();

    expect(integrationsService.validateIframeUrl).toHaveBeenCalled();

    const progressEvents = wrapper.emitted("url-validation-in-progress");
    expect(progressEvents).toBeTruthy();
    expect(progressEvents[0][0]).toBe(true);
    expect(progressEvents.at(-1)[0]).toBe(false);
  });

  it("copies the return URL to the clipboard and updates the button label", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion()
      }
    });

    await wrapper.vm.$nextTick();

    const copyButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("COPY URL"));

    await copyButton.trigger("click");
    await wrapper.vm.$nextTick();

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
      "https://terracotta.example.com/return"
    );
    expect(wrapper.text()).toContain("COPIED");
  });

  it("uses the configured lmsTitle when building the header copy", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion()
      }
    });

    const configurationStore = configurationModule();
    configurationStore.configurations = { lmsTitle: "Canvas" };

    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("assignment in Canvas");
  });

  it("resets iframe validity via the store when there is no launch URL on mount", async () => {
    const wrapper = mountComponent(ExternalIntegrationEditor, {
      props: {
        assessment: buildAssessment(),
        question: buildQuestion({
          integration: {
            previewUrl: "/preview",
            configuration: {
              launchUrl: "",
              client: { name: "Qualtrics", returnUrl: "https://x.test" }
            }
          }
        })
      }
    });

    await wrapper.vm.$nextTick();

    const integrationsStore = integrationsModule();
    expect(integrationsStore.isIframeUrlValid).toBe(true);
    expect(integrationsService.validateIframeUrl).not.toHaveBeenCalled();
  });
});
